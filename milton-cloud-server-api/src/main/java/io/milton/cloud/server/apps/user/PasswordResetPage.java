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
import io.milton.cloud.server.db.PasswordReset;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.JsonResult;
import io.milton.cloud.server.web.RootFolder;
import io.milton.cloud.server.web.TemplatedHtmlPage;
import io.milton.cloud.server.web.WebUtils;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.http.FileItem;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.PostableResource;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Website;
import io.milton.vfs.db.utils.SessionManager;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map;
import org.hibernate.Session;
import org.hibernate.Transaction;

import static io.milton.context.RequestContext._;
import java.io.ByteArrayOutputStream;

/**
 * 
 *
 * @author brad
 */
public class PasswordResetPage extends TemplatedHtmlPage implements PostableResource {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(PasswordResetPage.class);
    
    private JsonResult jsonResult;
    
    private PasswordReset passwordReset;
    
    public PasswordResetPage(String name, CommonCollectionResource parent) {
        super(name, parent, "theme/passwordReset", "Password reset");
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        log.info("processform");
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        if( parameters.containsKey("resetEmail")) {
            try {
                String email = parameters.get("resetEmail");
                if( createReset(email, session) ) {
                    tx.commit();
                    log.info("processform: sent ok");
                    jsonResult = new JsonResult(true);
                } else {
                    log.info("processform: coult not find user");
                    jsonResult = new JsonResult(false, "Could not locate email address");
                }
            } catch (IOException ex) {
                log.error("Exception generating password reset email", ex);
                jsonResult = new JsonResult(false, "There was an error generating the email. Please contact the administrator, or try again later");
            }
        }
        return null;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        System.out.println("xxxxxaaaaa");
        if( jsonResult != null ) {
            jsonResult.write(out);
        } else {
            super.sendContent(out, range, params, contentType);
        }
    }

    private boolean createReset(String email, Session session) throws IOException {
        Organisation org = getOrganisation();
        Profile user = Profile.findByEmail(email, org, session);
        if( user == null ) {
            System.out.println("user not found: " + email + "  in " + org.getName());
            return false;
        }
        RootFolder rootFolder = WebUtils.findRootFolder(this);
        Website website = null;
        String subject; // subject for the email
        String fromAddress = "noreply@";
        if( rootFolder instanceof WebsiteRootFolder) {
            website = ((WebsiteRootFolder)rootFolder).getWebsite();
            subject = "Password reset for " + website.getDomainName();
            String d = website.getDomainName();
            if( d.startsWith("www")) {
                d = d.replace("www.", "");
            }
            fromAddress += d;
        } else {
            subject = "Password reset for " + org.getName();
            fromAddress += org.getName();
        }
        String returnUrl = this.getHref();
        Date now = _(CurrentDateService.class).getNow();
        passwordReset = PasswordReset.create(user, now, returnUrl, website, session);
        log.info("created resetddd: " + passwordReset.getToken());
        
        String emailHtml = createEmailHtml();
        
        
        
        EmailItem emailItem = new EmailItem();
        emailItem.setCreatedDate(now);
        emailItem.setFromAddress(fromAddress); // TODO: perhaps delegate to RootFolder?
        emailItem.setReplyToAddress(fromAddress); // TODO: perhaps delegate to RootFolder?
        emailItem.setHtml(emailHtml);
        emailItem.setRecipient(user);
        emailItem.setRecipientAddress(email);
        emailItem.setSubject(subject);        
        emailItem.setSendStatusDate(now);
        session.save(emailItem);
        log.info("queue email itemxxxx");
        
        return true;
    }

    private String createEmailHtml() throws IOException {
        model = buildModel(null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        _(HtmlTemplater.class).writePage("theme/passwordResetEmail", this, null, out);
        return out.toString("UTF-8");
    }

    @Override
    public boolean isPublic() {
        return true;
    }

    public PasswordReset getPasswordReset() {
        return passwordReset;
    }

    
    

}
