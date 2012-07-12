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
package io.milton.cloud.server.mail;

import io.milton.cloud.server.apps.website.WebsiteApp;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.web.PrincipalResource;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.mail.MailResourceFactory;
import io.milton.mail.Mailbox;
import io.milton.mail.MailboxAddress;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Website;
import io.milton.vfs.db.utils.SessionManager;

import static io.milton.context.RequestContext._;         
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Group;

/**
 *
 * @author brad
 */
public class MiltonCloudMailResourceFactory implements MailResourceFactory{

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(MiltonCloudMailResourceFactory.class);
    
    private final SpliffyResourceFactory resourceFactory;

    public MiltonCloudMailResourceFactory(SpliffyResourceFactory resourceFactory) {
        this.resourceFactory = resourceFactory;
    }
        
    
    @Override
    public Mailbox getMailbox(MailboxAddress add) {
        log.info("getMailbox: " + add);
        WebsiteRootFolder wrf = _(WebsiteApp.class).getPage(null, add.domain);
        if( wrf == null ) {
            log.info("web not found: " + add.domain);
            return null;
        }
        Website w = wrf.getWebsite();

        // Now look for a profile which has a admin org that owns the website
        BaseEntity p = BaseEntity.find(w.getOrganisation(), add.user, SessionManager.session());
        if( p == null ) {
            log.info("baseentity not found: " + add.user + " in website: " + w.getName());
            return null;
        } else {
            PrincipalResource pr;
            try {
                pr = wrf.findEntity(p);
            } catch (NotAuthorizedException | BadRequestException ex) {
                throw new RuntimeException(ex);
            }
            if( pr == null ) {
                log.warn("Failed to find entity from profile: " + p.getName() + " id: " + p.getId());
                return null;
            }
            if( pr instanceof Mailbox) {
                return (Mailbox) pr;
            } else {
                log.warn("Entity is not a mailbox: " + p.getName() + " id: " + p.getId());
                return null;
            }
        }
        
    }
    
}
