package io.milton.cloud.server.manager;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.Session;
import io.milton.cloud.server.db.DirectoryMember;
import io.milton.cloud.server.db.ItemVersion;
import io.milton.cloud.server.web.MutableCollection;

/**
 *
 * @author brad
 */
public class LinkManager {

    /**
     * Called when a resource has changed and we need to ensure that linked
     * folders are updated to keep in sync
     *
     * Does NOT do a dirty check, only call if a change has actually occured
     *
     * @param name - name of the directory member being updated
     * @param origMemberIV - this is the item version of the directory member
     * which has just changed. We must look for other Directory Members linking
     * to the same ItemVersion and update them
     * @param newMemberIV - this is the newly created item version, which any
     * linked folders should be updated to
     * @param newParentIV - this is the parent which has already been
     * updated, and should not be changed by this method
     */
    public void updateLinkedFolders(String name, ItemVersion origMemberIV, ItemVersion newMemberIV, ItemVersion newParentIV, Session session) {
        for (DirectoryMember otherDM : origMemberIV.getLinked()) {
            if (otherDM.getParentItem().getItem() != newParentIV.getItem()) { // make sure is not the already saved one
                // create new DM's for sibling DM's of otherDM
                createNewVersion(name, otherDM.getParentItem(), newMemberIV, session);
            }
        }
    }

    /**
     * 
     * Return the newly created ItemVersion
     * 
     * @param otherDM
     * @param updatedMemberIV - this is a member of the directory which has changed
     * @return - the newly created parent version
     */
    private ItemVersion createNewVersion(String updatedName, ItemVersion oldParentIV, ItemVersion updatedMemberIV, Session session) {
        ItemVersion newParentIV = newParent(oldParentIV);
        List<DirectoryMember> newMembers = new ArrayList<>();
        newParentIV.setMembers(newMembers);
        for (DirectoryMember oldDm : oldParentIV.getMembers()) {
            // The item version of this directory member before it was updated
            ItemVersion origMemberIV = oldDm.getMemberItem();

            ItemVersion newMemberIV;
            if ( updatedMemberIV != null && updatedName.equals(oldDm.getName()) ) {
                newMemberIV = updatedMemberIV;
            } else {
                newMemberIV = oldDm.getMemberItem(); // use existing ItemVersion
            }
            //newMemberIV.setItemHash(r.getEntryHash());

            DirectoryMember newMemberDM = new DirectoryMember();
            newMemberDM.setParentItem(newParentIV);
            newMemberDM.setName(oldDm.getName());

            newMemberDM.setMemberItem(newMemberIV);
            session.save(newMemberDM);

            updateLinkedFolders(oldDm.getName(), origMemberIV, newMemberIV, newParentIV, session);

            newMembers.add(newMemberDM);
        }
        updateHash(newParentIV);
        return newParentIV;
    }

    /**
     * Create a new version of oldParent, but without its hash set yet
     * 
     * The hash will be set after children have been created
     * 
     * @param oldParent
     * @return 
     */
    private ItemVersion newParent(ItemVersion oldParent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Now that the new members have been set, calculate the hash
     * and set it on the new IV
     * 
     * @param newParentIV 
     */
    private void updateHash(ItemVersion newParentIV) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
