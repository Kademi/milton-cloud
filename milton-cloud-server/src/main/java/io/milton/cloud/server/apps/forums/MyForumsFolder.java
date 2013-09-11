package io.milton.cloud.server.apps.forums;

import io.milton.cloud.server.db.Forum;
import io.milton.cloud.server.web.AbstractCollectionResource;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.ResourceList;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.cloud.server.web.templating.TitledPage;
import static io.milton.context.RequestContext._;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.principal.Principal;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.GetableResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Website;
import io.milton.vfs.db.utils.SessionManager;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class MyForumsFolder extends AbstractCollectionResource implements GetableResource, IForumResource, TitledPage {

    private static final Logger log = LoggerFactory.getLogger(MyForumsFolder.class);
    private final String name;
    private final CommonCollectionResource parent;
    private final Website website;
    private ResourceList children;

    public MyForumsFolder(String name, CommonCollectionResource parent, Website website) {
        this.name = name;
        this.parent = parent;
        this.website = website;
    }

    @Override
    public String checkRedirect(Request request) throws NotAuthorizedException, BadRequestException {
        String s = super.checkRedirect(request);
        if (s != null) {
            return s;
        }
        // if a GET request to this folder, redirect to first forum
        if (request.getMethod().equals(Request.Method.GET)) {
            List<? extends Resource> list = getChildren().getOfType().get("forum");
            if (list.size() == 1) {
                return list.get(0).getName();
            }
        }
        return null;
    }

    @Override
    public ResourceList getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            children = new ResourceList();
            List<Forum> forums = Forum.findByWebsite(website, SessionManager.session());
            for (Forum f : forums) {
                MyForumFolder faf = new MyForumFolder(f, this);
                children.add(faf);
            }
        }
        return children;
    }

    @Override
    public Priviledge getRequiredPostPriviledge(Request request) {
        return Priviledge.READ_CONTENT;
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
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        // will show a list of posts for this topic, and allow new posts
        MenuItem.setActiveIds("menuCommunity");
        _(HtmlTemplater.class).writePage("forums/myForums", this, params, out);
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return null;
    }

    @Override
    public String getContentType(String accepts) {
        return null;
    }

    @Override
    public Long getContentLength() {
        return null;
    }

    @Override
    public String getTitle() {
        return "Community";
    }
}
