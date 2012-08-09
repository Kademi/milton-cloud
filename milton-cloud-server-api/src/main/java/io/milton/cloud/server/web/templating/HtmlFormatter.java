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

import io.milton.http.XmlWriter;
import java.io.ByteArrayOutputStream;

/**
 *
 * @author brad
 */
public class HtmlFormatter {

    public void update(HtmlPage r, ByteArrayOutputStream bout) {
        XmlWriter writer = new XmlWriter(bout);
        writer.setAllowNewlines(true);
        XmlWriter.Element html = writer.begin("html");
        html.writeText("\n");
        XmlWriter.Element head = html.begin("head");
        head.writeText("\n");
        if (r.getTitle() != null) {
            head.begin(null, "title", false).writeText(r.getTitle(), false).close(true);
        }
        for (WebResource wr : r.getWebResources()) {
            write(writer, wr);
        }
        writer.newLine();
        head.close(true);
        XmlWriter.Element body = html.begin("body");
        StringBuilder sb = null;
        for (String c : r.getBodyClasses()) {
            if (sb == null) {
                sb = new StringBuilder();
            } else {
                sb.append(" ");
            }
            sb.append(c);
        }
        if (sb != null) {
            body.writeAtt("class", sb.toString());
        }
        body.writeText("\n");
        if (r.getBody() != null) {
            body.writeText(r.getBody());
        }
        body.close(true);
        html.close(true);
        writer.flush();
    }

    private void write(XmlWriter writer, WebResource wr) {
        writer.writeText(wr.getRawHtml());
        writer.newLine();
    }
}
