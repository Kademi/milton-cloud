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

import java.io.IOException;
import java.io.Writer;
import org.apache.velocity.Template;
import org.apache.velocity.context.Context;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;

/**
 * Called from theme templates to delegate to content rendering template
 *
 * @author brad
 */
public class VelocityContentDirective extends Directive {

    private static final String NAME_CONTENT_TEMPLATE = "contentTemplate";
    
    public static void setContentTemplate(Template t, Context context) {
        context.put(NAME_CONTENT_TEMPLATE, t);
    }
    
    public static Template getContentTemplate(Context context) {
        return (Template) context.get(NAME_CONTENT_TEMPLATE);
    }

    @Override
    public String getName() {
        return "content";
    }

    @Override
    public int getType() {
        return LINE;
    }

    @Override
    public boolean render(InternalContextAdapter context, Writer writer, Node node) throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException {
        Template t = getContentTemplate(context);
        t.merge(context, writer);
        return true;
    }
    
}
