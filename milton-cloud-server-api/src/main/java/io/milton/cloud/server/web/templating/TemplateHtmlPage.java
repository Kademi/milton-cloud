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
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author brad
 */
public abstract class TemplateHtmlPage implements HtmlPage{
    private final List<String> bodyClasses;
    private final List<WebResource> webResources;
    private final String id;
    private String body;
    private String title;
    
    abstract long getTimestamp();
    
    public TemplateHtmlPage(String id) {
        this.id = id;
        this.webResources = new ArrayList<>();
        this.bodyClasses = new ArrayList<>();
    }

    @Override
    public List<String> getBodyClasses() {
        return bodyClasses;
    }

    @Override
    public List<WebResource> getWebResources() {
        return webResources;
    }

    @Override
    public String getBody() {
        return body;
    }

    @Override
    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    public String getId() {
        return id;
    }

    
    
}
