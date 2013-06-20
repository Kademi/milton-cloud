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
package io.milton.cloud.server.apps.user;

import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.db.EmailItem;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.JsonResult;
import io.milton.cloud.server.web.RootFolder;
import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.cloud.server.web.TemplatedHtmlPage;
import io.milton.cloud.server.web.WebUtils;
import io.milton.http.FileItem;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.PostableResource;
import io.milton.vfs.db.Profile;
import java.util.Map;

import io.milton.http.Range;
import io.milton.http.exceptions.NotFoundException;
import io.milton.vfs.db.GroupMembership;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.utils.SessionManager;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.Transaction;

import static io.milton.context.RequestContext._;
import io.milton.vfs.db.Website;
import java.util.Date;

/**
 * User's own profile page, for use within a website
 *
 * @author brad
 */
public class PublicProfilePage extends TemplatedHtmlPage implements PostableResource {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ProfilePage.class);
    public static final long MAX_SIZE = 10000000l;
    private final Profile p;
    private JsonResult jsonResult;
    private List<GroupMembership> memberships;

    public PublicProfilePage(String name, Profile p, CommonCollectionResource parent) {
        super(name, parent, "user/public", "Profile");
        this.p = p;
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        try {
            String message = WebUtils.getCleanedParam(parameters, "message");
            Long emailId = generateEmailItem(message, session);
            log.info("created email item: " + emailId);
            jsonResult = new JsonResult(true);
            tx.commit();
        } catch (Exception e) {
            log.error("Exeption processing contact email", e);
            tx.rollback();
            jsonResult = new JsonResult(false, "Couldnt send email: " + e.getMessage());
        }

        return null;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        if (jsonResult != null) {
            jsonResult.write(out);
        } else {
            super.sendContent(out, range, params, contentType);
        }
    }

    private Long generateEmailItem(String message, Session session) {
        RootFolder rootFolder = WebUtils.findRootFolder(this);
        Website website;
        if (rootFolder instanceof WebsiteRootFolder) {
            WebsiteRootFolder wrf = (WebsiteRootFolder) rootFolder;
            website = wrf.getWebsite();
        } else {
            throw new RuntimeException("Cant proceess contact for an organisation");
        }

        Profile currentUser = _(SpliffySecurityManager.class).getCurrentUser();
        Profile toUser = this.getProfile();
        
        String fromAdd = currentUser.getName() + "@" + website.getDomainName();
        
        EmailItem emailItem = new EmailItem();
        Date now = _(CurrentDateService.class).getNow();
        emailItem.setCreatedDate(now);
        emailItem.setFromAddress(fromAdd);
        emailItem.setRecipient(toUser);
        emailItem.setRecipientAddress(toUser.getEmail());
        emailItem.setReplyToAddress(currentUser.getEmail());
        emailItem.setSendStatusDate(now);
        emailItem.setSender(currentUser);
        emailItem.setSubject(website.getName() + " message from " + currentUser.getFormattedName());
        emailItem.setText(message);
        session.save(emailItem);
        
        return emailItem.getId();
    }

    public Profile getProfile() {
        return p;
    }

    public String getPhotoHref() {
        if (p.getPhotoHash() != null && p.getPhotoHash().length() > 0) {
            return "/_hashes/files/" + p.getPhotoHash();
        } else {
            return "/templates/apps/user/profile.png";
        }
    }

    public String getDisplayName() {
        if (p.getNickName() != null) {
            return p.getNickName();
        }
        return p.getName();
    }

    /**
     * List of memberships visible within this organisation
     *
     * @return
     */
    public List<GroupMembership> getLocalMemberships() {
        if (memberships == null) {
            memberships = new ArrayList<>();
            List<GroupMembership> allMemberships = getProfile().getMemberships();
            if (allMemberships != null) {
                Organisation websiteOrg = WebUtils.findRootFolder(this).getOrganisation();
                for (GroupMembership m : allMemberships) {
                    Organisation memberOfOrg = m.getWithinOrg();
                    if (memberOfOrg.isWithin(websiteOrg)) {
                        memberships.add(m);

                    }
                }
            }
        }
        return memberships;
    }
}
