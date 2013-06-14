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

import io.milton.cloud.common.With;
import io.milton.cloud.server.manager.CurrentRootFolderService;
import io.milton.cloud.server.web.RootFolder;
import io.milton.common.ContentTypeService;
import io.milton.common.DefaultContentTypeService;
import java.io.IOException;
import java.io.InputStream;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.HashStore;
import org.hashsplit4j.api.MemoryBlobStore;
import org.hashsplit4j.api.MemoryHashStore;
import org.hashsplit4j.api.Parser;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author brad
 */
public class HLSTest {

    private String ffmpeg = "avconv";
    private String primaryFileHash;
    private String primaryFileName = "in.mp4";
    private FormatSpec formatSpec = new FormatSpec("video", "m3u8", 640, 360, false, "-c:v", "libx264", "-c:a", "libvo_aacenc", "-map", "0", "-bsf", "h264_mp4toannexb", "-flags", "-global_header", "-f", "segment", "-segment_time", "3", "-segment_list", "cap.m3u8", "-segment_list_size", "99999", "-segment_format", "mpegts");
    private String ext = "m3u8";
    private ContentTypeService contentTypeService = new DefaultContentTypeService();
    private MemoryHashStore hashStore = new MemoryHashStore();
    private BlobStore blobStore = new MemoryBlobStore();
    private MediaInfoService mediaInfoService = new MediaInfoService(hashStore, blobStore);
    private CurrentRootFolderService currentRootFolderService = new CurrentRootFolderService() {

        @Override
        public RootFolder getRootFolder() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public RootFolder peekRootFolder() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public RootFolder getRootFolder(String host) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public String getPrimaryDomain() {
            return "localhost:8080";
        }
    };

    @Test
    public void testSomeMethod() throws IOException, Exception {
        InputStream in = this.getClass().getResourceAsStream(primaryFileName);
        assertNotNull(in);
        Parser parser = new Parser();
        primaryFileHash = parser.parse(in, hashStore, blobStore);
        primaryFileName = "in.mp4";
        AvconvConverter converter = new AvconvConverter(ffmpeg, primaryFileHash, primaryFileName, formatSpec, ext, contentTypeService, hashStore, blobStore, mediaInfoService, null);
//        converter.generate(new With<InputStream, String>() {
//
//            @Override
//            public String use(InputStream t) throws Exception {
//                System.out.println("use input stream..");
//                return null;
//                
//            }
//        });
    }
    
    
}