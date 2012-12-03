/*
 * Copyright 2012 McEvoy Software Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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

import io.milton.cloud.common.HashCalc;
import io.milton.http.HttpManager;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.Resource;
import io.milton.vfs.data.DataSession;
import io.milton.vfs.db.utils.SessionManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.hashsplit4j.api.HashStore;
import org.hibernate.Session;
import org.hibernate.Transaction;

import static io.milton.context.RequestContext._;

/**
 *
 * @author brad
 */
public class UploadUtils {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(UploadUtils.class);

    /**
     * Inserts the new node, instantiates a FileResource, does an updateModDate
     * and calls save on the parent folder, and wraps the lot in a transaction
     *
     * @param col
     * @param newName
     * @param inputStream
     * @param length
     * @param contentType
     * @return
     * @throws IOException
     * @throws ConflictException
     * @throws NotAuthorizedException
     * @throws BadRequestException
     */
    public static FileResource createNew(ContentDirectoryResource col, String newName, InputStream inputStream, Long length, String contentType) throws IOException, ConflictException, NotAuthorizedException, BadRequestException {
        log.info("createNew: newName=" + newName);
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();

        FileResource fr = createNew(session, col, newName, inputStream, length, contentType);
        col.save();
        tx.commit();
        return fr;
    }

    /**
     * Inserts the new node, instantiates a FileResource, does an updateModDate.
     * DOES NOT save parent or start or commit a transaction
     *
     * @param session
     * @param col
     * @param newName
     * @param inputStream
     * @param length
     * @param contentType
     * @return
     * @throws IOException
     * @throws ConflictException
     * @throws NotAuthorizedException
     * @throws BadRequestException
     */
    public static FileResource createNew(Session session, ContentDirectoryResource col, String newName, InputStream inputStream, Long length, String contentType) throws IOException, ConflictException, NotAuthorizedException, BadRequestException {
        log.info("createNew. get the parent node");
        DataSession.DirectoryNode thisNode = col.getDirectoryNode();
        log.info("createNew. got: " + thisNode.getName());

        if (thisNode.get(newName) != null) {
            throw new BadRequestException(col, "Resource with that name already exists: " + newName);
        }

        log.info("createNew file: " + newName);
        DataSession.FileNode newFileNode = thisNode.addFile(newName);
        FileResource fileResource = new FileResource(newFileNode, col);

        String ct = HttpManager.request().getContentTypeHeader();
        if (ct != null && ct.equals("spliffy/hash")) {
            // read the new hash and set it on this
            InputStreamReader r = new InputStreamReader(inputStream);
            BufferedReader br = new BufferedReader(r);
            String newHash = br.readLine();
            if (newHash == null) {
                throw new BadRequestException(col, "content type is spliffy/hash, but no hash was given");
            }
            newHash = newHash.trim();
            HashStore hashStore = _(HashStore.class);
            if (!hashStore.hasFile(newHash)) {
                throw new BadRequestException(col, "Attempted to set file hash on file which is not in the hashstore: " + newHash + " - " + hashStore);
            }
            newFileNode.setHash(newHash);
        } else {
            log.info("createNew: set content");
            // parse data and persist to stores
            newFileNode.setContent(inputStream);
        }
        col.onAddedChild(fileResource);
        fileResource.updateModDate();
        log.info("added ok");
        return fileResource;
    }

    public static void replaceContent(FileResource fr, InputStream in, Long length) throws BadRequestException, ConflictException, NotAuthorizedException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();

        try {
            String ct = HttpManager.request().getContentTypeHeader();
            if (ct != null && ct.equals("spliffy/hash")) {
                // read the new hash and set it on this                            
                String newHash = HashCalc.getInstance().readHash(in);
                if (newHash == null) {
                    throw new BadRequestException(fr, "content type is spliffy/hash, but no hash was given");
                }
                newHash = newHash.trim();
                if (!_(HashStore.class).hasFile(newHash)) {
                    throw new BadRequestException(fr, "Attempted to set file hash on file which is not in the hashstore: " + newHash);
                }
                fr.contentNode.setHash(newHash);

            } else {
                // just do a normal PUT
                setContent(fr, in);
            }
            fr.getParent().save();
            tx.commit();
        } catch (IOException ex) {
            tx.rollback();
            throw new BadRequestException("io ex", ex);
        }
    }

    public static void setContent(FileResource fr, InputStream in) throws BadRequestException {
        log.info("replaceContent: set content");
        try {
            // parse data and persist to stores
            fr.getFileNode().setContent(in);
            fr.updateModDate();
        } catch (IOException ex) {
            throw new BadRequestException("exception", ex);
        }
    }
}
