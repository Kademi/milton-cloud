package io.milton.cloud.server.apps.versions;

import io.milton.cloud.server.db.BaseEntity;
import io.milton.cloud.server.db.DirectoryMember;
import io.milton.cloud.server.db.Profile;
import io.milton.http.acl.Principal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import io.milton.cloud.server.web.AbstractResource;
import io.milton.http.AccessControlledResource;

/**
 * Based class for files and directories accessed within a versions folder
 * 
 * These are similar to their Mutable counterparts, but aren't mutable
 *
 * @author brad
 */
public abstract class AbstractVersionResource extends AbstractResource{

    protected final VersionCollectionResource parent;
    
    protected final DirectoryMember directoryMember;

    public AbstractVersionResource(VersionCollectionResource parent, DirectoryMember directoryMember) {
        super(parent.getServices());
        this.parent = parent;
        this.directoryMember = directoryMember;
    }
        
    
    @Override
    public Date getCreateDate() {
        return directoryMember.getMemberItem().getItem().getCreateDate();
    }

    @Override
    public String getName() {
        return directoryMember.getName();
    }

    @Override
    public Date getModifiedDate() {
        return directoryMember.getMemberItem().getModifiedDate();
    }

    @Override
    public VersionCollectionResource getParent() {
        return parent;
    }

    @Override
    public BaseEntity getOwner() {
        return parent.getOwner();
    }

    @Override
    public void addPrivs(List<Priviledge> list, Profile user) {
        getParent().addPrivs(list, user);
    }    
    
   /**
     * Get all allowed priviledges for all principals on this resource. Note
     * that a principal might be a user, a group, or a built-in webdav group
     * such as AUTHENTICATED
     *
     * @return
     */
    @Override
    public Map<Principal, List<AccessControlledResource.Priviledge>> getAccessControlList() {
        return null;

    }        
}
