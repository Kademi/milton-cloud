package io.milton.cloud.server.apps.groupresources;

import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.ChildPageApplication;
import io.milton.cloud.server.apps.MenuApplication;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.resource.Resource;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Website;

/**
 *
 * @author brad
 */
public class GroupResourcesApp implements MenuApplication, ChildPageApplication {

    @Override
    public String getInstanceId() {
        return "groupResources";
    }

    @Override
    public String getTitle(Organisation organisation, Website website) {
        return "Group resources and files";
    }

    
    
    @Override
    public String getSummary(Organisation organisation, Website website) {
        return "Allows users groups to have folders of files, which members of the group can download";
    }

    
    
    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {

    }

    @Override
    public Resource getPage(Resource parent, String requestedName) {
        if (parent instanceof WebsiteRootFolder) {
            WebsiteRootFolder wrf = (WebsiteRootFolder) parent;
            switch (requestedName) {
                case "resources":
                    MenuItem.setActiveIds("menuResources");
                    return new ResourcesPage(requestedName, wrf);
                    
            }
        }

        return null;
    }

    @Override
    public void appendMenu(MenuItem parent) {
        String parentId = parent.getId();
        switch (parentId) {
            case "menuRoot":
                if (parent.getRootFolder() instanceof WebsiteRootFolder) {
                    if (parent.getUser() != null) {
                        parent.getOrCreate("menuResources", "Resources", "/resources").setOrdering(30);
                    }
                }
        }
    }
}
