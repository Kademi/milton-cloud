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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Objects;

/**
 *
 * @author brad
 */
public class FileTemplateHtmlPage extends TemplateHtmlPage{
    private final File templateFile;
    private final long timestamp;

    public FileTemplateHtmlPage(File templateFile) {
        super(templateFile.getAbsolutePath());
        this.templateFile = templateFile;
        this.timestamp = templateFile.lastModified();
    }

    @Override
    public InputStream getInputStream() {
        FileInputStream fin;
        try {
            fin = new FileInputStream(templateFile);
        } catch (FileNotFoundException ex) {
            throw new RuntimeException(ex);
        }
        return fin;
    }

    @Override
    public String getSource() {
        return "file://" + templateFile.getAbsolutePath();
    }

    
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof FileTemplateHtmlPage) {
            FileTemplateHtmlPage other = (FileTemplateHtmlPage) obj;
            if (other.templateFile.equals(templateFile)) {
                return other.timestamp == timestamp;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.templateFile);
        hash = 29 * hash + (int) (this.timestamp ^ (this.timestamp >>> 32));
        return hash;
    }

    public File getTemplateFile() {
        return templateFile;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    boolean isValid() {
        return timestamp == templateFile.lastModified();
    }
}
