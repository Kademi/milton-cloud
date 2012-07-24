
package io.milton.cloud.server.web.alt;

import com.bradmcevoy.utils.XmlUtils2;
import java.io.InputStream;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.output.DOMOutputter;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xml.sax.SAXException;

public class MyTextRenderer extends ITextRenderer {
    public void setDocument(InputStream in, String url) throws SAXException {
        try {
            XmlUtils2 utils = new XmlUtils2();
            Document doc = null;
            doc = utils.getJDomDocument( in );
            DOMOutputter outputter = new DOMOutputter();
            org.w3c.dom.Document w3cDoc = outputter.output( doc );
            this.setDocument( w3cDoc, url );
        } catch( JDOMException ex ) {
            throw new RuntimeException( ex );
        }
        
    }
}
