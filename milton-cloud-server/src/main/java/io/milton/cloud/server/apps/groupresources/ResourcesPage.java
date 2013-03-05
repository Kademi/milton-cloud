package io.milton.cloud.server.apps.groupresources;

import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.web.*;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.Resource;
import io.milton.vfs.db.Profile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.milton.context.RequestContext._;
import io.milton.http.HttpManager;
import io.milton.http.exceptions.NotFoundException;
import io.milton.vfs.data.DataSession;
import io.milton.vfs.db.GroupMembership;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

/**
 * Displays resources based on what gruops the current user is enrolled in
 *
 * Reads from a conventional folder structure associated with the website:
 * /resources/[group-name]
 *
 * where group-name is the name of a group which is associated with the current
 * website
 *
 * @author brad
 */
public class ResourcesPage extends DirectoryResource<WebsiteRootFolder> {

    public ResourcesPage(DataSession.DirectoryNode dirNode, WebsiteRootFolder parent) {
        super(dirNode, parent);
    }

    @Override
    public String getTitle() {
        return "My Resources";
    }

    
    
    @Override
    public void renderPage(OutputStream out, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        WebUtils.setActiveMenu(getHref(), WebUtils.findRootFolder(this)); // For front end
        getTemplater().writePage("groupresources/myResources", this, params, out);
    }

    public Map<String, Object> getModel() {
        try {
            Map<String, Object> map = new HashMap<>();
            List<FileResource> resources = findResources(_(SpliffySecurityManager.class).getCurrentUser());
            map.put("resources", resources);
            return map;
        } catch (NotAuthorizedException | BadRequestException ex) {
            throw new RuntimeException(ex);
        }
    }

    private List<FileResource> findResources(Profile currentUser) throws NotAuthorizedException, BadRequestException {
        List<FileResource> list = new ArrayList<>();
        for (Resource rGroupResources : getChildren()) {
            if (rGroupResources instanceof ContentDirectoryResource) {
                // this should be a folder corresponding to a group name
                ContentDirectoryResource colGroupResources = (ContentDirectoryResource) rGroupResources;
                String folderName = colGroupResources.getName();
                // Check user is in a group with that name
                if (userIsInGroup(currentUser, folderName)) {
                    addResources(colGroupResources, currentUser, list);
                }
            }
        }
        return list;
    }

    /**
     * if the given directory name is a group name which the current user has
     * access to then append resources to the list
     *
     * @param colGroupResources
     * @param currentUser
     */
    private void addResources(ContentDirectoryResource colGroupResources, Profile currentUser, List<FileResource> list) throws NotAuthorizedException, BadRequestException {
        // TODO: filter out resources based on group membership
        for (Resource r : colGroupResources.getChildren()) {
            if (r instanceof FileResource) {
                FileResource fr = (FileResource) r;
                list.add(fr);
            }
        }
    }

    private boolean userIsInGroup(Profile currentUser, String folderName) {
        for( GroupMembership gm : currentUser.getMemberships()) {
            if( gm.getGroupEntity().getName().equals(folderName)) {
                if( getOrganisation() == gm.getGroupEntity().getOrganisation()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isPublic() {
        return false;
    }
    
    
}
