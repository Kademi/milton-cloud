/*
 * Copyright (C) 2012 McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.web.templating;

import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.web.CommonResource;
import io.milton.cloud.server.web.RootFolder;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.cloud.server.web.WebUtils;
import java.io.IOException;
import java.io.Writer;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;

import static io.milton.context.RequestContext._;
import io.milton.vfs.db.Profile;

/**
 * Called from theme templates to delegate to content rendering template
 *
 * @author brad
 */
public class PortletsDirective extends Directive {

    @Override
    public String getName() {
        return "portlets";
    }

    @Override
    public int getType() {
        return LINE;
    }

    @Override
    public boolean render(InternalContextAdapter context, Writer writer, Node node) throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException {
        String portletSection = null;
        if (node.jjtGetChild(0) != null) {
            portletSection = String.valueOf(node.jjtGetChild(0).value(context));
        }
        if( portletSection == null ) {
            return true;
        }
        Profile currentUser = _(SpliffySecurityManager.class).getCurrentUser();
        CommonResource page = (CommonResource) context.get("page");
        RootFolder rootFolder = null;
        if( page != null ) {
            rootFolder = WebUtils.findRootFolder(page);
        }        
        _(ApplicationManager.class).renderPortlets(portletSection, currentUser, rootFolder, context , writer); 
        return true;
    }
}
