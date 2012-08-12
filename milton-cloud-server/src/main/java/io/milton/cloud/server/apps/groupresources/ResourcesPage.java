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
public class ResourcesPage extends TemplatedHtmlPage {

    public ResourcesPage(String name, CommonCollectionResource parent) {
        super(name, parent, "groupresources/myResources", "My resources");
    }

    @Override
    protected Map<String, Object> buildModel(Map<String, String> params) {
        try {
            Map<String, Object> map = super.buildModel(params);
            List<FileResource> resources = findResources(_(SpliffySecurityManager.class).getCurrentUser());
            map.put("resources", resources);
            return map;
        } catch (NotAuthorizedException | BadRequestException ex) {
            throw new RuntimeException(ex);
        }
    }

    private List<FileResource> findResources(Profile currentUser) throws NotAuthorizedException, BadRequestException {
        List<FileResource> list = new ArrayList<>();
        WebsiteRootFolder wrf = (WebsiteRootFolder) WebUtils.findRootFolder(this);
        Resource rContent = wrf.child("content");
        if (rContent instanceof ContentDirectoryResource) {
            ContentDirectoryResource content = (ContentDirectoryResource) rContent;
            Resource rResources = content.child("resources");
            if (rResources instanceof ContentDirectoryResource) {
                ContentDirectoryResource colResources = (ContentDirectoryResource) rResources;
                for (Resource rGroupResources : colResources.getChildren()) {
                    if (rGroupResources instanceof ContentDirectoryResource) {
                        // this should be a folder corresponding to a group name
                        ContentDirectoryResource colGroupResources = (ContentDirectoryResource) rGroupResources;
                        if( colGroupResources.is("folder")) {
                            addResources(colGroupResources, currentUser, list);
                        }
                    }

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
        for( Resource r : colGroupResources.getChildren()) {
            if( r instanceof FileResource) {
                FileResource fr = (FileResource) r;
                list.add(fr);
            }
        }
    }
}

