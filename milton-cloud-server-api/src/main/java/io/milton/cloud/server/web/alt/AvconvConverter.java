package io.milton.cloud.server.web.alt;

import com.bradmcevoy.utils.FileUtils;
import io.milton.cloud.common.With;
import io.milton.cloud.server.manager.CurrentRootFolderService;
import io.milton.cloud.util.ScriptExecutor;
import io.milton.common.ContentTypeService;
import io.milton.context.RootContext;
import io.milton.vfs.db.utils.SessionManager;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.Combiner;
import org.hashsplit4j.api.Fanout;
import org.hashsplit4j.api.HashStore;

public class AvconvConverter implements Closeable {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AvconvConverter.class);
    public static final int HLS_VERSION = 3;
    private static final File TEMP_DIR = new File(System.getProperty("java.io.tmpdir"));
    public static HlsSpec[] HLS_DIMENSIONS = {new HlsSpec(360, 480, 512, 64, 12), new HlsSpec(720, 1024, 1024, 128, 24)};
    private final ContentTypeService contentTypeService;
    private final RootContext rootContext;
    private final SessionManager sessionManager;
    private final String videoConverterProcess;
    private final String imageConverterProcess = "convert"; // ImageMagick
    private final String primaryMediaHash;
    private final FormatSpec format;
    private final String inputName;
    private final String inputExt;
    private final HashStore hashStore;
    private final BlobStore blobStore;
    private final MediaInfoService mediaInfoService;
    private final CurrentRootFolderService currentRootFolderService;
    private File source;
    private final File dest;
    private File generatedOutput;
    private long sourceLength;

    public AvconvConverter(String process, String primaryMediaHash, String inputName, FormatSpec format, String inputFormat, ContentTypeService contentTypeService, HashStore hashStore, BlobStore blobStore, MediaInfoService mediaInfoService, CurrentRootFolderService currentRootFolderService, RootContext rootContext, SessionManager sessionManager) {
        this.rootContext = rootContext;
        this.sessionManager = sessionManager;
        this.hashStore = hashStore;
        this.blobStore = blobStore;
        this.currentRootFolderService = currentRootFolderService;
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

    public File getGeneratedOutputFile() {
        return generatedOutput;
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
            generatedOutput = dest;
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

    public void generateHlsVideo(HlsGeneratorListener generatorListener, int segmentDurationSecs) throws Exception {
        try {
            source = createSourceFile();
            List<SegmentWriterListener> tailListeners = new ArrayList<>(); // listeners for writing to segments files
            List<String> args = new ArrayList<>();
            args.add("-i");
            args.add(source.getAbsolutePath());

            args.add("-strict");
            args.add("experimental");

            // determine original dimensions, then choose x or y axis to scale on so that 
            // the resulting image or video is bound by the format dimensions            
            MediaInfo info = mediaInfoService.getInfo(source);

            long primaryId = generatorListener.onNewPrimary(segmentDurationSecs, HLS_VERSION);

            for (HlsSpec dimension : HLS_DIMENSIONS) {
                String scale;
                if (info == null) {
                    log.warn("Failed to find dimensions of source file, might not be able to generate thumb");
                    scale = "scale=" + dimension.getWidth() + ":-1"; // only scale on width            
                } else {
                    Proportion prop = new Proportion(info.getWidth(), info.getHeight(), dimension.getWidth(), dimension.getHeight());

                    if (info.getWidth() > info.getHeight()) {
                        //if (prop.scaleByHeight()) {
                        System.out.println("by height, crop");
                        scale = "scale=-1:" + prop.maxHeight;
                    } else {
                        System.out.println("by width, crop");
                        scale = "scale=" + prop.maxWidth + ":-1";
                    }
                    scale += ",crop=" + prop.maxWidth + ":" + prop.maxHeight;
                }

                // set the input file
                if (!source.exists() || !source.isFile()) {
                    throw new RuntimeException("Source file does not exist or is not a file: " + source.getAbsolutePath());
                }

                args.add("-vf");
                args.add(scale);

                args.add("-b:v");
                args.add( dimension.getTargetVideoBandwidthK() + "k");
                
                args.add("-maxrate");
                args.add( dimension.getTargetVideoBandwidthK() + "k");

                args.add("-bufsize"); // from namrekka - http://www.longtailvideo.com/support/forums/jw-player/video-encoding/33049/hls-skipping-forward-plays-audio-but-not-video
                args.add( dimension.getTargetVideoBandwidthK() + "k");
                
                args.add("-b:a");
                args.add( dimension.getTargetAudioBandwidthK() + "k");

                args.add("-r"); // target framerate
                args.add("" + dimension.getFrameRate());                                

                args.add("-keyint_min");
                args.add("" + dimension.getFrameRate());
                
                args.add("-refs");
                args.add("1");
                
                args.add("-trellis");
                args.add("0");                                
                
                args.add("-g"); // one key frame every second
                args.add("" + dimension.getFrameRate());                
                //-crf 25 -profile:v baseline 
                //-g 48 -sc_threshold 0 
                //-flags +cgop -c:a aac -strict -2 -b:a 112k -map 0:v:0 -map 0:a:0 -f ssegment -metadata "service_provider=Some Provider" -metadata "service_name=Some Channel Name" -segment_time 10 -segment_format mpegts "hls_%02d.ts"
                
                // disables CABAC, but not sure how important it is
//                args.add("-coder");
//                args.add("0");
                                
                args.add("-profile:v");
                args.add("baseline");
                                
                args.add("-level");
                args.add("30");

                
                args.add("-segment_time");
                args.add(segmentDurationSecs + "");

                // "flags +cgop" ... ?
                String[] hlsArgs = {"-c:v", "libx264", "-c:a", "libvo_aacenc", "-map", "0:0", "-map", "0:1", "-bsf", "h264_mp4toannexb", "-flags", "+cgop-global_header", "-f", "segment", "-segment_list_size", "99999", "-segment_format", "mpegts"};
                for (String s : hlsArgs) {
                    args.add(s);
                }

                File rawSegmentFile = new File(dest, "raw" + dimension + ".m3u8"); // generated by avconv, but dosnt have HLS tags
                File finalSegmentFile = new File(dest, "segments" + dimension + ".m3u8"); // we'll add segments from rawSegmentFile and add special tags            
                File segmentsPatternFile = new File(dest, "seg" + dimension + "-%03d.ts"); // not a real file
                dest.mkdirs();

                args.add("-segment_list");
                args.add(rawSegmentFile.getAbsolutePath());
                args.add(segmentsPatternFile.getAbsolutePath());
                SegmentWriterListener listener = new SegmentWriterListener(primaryId, generatorListener, segmentDurationSecs);
                Tailer tailer = Tailer.create(rawSegmentFile, listener, 25);
                listener.setTailer(tailer);
                listener.setDimension(dimension);
                tailListeners.add(listener);
            }

            int successCode = 0;
            ScriptExecutor exec = new ScriptExecutor(videoConverterProcess, args, successCode);
            try {
                exec.exec();
            } catch (Exception ex) {
                throw new Exception("Failed to generate alternate format: " + format, ex);
            } finally {
                log.info("Executor finished, just wait a bit for tailing to finish...");
                doSleep(200); // ensure tailer has time to finish scanning file
                log.info("ok, thats enough");
                for (SegmentWriterListener listener : tailListeners) {
                    listener.getTailer().stop();
                }
            }



            log.debug(" ffmpeg ran ok.");
            generatorListener.onComplete(primaryId);
        } finally {
//            if (dest.exists()) {
//                dest.delete();
//            }
        }
    }

    private String generateFromVideo(With<InputStream, String> with) throws Exception {
        try {
            generatedOutput = dest;
            log.info("generateFromVideo: generatedOutput=" + generatedOutput.getAbsolutePath());
            List<String> args = new ArrayList<>();
            args.add("-i");
            args.add(source.getAbsolutePath());

            args.add("-strict");
            args.add("experimental");


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
            if (!source.exists() || !source.isFile()) {
                throw new RuntimeException("Source file does not exist or is not a file: " + source.getAbsolutePath());
            }
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

    private void doSleep(long sleepMillis) {
        try {
            Thread.sleep(sleepMillis);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    public class SegmentWriterListener extends TailerListenerAdapter {

        private final HlsGeneratorListener generatorListener;
        private final int targetSegmentDuration;
        private final long primaryId;
        private String previousLine;
        private Tailer tailer;
        private Dimension dimension;
        private Long programId;
        private int sequence;

        public SegmentWriterListener(long primaryId, HlsGeneratorListener generatorListener, int targetSegmentDuration) throws FileNotFoundException {
            this.primaryId = primaryId;
            this.generatorListener = generatorListener;
            this.targetSegmentDuration = targetSegmentDuration;
        }

        @Override
        public void handle(final String line) {
            try {
                System.out.println("line: " + line + " rootContext=" + rootContext);
                if (previousLine != null) {
                    System.out.println("  process previous: " + previousLine);
                    handleLine(previousLine);
                } else {
                    System.out.println("  thats first line");
                }
                previousLine = line;
            } catch (Throwable e) {
                log.error("ex", e);
            }
        }

        private void handleLine(String line) {

            try {
                // The line is the path to the written segment. We need to 
                //  1. find its exact duration and write the EXTINF tag
                //  2. load the bytes into the blob store and find its hash
                //  3. write a url to the segmentsOut file with the url to the blob
                File newSegment = new File(line);
                if (!newSegment.exists()) {
                    throw new RuntimeException("Couldnt find segment file: " + line);
                }

                MediaInfo info = null;
                try {
                    info = mediaInfoService.getInfo(newSegment);
                } catch (IOException e) {
                    throw new RuntimeException("Couldnt get media info for file: " + newSegment.getAbsolutePath(), e);
                }
                Double secs = info.getDurationSecs(); // todo: should be floating point
                if (secs == null) {
                    log.error("Couldnt get duration for segment: " + newSegment.getAbsolutePath());
                    secs = (double) targetSegmentDuration;
                }

                if (programId == null) {
                    Integer likelyBandwith = null;
                    if (secs > 0) {
                        likelyBandwith = (int) (newSegment.length()*8 / secs);
                        likelyBandwith = (int) (likelyBandwith * 1.2); // this is meant to be an upper bound, so build in a bit of room to allow for peaks
                    }
                    programId = generatorListener.onNewProgram(primaryId, dimension, likelyBandwith);
                }

                FileInputStream newSegmentIn = null;
                try {
                    newSegmentIn = new FileInputStream(newSegment);
                    generatorListener.onNewSegment(programId, sequence++, newSegmentIn, secs);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    IOUtils.closeQuietly(newSegmentIn);
                }
            } catch (Throwable e) {
                log.error("Exception writing line", e);
            }
        }

        public Tailer getTailer() {
            return tailer;
        }

        public void setTailer(Tailer tailer) {
            this.tailer = tailer;
        }

        private void setDimension(Dimension dimension) {
            this.dimension = dimension;
        }
    }
}
