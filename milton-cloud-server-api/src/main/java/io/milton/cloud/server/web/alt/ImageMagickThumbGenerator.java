/*
 * Copyright 2013 McEvoy Software Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.web.alt;

import com.bradmcevoy.utils.FileUtils;
import io.milton.cloud.common.With;
import io.milton.cloud.util.ScriptExecutor;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author brad
 */
public class ImageMagickThumbGenerator implements ThumbGenerator{

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ImageMagickThumbGenerator.class);
    
    private final String imageConverterProcess = "convert"; // ImageMagick
    private final MediaInfoService mediaInfoService;

    public ImageMagickThumbGenerator(MediaInfoService mediaInfoService) {
        this.mediaInfoService = mediaInfoService;
    }
        
    
    @Override
    public String generateImage(FormatSpec format, File source, File dest, With<InputStream, String> with) throws Exception {
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

            log.debug("thumb gen done. reading temp file back to out stream");
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
    
}
