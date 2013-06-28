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

import io.milton.common.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 *
 * @author brad
 */
public class WebResource {

    public static WebResource param(List<WebResource> webResources, String name) {
        for (WebResource wr : webResources) {
            if (wr.getTag().equals("script")) {
                String type = wr.getAtts().get("type");
                if ("data/parameter".equals(type)) {
                    String title = wr.getAtts().get("title");
                    if (name.equals(title)) {
                        return wr;
                    }
                }
            }

        }
        return null;
    }
    private Map<String, String> atts = new HashMap<>();
    private String tag;
    private String body;

    /**
     * Eg <script>, <link>, <meta>
     *
     * @return
     */
    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    /**
     * Eg src, property, content, type
     *
     * @return
     */
    public Map<String, String> getAtts() {
        return atts;
    }

    public void setAtts(Map<String, String> atts) {
        this.atts = atts;
    }

    /**
     * The body of the tag, such as an inline script
     *
     * @return
     */
    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String toHtml(String themeName, Path webPath) {
        if (tag.length() > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("<").append(tag).append(" ");
            for (Map.Entry<String, String> entry : atts.entrySet()) {
                String adjustedValue = adjustRelativePath(entry.getKey(), entry.getValue(), themeName, webPath);
                sb.append(entry.getKey()).append("=\"").append(adjustedValue).append("\" ");
            }
            if (body != null && body.length() > 0) {
                sb.append(">").append(body).append("</").append(tag).append(">");
            } else {
                sb.append("/>");
            }
            return sb.toString();
        } else {
            return body; // is a comment
        }
    }

    public String getRawHtml() {
        if (tag.length() > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("<").append(tag).append(" ");
            for (Map.Entry<String, String> entry : atts.entrySet()) {
                String v = entry.getValue();
                sb.append(entry.getKey()).append("=\"").append(v).append("\" ");
            }
            if (body != null && body.length() > 0) {
                sb.append(">").append(body).append("</").append(tag).append(">");
            } else {
                sb.append("/>");
            }
            return sb.toString();
        } else {
            return body; // is a comment
        }
    }

    /**
     * If the attribute name is src or href, checks the value to see if its
     * relative, and if so return an absolute path, assuming webresource root is
     * /templates
     *
     *
     * @param name
     * @param value
     * @param themeName
     * @param webPath - path of the directory containing the template/page
     *
     * @return
     */
    public String adjustRelativePath(String name, String value, String themeName, Path webPath) {
        if (name.equals("href") || name.equals("src")) {
            if (value != null && value.length() > 0) {
                if (!value.startsWith("/") && !value.startsWith("http")) {
                    return evaluateRelativePath(value, themeName, webPath);
                }
            }
        }
        return value;
    }

    private String evaluateRelativePath(String value, String themeName, Path webPath) {
        Path relative = Path.path(value);
        Path p = webPath;
        for (String relPart : relative.getParts()) {
            switch (relPart) {
                case "..":
                    p = p.getParent();
                    break;
                case ".":
                    break;
                default:
                    // we want to transform hard coded theme references to the configured theme
                    // so if we have eg "../themes/yellow/style.css", but configured theme is "blue", then need to transform to -> "/templates/themes/blue/style.css"
                    if (p == null || p.getName() == null) {
                        // invalid path
                        return "";
                    } else {
                        if (p.getName().equals("themes")) {
                            p = p.child(themeName);
                        } else {
                            p = p.child(relPart);
                        }
                    }
                    break;
            }
        }
        return p.toString();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + Objects.hashCode(this.atts);
        hash = 79 * hash + Objects.hashCode(this.tag);
        hash = 79 * hash + Objects.hashCode(this.body);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final WebResource other = (WebResource) obj;
        if (!Objects.equals(this.atts, other.atts)) {
            return false;
        }
        if (!Objects.equals(this.tag, other.tag)) {
            return false;
        }
        if (!Objects.equals(this.body, other.body)) {
            return false;
        }
        return true;
    }

    /**
     * Create a copy of this WebResource
     * 
     * @return 
     */
    public WebResource duplicate() {
        WebResource dup = new WebResource();
        dup.setBody(body);
        dup.setTag(tag);
        Map<String,String> dupedAtts = new HashMap<>();
        for( Map.Entry<String, String> entry : atts.entrySet()) {
            dupedAtts.put(entry.getKey(), entry.getValue());
        }
        dup.setAtts(dupedAtts);
        return dup;
    }
}
