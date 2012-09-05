package io.milton.cloud.blobby;

import java.util.Date;
import io.milton.http.Auth;
import io.milton.http.Request;
import io.milton.http.Request.Method;
import io.milton.resource.Resource;
import org.apache.log4j.Logger;

/**
 * Base class for other hashing related resources
 *
 * @author brad
 */
public abstract class BaseResource implements Resource{
    private static final Logger log = Logger.getLogger(BaseResource.class);
    protected final BlobbyResourceFactory resourceFactory;

    public BaseResource(BlobbyResourceFactory resourceFactory) {
        this.resourceFactory = resourceFactory;
    }
        

    @Override
    public String getUniqueId() {
        return getName(); // all our resources are immutable
    }

    @Override
    public Object authenticate(String user, String password) {
        return resourceFactory.getSecurityManager().authenticate(user, password)    ;
    }

    @Override
    public boolean authorise(Request rqst, Method method, Auth auth) {
        return auth != null && auth.getTag() != null;
    }

    @Override
    public String getRealm() {
        return resourceFactory.getSecurityManager().getRealm(null);
    }

    @Override
    public Date getModifiedDate() {
        return null;
    }

    @Override
    public String checkRedirect(Request rqst) {
        return null;
    }

}
