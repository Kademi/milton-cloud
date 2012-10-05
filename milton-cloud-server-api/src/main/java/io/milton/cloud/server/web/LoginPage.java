package io.milton.cloud.server.web;

import io.milton.cloud.server.web.templating.HtmlTemplater;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import io.milton.vfs.db.Organisation;
import io.milton.resource.AccessControlledResource.Priviledge;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.Request.Method;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.GetableResource;

import static io.milton.context.RequestContext._;

/**
 *
 * @author brad
 */
public class LoginPage extends AbstractResource implements GetableResource, CommonResource {

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
    public Long getContentLength() {
        return null;
    }

    @Override
    public String getName() {
        return "login";
    }

    @Override
    public boolean authorise(Request request, Method method, Auth auth) {
        return auth != null; // this is just to force authentication
    }


    @Override
    public String checkRedirect(Request request) {
        if (request.getAuthorization() != null && request.getAuthorization().getTag() != null) {
            // logged in, so go to user's home page
            Object oUser = request.getAuthorization().getTag();
            if( oUser instanceof UserResource ) {
                UserResource user = (UserResource) oUser;
                return "/" + user.getName() + "/";
            }
            throw new RuntimeException("Unexpected tag type: " + oUser.getClass() + " - expected UserResource");
        } else {
            return null;
        }
    }

    @Override
    public CommonCollectionResource getParent() {
        return parent;
    }

    @Override
    public Organisation getOrganisation() {
        return parent.getOrganisation();
    }

    @Override
    public boolean is(String type) {
        if( type.equals("loginPage") ) {
            return true;
        }
        return super.is(type);
    }

    @Override
    public boolean isPublic() {
        boolean b = parent.isPublic();
        return b;
    }
    
    @Override
    public Priviledge getRequiredPostPriviledge(Request request) {
        return null;
    }      
    
}
