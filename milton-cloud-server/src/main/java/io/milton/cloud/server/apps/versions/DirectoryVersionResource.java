package io.milton.cloud.server.apps.versions;

import io.milton.http.*;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import io.milton.cloud.server.db.DirectoryMember;
import io.milton.cloud.server.db.ItemVersion;
import io.milton.cloud.server.db.Organisation;
import io.milton.cloud.server.web.Utils;
import io.milton.resource.GetableResource;
import io.milton.resource.Resource;

/**
 * This is a version of a folder. ie its the complete state of the folder at
 * some point in time.
 *
 * @author brad
 */
public class DirectoryVersionResource extends AbstractVersionResource implements VersionCollectionResource, GetableResource {

    private List<AbstractVersionResource> children;

    public DirectoryVersionResource(VersionCollectionResource parent, DirectoryMember directoryMember) {
        super(parent, directoryMember);
    }

    public ItemVersion getItemVersion() {
        return directoryMember.getMemberItem();
    }

    @Override
    public Resource child(String childName) throws NotAuthorizedException, BadRequestException {
        return Utils.childOf(getChildren(), childName);
    }

    @Override
    public boolean isDir() {
        return true;
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            if (getItemVersion() != null) {
                List<DirectoryMember> members = getItemVersion().getMembers();
                children = VersionUtils.toResources(this, members);
            } else {
                children = new ArrayList<>();
            }
        }
        return children;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        getTemplater().writePage("directoryVersion", this, params, out);
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return null;
    }

    @Override
    public String getContentType(String accepts) {
        return "text/html";
    }

    @Override
    public Long getContentLength() {
        return null;
    }
    
    @Override
    public Organisation getOrganisation() {
        return parent.getOrganisation();
    }    
}
