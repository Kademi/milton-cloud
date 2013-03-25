package io.milton.cloud.server.web.templating;

import io.milton.common.Path;
import java.io.InputStream;
import java.net.URL;
import net.htmlparser.jericho.Segment;
import net.htmlparser.jericho.Source;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;



import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author brad
 */
public class HtmlTemplateParserTest {
    
    
    @Test
    public void testComments() {
        String html = "<html><head><!-- a comment  --></head><body>hi</body></html>";
        Source source = new Source(html);
        net.htmlparser.jericho.Element elHead = source.getFirstElement("head");
        for (net.htmlparser.jericho.Element wrTag : elHead.getChildElements()) {
            System.out.println(" - name: " + wrTag.getName() + " - " + wrTag.getClass() );
            Segment seg = wrTag.getContent();
            System.out.println(" - content: " + seg.getClass() );
            String s = wrTag.toString();
            System.out.println(s);
            System.out.println("");
        }
    }
    
    @Test
    public void testParse() throws Exception {
        HtmlTemplateParser parser = new HtmlTemplateParser();
        URL resource = this.getClass().getResource("/test.html");
        HtmlPage htmlPage = new ClassPathTemplateHtmlPage(resource);
        parser.parse(htmlPage, Path.root);
        
        WebResource wrScript = null;
        WebResource wrParam = null;
        
        for( WebResource wr : htmlPage.getWebResources()) {
            if( wr.getTag().equals("script")) {
                String type = wr.getAtts().get("type");
                if( type != null && type.equals("data/parameter")) {
                    wrParam = wr;
                }
                if( type != null && type.equals("text/javascript")) {
                    wrScript = wr;
                }
            }
        }
        assertNotNull(wrParam);
        assertNotNull(wrScript);
        assertEquals("//", wrScript.getBody());
        assertTrue(wrParam.getBody().startsWith("<p>Registering as a Professional"));
    }
    
    @Test
    public void testStripCdata() throws Exception {
        String s = "<![CDATA[\n" +
                    "2\n" +
                    "]]>";
        String s2 = HtmlTemplateParser.stripCDATA(s);
        assertEquals("2", s2);
                
    }

    @Test
    public void testStripCdata_Nochange() throws Exception {
        String s = "2";
        String s2 = HtmlTemplateParser.stripCDATA(s);
        assertEquals("2", s2);
                
    }
    
}
