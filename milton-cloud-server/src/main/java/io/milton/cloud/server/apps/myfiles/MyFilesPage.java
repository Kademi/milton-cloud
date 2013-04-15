package io.milton.cloud.server.apps.myfiles;

import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.web.*;

/**
 *
 * @author brad
 */
public class MyFilesPage extends TemplatedHtmlPage {

    public MyFilesPage(String name, WebsiteRootFolder parent) {
        super(name, parent, "myfiles/myfilesHome", "My Files");
    }

    @Override
    public boolean isPublic() {
        return false;
    }
    
    
}
