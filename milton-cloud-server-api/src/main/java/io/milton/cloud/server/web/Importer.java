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
package io.milton.cloud.server.web;

import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.httpclient.File;
import io.milton.httpclient.Folder;
import io.milton.httpclient.Host;
import io.milton.httpclient.HttpException;
import io.milton.httpclient.Resource;
import io.milton.httpclient.Utils.CancelledException;
import io.milton.resource.ReplaceableResource;
import io.milton.vfs.db.Profile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author brad
 */
public class Importer implements Runnable {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(Importer.class);    
    
    public static Importer getImporter(Profile p, ContentDirectoryResource dir) {
        String key = getImporterKey(p, dir);
        return mapOfImporters.get(key);
    }
    
    private static String getImporterKey(Profile p, ContentDirectoryResource dir) {
        return p.getId() + "_" + dir.getPath();
    }    
    
    public static Importer create(Profile p, ContentDirectoryResource dir, URI uri) {
        Importer importer = new Importer(dir, uri, null, null);
        String key = getImporterKey(p, dir);
        mapOfImporters.put(key, importer);
        return importer;
    }
    
    /**
     * Keyed by the id of the profile which initiated it conctated with the
     * directory path
     */
    private static final Map<String, Importer> mapOfImporters = new ConcurrentHashMap<>();
    
    
    private transient final ContentDirectoryResource directoryResource;
    private transient final URI uri;
    private transient final String remoteUser;
    private transient final String remotePassword;
    private boolean finished;
    private final List<String> info = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    private Importer(ContentDirectoryResource directoryResource, URI uri, String remoteUser, String remotePassword) {
        this.directoryResource = directoryResource;
        this.uri = uri;
        this.remoteUser = null;
        this.remotePassword = null;
    }


    @Override
    public void run() {
        try {
            doImport();
        } catch (URISyntaxException | NotAuthorizedException | BadRequestException e) {
            warnings.add("Exception " + e.getMessage());
        }
    }    
    
    public void doImport() throws URISyntaxException, NotAuthorizedException, BadRequestException {
        io.milton.httpclient.Host remoteHost = new Host(uri.getHost(), uri.getPath(), uri.getPort(), remoteUser, remotePassword, null, null);
        try {
            // copy all items in remoteHost to into the current folder
            doImport(remoteHost, directoryResource);
        } catch (IOException ex) {
            warnings.add("Interrupted transfer, some files may have been imported");
        } catch (HttpException ex) {
            warnings.add("HTTP error, some files may have been imported");
        }
        log.info("Finished import");
        finished = true;
    }

    private void doImport(Folder remoteFolder, ContentDirectoryResource localFolder) throws IOException, HttpException, NotAuthorizedException, BadRequestException {
        log.trace("doImport: " + remoteFolder.href());

        try {
            //Now import content files in this folder
            importFiles(remoteFolder, localFolder);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        //Finally process sub folders. Note that local folders will have been created already due to their meta files
        for (Resource r : remoteFolder.children()) {
            if (r instanceof Folder) {
                Folder remoteChildFolder = (Folder) r;
                DirectoryResource newLocalFolder = (DirectoryResource) localFolder.getOrCreateDirectory(remoteChildFolder.name, true);
                if (newLocalFolder == null) {
                    warnings.add("Could not create local directory " + remoteChildFolder.name + " in " + localFolder.getPath());
                } else {
                    doImport(remoteChildFolder, newLocalFolder);
                }
            }
        }
    }

    private void importFiles(Folder remoteFolder, ContentDirectoryResource localFolder) throws Exception {
        for (Resource r : remoteFolder.children()) {
            if (r instanceof Folder) {
                // will be done seperately
            } else {
                File remoteFile = (File) r;
                try {
                    importFile(remoteFile, localFolder);
                } catch (HttpException ex) {
                    warnings.add("Error importing " + r.href() + " because " + ex.getMessage());
                    //sb.append("<br/><span style='color: red'>Error importing: ").append(r.href()).append(" - ").append(ex.getMessage()).append("</span>");
                    log.error("Error importing: " + ex.getMessage() + " Local: " + localFolder.getName(), ex);
                }
            }
        }
    }

    private void importFile(File remoteFile, ContentDirectoryResource localFolder) throws HttpException {
        // create or replace a file called remoteFile.name in localFolder		
        log.trace("importFile: " + remoteFile.href());
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try {
            remoteFile.download(bout, null);
        } catch (CancelledException ex) {
            throw new RuntimeException(ex);
        }
        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        try {
            io.milton.resource.Resource r = localFolder.child(remoteFile.name);
            if (r != null) {
                if (r instanceof ReplaceableResource) {
                    ReplaceableResource replaceable = (ReplaceableResource) r;
                    replaceable.replaceContent(bin, (long) bout.size());
                    StringBuffer sb = new StringBuffer();
                    sb.append("<br/>Updated: ").append(localFolder.getName()).append("/").append(r.getName()).append(" (").append(r.getClass()).append(")");
                    info.add(sb.toString());
                } else {
                    warnings.add("Local resource exists but could not be replaced: " + r.getName() + " in " + localFolder.getPath());
                }
            } else {
                r = localFolder.createNew(remoteFile.name, bin, (long) bout.size(), remoteFile.contentType); 
                StringBuffer sb = new StringBuffer();
                sb.append("Created: ").append(localFolder.getName()).append("/").append(r.getName()).append(" (").append(r.getClass()).append(")");
                info.add(sb.toString());
            }
        } catch (Exception e) {
            StringBuffer sb = new StringBuffer();
            sb.append(">Error importing: ").append(remoteFile.href()).append(" - ").append(e.getMessage());
            warnings.add(sb.toString());
            log.error("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
            log.error("Exception importing code", e);
            log.error(bout.toString());
            log.error("OOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO");
        }

    }

    public List<String> getWarnings() {
        return warnings;
    }

    public List<String> getInfo() {
        return info;
    }

    public boolean isFinished() {
        return finished;
    }

    
}
