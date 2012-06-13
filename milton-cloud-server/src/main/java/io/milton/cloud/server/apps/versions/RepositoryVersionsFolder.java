package io.milton.cloud.server.apps.versions;

import io.milton.cloud.server.db.BaseEntity;
import io.milton.cloud.server.db.Organisation;
import io.milton.cloud.server.db.Profile;
import io.milton.cloud.server.db.Repository;
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
 * Lists all of the versions within a repository
 * 
 * TODO: refactor this to group versions to make it more manageable. Perhaps
 * groups by month, user, etc
 *
 * @author brad
 */
public class RepositoryVersionsFolder extends AbstractCollectionResource implements VersionCollectionResource, GetableResource{
    private final Repository repo;
    private final SpliffyCollectionResource parent;
    private List<RepoVersionFolder> children;

    public RepositoryVersionsFolder(SpliffyCollectionResource parent, Repository repo, Services services) {
        super(services);
        this.parent = parent;
        this.repo = repo;
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            children = new ArrayList<>();
            
            // TODO: integrate with branches
            
//            for( Commit rv : repo.getBranches()) {
//                RepoVersionFolder f = new RepoVersionFolder(this, rv, services);
//                children.add(f);
//            }
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
    public Date getCreateDate() {
        return repo.getCreatedDate();
    }

    @Override
    public String getName() {
        return repo.getName();
    }

    @Override
    public Date getModifiedDate() {
        return repo.getCreatedDate();
    }

    @Override
    public Resource child(String childName) throws NotAuthorizedException, BadRequestException {
        return Utils.childOf(getChildren(), childName);
    }

    @Override
    public SpliffyCollectionResource getParent() {
        return parent;
    }

    @Override
    public BaseEntity getOwner() {
        return repo.getBaseEntity();
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
