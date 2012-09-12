package io.milton.cloud.server.web;

import io.milton.cloud.server.web.templating.HtmlTemplater;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.HashStore;
import io.milton.vfs.db.Profile;
import io.milton.common.Path;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.AccessControlledResource.Priviledge;
import io.milton.http.Auth;
import io.milton.http.Request;
import io.milton.http.Request.Method;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.principal.Principal;
import io.milton.http.http11.auth.DigestResponse;
import io.milton.http.values.HrefList;
import io.milton.resource.CollectionResource;
import io.milton.resource.PropFindableResource;
import io.milton.resource.ReportableResource;

import static io.milton.context.RequestContext._;
import io.milton.resource.GetableResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.Organisation;
import java.util.Set;

/**
 *
 * @author brad
 */
public abstract class AbstractResource implements CommonResource, PropFindableResource, AccessControlledResource, ReportableResource {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AbstractResource.class);

    public AbstractResource() {
    }

    @Override
    public String getUniqueId() {
        return null;
    }

    @Override
    public Object authenticate(String user, String password) {
        Profile u = _(SpliffySecurityManager.class).authenticate(getOrganisation(), user, password);
        if (u != null) {
            try {
                PrincipalResource p = SpliffyResourceFactory.getRootFolder().findEntity(u);
                if (p == null) {
                    log.warn("Could not locate a PrincipalResource for user: " + u.getName());
                }
                return p;
            } catch (NotAuthorizedException | BadRequestException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            log.warn("authentication did not return a profile");
            return null;
        }
    }

    @Override
    public Object authenticate(DigestResponse digestRequest) {
        Organisation org = getOrganisation();
        if (org == null) {
            throw new RuntimeException("Got null org for: " + this.getClass());
        }
        Profile u = (Profile) _(SpliffySecurityManager.class).authenticate(org, digestRequest);
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
    public boolean authorise(Request request, Method method, Auth auth) {
        boolean b = _(SpliffySecurityManager.class).authorise(request, method, auth, this);
        if (!b) {
//            LogUtils.info(log, "authorisation failed", auth, "resource:", getName(), "method:", method);
        }
        //log.info("authorise: " + getName() + " auth: " + auth + " = " + b);
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
    public String checkRedirect(Request request) throws NotAuthorizedException, BadRequestException {
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
        return _(BlobStore.class);
    }

    public HashStore getHashStore() {
        return _(HashStore.class);
    }

    public HtmlTemplater getTemplater() {
        return _(HtmlTemplater.class);
    }

    @Override
    public boolean isDigestAllowed() {
        return true;
    }

    @Override
    public String getPrincipalURL() {
        CommonResource r = this;
        while (!(r instanceof PrincipalResource)) {
            r = r.getParent();
        }
        if (r != null) {
            PrincipalResource pr = (PrincipalResource) r;
            return pr.getHref();
        }
        return null;
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
        Profile curUser = _(SpliffySecurityManager.class).getCurrentUser();
        Set<AccessControlledResource.Priviledge> privs = _(SpliffySecurityManager.class).getPriviledges(curUser, this);
        list.addAll(privs);
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
        if (type.equals("resource")) {
            return true;
        }
        return false;
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

    public String getHref() {
        Path p = getPath();
        String s = p.toString();
        if (this instanceof CollectionResource) {
            s += "/";
        }
        return s;
    }

    @Override
    public boolean isPublic() {
        return false;
    }

    public Resource find(String path) throws NotAuthorizedException, BadRequestException {
        Path p = Path.path(path);
        return find(p);
    }

    public Resource find(Path p) throws NotAuthorizedException, BadRequestException {
        Resource current = this;
        if (p.isRelative()) {
            current = this;
        } else {
            current = WebUtils.findRootFolder(this);
        }
        for (String part : p.getParts()) {
            if (part.equals(".")) {
                // do nothing
            } else if (part.equals("..")) {
                if (current instanceof AbstractResource) {
                    AbstractResource ar = (AbstractResource) current;
                    current = ar.getParent();
                } else {
                    return null;
                }
            } else {
                if (current instanceof CollectionResource) {
                    CollectionResource col = (CollectionResource) current;
                    current = col.child(part);
                } else {
                    return null;
                }
            }
        }
        return current;
    }

    public boolean isDir() {
        return this instanceof CollectionResource;
    }

    /**
     * Since a RFR will always output XML we want to use the xhtml content type
     * where possible. However, many browsers do not support it, so in that case
     * we want to use text/html
     *
     * see http://www.w3.org/TR/xhtml-media-types/#media-types
     *
     * @param accepts
     * @return
     */
    public String getContentType(String accepts) {
        if (this instanceof GetableResource) {
            if (accepts != null && accepts.contains("application/xhtml+xml")) {
                // can't use it because of CKEditor - http://dev.ckeditor.com/ticket/4576
                //return "application/xhtml+xml";
                
                return "text/html";
            } else {
                return "text/html";
            }
        } else {
            return "";
        }
    }
}
