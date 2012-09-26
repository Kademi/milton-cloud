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
    private final String process;
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
        this.process = process;
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
                    if (info.getWidth() > info.getHeight()) {
                        //if (prop.scaleByHeight()) {
                        System.out.println("by height, scale to fit");
                        scale += "scale=-1:" + prop.getContrainedHeight();
                    } else {
                        System.out.println("by width, scale to fit");
                        scale += "scale=" + prop.getConstrainedWidth() + ":-1";
                    }
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
            ScriptExecutor exec = new ScriptExecutor(process, args, successCode);
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

    /**
     * Eg: Given an image of these dimensions 2592h x 3888w, and lets say we
     * want that to fit into a bounding box of 52x52..
     *
     * First we determine which is the contrained dimension. In this case the
     * target ratio (width/height) is 1, and the actual ration is 0.666
     *
     * Because the actualRatio is less then targetRation we need to constrain
     * the width, ie we will set the width to 52 and allow the height to be
     * whatever maintains the actual ration = 52 x 0.666 = 34.6
     *
     * Because avconv/ffmpeg will only scale on the smaller of the dimensions,
     * we must tell it to scale height to 34.6, and allow it to adjust the width
     *
     */
    private class Proportion {

        double targetRatio;
        double actualRatio;
        double maxWidth;
        double maxHeight;
        double origHeight;
        double origWidth;

        public Proportion(double width, double height, double maxWidth, double maxHeight) {
            targetRatio = maxWidth / maxHeight;
            actualRatio = width / height;
            origHeight = height;
            origWidth = width;
            this.maxHeight = maxHeight;
            this.maxWidth = maxWidth;
        }

        public boolean scaleByHeight() {
            return actualRatio > targetRatio;
        }

        public boolean scaleByWidth() {
            return !scaleByHeight();
        }

        public double getConstrainedWidth() {
            if (scaleByHeight()) {
                return maxWidth;
            } else {
                return actualRatio * maxHeight;
            }
        }

        public double getContrainedHeight() {
            if (scaleByWidth()) {
                return maxHeight;
            } else {
                return maxWidth / actualRatio;
            }
        }

        @Override
        public String toString() {
            return "orig " + origHeight + "h x" + origWidth + "w -scaleByHeight=" + scaleByHeight() + " -->> " + getContrainedHeight() + "h x " + getConstrainedWidth() + "w";
        }
    }
}
