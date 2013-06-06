
package io.milton.cloud.server.web.alt;

import io.milton.sync.HttpUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;
import org.xhtmlrenderer.pdf.ITextRenderer;

public class MyTextRenderer extends ITextRenderer {
    
    @Override
    public void setDocument(String uri) {
        HttpClient httpClient = new DefaultHttpClient();
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        HttpUriRequest m = new HttpGet(uri);
        try {
            int result = HttpUtils.executeHttpWithStatus(httpClient, m, bout);
            System.out.println("result=" + result);
            ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
            setDocument(bin, uri);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public void setDocument(InputStream in, String url) {
        Tidy tidy = new Tidy();
        tidy.setOnlyErrors(true);
        tidy.setOutputEncoding("utf-8");
        tidy.setQuiet(true);
        tidy.setXHTML(true);
        Document doc = tidy.parseDOM(in, null);
        this.setDocument( doc, url );
        
        //        try {
        //            this.getSharedContext().setBaseURL(url);
        //            ByteArrayOutputStream bout = new ByteArrayOutputStream();
        //            IOUtils.copy(in, bout);
        //            String s = bout.toString("UTF-8");
        //            setDocumentFromString(s);
        //        } catch (IOException ex) {
        //            throw new RuntimeException(ex);
        //        }
        //
        //        try {
        //            XmlUtils2 utils = new XmlUtils2();
        //            Document doc = null;
        //            doc = utils.getJDomDocument( in );
        //            DOMOutputter outputter = new DOMOutputter();
        //            org.w3c.dom.Document w3cDoc = outputter.output( doc );
        //            this.setDocument( w3cDoc, url );
        //        } catch( JDOMException ex ) {
        //            throw new RuntimeException( ex );
        //        }
        
    }
}
