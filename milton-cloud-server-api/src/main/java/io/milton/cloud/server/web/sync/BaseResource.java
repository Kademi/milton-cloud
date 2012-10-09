package io.milton.cloud.server.web.sync;

import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.CommonResource;
import io.milton.cloud.server.web.PrincipalResource;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import java.util.Date;
import io.milton.vfs.db.Organisation;
import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.common.Path;
import io.milton.http.Auth;
import io.milton.http.Request;
import io.milton.http.Request.Method;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.http11.auth.DigestResponse;
import io.milton.vfs.db.Profile;

import static io.milton.context.RequestContext._;
import io.milton.resource.AccessControlledResource;
import org.apache.log4j.Logger;

/**
 * Base class for other hashing related resources
 *
 * @author brad
 */
public abstract class BaseResource implements CommonResource{
    private static final Logger log = Logger.getLogger(BaseResource.class);
    protected final SpliffySecurityManager securityManager;
    protected final Organisation org;

    public BaseResource(SpliffySecurityManager securityManager, Organisation org) {
        this.securityManager = securityManager;
        this.org = org;
    }
    
    

    @Override
    public String getUniqueId() {
        return getName(); // all our resources are immutable
    }

    @Override
    public Object authenticate(String user, String password) {
        Profile u = _(SpliffySecurityManager.class).authenticate(getOrganisation(), user, password);
        if (u != null) {            
            try {
                return SpliffyResourceFactory.getRootFolder().findEntity(u);
            } catch (NotAuthorizedException | BadRequestException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            return null;
        }        
    }
    
    @Override
    public Object authenticate(DigestResponse digestRequest) {
        Profile u = (Profile) _(SpliffySecurityManager.class).authenticate(getOrganisation(), digestRequest);
        if (u != null) {
            try {
                PrincipalResource ur = SpliffyResourceFactory.getRootFolder().findEntity(u);
                if (ur == null) {
                    log.error("Failed to find UserResource for: " + u.getName() + " in root folder: " + SpliffyResourceFactory.getRootFolder().getName() + ", " + SpliffyResourceFactory.getRootFolder().getClass());
                    return null;
                }
                return ur;
            } catch (NotAuthorizedException | BadRequestException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            log.warn("digest auth failed, got null user");
            return null;
        }
    }    

    @Override
    public boolean authorise(Request rqst, Method method, Auth auth) {
        return auth != null && auth.getTag() != null;
    }

    @Override
    public String getRealm() {
        return securityManager.getRealm();
    }

    @Override
    public Date getModifiedDate() {
        return SpliffySyncResourceFactory.LONG_LONG_AGO;
    }

    @Override
    public String checkRedirect(Request rqst) {
        return null;
    }

    @Override
    public boolean isPublic() {
        return false;
    }

    @Override
    public Path getPath() {
        return null;
    }

    @Override
    public CommonCollectionResource getParent() {
        return null;
    }

    @Override
    public Organisation getOrganisation() {
        return org;
    }

    @Override
    public boolean is(String type) {
        return false;
    }

    @Override
    public boolean isDigestAllowed() {
        return true;
    }

    @Override
    public AccessControlledResource.Priviledge getRequiredPostPriviledge(Request request) {
        return null;
    }     
    
    
    @Override
    public CommonResource closest(String type) {
        return null; // ignore, not used for templating
    }    
    
    

    
    
}
