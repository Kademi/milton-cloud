package io.milton.cloud.server.apps.versions;

import io.milton.http.*;
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
import io.milton.cloud.server.db.BaseEntity;
import io.milton.cloud.server.db.Organisation;
import io.milton.cloud.server.db.Repository;
import io.milton.cloud.server.db.Profile;
import io.milton.cloud.server.web.AbstractCollectionResource;
import io.milton.cloud.server.web.Services;
import io.milton.cloud.server.web.SpliffyCollectionResource;
import io.milton.cloud.server.web.Utils;
import io.milton.resource.GetableResource;
import io.milton.resource.Resource;

/**
 *
 * @author brad
 */
public class VersionsRootFolder extends  AbstractCollectionResource implements GetableResource{

    private final BaseEntity baseEntity;
    
    private final SpliffyCollectionResource parent;
    
    private List<RepositoryVersionsFolder> children;

    public VersionsRootFolder(SpliffyCollectionResource parent, BaseEntity baseEntity, Services services) {
        super(services);
        this.parent = parent;
        this.baseEntity = baseEntity;
    }

    
    @Override
    public Resource child(String childName) throws NotAuthorizedException, BadRequestException {
        return Utils.childOf(getChildren(), childName);
    }
    
    
    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            children = new ArrayList();
            if (baseEntity.getRepositories() != null) {
                for (Repository r : baseEntity.getRepositories()) {
                    RepositoryVersionsFolder rr = new RepositoryVersionsFolder(parent, r, services);
                    children.add(rr);
                }
            }
        }
        return children;
    }
       
    @Override
    public Date getCreateDate() {
        return baseEntity.getCreatedDate();
    }

    @Override
    public String getName() {
        return "versions";
    }

    @Override
    public Date getModifiedDate() {
        return baseEntity.getModifiedDate();
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        getTemplater().writePage("versionsHome", this, params, out);
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
        return baseEntity;
    }

    @Override
    public void addPrivs(List<Priviledge> list, Profile user) {
        parent.addPrivs(list, user);
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
