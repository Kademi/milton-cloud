package io.milton.cloud.server.web.sync;

import io.milton.resource.Resource;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import java.io.*;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.hashsplit4j.api.BlobStore;
import io.milton.cloud.server.db.Organisation;
import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.resource.PutableResource;

/**
 *
 * @author brad
 */
class BlobFolder extends  BaseResource implements PutableResource {

    private final BlobStore blobStore;
    private final String name;
    
    public BlobFolder(BlobStore blobStore, String name, SpliffySecurityManager securityManager,Organisation org) {
        super(securityManager, org);
        this.blobStore = blobStore;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Resource createNew(String newName, InputStream inputStream, Long length, String contentType) throws IOException, ConflictException, NotAuthorizedException, BadRequestException {
        long hash = Long.parseLong(newName);
        ByteArrayOutputStream bout = new ByteArrayOutputStream(length.intValue());
        IOUtils.copy(inputStream, bout);
        byte[] bytes = bout.toByteArray();
        blobStore.setBlob(hash, bytes);
        return new BlobResource(bytes, hash, securityManager, org);
    }

    @Override
    public Resource child(String string) throws NotAuthorizedException, BadRequestException {
        return null;
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        return Collections.EMPTY_LIST;
    }}
