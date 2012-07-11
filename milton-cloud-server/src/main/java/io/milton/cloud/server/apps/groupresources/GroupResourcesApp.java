package io.milton.cloud.server.apps.groupresources;

import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.Application;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.web.ResourceList;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;

/**
 *
 * @author brad
 */
public class GroupResourcesApp implements Application {

    @Override
    public String getInstanceId() {
        return "groupResources";
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
                    return new ResourcesPage(requestedName, wrf);
                    
            }
        }

        return null;
    }

    @Override
    public void addBrowseablePages(CollectionResource parent, ResourceList children) {

    }
}
