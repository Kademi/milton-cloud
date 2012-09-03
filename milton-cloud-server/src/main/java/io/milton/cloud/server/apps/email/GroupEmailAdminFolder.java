package io.milton.cloud.server.apps.email;

import io.milton.cloud.server.db.GroupEmailJob;
import io.milton.cloud.server.apps.forums.*;
import io.milton.cloud.server.web.AbstractCollectionResource;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.ResourceList;
import io.milton.http.Request;
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
public class GroupEmailAdminFolder extends AbstractCollectionResource {

    private final String name;
    private final CommonCollectionResource parent;
    private final Organisation org;
    private ResourceList children;
    

    public GroupEmailAdminFolder(String name, CommonCollectionResource parent, Organisation org) {
        this.name = name;
        this.parent = parent;
        this.org = org;
    }
    

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            children = new ResourceList();
            List<GroupEmailJob> jobs = GroupEmailJob.findByOrg(org, SessionManager.session()); 
            for( GroupEmailJob f : jobs ) {
                GroupEmailPage faf = new GroupEmailPage(f, parent);
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
    public Organisation getOrganisation() {
        return parent.getOrganisation();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<Principal, List<AccessControlledResource.Priviledge>> getAccessControlList() {
        return null;
    }
    
    @Override
    public Priviledge getRequiredPostPriviledge(Request request) {
        return null;
    }            
}
