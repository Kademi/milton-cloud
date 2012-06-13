package io.milton.cloud.server.apps.versions;

import io.milton.cloud.server.db.*;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.resource.Resource;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.http.acl.Principal;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import io.milton.cloud.server.web.AbstractCollectionResource;
import io.milton.cloud.server.web.Services;
import io.milton.cloud.server.web.SpliffyCollectionResource;
import io.milton.cloud.server.web.Utils;
import io.milton.resource.GetableResource;

/**
 * Represents the complete state of a repository at some point in time
 *
 * @author brad
 */
public class RepoVersionFolder extends AbstractCollectionResource implements VersionCollectionResource, GetableResource{
    private final Commit repoVersion;
    
    private final SpliffyCollectionResource parent;
    
    private List<AbstractVersionResource> children;

    public RepoVersionFolder(SpliffyCollectionResource parent, Commit repoVersion, Services services) {
        super(services);
        this.parent = parent;
        this.repoVersion = repoVersion;
    }

    @Override
    public Date getCreateDate() {
        return repoVersion.getCreatedDate();
    }

    @Override
    public String getName() {
        return repoVersion.getCreatedDate().toString(); // TODO: need a nicer date format
    }

    @Override
    public Date getModifiedDate() {
        return repoVersion.getCreatedDate();
    }

    @Override
    public Resource child(String childName) throws NotAuthorizedException, BadRequestException {
        return Utils.childOf(getChildren(), childName);
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            if (repoVersion != null) {
                List<DirectoryMember> members = repoVersion.getRootItemVersion().getMembers();
                children = VersionUtils.toResources(this, members);
            } else {
                children = new ArrayList<>();
            }
        }
        return children;
    }
    
    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        getTemplater().writePage("repoVersion", this, params, out);
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
    public SpliffyCollectionResource getParent() {
        return parent;
    }

    @Override
    public BaseEntity getOwner() {
        return parent.getOwner();
    }

    @Override
    public void addPrivs(List<Priviledge> list, Profile user) {

    }

    @Override
    public Map<Principal, List<Priviledge>> getAccessControlList() {
        return null;
    }

    
    
    @Override
    public Organisation getOrganisation() {
        return parent.getOrganisation();
    }
}
