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

import com.bradmcevoy.xml.XmlHelper;
import io.milton.cloud.server.web.RenderFileResource;
import io.milton.common.Path;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
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
    public void parse(HtmlPage meta, Path webPath) throws IOException {
        log.info("parse");
        Document doc;
        try (InputStream fin = meta.getInputStream()) {
            if (fin != null) {
                BufferedInputStream bufIn = new BufferedInputStream(fin);
                doc = getJDomDocument(bufIn);
            } else {
                doc = null;
            }
        }
        if (doc != null) {
            Element elRoot = doc.getRootElement();
            if (!elRoot.getName().equals("html")) {
                throw new RuntimeException("Document is not an html doc");
            }
            Element elHead = getChild(elRoot, "head");
            parseWebResourcesFromHtml(elHead, meta, webPath);

            Element elBody = getChild(elRoot, "body");
            if (elBody != null) {
                String sBodyClasses = elBody.getAttributeValue("class");
                if (sBodyClasses != null) {
                    meta.getBodyClasses().addAll(Arrays.asList(sBodyClasses.split(" ")));
                }
            }

            // TODO: move ftl directive into top level trmplate only
            String body = getValueOf(elRoot, "body");
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

    public org.jdom.Document getJDomDocument(InputStream fin) {
        try {
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            if (!inputFactory.isPropertySupported(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES)) {
                throw new RuntimeException(":EEEk");
            }
            inputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.FALSE);
            inputFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
            inputFactory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
            inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
            inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
            XMLResolver xMLResolver = new XMLResolver() {

                @Override
                public Object resolveEntity(String publicID, String systemID, String baseURI, String namespace) throws XMLStreamException {
                    return new ByteArrayInputStream(new byte[0]);
                }
            };
            inputFactory.setProperty(XMLInputFactory.RESOLVER, xMLResolver);
            StaxBuilder staxBuilder = new StaxBuilder();
            XMLStreamReader streamReader = inputFactory.createXMLStreamReader(fin);
            return staxBuilder.build(streamReader);
        } catch (XMLStreamException ex) {
            throw new RuntimeException(ex);
//        } catch (IOException ex) {
//            throw new RuntimeException(ex);
        }
    }

    private Element getChild(Element el, String name) {
        for (Object o : el.getChildren()) {
            if (o instanceof Element) {
                Element elChild = (Element) o;
                if (elChild.getName().equals(name)) {
                    return elChild;
                }
            }
        }
        return null;
    }

    private String getValueOf(Element el, String name) {
        Element elChild = getChild(el, name);
        if (elChild == null) {
            return null;
        } else {
            return getValue(elChild);
        }
    }

    private String getValue(Element el) {
        Attribute att = el.getAttribute("value");
        String v;
        if (att != null) {
            v = att.getValue();
        } else {
            v = XmlHelper.getAllText(el);
        }
        if (v == null) {
            return null;
        }
        return v.trim();
    }

    private void parseWebResourcesFromHtml(Element elHead, HtmlPage meta, Path webPath) {
        for (Element elHeadTag : children(elHead)) {
            if (elHeadTag.getName().equals("title")) {
                meta.setTitle(getValueOf(elHead, "title"));
            } else {
                WebResource wr = new WebResource(webPath);
                meta.getWebResources().add(wr);
                wr.setTag(elHeadTag.getName());
                wr.setBody(elHeadTag.getText());
                for (Object oAtt : elHeadTag.getAttributes()) {
                    Attribute att = (Attribute) oAtt;
                    wr.getAtts().put(att.getName(), att.getValue());
                }
            }
        }
    }

    private List<org.jdom.Element> children(org.jdom.Element e2) {
        List list = e2.getChildren();
        List<org.jdom.Element> els = new ArrayList<>();
        for (Object o : list) {
            els.add((org.jdom.Element) o);
        }
        return els;
    }
}
