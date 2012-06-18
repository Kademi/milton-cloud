package io.milton.cloud.server.web;


import com.ettrema.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.HashStore;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Profile;
import io.milton.cloud.server.web.templating.Templater;
import io.milton.common.Path;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.AccessControlledResource.Priviledge;
import io.milton.http.Auth;
import io.milton.http.Request;
import io.milton.http.Request.Method;
import io.milton.principal.Principal;
import io.milton.http.http11.auth.DigestResponse;
import io.milton.http.values.HrefList;
import io.milton.resource.CollectionResource;
import io.milton.resource.PropFindableResource;
import io.milton.resource.ReportableResource;

/**
 *
 * @author brad
 */
public abstract class AbstractResource implements CommonResource, PropFindableResource, AccessControlledResource, ReportableResource {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AbstractResource.class);

    /**
     * For templating, return true if this is a directory, false for a file
     */
    public abstract boolean isDir();
    
    protected final Services services;

    public AbstractResource(Services services) {
        this.services = services;
        if( services == null ) {
            throw new NullPointerException("services");
        }
    }

    @Override
    public String getUniqueId() {
        return null;
    }

    @Override
    public Object authenticate(String user, String password) {
        Profile u = (Profile) services.getSecurityManager().authenticate(getOrganisation(), user, password);
        if (u != null) {
            return SpliffyResourceFactory.getRootFolder().findEntity(u.getName());
        } else {
            return null;
        }
    }

    @Override
    public Object authenticate(DigestResponse digestRequest) {
        Profile u = (Profile) services.getSecurityManager().authenticate(getOrganisation(), digestRequest);
        if (u != null) {
            PrincipalResource ur = SpliffyResourceFactory.getRootFolder().findEntity(u.getName());
            if (ur == null) {
                throw new RuntimeException("Failed to find UserResource for: " + u.getName());
            }
            log.warn("sigest auth ok: " + ur);
            return ur;
        } else {
            log.warn("digest auth failed, got null user");
            return null;
        }
    }

    @Override
    public boolean authorise(Request request, Method method, Auth auth) {
        boolean b = services.getSecurityManager().authorise(request, method, auth, this);
        if (!b) {
            LogUtils.info(log, "authorisation failed", auth, "resource:", getName(), "method:", method);
        }
        return b;
    }

    @Override
    public String getRealm() {
        return "spliffy";
    }

    /**
     * Check for correctly formed folder paths on GET requests
     *
     * If request is a GET, and the resource is a collection, then if the url
     * does NOT end with a slash redirect to ../
     *
     * @param request
     * @return
     */
    @Override
    public String checkRedirect(Request request) {
        if (request.getMethod().equals(Request.Method.GET)) {
            if (this instanceof CollectionResource) {
                if (request.getParams().isEmpty()) { // only do redirect if no request params
                    String url = request.getAbsolutePath();
                    if (!url.endsWith("/")) {
                        return url + "/";
                    }
                }
            }
        }
        return null;
    }

    public BlobStore getBlobStore() {
        return services.getBlobStore();
    }

    public HashStore getHashStore() {
        return services.getHashStore();
    }

    public Templater getTemplater() {
        return services.getHtmlTemplater();
    }

    @Override
    public Services getServices() {
        return services;
    }

    @Override
    public boolean isDigestAllowed() {
        return true;
    }

    @Override
    public Profile getCurrentUser() {
        return services.getSecurityManager().getCurrentUser();
    }

    @Override
    public String getPrincipalURL() {
        BaseEntity entity = getOwner();
        if (entity == null) {
            return null;
        } else {
            return "/" + entity.getName(); // probably would be good to put this into a UrlMapper interface
        }
    }

    /**
     * Return the list of privlidges which the current user (given by the auth
     * object) has access to, on this resource.
     *
     * @param auth
     * @return
     */
    @Override
    public List<AccessControlledResource.Priviledge> getPriviledges(Auth auth) {
        List<AccessControlledResource.Priviledge> list = new ArrayList<>();
        Profile user = null;
        if (auth != null && auth.getTag() != null) {
            UserResource userRes = (UserResource) auth.getTag();
            user = userRes.getThisUser();
        }        
        addPrivs(list, user);
        
        return list;
    }

    @Override
    public void setAccessControlList(Map<Principal, List<Priviledge>> privs) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Return the hrefs (either fully qualified URLs or absolute paths) to the
     * collections which contain principals. This is to allow user agents to
     * display a list of users to display.
     *
     * Most implementations will only have a single value which will be the path
     * to the users folder. Eg:
     *
     * return Arrays.asList("/users/");
     *
     * @return - a list of hrefs
     */
    @Override
    public HrefList getPrincipalCollectionHrefs() {
        HrefList list = new HrefList();
        list.add("/users/");
        return list;
    }

    @Override
    public boolean is(String type) {
        return type.equals("resource");
    }
    
    @Override
    public Path getPath() {
        CommonCollectionResource p = getParent();
        if( p != null ) {
            return p.getPath().child(this.getName());
        } else {
            return Path.root;
        }
    }
}
