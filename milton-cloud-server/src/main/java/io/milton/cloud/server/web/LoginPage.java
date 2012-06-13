package io.milton.cloud.server.web;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import io.milton.cloud.server.db.BaseEntity;
import io.milton.cloud.server.db.Organisation;
import io.milton.cloud.server.db.Profile;
import io.milton.common.Path;
import io.milton.http.AccessControlledResource.Priviledge;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.Request.Method;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.http.http11.auth.DigestResponse;
import io.milton.resource.GetableResource;

/**
 *
 * @author brad
 */
public class LoginPage implements GetableResource, SpliffyResource {
    private final SpliffySecurityManager securityManager;
    private final SpliffyCollectionResource parent;

    public LoginPage(SpliffySecurityManager securityManager, SpliffyCollectionResource parent) {
        this.securityManager = securityManager;
        this.parent = parent;
    }


    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        parent.getServices().getHtmlTemplater().writePage("login", this, params, out); 
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
        return securityManager.authenticate(parent.getOrganisation(), user, password);
    }
    
    @Override
    public Object authenticate(DigestResponse digestRequest) {
        return securityManager.authenticate(parent.getOrganisation(), digestRequest);
    }    

    @Override
    public boolean authorise(Request request, Method method, Auth auth) {
        return auth != null; // this is just to force authentication
    }

    @Override
    public String getRealm() {
        return securityManager.getRealm();
    }

    @Override
    public Date getModifiedDate() {
        return null;
    }

    @Override
    public String checkRedirect(Request request) {
        if( request.getAuthorization() != null && request.getAuthorization().getTag() != null ) {
            // logged in, so go to user's home page
            Profile user = (Profile) request.getAuthorization().getTag();
            return "/" + user.getName() + "/";
        } else {
            return null;
        }
    }

    @Override
    public SpliffyCollectionResource getParent() {
        return parent;
    }

    @Override
    public Services getServices() {
        return parent.getServices();
    }


    @Override
    public boolean isDigestAllowed() {
        return true;
    }

    @Override
    public BaseEntity getOwner() {
        return null;
    }

    @Override
    public Profile getCurrentUser() {
        return null;
    }

    @Override
    public void addPrivs(List<Priviledge> list, Profile user) {

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
        SpliffyCollectionResource p = getParent();
        if( p != null ) {
            return p.getPath().child(this.getName());
        } else {
            return Path.root;
        }
    }    
}
