/*
 * Copyright 2013 McEvoy Software Ltd.
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
package io.milton.cloud.server.text;

import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.FileResource;
import io.milton.cloud.server.web.RenderFileResource;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.Resource;
import java.io.ByteArrayInputStream;
import java.util.Iterator;
import java.util.List;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.HTMLElements;
import net.htmlparser.jericho.Segment;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.StartTagType;
import net.htmlparser.jericho.Tag;

/**
 *
 * @author brad
 */
public class TextFromHtmlService {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TextFromHtmlService.class);

    public String generateTextFromHtml(String html) {
        try {
            Source source = new Source(new ByteArrayInputStream(html.getBytes("UTF-8")));
            source.fullSequentialParse();
            Processor processor = new Processor(source, true);
            return processor.toString();
        } catch (Exception e) {
            log.error("Failed to generate text from HTML", e);
            return null;
        }
    }

    public String getText(List<Resource> list) {
        System.out.println("getText: " + list.size());
        StringBuilder sb = new StringBuilder();
        try {
            for (Resource r : list) {
                appendText(sb, r);
            }
        } catch (NotAuthorizedException | BadRequestException ex) {
            throw new RuntimeException(ex);
        }
        return sb.toString();
    }

    public String getText(Resource r) {
        StringBuilder sb = new StringBuilder();
        try {
            appendText(sb, r);
        } catch (NotAuthorizedException | BadRequestException ex) {
            throw new RuntimeException(ex);
        }
        return sb.toString();
    }

    private void appendText(StringBuilder sb, Resource r) throws NotAuthorizedException, BadRequestException {
        if (r instanceof RenderFileResource) {
            appendText(sb, (RenderFileResource) r);
        } else if (r instanceof FileResource) {
            appendText(sb, (FileResource) r);
        } else if (r instanceof CommonCollectionResource) {
            CommonCollectionResource col = (CommonCollectionResource) r;
            appendText(sb, col);
        }
    }

    private void appendText(StringBuilder sb, CommonCollectionResource r) throws NotAuthorizedException, BadRequestException {
        for (Resource child : r.getChildren()) {
            appendText(sb, child);
        }
    }

    private void appendText(StringBuilder sb, RenderFileResource r) {
        String title = r.getTitle();
        String body = r.getBody();
        if( title != null ) {
            sb.append(title).append("\n");
        } else {
            sb.append(r.getName()).append("\n");
        }
        if( body != null ) {
            String s = generateTextFromHtml(body);
            sb.append(s).append("\n");
        }
    }

    private void appendText(StringBuilder sb, FileResource r) {
        RenderFileResource rfr = r.getHtml();
        if( rfr != null ) {
            appendText(sb, rfr);
        } else {
            sb.append(r.getName()).append("\n");
        }
    }

    private final class Processor {

        private final Segment rootSegment;
        private final boolean excludeNonHTMLElements;

        public Processor(final Segment segment, final boolean excludeNonHTMLElements) {
            this.rootSegment = segment;
            this.excludeNonHTMLElements = excludeNonHTMLElements;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder(rootSegment.length());
            boolean blankLine = false;
            boolean skipTagContent = false;
            for (Iterator<Segment> nodeIterator = rootSegment.getNodeIterator(); nodeIterator.hasNext();) {
                Segment segment = nodeIterator.next();
                if (segment instanceof Tag) {
                    final Tag tag = (Tag) segment;
                    if (tag.getTagType().isServerTag()) {
                        continue;
                    }
                    if (tag.getTagType() == StartTagType.NORMAL) {
                        if (tag.getName().equals(HTMLElementName.A)) {
                            skipTagContent = true;
                            Element linkElement = tag.getElement();
                            String href = linkElement.getAttributeValue("href");
                            if (href == null) {
                                continue;
                            }
                            // A element can contain other tags so need to extract the text from it:
                            String label = linkElement.getContent().getTextExtractor().toString();
                            sb.append(label + " <" + href + '>');
                            blankLine = false;
                        } else if (tag.getName().equals(HTMLElementName.TITLE)) {
                            skipTagContent = true;
                        } else if (tag.getName().equals(HTMLElementName.BR) || !HTMLElements.getInlineLevelElementNames().contains(tag.getName())) {
                            skipTagContent = false;
                            if (!blankLine) {
                                sb.append("\n");
                            }
                            blankLine = true;
                        }
                    } else {
                        skipTagContent = false;
                    }
                } else {
                    if (!skipTagContent) {
                        String s = segment.toString().trim();
                        if (s.length() > 0) {
                            sb.append(s);
                            blankLine = false;
                        }
                    }
                }
            }
            //final String decodedText = CharacterReference.decodeCollapseWhiteSpace(sb);
            return sb.toString();
        }
    }
}
