package io.milton.cloud.server.web.alt;

import com.bradmcevoy.utils.FileUtils;
import com.ettrema.logging.LogUtils;
import io.milton.cloud.common.With;
import io.milton.cloud.util.ScriptExecutor;
import io.milton.common.ContentTypeService;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.Combiner;
import org.hashsplit4j.api.Fanout;
import org.hashsplit4j.api.HashStore;

public class AvconvConverter implements Closeable {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AvconvConverter.class);
    private static final File TEMP_DIR = new File(System.getProperty("java.io.tmpdir"));
    private final ContentTypeService contentTypeService;
    private final String videoConverterProcess;
    private final String imageConverterProcess = "convert"; // ImageMagick
    private final String primaryMediaHash;
    private final FormatSpec format;
    private final String inputName;
    private final String inputExt;
    private final HashStore hashStore;
    private final BlobStore blobStore;
    private final MediaInfoService mediaInfoService;
    private File source;
    private final File dest;
    private long sourceLength;

    public AvconvConverter(String process, String primaryMediaHash, String inputName, FormatSpec format, String inputFormat, ContentTypeService contentTypeService, HashStore hashStore, BlobStore blobStore, MediaInfoService mediaInfoService) {
        this.hashStore = hashStore;
        this.blobStore = blobStore;
        this.videoConverterProcess = process;
        this.mediaInfoService = mediaInfoService;
        this.format = format;
        if (format == null) {
            throw new RuntimeException("Format cannot be null");
        }
        dest = new File(TEMP_DIR, "convert_" + System.currentTimeMillis() + "." + format.getOutputType());
        this.contentTypeService = contentTypeService;
        this.primaryMediaHash = primaryMediaHash;
        this.inputName = inputFormat;
        this.inputExt = FileUtils.getExtension(inputName);
    }

    @Override
    public void close() {
        if (source.exists()) {
            boolean deleteOk = source.delete();
            if (!deleteOk) {
                log.warn("failed to delete: " + source.getAbsolutePath());
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("deleted: " + source.getAbsolutePath());
                }
            }
        }
    }

    public File getDest() {
        return dest;
    }

    /**
     * Returns whatever the with callback returns, usually hash or content
     * length
     *
     * @param format
     * @param with
     * @param out - if not null the generated file will be streamed out as its
     * generated
     * @return - null indicates no file was generated. Oterhwise returns
     * whatever the callback returned, generally the hash of the new file
     */
    public String generate(With<InputStream, String> with) throws Exception {
        log.info("generateThumb: " + format + " to " + dest.getAbsolutePath());
        source = createSourceFile();
        if (format.inputType.equals("video")) {
            return generateFromVideo(with);
        } else {
            return generateFromImage(with);
        }
    }

    private String generateFromImage(With<InputStream, String> with) throws Exception {
        try {
            List<String> args = new ArrayList<>();
            if (source.length() > 1000000) {
                args.add("-thumbnail");
            } else {
                args.add("-resize");
            }

            // determine original dimensions, then choose x or y axis to scale on so that 
            // the resulting image or video is bound by the format dimensions            
            MediaInfo info = mediaInfoService.getInfo(source);
            String scale;
            if (info == null) {
                log.warn("Failed to find dimensions of source file, might not be able to generate thumb");
                scale = format.getWidth() + "";
            } else {
                Proportion prop = new Proportion(info.getWidth(), info.getHeight(), format.getWidth(), format.getHeight());
                if (info.getWidth() > info.getHeight()) {
                    scale = "x" + prop.getContrainedHeight();
                } else {
                    scale = prop.getConstrainedWidth() + "";
                }
            }
            args.add(scale);            

            // set the input file
            args.add(source.getAbsolutePath());

            // set output file - just a temp file
            args.add(dest.getAbsolutePath());

            int successCode = 0;
            ScriptExecutor exec = new ScriptExecutor(imageConverterProcess, args, successCode);
            try {
                exec.exec();
            } catch (Exception ex) {
                throw new Exception("Failed to generate alternate format: " + format, ex);
            }

            if (!dest.exists()) {
                throw new Exception("Conversion failed. Dest temp file was not created. Format: " + format);
            }
            if (dest.length() == 0) {
                throw new Exception("Conversion failed. Dest temp file has size zero. format: " + format);
            }

            if (sourceLength > 0) {
                long percent = dest.length() * 100 / sourceLength;
                log.info("Compression: " + percent + "% of source file: " + sourceLength / 1000000 + "Mb");
            }

            log.debug(" ffmpeg ran ok. reading temp file back to out stream");
            FileInputStream tempIn = null;
            try {
                tempIn = new FileInputStream(dest);
                return with.use(tempIn);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            } finally {
                FileUtils.close(tempIn);
            }
        } finally {
//            if (dest.exists()) {
//                dest.delete();
//            }
        }
    }

    private String generateFromVideo(With<InputStream, String> with) throws Exception {
        try {
            List<String> args = new ArrayList<>();

            // determine original dimensions, then choose x or y axis to scale on so that 
            // the resulting image or video is bound by the format dimensions            
            MediaInfo info = mediaInfoService.getInfo(source);
            String scale;
            if (info == null) {
                log.warn("Failed to find dimensions of source file, might not be able to generate thumb");
                scale = "scale=" + format.getWidth() + ":-1"; // only scale on width            
            } else {
                if (format.getWidth() < 100 || format.getHeight() < 100 && (info.getHeight() > 2000 || info.getWidth() > 2000)) {
                    scale = "scale=-1:500,";  // bring down to a reasonable size, avconv fails if trying to downsize by too much (eg 3000 to 40 seems to fail)
                } else {
                    scale = "";
                }

                Proportion prop = new Proportion(info.getWidth(), info.getHeight(), format.getWidth(), format.getHeight());
                System.out.println("prop: " + prop);
                if (format.cropToFit) {
                    if (info.getWidth() > info.getHeight()) {
                        //if (prop.scaleByHeight()) {
                        System.out.println("by height, crop");
                        scale += "scale=-1:" + prop.maxHeight;
                    } else {
                        System.out.println("by width, crop");
                        scale += "scale=" + prop.maxWidth + ":-1";
                    }
                    scale += ",crop=" + prop.maxWidth + ":" + prop.maxHeight;
                } else {
                    scale += "scale=" + prop.getConstrainedWidth() + ":" + prop.getContrainedHeight();
//                    if (info.getWidth() > info.getHeight()) {
//                        //if (prop.scaleByHeight()) {
//                        System.out.println("by height, scale to fit");
//                        scale += "scale=" + prop.getMaxWidth() + ":" + prop.getContrainedHeight();
//                    } else {
//                        System.out.println("by width, scale to fit");
//                        scale += "scale=" + prop.getConstrainedWidth() + ":" + prop.getMaxHeight();
//                    }
                }
            }

            // set the input file
            args.add("-i");
            args.add(source.getAbsolutePath());

            args.add("-strict");
            args.add("experimental");
            args.add("-vf");
            args.add(scale);

            for (String s : format.getConverterArgs()) {
                args.add(s);
            }

            args.add(dest.getAbsolutePath());

            int successCode = 0;
            ScriptExecutor exec = new ScriptExecutor(videoConverterProcess, args, successCode);
            try {
                exec.exec();
            } catch (Exception ex) {
                throw new Exception("Failed to generate alternate format: " + format, ex);
            }

            if (!dest.exists()) {
                throw new Exception("Conversion failed. Dest temp file was not created. Format: " + format);
            }
            if (dest.length() == 0) {
                throw new Exception("Conversion failed. Dest temp file has size zero. format: " + format);
            }

            if (sourceLength > 0) {
                long percent = dest.length() * 100 / sourceLength;
                log.info("Compression: " + percent + "% of source file: " + sourceLength / 1000000 + "Mb");
            }

            log.debug(" ffmpeg ran ok. reading temp file back to out stream");
            FileInputStream tempIn = null;
            try {
                tempIn = new FileInputStream(dest);
                return with.use(tempIn);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            } finally {
                FileUtils.close(tempIn);
            }
        } finally {
//            if (dest.exists()) {
//                dest.delete();
//            }
        }
    }

    private File createSourceFile() {
        File temp = null;
        FileOutputStream out = null;
        BufferedOutputStream out2 = null;
        try {
            temp = File.createTempFile("convert_vid_in_" + System.currentTimeMillis(), "." + inputExt);
            if (temp.exists()) {
                temp = File.createTempFile("convert_vid_in_" + inputName, "." + inputExt);
            }
            Fanout fanout = hashStore.getFileFanout(primaryMediaHash);
            out = new FileOutputStream(temp);
            out2 = new BufferedOutputStream(out);
            Combiner combiner = new Combiner();
            combiner.combine(fanout.getHashes(), hashStore, blobStore, out);
            out2.flush();
            out.flush();
            sourceLength = temp.length();
        } catch (IOException ex) {
            throw new RuntimeException("Writing to: " + temp.getAbsolutePath(), ex);
        } finally {
            FileUtils.close(out2);
            FileUtils.close(out);
        }
        return temp;
    }

    public long getSourceLength() {
        if (source == null) {
            return 0;
        } else {
            return source.length();
        }
    }

    private boolean isVideoOutput(String outputType) {
        List<String> list = contentTypeService.findContentTypes("x." + outputType);
        if (list != null) {
            for (String ct : list) {
                if (ct.contains("video")) {
                    return true;
                }
            }
        }
        return false;
    }
}
