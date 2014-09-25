package com.bradmcevoy.xml;

import com.bradmcevoy.utils.XmlUtils2;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.jdom.CDATA;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.EntityRef;
import org.jdom.JDOMException;
import org.jdom.Text;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.HtmlXmlOutputter;
import org.xml.sax.SAXParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class XmlHelper {

    private static final Logger log = LoggerFactory.getLogger(XmlHelper.class);
    public static final String DTD = "<!DOCTYPE root PUBLIC '-//MyDT//DTD MYDTD-XML//MYDTD' 'xhtml-lat1.ent'>";

    private XmlHelper() {
    }

    public static String getAllText(Element el) {
        //Format format = Format.getPrettyFormat();
        Format format = Format.getRawFormat();


        List children = el.getContent();
        if (children == null) {
            return "";
        }
//        return outputter.outputString(children);


        //XMLOutputter outputter = new XMLOutputter(format);
        HtmlXmlOutputter outputter = new HtmlXmlOutputter(format);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(bout);
        for (Object o : children) {
            if (o instanceof CDATA) {
                CDATA cd = (CDATA) o;
                String s = cd.getText();
                pw.print(s);
            } else if (o instanceof org.jdom.Element) {
                String xml = toString((Element) o, format);
                pw.print(xml);
            } else if (o instanceof org.jdom.Text) {
                try {
                    Text t = (Text) o;
                    outputter.output(t, pw);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            } else if( o instanceof org.jdom.EntityRef) {
                org.jdom.EntityRef ref = (EntityRef) o;
                // Note that value entities (eg  &#62; ) seem to be translated to textual
                // values before we get here and do not produce a EntityRef
                pw.print("&" + ref.getName() + ";");
            } else {
                if (o != null) {
                    log.warn("unsupported XML type: " + o.getClass());
                }
            }
        }
        pw.flush();
        try {
            String s = bout.toString("UTF-8");
            return s;
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String toString(org.jdom.Element el) {
        return toString(el, Format.getPrettyFormat());
    }

    public static String toString(org.jdom.Element el, Format format) {
        HtmlXmlOutputter outputter = new HtmlXmlOutputter(format);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            outputter.output(el, out);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        try {
            String s = out.toString("UTF-8");
            return s;
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("Couldnt output to UTF-8", ex);
        }
    }

    /**
     * Convert to a list of Element objects if possible, otherwise a
     * CDATA object
     *
     * @param text
     * @return
     */
    public static List getContent(String text) {
        String xml = "<?xml version='1.0' encoding='UTF-8'?>" + DTD + "<a>" + text + "</a>";
        Document doc;
        try {
			XmlUtils2 utils = new XmlUtils2();
			doc = utils.getJDomDocument(new ByteArrayInputStream(xml.getBytes("UTF-8")));
			
//			System.out.println("doc: " + toString(doc.getRootElement()));
//            StringReader sr = new StringReader(xml);
//            SAXBuilder builder = new SAXBuilder();
//            //builder.setExpandEntities( true );
//            builder.setExpandEntities(false);
//            doc = builder.build(sr);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch(Throwable e) {
            if( e instanceof SAXParseException || e instanceof JDOMException ) {
//				e.printStackTrace();
                log.debug("caught invalid markup, wrapping with CDATA", e);
                List list = new ArrayList();
                list.add(new CDATA(text));
                return list;
            } else {
                throw new RuntimeException(e);
            }
        }
        List list = doc.getRootElement().getContent();
        return detach(list);
    }

    public static org.jdom.Document getJDomDocument(String s) throws JDOMException {
        try {
            StringReader sr = new StringReader(s);
            SAXBuilder builder = new SAXBuilder();
            builder.setExpandEntities(false);
            return builder.build(sr);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static List<Content> detach(List list) {
        List<Content> contents = contentList(list);
        for (Content c : contents) {
            c.detach();
        }
        return contents;
    }

    private static List<Content> contentList(List list) {
        List<Content> contents = new ArrayList<Content>();
        for (Object o : list) {
            if (o instanceof Content) {
                Content c = (Content) o;
                contents.add(c);
            }
        }
        return contents;
    }
}
