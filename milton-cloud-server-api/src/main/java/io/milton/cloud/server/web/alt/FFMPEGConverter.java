package io.milton.cloud.server.web.alt;

import com.bradmcevoy.common.ScriptExecutor;
import com.bradmcevoy.io.ReadingException;
import com.bradmcevoy.io.StreamToStream;
import com.bradmcevoy.io.WritingException;
import com.bradmcevoy.utils.FileUtils;
import io.milton.cloud.common.With;
import io.milton.cloud.server.web.FileResource;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FFMPEGConverter implements Closeable {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(FFMPEGConverter.class);
    private static final File TEMP_DIR = new File(System.getProperty("java.io.tmpdir"));
    private final String process;
    private final File source;

    public FFMPEGConverter(String process, FileResource fr, String inputFormat) {
        this.process = process;
        source = createSourceFile(fr, inputFormat);
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

    /**
     * Returns whatever the with callback returns, usually hash or content length
     * 
     * @param format
     * @param with
     * @return 
     */
    public Long generate(FormatSpec format, With<InputStream,Long> with) {
        log.info("generateThumb: " + format);
        File dest = getDestFile(format.getOutputType());
        try {
            String dimensions = format.getWidth() + "x" + format.getHeight();
            String[] args;
            if( isVideoOutput(format.getOutputType())) {
                args = new String[]{"-i", source.getAbsolutePath(), "-s", dimensions, "-ar", "22050", dest.getAbsolutePath()};
            } else {
                args = new String[] {"-i", source.getAbsolutePath(), "-s", dimensions, "-ss", "1", "-vframes", "1", "-f", "mjpeg", dest.getAbsolutePath()};            
            }
            int successCode = 0;
            ScriptExecutor exec = new ScriptExecutor(process, args, successCode);
            exec.exec();

            if (!dest.exists()) {
                throw new RuntimeException("Conversion failed. Dest temp file was not created");
            }
            if (dest.length() == 0) {
                throw new RuntimeException("Conversion failed. Dest temp file has size zero.");
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
            if (dest.exists()) {
                dest.delete();
            }
        }
    }


    private Long convert(OutputStream out, String outputFormat, int height, int width) {
        log.debug("convert");
        File dest = getDestFile(outputFormat);
        log.debug(" converting: " + source.getAbsolutePath() + "(" + source.length() + ") to: " + dest.getAbsolutePath());
        String[] args;

        String dimensions = width + "x" + height;
        args = new String[]{"-i", source.getAbsolutePath(), "-s", dimensions, "-ar", "22050", dest.getAbsolutePath()};
        int successCode = 0;
        ScriptExecutor exec = new ScriptExecutor(process, args, successCode);
        exec.exec();

        if (!dest.exists()) {
            throw new RuntimeException("Conversion failed. Dest temp file was not created");
        }
        if (dest.length() == 0) {
            throw new RuntimeException("Conversion failed. Dest temp file has size zero.");
        }

        log.debug(" ffmpeg ran ok. reading temp file back to out stream");
        FileInputStream tempIn = null;
        try {
            tempIn = new FileInputStream(dest);
            return StreamToStream.readTo(tempIn, out);
        } catch (ReadingException | WritingException | FileNotFoundException ex) {
            throw new RuntimeException(ex);
        } finally {
            FileUtils.close(tempIn);
            if (dest.exists()) {
                dest.delete();
            }
        }
    }

    private File createSourceFile(FileResource fr, String suffix) {
        File temp = null;
        FileOutputStream out = null;
        BufferedOutputStream out2 = null;
        try {
            temp = File.createTempFile("convert_vid_in_" + System.currentTimeMillis(), "." + suffix);
            if (temp.exists()) {
                temp = File.createTempFile("convert_vid_in_" + fr.getName(), "." + suffix);
            }
            out = new FileOutputStream(temp);
            out2 = new BufferedOutputStream(out);
            fr.sendContent(out2, null, null, null);
            out2.flush();
            out.flush();
        } catch (IOException ex) {
            throw new RuntimeException("Writing to: " + temp.getAbsolutePath(), ex);
        } finally {
            FileUtils.close(out2);
            FileUtils.close(out);
        }
        return temp;
    }

    private static File getDestFile(String suffix) {
        return createTempFile("convert_" + System.currentTimeMillis(), "." + suffix);
    }

    private static File createTempFile(String prefix, String suffix) {
        return new File(TEMP_DIR, prefix + suffix);
    }

    public long getSourceLength() {
        if (source == null) {
            return 0;
        } else {
            return source.length();
        }
    }

    private boolean isVideoOutput(String outputType) {
        return outputType.equals("flv") || outputType.equals("m4v") || outputType.equals("ogv");
    }
}
