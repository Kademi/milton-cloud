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

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import io.milton.cloud.server.web.RenderFileResource;
import io.milton.common.Path;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import javax.xml.stream.XMLStreamException;
import net.htmlparser.jericho.Segment;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author brad
 */
public class HtmlTemplateParser {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(HtmlTemplateParser.class);
    public static String CDATA_START = "<![CDATA[";
    public static String CDATA_END = "]]>";
    private static long time;
    private HtmlFormatter htmlFormatter = new HtmlFormatter();
    private final ConcurrentMap<String, ParsedResource> cache; // cache of ParsedResources, keyed on hash, for FileResource's

    public HtmlTemplateParser() {
        cache = new ConcurrentLinkedHashMap.Builder()
                .maximumWeightedCapacity(1000)
                .build();
    }

    public void parse(RenderFileResource meta) throws IOException, XMLStreamException {
        System.out.println("parse: " + meta.getName());
        String hash = meta.getHash();
        ParsedResource pr = cache.get(hash);
        if (pr == null) {
            try (InputStream fin = meta.getInputStream()) {
                if (fin != null) {
                    BufferedInputStream bufIn = new BufferedInputStream(fin);
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    IOUtils.copy(bufIn, bout);
                    String sourceXml = bout.toString("UTF-8");
                    pr = new ParsedResource(sourceXml);
                    cache.putIfAbsent(hash, pr);
                }
            }
        }
        meta.setBody(pr.body);
        meta.setTitle(pr.title);
        meta.getBodyClasses().clear();
        meta.getBodyClasses().addAll(pr.bodyClasses);
        meta.getWebResources().clear();        
        meta.getWebResources().addAll(copy(pr.webResources));

    }

    
    /**
     * Parse the file associated with the meta, extracting webresources, body
     * class attributes and the template, and setting that information on the
     * meta object
     *
     * @param meta
     */
    public void parse(HtmlPage meta) throws IOException, XMLStreamException {
        log.info("parse: " + meta.getSource() + " - " + meta.getClass() + " accumulated time=" + time + "ms");
        long tm = System.currentTimeMillis();

        try (InputStream fin = meta.getInputStream()) {
            if (fin != null) {
                BufferedInputStream bufIn = new BufferedInputStream(fin);
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                IOUtils.copy(bufIn, bout);
                String sourceXml = bout.toString("UTF-8");
                ParsedResource pr = new ParsedResource(sourceXml);
                meta.setBody(pr.body);
                meta.setTitle(pr.title);
                meta.getBodyClasses().clear();
                meta.getBodyClasses().addAll(pr.bodyClasses);
                meta.getWebResources().clear();
                meta.getWebResources().addAll(pr.webResources);
            }
        }

        tm = System.currentTimeMillis() - tm;
        time += tm;
    }


    public ParsedResource parse(String sourceXml, String hash) throws IOException, XMLStreamException {
        ParsedResource pr = cache.get(hash);
        if (pr == null) {
            pr = new ParsedResource(sourceXml);
            cache.putIfAbsent(hash, pr);
        }
        return pr;
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

    public static String getContent(net.htmlparser.jericho.Element el) {
        if (el == null) {
            return null;
        }
        Segment seg = el.getContent();
        if (seg == null) {
            return null;
        }
        String s = seg.toString().trim();
        s = stripCDATA(s);
        return s;
    }

    public static String stripCDATA(String s) {
        if (s.startsWith(CDATA_START)) {
            s = s.substring(CDATA_START.length());
        }
        if (s.endsWith(CDATA_END)) {
            s = s.substring(0, s.length() - CDATA_END.length());
        }
        return s.trim();


    }

    private Collection<? extends WebResource> copy(List<WebResource> webResources) {
        List<WebResource> copy = new ArrayList<>();
        for( WebResource wr : webResources) {
            copy.add(wr.duplicate());
        }
        return copy;
    }
}
