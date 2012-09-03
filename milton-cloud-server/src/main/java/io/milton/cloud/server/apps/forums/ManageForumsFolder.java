package io.milton.cloud.server.apps.forums;

import io.milton.cloud.server.web.AbstractCollectionResource;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.ResourceList;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.principal.Principal;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.GetableResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Website;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import static io.milton.context.RequestContext._;
import io.milton.http.Auth;
import io.milton.http.Request;
import java.util.Collections;

/**
 * Just provides a list of manage forums by website folders
 *
 *
 * @author brad
 */
public class ManageForumsFolder extends AbstractCollectionResource implements GetableResource {

    private final String name;
    private final CommonCollectionResource parent;
    private ResourceList children;

    public ManageForumsFolder(String name, CommonCollectionResource parent) {
        this.name = name;
        this.parent = parent;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        MenuItem.setActiveIds("menuTalk", "menuManageForums", "menuEditForums");
        _(HtmlTemplater.class).writePage("admin", "forums/manageForums", this, params, out);
    }    
    
    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            children = new ResourceList();
            List<Website> list = getOrganisation().getWebsites();
            if (list != null) {
                for( Website w : list ) {
                    ManageWebsiteForumsFolder p = new ManageWebsiteForumsFolder( getOrganisation(), this, w);
                    children.add(p);
                }
            }
        }
        return children;
    }

    public ManageForumsFolder getForumsRoot() {
        return this;
    }

    public List<Website> getWebsites() {
        List<Website> list = getOrganisation().getWebsites();
        if (list == null) {
            list = Collections.EMPTY_LIST;
        }
        return list;
    }
    
    public String getTitle() {
        return "Manage Forums";
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
    public String getName() {
        return name;
    }

    @Override
    public Map<Principal, List<AccessControlledResource.Priviledge>> getAccessControlList() {
        return null;
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
    public Priviledge getRequiredPostPriviledge(Request request) {
        return Priviledge.WRITE_CONTENT;
    }        
}
