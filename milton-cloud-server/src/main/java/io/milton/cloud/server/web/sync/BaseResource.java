package io.milton.cloud.server.web.sync;

import java.util.Date;
import io.milton.vfs.db.Organisation;
import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.http.Auth;
import io.milton.http.Request;
import io.milton.http.Request.Method;
import io.milton.resource.Resource;

/**
 * Base class for other hashing related resources
 *
 * @author brad
 */
public abstract class BaseResource implements Resource{

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
        return securityManager.authenticate(org, user, password);
    }

    @Override
    public boolean authorise(Request rqst, Method method, Auth auth) {
        return securityManager.authorise(rqst, method, auth, this);
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

}
