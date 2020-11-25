/*
 * Copyright (C) McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.sync.triplets;

import org.hashsplit4j.store.berkeleyDbEnv.BerkeleyDbEnv;
import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class BerkeleyDbFileHashCache implements SyncHashCache{

    private final Logger log = LoggerFactory.getLogger(BerkeleyDbFileHashCache.class);


    private final ScheduledExecutorService scheduler = Executors
            .newScheduledThreadPool(1);

    private Date lastCommit = new Date();
    private Boolean doCommit = false;
    private int commitCount = 0;
    private final EntityStore store;
    private PrimaryIndex<String, FileHashEntity> hashByKeyIndex;

    private final ScheduledFuture<?> taskHandle;

    /**
     * Encapsulates the environment and data store
     */
    private final BerkeleyDbEnv dbEnv = new BerkeleyDbEnv();

    public BerkeleyDbFileHashCache(File envHome) {
        this.taskHandle = scheduler.scheduleAtFixedRate(() -> {
            try {
                writeToDisk();
            } catch (Exception ex) {
                log.warn("Error writing db changes to disk", ex);
            }
        }, 0, 5, TimeUnit.SECONDS);

        dbEnv.openEnv(envHome, false);
        store = dbEnv.getEntityStore();

        hashByKeyIndex = store.getPrimaryIndex(String.class, FileHashEntity.class);
    }

    @Override
    public void put(File file, String hash) {
        String key = getKey(file);
        FileHashEntity fileHash = new FileHashEntity(key, hash);
        hashByKeyIndex.put(fileHash);

        lastCommit = new Date();
        doCommit = true;
        commitCount++;
    }

    @Override
    public String get(File file) {
        String key = getKey(file);
        FileHashEntity fileHash = hashByKeyIndex.get(key);
        if( fileHash == null ) {
            return null;
        }
        return fileHash.hash;
    }

    private String getKey(File file) {
        String key = file.getAbsolutePath() + "-" + file.length() + "-" + file.lastModified();
        return key;
    }

    /**
     * Close the database environment and database store transaction
     */
    public void closeEnv() {
        dbEnv.closeEnv();
    }

    /**
     * Remove all of files for the given file directory
     *
     * @param envHome
     */
    public void removeDbFiles(File envHome) {
        dbEnv.removeDbFiles(envHome);
    }


    private void writeToDisk() {
        Date now = new Date();
        if ((lastCommit == null || (now.getTime() - lastCommit.getTime()) > 5000) && doCommit) {
            this.dbEnv.getEnv().sync();
            doCommit = false;
            commitCount = 0;
        }
    }

    @Entity
    public static class FileHashEntity {
        @PrimaryKey
        private String key;
        private String hash;

        public FileHashEntity() {
        }

        public FileHashEntity(String key, String hash) {
            this.key = key;
            this.hash = hash;
        }



        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getHash() {
            return hash;
        }

        public void setHash(String hash) {
            this.hash = hash;
        }

    }
}
