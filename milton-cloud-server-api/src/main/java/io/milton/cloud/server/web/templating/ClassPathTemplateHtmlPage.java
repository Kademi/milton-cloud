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

import java.io.*;
import java.net.URL;
import java.util.Objects;

/**
 *
 * @author brad
 */
public class ClassPathTemplateHtmlPage extends TemplateHtmlPage{

    private final URL resource;
    private final long timestamp;
    
    public ClassPathTemplateHtmlPage(URL resource) {
        super(resource.toString());
        this.resource = resource;
        timestamp = System.currentTimeMillis();
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }   
    
    @Override
    public InputStream getInputStream() {
        System.out.println("getInputstream: " + resource);
        try {
            return resource.openStream();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof ClassPathTemplateHtmlPage) {
            ClassPathTemplateHtmlPage other = (ClassPathTemplateHtmlPage) obj;
            return (other.resource.equals(resource));
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 67 * hash + Objects.hashCode(this.resource);
        return hash;
    }

    @Override
    boolean isValid() {
        return true;
    }
}
