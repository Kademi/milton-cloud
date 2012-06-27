package io.milton.cloud.server.apps.forums;

import io.milton.cloud.server.web.AbstractCollectionResource;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.ResourceList;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.principal.Principal;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Website;
import io.milton.vfs.db.utils.SessionManager;
import java.util.List;
import java.util.Map;


/**
 * 
 *
 * @author brad
 */
public class ForumsAdminFolder extends AbstractCollectionResource {

    private final String name;
    private final CommonCollectionResource parent;
    private final Website website;
    private ResourceList children;
    

    public ForumsAdminFolder(String name, CommonCollectionResource parent, Website website) {
        super(parent.getServices());
        this.name = name;
        this.parent = parent;
        this.website = website;
    }
    

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            children = new ResourceList();
            List<Forum> forums = Forum.findByWebsite(website, SessionManager.session());
            for( Forum f : forums ) {
                ForumAdminFolder faf = new ForumAdminFolder(f, parent);
                children.add(faf);
            }
        }
        return children;
    }

    @Override
    public CommonCollectionResource getParent() {
        return parent;
    }

    @Override
    public BaseEntity getOwner() {
        return parent.getOwner();
    }

    @Override
    public Organisation getOrganisation() {
        return parent.getOrganisation();
    }

    @Override
    public void addPrivs(List<AccessControlledResource.Priviledge> list, Profile user) {
        parent.addPrivs(list, user);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<Principal, List<AccessControlledResource.Priviledge>> getAccessControlList() {
        return null;
    }
}
