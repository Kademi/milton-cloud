package io.milton.cloud.server.web.sync;

import io.milton.cloud.common.CurrentDateService;
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
import io.milton.http.Request;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.CollectionResource;
import io.milton.resource.PostableResource;
import io.milton.resource.PutableResource;
import io.milton.vfs.data.DataSession;
import io.milton.vfs.db.utils.SessionManager;
import java.util.Arrays;
import java.util.Map;
import org.hashsplit4j.api.HashStore;
import org.hashsplit4j.api.Parser;

/**
 * Implements raw access to file data. These are assembled blobs referenced by
 * hash
 *
 * @author brad
 */
public class DirectoryHashesFolder extends BaseResource implements CollectionResource  {

    private final BlobStore blobStore;
    private final HashStore hashStore;
    private final String name;

    public DirectoryHashesFolder(BlobStore blobStore, HashStore hashStore, String name, SpliffySecurityManager securityManager, Organisation org) {
        super(securityManager, org);
        this.blobStore = blobStore;
        this.hashStore = hashStore;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Resource child(String string) throws NotAuthorizedException, BadRequestException {
        return null;
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        return Collections.EMPTY_LIST;
    }

    @Override
    public AccessControlledResource.Priviledge getRequiredPostPriviledge(Request request) {
        return null;
    }     
}
