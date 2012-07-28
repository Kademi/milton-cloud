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

import java.io.InputStream;
import java.util.List;

/**
 * Represents a page, either an actual page to be rendered or a template
 * 
 * The SmartTemplateRenderer combines templates and pages into rendered, themed web pages
 * 
 * The SmartTemplateParser parses the raw bytes of a HtmlPage object and sets the
 * data back into a structured representation which can be consumer by the renderer
 *
 * @author brad
 */
public interface HtmlPage extends  TitledPage{
    
    /**
     * Css classes defined on the body element
     * 
     * @return 
     */
    List<String> getBodyClasses();

    /**
     * Any elements defined in the head, except title
     * 
     * @return 
     */
    List<WebResource> getWebResources();

    /**
     * Body content, ie everything between the <body> start and end tags
     * 
     * @return 
     */
    String getBody();

    void setBody(String b);
    
    void setTitle(String t);

    /**
     * Get the raw bytes representing this page. This is for parsers to use to
     * parse the bytes into the structured representation of the page
     * 
     * @return 
     */
    InputStream getInputStream();

    /**
     * Just an identifier so we know where this came from
     * 
     * @return 
     */
    String getSource();
    
}
