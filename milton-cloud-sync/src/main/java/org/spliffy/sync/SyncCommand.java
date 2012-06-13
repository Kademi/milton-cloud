/*
 * Copyright (C) 2012 McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.spliffy.sync;

import io.milton.event.EventManager;
import io.milton.event.EventManagerImpl;
import io.milton.httpclient.Host;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * Allows SpliffySync to be run from the command line
 *
 * @author brad
 */
public class SyncCommand {

    /**
     * Run once over a single local directory:
     *         sDbFile
        sLocalDir
        sRemoteAddress
        user
        pwd
     * 
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
        String sDbFile = args[0];
        String sLocalDir = args[1];
        String sRemoteAddress = args[2];
        String user = args[3];
        String pwd = args[4];
        runOnce(sDbFile, sLocalDir, sRemoteAddress, user, pwd);
    }

    public static void runOnce(String sDbFile, String sLocalDir, String sRemoteAddress, String user, String pwd) throws Exception {
        File dbFile = new File(sDbFile);
        File localDir = new File(sLocalDir);
        SyncJob job = new SyncJob(localDir, sRemoteAddress, user, pwd, false);
        start(dbFile, Arrays.asList(job));
    }
    
    public static void monitor(String sDbFile, String sLocalDir, String sRemoteAddress, String user, String pwd) throws Exception {
        File dbFile = new File(sDbFile);
        File localDir = new File(sLocalDir);
        SyncJob job = new SyncJob(localDir, sRemoteAddress, user, pwd, true);
        start(dbFile, Arrays.asList(job));
    }

    public static void start(File dbFile, List<SyncJob> jobs) throws Exception {        
        System.out.println("Using database: " + dbFile.getAbsolutePath());

        DbInitialiser dbInit = new DbInitialiser(dbFile);

        JdbcHashCache fanoutsHashCache = new JdbcHashCache(dbInit.getUseConnection(), dbInit.getDialect(), "h");
        JdbcHashCache blobsHashCache = new JdbcHashCache(dbInit.getUseConnection(), dbInit.getDialect(), "b");

        for (SyncJob job : jobs) {
            File localRootDir = job.getLocalDir();
            URL url = new URL(job.getRemoteAddress());

            Host client = new Host(url.getHost(), url.getPort(), job.getUser(), job.getPwd(), null);
            boolean secure = url.getProtocol().equals("https");
            client.setSecure(secure);


            System.out.println("Sync: " + localRootDir.getAbsolutePath() + " - " + job.getRemoteAddress());

            HttpHashStore httpHashStore = new HttpHashStore(client, fanoutsHashCache);
            httpHashStore.setBaseUrl("/_hashes/fanouts/");
            HttpBlobStore httpBlobStore = new HttpBlobStore(client, blobsHashCache);
            httpBlobStore.setBaseUrl("/_hashes/blobs/");

            Archiver archiver = new Archiver();
            EventManager eventManager = new EventManagerImpl();
            Syncer syncer = new Syncer(eventManager, localRootDir, httpHashStore, httpBlobStore, client, archiver, url.getPath());

            SpliffySync spliffySync = new SpliffySync(localRootDir, client, url.getPath(), syncer, archiver, dbInit, eventManager);
            if (job.isMonitor()) {
                spliffySync.start();
            } else {
                spliffySync.scan();
            }
        }
    }

    public static class SyncJob {

        private final File localDir;
        private final String remoteAddress;
        private final String user;
        private final String pwd;
        private final boolean monitor;

        public SyncJob(File sLocalDir, String sRemoteAddress, String user, String pwd, boolean monitor) {
            this.localDir = sLocalDir;
            this.remoteAddress = sRemoteAddress;
            this.user = user;
            this.pwd = pwd;
            this.monitor = monitor;
        }

        public String getPwd() {
            return pwd;
        }

        public String getUser() {
            return user;
        }

        public File getLocalDir() {
            return localDir;
        }

        public String getRemoteAddress() {
            return remoteAddress;
        }

        public boolean isMonitor() {
            return monitor;
        }
    }
}
