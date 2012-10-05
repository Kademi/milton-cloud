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

import io.milton.cloud.server.web.RenderFileResource;
import io.milton.cloud.util.JDomUtils;
import io.milton.common.Path;
import java.io.*;
import java.util.Arrays;
import javax.xml.stream.XMLStreamException;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;

/**
 *
 * @author brad
 */
public class HtmlTemplateParser {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(HtmlTemplateParser.class);
    private HtmlFormatter htmlFormatter = new HtmlFormatter();

    /**
     * Parse the file associated with the meta, extracting webresources, body
     * class attributes and the template, and setting that information on the
     * meta object
     *
     * @param meta
     */
    public void parse(HtmlPage meta, Path webPath) throws IOException, XMLStreamException {
        log.info("parse: " + meta.getSource() + " - " + meta.getClass());
        Document doc;
        try (InputStream fin = meta.getInputStream()) {
            if (fin != null) {
                BufferedInputStream bufIn = new BufferedInputStream(fin);
                doc = JDomUtils.getJDomDocument(bufIn);
            } else {
                doc = null;
            }
        }
        if (doc != null) {
            Element elRoot = doc.getRootElement();
            if (!elRoot.getName().equals("html")) {
                throw new RuntimeException("Document is not an html doc");
            }
            Element elHead = JDomUtils.getChild(elRoot, "head");
            parseWebResourcesFromHtml(elHead, meta, webPath);

            Element elBody = JDomUtils.getChild(elRoot, "body");
            if (elBody != null) {
                String sBodyClasses = elBody.getAttributeValue("class");
                if (sBodyClasses != null) {
                    meta.getBodyClasses().addAll(Arrays.asList(sBodyClasses.split(" ")));
                }
            }

            // TODO: move ftl directive into top level trmplate only
            String body = JDomUtils.getValueOf(elRoot, "body");
            meta.setBody(body);
        }
    }

    /**
     * Does the opposite of parse, formats the structured fields into HTML
     *
     * @param aThis
     * @param bout
     */
    public void update(RenderFileResource r, ByteArrayOutputStream bout) {
        htmlFormatter.update(r, bout);
    }




    private void parseWebResourcesFromHtml(Element elHead, HtmlPage meta, Path webPath) {
        for (Element wrTag : JDomUtils.children(elHead)) {
            if (wrTag.getName().equals("title")) {
                meta.setTitle(JDomUtils.getValueOf(elHead, "title"));
            } else {
                WebResource wr = new WebResource(webPath);                
                meta.getWebResources().add(wr);
                wr.setTag(wrTag.getName());
                String body = getContent(wrTag);
                wr.setBody(body);
                for (Object oAtt : wrTag.getAttributes()) {
                    Attribute att = (Attribute) oAtt;
                    wr.getAtts().put(att.getName(), att.getValue());
                }
            }
        }
    }

    
    public String getContent(org.jdom.Element el) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JDomUtils.transformDocument(out, el);
        return out.toString().trim();
    }    
}
