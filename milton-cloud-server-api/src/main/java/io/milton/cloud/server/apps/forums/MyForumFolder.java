package io.milton.cloud.server.apps.forums;

import io.milton.cloud.server.web.AbstractCollectionResource;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.ResourceList;
import io.milton.cloud.server.web.templating.HtmlTemplater;
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
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import static io.milton.context.RequestContext._;

/**
 *
 * @author brad
 */
public class MyForumFolder extends AbstractCollectionResource implements GetableResource{
   
    private final Forum forum;
    private final CommonCollectionResource parent;
    private ResourceList children;

    public MyForumFolder(Forum forum, CommonCollectionResource parent) {        
        this.forum = forum;
        this.parent = parent;
    }

    @Override
    public String checkRedirect(Request request) throws NotAuthorizedException, BadRequestException {
        String s = super.checkRedirect(request);
        if( s != null ) {
            return s;
        }
        
        // if a GET request to this folder, redirect to first forum topic
        if( request.getMethod().equals(Request.Method.GET)) {
            List<? extends Resource> list = getChildren();
            for( Resource r : list ) {
                if( r instanceof MyTopicFolder) {
                    return r.getName();
                }
            }
        }
        return null;
    }    

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            children = new ResourceList();
            List<ForumTopic> topics = forum.getForumTopics();
            if( topics != null ) {
                for( ForumTopic t : topics ) {
                    MyTopicFolder ftaf = new MyTopicFolder(t, this);
                    children.add(ftaf);
                }
            }
        }
        return children;
    }

    @Override
    public CommonCollectionResource getParent() {
        return parent;
    }

    @Override
    public BaseEntity getOwner() {
        return parent.getOwner();
    }

    @Override
    public Organisation getOrganisation() {
        return parent.getOrganisation();
    }

    @Override
    public void addPrivs(List<AccessControlledResource.Priviledge> list, Profile user) {
        parent.addPrivs(list, user);
    }

    @Override
    public String getName() {
        return forum.getName();
    }

    @Override
    public Map<Principal, List<AccessControlledResource.Priviledge>> getAccessControlList() {
        return null;
    }
    
    public String getTitle() {
        return forum.getTitle();
    }

    public String getNotes() {
        return forum.getNotes();
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        _(HtmlTemplater.class).writePage("forums/myForum", this, params, out);
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
    
}
