/*
 * Copyright 2012 McEvoy Software Ltd.
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
package io.milton.cloud.util;

import com.bradmcevoy.xml.XmlHelper;
import io.milton.cloud.server.web.templating.StaxBuilder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 *
 * @author brad
 */
public class JDomUtils {
    
    public static org.jdom.Document getJDomDocument(InputStream fin) throws XMLStreamException {
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
    }

    
    
    public static List<org.jdom.Element> children(org.jdom.Element e2) {
        if( e2 == null ) {
            return Collections.EMPTY_LIST;
        }
        List list = e2.getChildren();
        List<org.jdom.Element> els = new ArrayList<>();
        for (Object o : list) {
            els.add((org.jdom.Element) o);
        }
        return els;
    }
    
    public static String getValue(Element el) {
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
    public static  Element getChild(Element el, String name) {
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

    public static  String getValueOf(Element el, String name) {
        Element elChild = getChild(el, name);
        if (elChild == null) {
            return null;
        } else {
            return JDomUtils.getValue(elChild);
        }
    }    
    
    /**
     * Just outputs the content of the element, not the element itslef
     * 
     * @param out
     * @param el 
     */
    public static void transformDocument(OutputStream out, org.jdom.Element el) {
        Format f = Format.getPrettyFormat();
        f.setTextMode(Format.TextMode.PRESERVE);
        f.setOmitDeclaration(true);
        XMLOutputter outputter = new XMLOutputter(f);
        try {
            outputter.output(el.getContent(), out);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }        
    
    public static void transformDocument(OutputStream out, org.jdom.Document doc) {
        Format f = Format.getPrettyFormat();
        f.setTextMode(Format.TextMode.PRESERVE);
        f.setOmitDeclaration(true);
        XMLOutputter outputter = new XMLOutputter(f);
        try {
            outputter.output(doc, out);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }      
}
