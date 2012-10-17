package io.milton.cloud.server.web.sync;

import io.milton.cloud.server.web.JsonResult;
import io.milton.resource.Resource;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import java.io.*;
import java.util.Collections;
import java.util.List;
import org.hashsplit4j.api.BlobStore;
import io.milton.vfs.db.Organisation;
import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.http.Auth;
import io.milton.http.FileItem;
import io.milton.http.HttpManager;
import io.milton.http.Range;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.PostableResource;
import io.milton.resource.PutableResource;
import java.util.Arrays;
import java.util.Map;
import org.hashsplit4j.api.Fanout;
import org.hashsplit4j.api.HashStore;
import org.hashsplit4j.api.Parser;

/**
 * Implements raw access to file data. These are assembled blobs referenced by
 * hash
 * 
 * Allows files to be uploaded whole, but without being connected to a directory
 *
 * @author brad
 */
class FilesFolder extends BaseResource implements PutableResource, PostableResource {

    private final BlobStore blobStore;
    private final HashStore hashStore;
    private final String name;
    
    private JsonResult jsonResult;

    public FilesFolder(BlobStore blobStore, HashStore hashStore, String name, SpliffySecurityManager securityManager, Organisation org) {
        super(securityManager, org);
        this.blobStore = blobStore;
        this.hashStore = hashStore;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Note that, contrary to the spec, this will create resources which do not
     * have the name given. The name of the resource will always be that of its hash
     * 
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
    @Override
    public Resource createNew(String newName, InputStream inputStream, Long length, String contentType) throws IOException, ConflictException, NotAuthorizedException, BadRequestException {
        Parser parser = new Parser();
        String hash = parser.parse(inputStream, hashStore, blobStore);
        Fanout fanout = hashStore.getFileFanout(hash);
        return new GetResource(fanout, hash, securityManager, org, blobStore, hashStore);
    }

    @Override
    public Resource child(String string) throws NotAuthorizedException, BadRequestException {
        return null;
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        return Collections.EMPTY_LIST;
    }

    /**
     * For compatibility with qq ajax upload js library
     * 
     * @param parameters
     * @param files
     * @return
     * @throws BadRequestException
     * @throws NotAuthorizedException
     * @throws ConflictException 
     */
    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        if (parameters.containsKey("qqfile")) { 
            try {
                InputStream in = HttpManager.request().getInputStream();
                Resource r = createNew(null, in, null, null);
                jsonResult = new JsonResult(true);
                jsonResult.setNextHref(r.getName());
            } catch (IOException ex) {
                jsonResult = new JsonResult(false);
                jsonResult.setMessages(Arrays.asList("Failed to save the file"));
            }
        }
        return null;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        if( jsonResult != null ) {
            jsonResult.write(out);
        }
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return null;
    }

    @Override
    public String getContentType(String accepts) {
        return null;
    }

    @Override
    public Long getContentLength() {
        return null;
    }
}
