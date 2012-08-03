/*
 * Copyright 2012 McEvoy Software Ltd.
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
import io.milton.cloud.server.web.FileResource;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.Combiner;
import org.hashsplit4j.api.Fanout;
import org.hashsplit4j.api.HashStore;

/**
 *
 * @author brad
 */
public class MediaInfoService {
    
    private final HashStore hashStore;
    private final BlobStore blobStore;

    public MediaInfoService(HashStore hashStore, BlobStore blobStore) {
        this.hashStore = hashStore;
        this.blobStore = blobStore;
    }
        
    
    public MediaInfo getInfo(FileResource fileResource) throws IOException {
        String ext = FileUtils.getExtension(fileResource.getName());
        File file = createSourceFile(fileResource.getHash(), ext);
        System.out.println("run media info runner...");
        MediaInfoRunner runner = new MediaInfoRunner();
        return runner.getInfo(file);
    }
    
    private File createSourceFile(String fileHash, String ext) {
        File temp = null;
        FileOutputStream out = null;
        BufferedOutputStream out2 = null;
        try {                
            String name = "mediainfo_in_" + System.currentTimeMillis();
            temp = File.createTempFile(name, "." + ext);
            System.out.println("write to : " + temp.getCanonicalPath());
            Fanout fanout = hashStore.getFileFanout(fileHash);
            out = new FileOutputStream(temp);
            out2 = new BufferedOutputStream(out);
            Combiner combiner = new Combiner();
            System.out.println("combine");
            combiner.combine(fanout.getHashes(), hashStore, blobStore, out);
            out2.flush();
            out.flush();
            System.out.println("finished writing file");
        } catch (IOException ex) {
            throw new RuntimeException("Writing to: " + temp.getAbsolutePath(), ex);
        } finally {
            FileUtils.close(out2);
            FileUtils.close(out);
        }
        return temp;
    }
    
}
