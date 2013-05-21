/*
 * Copyright 2013 McEvoy Software Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.apps;

import io.milton.cloud.server.DataSessionManager;
import io.milton.cloud.server.web.RootFolder;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.cloud.server.web.templating.TextTemplater;
import io.milton.common.Path;
import static io.milton.context.RequestContext._;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Repository;
import java.io.IOException;
import java.io.Writer;
import org.apache.velocity.context.Context;

import io.milton.vfs.data.DataSession;
import io.milton.vfs.data.DataSession.FileNode;

/**
 * A CustomApp is one which can be added to an Organisation's repositories
 * dynamically, ie at runtime.
 *
 * @author brad
 */
public class CustomApp implements PortletApplication {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(CustomApp.class);
    
    private final Repository repository;
    private static final Path PORTLETS_PATH = Path.path("/portlets");

    public CustomApp(Repository repository) {
        this.repository = repository;
    }
    
    
    
    @Override
    public void renderPortlets(String portletSection, Profile currentUser, RootFolder rootFolder, Context context, Writer writer) throws IOException {
        DataSession dataSession = _(DataSessionManager.class).get(repository.liveBranch());
        DataSession.DataNode node = dataSession.find(PORTLETS_PATH.child(portletSection));
        if( node instanceof DataSession.FileNode) {
            log.info("Found portlet: " + portletSection);
            FileNode templateFile = (FileNode) node;
            TextTemplater.setInjectedTemplate(templateFile.getContent());
            _(TextTemplater.class).writePage(TextTemplater.INJECTED_PATH, currentUser, rootFolder, context, writer);
        }
    }

    @Override
    public String getInstanceId() {
        return repository.getName();
    }

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
        
    }

    @Override
    public String getTitle(Organisation organisation, Branch websiteBranch) {
        if( repository.getTitle() != null ) {
            return repository.getTitle();
        } else {
            return repository.getName();
        }
    }

    @Override
    public String getSummary(Organisation organisation, Branch websiteBranch) {
        return "Custom application: " + repository.getName() + " on live branch: " + repository.getLiveBranch();
    }
    
}
