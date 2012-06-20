/*
 * Copyright 2012 McEvoy Software Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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

import io.milton.common.Path;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;

/**
 *
 * @author brad
 */
public class HtmlFormatterTest {
    

    /**
     * Test of update method, of class HtmlFormatter.
     */
    @Test
    public void testUpdate() {
        HtmlFormatter formatter = new HtmlFormatter();
        HtmlPage page = new SimpleHtmlPage();
        page.setTitle("XXX");
        page.setBody("YYY");
        page.getBodyClasses().add("normal");
        page.getBodyClasses().add("module");
        page.getWebResources().add(newScriptTag());
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        formatter.update(page, bout);
        String html = bout.toString();
        System.out.println(html);
        assertTrue(html.contains("normal"));
        assertTrue(html.contains("module"));
        assertTrue(html.contains("YYY"));
        assertTrue(html.contains("XXX"));
        assertTrue(html.contains("/js/utils.js"));
    }

    private WebResource newScriptTag() {
        WebResource wr = new WebResource(Path.root);
        wr.setTag("script");
        wr.getAtts().put("src", "/js/utils.js");
        return wr;
    }
    
    public class SimpleHtmlPage implements HtmlPage {

        List<String> bodyClasses = new ArrayList<>();
        List<WebResource> webResources = new ArrayList<>();
        String title;
        String body;
        String template;
        
        
        @Override
        public List<String> getBodyClasses() {
            return bodyClasses;
        }

        @Override
        public List<WebResource> getWebResources() {
            return webResources;
        }

        @Override
        public String getBody() {
            return body;
        }

        @Override
        public void setBody(String b) {
            this.body = b;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public void setTitle(String t) {
            this.title = t;
        }

        @Override
        public InputStream getInputStream() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
    }
}
