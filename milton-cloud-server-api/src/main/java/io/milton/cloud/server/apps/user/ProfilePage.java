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

import io.milton.cloud.server.manager.PasswordManager;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.JsonResult;
import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.cloud.server.web.TemplatedHtmlPage;
import io.milton.cloud.server.web.templating.DataBinder;
import io.milton.http.FileItem;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.PostableResource;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.utils.SessionManager;
import java.util.Map;
import org.hibernate.Session;
import org.hibernate.Transaction;

import static io.milton.context.RequestContext._;
import io.milton.http.Range;
import io.milton.http.exceptions.NotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;

/**
 * User's own profile page, for use within a website
 *
 * @author brad
 */
public class ProfilePage extends TemplatedHtmlPage implements PostableResource {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ProfilePage.class);
    
    private JsonResult jsonResult;
    
    public ProfilePage(String name, CommonCollectionResource parent) {
        super(name, parent, "user/profile", "Profile");
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        Profile p = _(SpliffySecurityManager.class).getCurrentUser();
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        if (parameters.containsKey("nickName")) {
            try {
                _(DataBinder.class).populate(p, parameters);
                tx.commit();
                jsonResult = new JsonResult(true);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                log.error("exception: " + p.getId(), ex);
                jsonResult = new JsonResult(false, ex.getMessage());
            }
        } else if(parameters.containsKey("password")) {
            String newPassword = parameters.get("password");
             _(PasswordManager.class).setPassword(p, newPassword);   
             jsonResult = new JsonResult(true);
             tx.commit();
        }
        return null;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        if( jsonResult != null ) {
            jsonResult.write(out);
        } else {
            super.sendContent(out, range, params, contentType);
        }
    }        
    
    public Profile getProfile() {
        return _(SpliffySecurityManager.class).getCurrentUser();
    }
}
