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
package io.milton.cloud.server.mail;

import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.db.BaseEmailJob;
import io.milton.cloud.server.db.EmailItem;
import io.milton.cloud.server.manager.CurrentRootFolderService;
import io.milton.cloud.server.web.TemplatedHtmlPage;
import io.milton.vfs.db.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.Session;

import static io.milton.context.RequestContext._;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.HTMLElements;
import net.htmlparser.jericho.Segment;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.StartTagType;
import net.htmlparser.jericho.Tag;
import org.hibernate.HibernateException;
import org.mvel2.templates.TemplateRuntime;

/**
 *
 * @author brad
 */
public class BatchEmailService {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(BatchEmailService.class);

    public BatchEmailService() {
    }


    /**
     * Generate EmailItem records to send. These will be sent by a seperate
     * process
     * 
     * @param j
     * @param directRecipients
     * @param callback - may be null, otherwise is called just prior to generating the email content
     * @param session
     * @throws IOException 
     */
    public void generateEmailItems(BaseEmailJob j, List<BaseEntity> directRecipients, BatchEmailCallback callback, Session session) throws IOException {
        if (j.getGroupRecipients() == null && directRecipients.isEmpty()) {
            log.warn("No recipients!! For job: " + j.getId());
            return;
        }
        Set<Profile> profiles = new HashSet<>();
        for (BaseEntity e : directRecipients) {
            if (e != null) {
                append(e, profiles);
            } else {
                log.warn("Found null recipient, ignoring");
            }
        }
        log.info("recipients: " + profiles.size());
        if (j.getEmailItems() == null) {
            j.setEmailItems(new ArrayList<EmailItem>());
        }
        for (Profile p : profiles) {
            sendSingleEmail(j, p, callback, session);
        }
        session.save(j);
    }

    private void append(BaseEntity g, final Set<Profile> profiles) {
        log.info("append: group: " + g.getId() + " - " + g.getId());

        final VfsVisitor visitor = new AbstractVfsVisitor() {
            @Override
            public void visit(Group r) {
                if (r.getGroupMemberships() != null) {
                    for (GroupMembership m : r.getGroupMemberships()) {
                        append(m.getMember(), profiles);
                    }
                }
            }

            @Override
            public void visit(Profile p) {
                profiles.add(p);
            }
        };
        g.accept(visitor);
    }

    public void sendSingleEmail(BaseEmailJob j, Profile recipientProfile, BatchEmailCallback callback, Session session) throws HibernateException, IOException {
        String from = "sys@" + _(CurrentRootFolderService.class).getPrimaryDomain();
        String replyTo = j.getFromAddress();
        if (replyTo == null) {
            replyTo = from;
        }
        Date now = _(CurrentDateService.class).getNow();
        EmailItem i = new EmailItem();
        i.setCreatedDate(now);
        i.setFromAddress(from);        
        i.setReplyToAddress(replyTo);

        // Templating requires a HtmlPage to represent the template        
        String html = generateHtml(j, recipientProfile, callback);
        i.setHtml(html);
        String text = generateTextFromHtml(html);
        i.setText(text);
        i.setJob(j);
        i.setRecipient(recipientProfile);
        i.setRecipientAddress(recipientProfile.getEmail());
        i.setSendStatusDate(now);
        String subject = j.getSubject();
        if( subject == null || subject.trim().length() == 0 ) {
            subject = "Auto mail from " + j.getOrganisation().getFormattedName();
        }
        i.setSubject(subject);
        
        j.getEmailItems().add(i);
        session.save(i);
        log.info("Created email item: " + i.getId() + " to " + recipientProfile.getEmail());
    }

    private String generateTextFromHtml(String html) {
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
    
    
    private String generateHtml(final BaseEmailJob j, final Profile p, BatchEmailCallback callback) throws IOException {
        return generateHtml(j.getThemeSite(), j.getHtml(), p, callback);
    }
    
    public String generateHtml(Website themeSite, String html, final Profile p, BatchEmailCallback callback) throws IOException {
        Map localVars = new HashMap();
        localVars.put("profile", p);
        
        if (themeSite != null) {
            Branch b = themeSite.liveBranch();
            if (b != null) {
                WebsiteRootFolder websiteRootFolder = new WebsiteRootFolder(_(ApplicationManager.class), themeSite, b);
                localVars.put("website", websiteRootFolder);
            }
        }

        String template = html == null ? "" : html;
        if (callback != null) {
            template = callback.beforeSend(p, template, localVars);
        }
               
        final String bodyHtml = TemplateRuntime.eval(template, localVars).toString();
        
        Map<String, String> params = new HashMap<>();

        if (themeSite != null) {
            Branch b = themeSite.liveBranch();
            if (b != null) {
                WebsiteRootFolder websiteRootFolder = new WebsiteRootFolder(_(ApplicationManager.class), themeSite, b);
                TemplatedHtmlPage page = new TemplatedHtmlPage("email", websiteRootFolder, "email/genericEmail", "Email") {
                    @Override
                    protected Map<String, Object> buildModel(Map<String, String> params) {
                        Map<String, Object> map = super.buildModel(params);                        
                        map.put("bodyHtml", bodyHtml);
                        return map;
                    }
                };

                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                try {
                    page.sendContent(bout, null, params, null); 
                } catch (IOException | NotAuthorizedException | BadRequestException | NotFoundException ex) {
                    throw new RuntimeException(ex);
                }
                return bout.toString("UTF-8");
            }
        } 
        log.info(" no theme, cant do templating");
        return bodyHtml;        
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
