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
import io.milton.cloud.server.web.alt.FormatSpec.SeekUnit;
import io.milton.cloud.util.ScriptExecutor;
import io.milton.common.Utils;
import io.milton.http.DateUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This uses the ffmpegthumbnailer program
 * 
 *     // sudo apt-get install ffmpegthumbnailer
    // ffmpegthumbnailer -i dodgy-convert.mp4 -o out.png

 *
 * @author brad
 */
public class FTNThumbGenerator implements ThumbGenerator{

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(FTNThumbGenerator.class);
    
    private final String imageConverterProcess = "ffmpegthumbnailer"; // ImageMagick

    
    
    @Override
    public String generateImage(FormatSpec format, File source, File dest, With<InputStream, String> with) throws Exception {
        try {
            List<String> args = new ArrayList<>();
            args.add("-s");
            args.add(format.getWidth()+""); // will scale to fit by default. We ignore height because always same (At the moment!)
            if( format.getSeekAmount() != null && format.getSeekUnit() != null ) {
                args.add("-t");
                args.add( formatSeek(format) );
            }
            args.add("-i");
            // set the input file
            args.add(source.getAbsolutePath());
            args.add("-o");
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

    private String formatSeek(FormatSpec format) {
        if( format.getSeekUnit().equals(SeekUnit.PERC)) {
            return format.getSeekAmount() + "%";
        } else {
            int secs = format.getSeekAmount() % 60;
            int mins = format.getSeekAmount() % (60*60);
            int hours = format.getSeekAmount()/(60*60);
            return DateUtils.pad2(hours) + ":" + DateUtils.pad2(mins) + ":" + DateUtils.pad2(secs);
        }
    }
    
}
