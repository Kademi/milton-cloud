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
import io.milton.cloud.server.db.PasswordReset;
import io.milton.cloud.server.manager.PasswordManager;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.JsonResult;
import io.milton.cloud.server.web.PrincipalResource;
import io.milton.cloud.server.web.RootFolder;
import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.cloud.server.web.TemplatedHtmlPage;
import io.milton.cloud.server.web.templating.Formatter;
import io.milton.http.FileItem;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.PostableResource;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.utils.SessionManager;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map;
import org.hibernate.Session;
import org.hibernate.Transaction;

import static io.milton.context.RequestContext._;
import io.milton.http.HttpManager;
import io.milton.http.http11.auth.CookieAuthenticationHandler;

/**
 *
 *
 * @author brad
 */
public class DoPasswordResetPage extends TemplatedHtmlPage implements PostableResource {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(DoPasswordResetPage.class);
    private final CookieAuthenticationHandler cookieAuthenticationHandler;
    private JsonResult jsonResult;

    public DoPasswordResetPage(String name, CommonCollectionResource parent, CookieAuthenticationHandler cookieAuthenticationHandler) {
        super(name, parent, "user/passwordDoReset", "Password reset");
        this.cookieAuthenticationHandler = cookieAuthenticationHandler;
        if( this.cookieAuthenticationHandler == null ) {
            throw new RuntimeException("no cookie auth");
        }
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        log.info("processform");
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        if (parameters.containsKey("newPassword")) {

            String newPassword = parameters.get("newPassword");
            String token = parameters.get("token");
            PasswordReset passwordReset = PasswordReset.find(token, session);
            if (passwordReset == null) {
                jsonResult = new JsonResult(false, "Token not found");
            } else if (isExpired(passwordReset)) {
                jsonResult = new JsonResult(false, "The token is expired");
            } else {
                doReset(newPassword, passwordReset, session);
                tx.commit();
                log.info("processform: sent ok");
                jsonResult = new JsonResult(true);
            }

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

    @Override
    public boolean isPublic() {
        return true;
    }

    private boolean isExpired(PasswordReset passwordReset) {
        Date now = _(CurrentDateService.class).getNow();
        long hours = _(Formatter.class).durationHours(passwordReset.getCreatedDate(), now);
        return hours > 24;
    }

    private void doReset(String newPassword, PasswordReset reset, Session session) throws NotAuthorizedException, BadRequestException {
        Profile p = reset.getProfile();
        _(PasswordManager.class).setPassword(p, newPassword);
        PrincipalResource userRes = UserApp.findEntity(p, (RootFolder)getParent());
        cookieAuthenticationHandler.setLoginCookies(userRes, HttpManager.request());
    }
}
