package com.bradmcevoy.utils;

import com.bradmcevoy.common.Path;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmlUtils2 {

    private static final Logger log = LoggerFactory.getLogger(XmlUtils2.class);
    private static final Properties entities;
    private static List<String> knownSystemIds = Arrays.asList(
            "xhtml-lat1.ent",
            "xhtml-special.ent",
            "xhtml-symbol.ent",
            "xhtml1-strict.dtd");

    static {
        entities = new Properties();
        InputStream is = XmlUtils2.class.getResourceAsStream("entities.properties");
        try {
            entities.load(is);
        } catch (IOException ex) {
            System.out.println("Failed to load HTML entities from file entities.properties which should be in package com.bradmcevoy.utils");
            ex.printStackTrace();
        }
    }

    public static List<org.jdom.Element> children(org.jdom.Element e2, String elementName) {
        List list = e2.getChildren(elementName);
        List<org.jdom.Element> els = new ArrayList<org.jdom.Element>();
        for (Object o : list) {
            els.add((org.jdom.Element) o);
        }
        return els;
    }
    private DocumentBuilderFactory mDomFactory = null;
    private DocumentBuilder mDomBuilder = null;

    public org.jdom.Document getJDomDocument(File f) throws FileNotFoundException, JDOMException {
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(f);
            BufferedInputStream bufin = new BufferedInputStream(fin);
            return getJDomDocument(bufin);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            FileUtils.close(fin);
        }
    }

    public org.jdom.Document getJDomDocument(InputStream fin) throws JDOMException {
        try {
            SAXBuilder builder = new SAXBuilder();
            builder.setExpandEntities(false);
            builder.setEntityResolver(new MyEntityResolver());
            //builder.setFeature(  "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            return builder.build(fin);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public org.jdom.Document getJDomDocument(String s) throws JDOMException {
        ByteArrayInputStream bin = null;
        try {
            bin = new ByteArrayInputStream(s.getBytes("UTF-8"));
            return getJDomDocument(bin);
            //        log.info("getJdomDocument: String");
            //        try {
            //            StringReader sr = new StringReader(s);
            //            SAXBuilder builder = new SAXBuilder();
            //            builder.setExpandEntities(false);
            //            builder.setValidation(false);
            //            builder.setEntityResolver(new MyEntityResolver());
            //            return builder.build(sr);
            //        } catch (IOException ex) {
            //        }
            //        } finally {
            //        }
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        } finally {
            try {
                bin.close();
            } catch (IOException ex) {
                log.error("ex", ex);
            }
        }
    }

    /**
     * --- resolveEntity: -//W3C//DTD XHTML 1.0 Strict//EN -
     * http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd --- resolveEntity:
     * -//W3C//ENTITIES Latin 1 for XHTML//EN -
     * http://www.w3.org/TR/xhtml1/DTD/xhtml-lat1.ent --- resolveEntity:
     * -//W3C//ENTITIES Symbols for XHTML//EN -
     * http://www.w3.org/TR/xhtml1/DTD/xhtml-symbol.ent --- resolveEntity:
     * -//W3C//ENTITIES Special for XHTML//EN -
     * http://www.w3.org/TR/xhtml1/DTD/xhtml-special.ent
     */
    public class MyEntityResolver implements EntityResolver {

        @Override
        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
            //System.out.println("resolveEntoty: " + systemId);
            Path p = Path.path(systemId);
            if (knownSystemIds.contains(p.getName())) {
                if (log.isTraceEnabled()) {
                    log.trace("using embedded resource: " + p.getName());
                }
                return new InputSource(this.getClass().getResourceAsStream("/" + p.getName()));
            } else {
                log.warn("entity resource not found: publicId: " + publicId + " systemId: " + systemId);
                return null;
            }
        }
    }

    public String getXml(org.jdom.Document doc) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        transformDocument(out, doc);
        return out.toString();
    }

    public String getXml(org.jdom.Element el) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        transformDocument(out, el);
        return out.toString();
    }

    public void saveXMLDocument(OutputStream out, org.jdom.Document doc) {
        Format format = Format.getPrettyFormat();
        XMLOutputter outputter = new XMLOutputter(format);
        try {
            outputter.output(doc, out);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void transformDocument(PrintWriter pw, org.jdom.Document doc) {
        Format f = Format.getPrettyFormat();
        f.setTextMode(Format.TextMode.PRESERVE);
        XMLOutputter outputter = new XMLOutputter(f);
        try {
            outputter.output(doc, pw);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void transformElement(PrintWriter pw, org.jdom.Element el) {
        Format f = Format.getPrettyFormat();
        f.setTextMode(Format.TextMode.PRESERVE);
        XMLOutputter outputter = new XMLOutputter(f);
        try {
            outputter.output(el, pw);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void transformDocument(OutputStream out, org.jdom.Document doc) {
        Format f = Format.getPrettyFormat();
        f.setTextMode(Format.TextMode.PRESERVE);
        XMLOutputter outputter = new XMLOutputter(f);
        try {
            outputter.output(doc, out);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void transformDocument(OutputStream out, org.jdom.Element el) {
        Format f = Format.getPrettyFormat();
        f.setTextMode(Format.TextMode.PRESERVE);
        XMLOutputter outputter = new XMLOutputter(f);
        try {
            outputter.output(el, out);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public synchronized DocumentBuilderFactory domBuilderFactory() {
        if (mDomFactory == null) {
            mDomFactory = DocumentBuilderFactory.newInstance();
            mDomFactory.setValidating(false);
            mDomFactory.setExpandEntityReferences(false);
        }
        return mDomFactory;
    }

    public synchronized DocumentBuilder domBuilder() {
        try {
            if (mDomBuilder == null) {
                mDomBuilder = domBuilderFactory().newDocumentBuilder();
            }
            return mDomBuilder;
        } catch (javax.xml.parsers.ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized org.w3c.dom.Document getDomDocument() {
        return domBuilder().newDocument();
    }

    public synchronized org.w3c.dom.Document getDomDocument(File file) throws java.io.FileNotFoundException, SAXException {
        try {
            FileReader xmlReader = new FileReader(file);
            org.xml.sax.InputSource xmlSource = new org.xml.sax.InputSource(xmlReader);
            return domBuilder().parse(xmlSource);
        } catch (java.io.FileNotFoundException fnf) {
            throw fnf;
        } catch (java.io.IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public synchronized org.w3c.dom.Document getDomDocument(String xml) throws SAXParseException {
        try {
            domBuilder();
            synchronized (mDomBuilder) {
                BufferedReader xmlReader = new BufferedReader(new StringReader(xml));
                org.xml.sax.InputSource xmlSource = new org.xml.sax.InputSource(xmlReader);
                return domBuilder().parse(xmlSource);
            }
        } catch (SAXParseException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized org.w3c.dom.Document getDomDocument(org.xml.sax.InputSource is) {
        try {
            return domBuilder().parse(is);
        } catch (org.xml.sax.SAXException se) {
            throw new RuntimeException(se);
        } catch (java.io.IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public synchronized org.w3c.dom.Document getDomDocument(InputStream is) throws SAXException {
        try {
            return domBuilder().parse(is);
        } catch (java.io.IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public static ElementList getElements(Element el, String tagName) {
        NodeList nodes = el.getChildNodes();
        ElementList elements = getElements(nodes);
        Iterator<Element> it = elements.iterator();
        while (it.hasNext()) {
            Element elChild = it.next();
            if (!elChild.getNodeName().equals(tagName)) {
                it.remove();
            }
        }
        return elements;
    }

    public static Element getElement(Element el, String tagName) {
        for (Element elChild : getElementList(el.getChildNodes())) {
            if (elChild.getNodeName().equals(tagName)) {
                return elChild;
            }
        }
        return null;
    }

    public static ElementList getElements(NodeList nodes) {
        return new ElementList(nodes);
    }

    public static Collection<Node> getList(NodeList nodes) {
        ArrayList arr = new ArrayList();
        for (int i = 0; i < nodes.getLength(); i++) {
            arr.add(nodes.item(i));
        }
        return arr;
    }

    public static Collection<Element> getElementList(NodeList nodes) {
        return getElementList(nodes, null);
    }

    public static Collection<Element> getElementList(NodeList nodes, String tagName) {
        ArrayList<Element> arr = new ArrayList<Element>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if (n instanceof Element) {
                Element e = (Element) n;
                if (tagName == null || tagName.equals(e.getNodeName())) {
                    arr.add((Element) n);
                }
            }
        }
        return arr;
    }

    public void saveXMLDocument(File file, org.jdom.Document doc) throws FileNotFoundException {
        if (file.exists()) {
            file.delete();
        }
        FileOutputStream fos;
        fos = new FileOutputStream(file);
        try {
            XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
            try {
                outputter.output(doc, fos);
            } catch (IOException e) {
                throw new RuntimeException(file.getAbsolutePath(), e);
            }
        } finally {
            try {
                fos.flush();
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException ex) {
                // ignore
            }
        }
    }

    public void saveXMLDocument(File file, Document doc) throws FileNotFoundException {
        if (file.exists()) {
            file.delete();
        }
        FileOutputStream fos;
        fos = new FileOutputStream(file);
        try {
            saveXMLDocument(fos, doc);
        } finally {
            try {
                fos.flush();
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException ex) {
                // ignore
            }
        }
    }

    public void saveXMLDocument(OutputStream out, Document doc) {
        transformDocument(out, doc);
    }

    public void transformDocument(Writer writer, Document doc) {
        Node node = doc.getDocumentElement();
        StreamResult result = new StreamResult(writer);
        transformDocument(result, node);
    }

    public void transformDocument(OutputStream os, Document doc) {
        transformDocument(os, doc.getDocumentElement());
    }

    public void transformDocument(OutputStream os, Node node) {
        transformDocument(os, node, false);
    }

    public void transformDocument(OutputStream os, Node node, boolean omitDeclaration) {
        StreamResult result = new StreamResult(os);
        transformDocument(result, node, omitDeclaration);
    }

    public void transformDocument(StreamResult result, Node node) {
        transformDocument(result, node, false);
    }

    public void transformDocument(StreamResult result, Node node, boolean omitDeclaration) {
        Transformer transformer;
        // Use a Transformer for output
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
//        transformerFactory.setAttribute("indent-number", new Integer(4));
        try {
            transformer = transformerFactory.newTransformer();
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException(e);
        }
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "3");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, omitDeclaration ? "yes" : "no");
        DOMSource source = new DOMSource(node);
        // transform source into result will do save
        try {
            transformer.transform(source, result);
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    public String getChildXml(Node el) {
        StringBuilder sb = new StringBuilder();
        NodeList nl = el.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (i != 0) {
                sb.append("\n");
            }
            sb.append(getXml(n).trim());
        }
        return sb.toString();
    }

    public String getXml(Node el) {
        //return el.getNodeValue();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        transformDocument(os, el);
        String xml = os.toString();
        int start = xml.indexOf("?>");
        xml = xml.substring(start + 2);
        return xml;
    }

    /**
     * parses the given text or xml fragment and appends to the given element
     *
     * is a fairly inefficient operation
     *
     * returns the parent
     */
    public Element append(Element parent, String textOrXml) throws SAXParseException {
        StringBuffer xml = new StringBuffer("<a>").append(textOrXml).append("</a>");
        Document docChild = getDomDocument(xml.toString());
        NodeList childNodes = docChild.getDocumentElement().getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node n = childNodes.item(i);
            parent.appendChild(parent.getOwnerDocument().importNode(n, true));
        }
        return parent;
    }

    public static String expandHtmlEntities(String s) {
        for (Object o : entities.keySet()) {
            String entity = (String) o;
            String val = entities.getProperty(entity);
            s = s.replaceAll(entity, val);
        }
        return s;
    }

    public static String htmlEntityEncode(String s) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9') {
                buf.append(c);
            } else {
                buf.append("&#" + (int) c + ";");
            }
        }
        return buf.toString();
    }

    public Element getChild(Element el, String name) {
        NodeList list = el.getElementsByTagName(name);
        if (list == null || list.getLength() == 0) {
            return null;
        }
        Element elChild = (Element) list.item(0);
        return elChild;
    }

    public String getText(Element el) {
        StringBuilder sb = new StringBuilder();
        NodeList nodes = el.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if (n instanceof Text) {
                Text t = (Text) n;
                sb.append(t.getTextContent());
            }
        }
        return sb.toString();
    }

    public boolean getBooleanAttribute(Element el, String attName) {
        String s = el.getAttribute(attName);
        if (s == null) {
            return false;
        }
        return (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("yes"));
    }

    public String getTextData(Element el) {
        Text c = getChildOfType(el, Text.class);
        if (c == null) {
            return null;
        }
        return c.getData();
    }

    public String getCData(Element el) {
        CDATASection c = getChildOfType(el, CDATASection.class);
        if (c == null) {
            return null;
        }
        return c.getData();
    }

    public <T> T getChildOfType(Element el, Class<T> c) {
        Node n = el.getFirstChild();
        while (n != null) {
            if (c.isAssignableFrom(n.getClass())) {
                return (T) n;
            }
            n = n.getNextSibling();
        }
        return null;
    }

    public static Object restoreObject(org.jdom.Element el, Object parent) {
        String className = el.getAttributeValue("class");
        if (className == null || className.length() == 0) {
            throw new IllegalArgumentException("class is empty");
        }
        return ReflectionUtils.create(className, parent, el);
    }

    public static Object restoreObject(Element el, Object parent) {
        String className = el.getAttribute("class");
        if (className == null || className.length() == 0) {
            throw new IllegalArgumentException("class is empty");
        }
        return ReflectionUtils.create(className, parent, el);
    }

    public static void process(Node n, XmlNodeOperator r) {
        r.process(n);
        Node child = n.getFirstChild();
        while (child != null) {
            process(child, r);
            child = child.getNextSibling();
        }
    }

    public static interface XmlNodeOperator {

        void process(Node n);
    }
}
