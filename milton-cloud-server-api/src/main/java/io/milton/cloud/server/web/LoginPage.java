package io.milton.cloud.server.web;

import io.milton.cloud.server.web.templating.HtmlTemplater;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.common.Path;
import io.milton.resource.AccessControlledResource.Priviledge;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.Request.Method;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.http.http11.auth.DigestResponse;
import io.milton.resource.GetableResource;

import static io.milton.context.RequestContext._;

/**
 *
 * @author brad
 */
public class LoginPage implements GetableResource, CommonResource {

    private final CommonCollectionResource parent;

    public LoginPage(CommonCollectionResource parent) {
        this.parent = parent;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        String template = "login/login";
        if( !isPublic() ) {
            template = "admin/login";
        }
        _(HtmlTemplater.class).writePage(template, this, params, out);
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return null;
    }

    @Override
    public String getContentType(String accepts) {
        return "text/html";
    }

    @Override
    public Long getContentLength() {
        return null;
    }

    @Override
    public String getUniqueId() {
        return null;
    }

    @Override
    public String getName() {
        return "login";
    }

    @Override
    public Object authenticate(String user, String password) {
        return _(SpliffySecurityManager.class).authenticate(parent.getOrganisation(), user, password);
    }

    @Override
    public Object authenticate(DigestResponse digestRequest) {
        return _(SpliffySecurityManager.class).authenticate(parent.getOrganisation(), digestRequest);
    }

    @Override
    public boolean authorise(Request request, Method method, Auth auth) {
        return auth != null; // this is just to force authentication
    }

    @Override
    public String getRealm() {
        return _(SpliffySecurityManager.class).getRealm();
    }

    @Override
    public Date getModifiedDate() {
        return null;
    }

    @Override
    public String checkRedirect(Request request) {
        if (request.getAuthorization() != null && request.getAuthorization().getTag() != null) {
            // logged in, so go to user's home page
            Profile user = (Profile) request.getAuthorization().getTag();
            return "/" + user.getName() + "/";
        } else {
            return null;
        }
    }

    @Override
    public CommonCollectionResource getParent() {
        return parent;
    }

    @Override
    public boolean isDigestAllowed() {
        return true;
    }

    @Override
    public Organisation getOrganisation() {
        return parent.getOrganisation();
    }

    @Override
    public boolean is(String type) {
        return type.equals("loginPage");
    }

    @Override
    public Path getPath() {
        CommonCollectionResource p = getParent();
        if (p != null) {
            return p.getPath().child(this.getName());
        } else {
            return Path.root;
        }
    }

    @Override
    public boolean isPublic() {
        boolean b = parent.isPublic();
        return b;
    }
}
