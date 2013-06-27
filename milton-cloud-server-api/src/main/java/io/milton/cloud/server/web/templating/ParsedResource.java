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
package io.milton.cloud.server.web.templating;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.htmlparser.jericho.Attributes;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.Source;

/**
 *
 * @author brad
 */
public class ParsedResource {
    final String title;
    final List<WebResource> webResources;
    final List<String> bodyClasses;
    final String body;

    public ParsedResource(String sourceXml) {
        Source source = new Source(sourceXml);
        net.htmlparser.jericho.Element elHead = source.getFirstElement("head");
        net.htmlparser.jericho.Element elBody = source.getFirstElement("body");
        if (elBody != null) {
            String sBodyClasses = elBody.getAttributeValue("class");
            if (sBodyClasses != null) {
                bodyClasses = Arrays.asList(sBodyClasses.split(" "));
            } else {
                bodyClasses = Collections.EMPTY_LIST;
            }
        } else {
            bodyClasses = Collections.EMPTY_LIST;
        }
        this.body = HtmlTemplateParser.getContent(elBody);
        String title = null;
        List<WebResource> list = new ArrayList<>();
        if (elHead != null) {
            List<Element> headElements = elHead.getChildElements();
            if (headElements != null) {
                for (net.htmlparser.jericho.Element wrTag : headElements) {
                    //System.out.println("tag: " + wrTag.getName());
                    if (wrTag.getName().equals("title")) {
                        String s = wrTag.getRenderer().toString();
                        title = s;
                    } else {
                        if (!wrTag.getName().startsWith("!")) {
                            WebResource wr = new WebResource();
                            list.add(wr);
                            wr.setTag(wrTag.getName());
                            String tagBody = HtmlTemplateParser.getContent(wrTag);
                            wr.setBody(tagBody);
                            Attributes atts = wrTag.getAttributes();
                            if (atts != null) {
                                for (net.htmlparser.jericho.Attribute att : atts) {
                                    wr.getAtts().put(att.getName(), att.getValue());
                                }
                            }
                        } else {
                            String comment = HtmlTemplateParser.getContent(wrTag);
                            WebResource wr = new WebResource();
                            list.add(wr);
                            wr.setTag(""); // indictes comment
                            wr.setBody(wrTag.toString());
                            //System.out.println("comment : " + wrTag.getName() + " - " + comment );
                        }
                    }
                }
            }
        }
        this.title = title;        
        this.webResources = Collections.unmodifiableList(list);
    }

    public String getTitle() {
        return title;
    }

    public List<WebResource> getWebResources() {
        return webResources;
    }

    public String getBody() {
        return body;
    }

    public List<String> getBodyClasses() {
        return bodyClasses;
    }
    
}
