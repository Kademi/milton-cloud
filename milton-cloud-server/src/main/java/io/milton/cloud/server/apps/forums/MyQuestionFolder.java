 package io.milton.cloud.server.apps.forums;

import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.server.db.ForumPost;
import io.milton.cloud.server.db.ForumReply;
import io.milton.cloud.server.web.*;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.http.Auth;
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
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

 import static io.milton.context.RequestContext._;
import io.milton.http.FileItem;
import io.milton.http.exceptions.ConflictException;
import io.milton.resource.PostableResource;
import io.milton.vfs.db.utils.SessionManager;
import java.util.Collections;
import java.util.Date;
import org.hibernate.Session;
import org.hibernate.Transaction;
 
/**
 *
 * @author brad
 */
public class MyQuestionFolder extends AbstractCollectionResource implements GetableResource, PostableResource, IForumResource {

    private final ForumPost forumPost;
    private final CommonCollectionResource parent;
    private ResourceList children;

    private JsonResult jsonResult;
    
    public MyQuestionFolder(ForumPost forumPost, CommonCollectionResource parent) {        
        this.forumPost = forumPost;
        this.parent = parent;
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        if( parameters.containsKey("newComment")) {
            String newComment = parameters.get("newComment");
            ForumReply p = createComment(newComment, session);
            tx.commit();
            jsonResult = new JsonResult(true, "Created", p.getId()+"");
        }
        return null;
    }

    
    
    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        // will show a list of posts for this topic, and allow new posts
        if( jsonResult != null ) {
            jsonResult.write(out);
        } else {
            MenuItem.setActiveIds("menuCommunity");
            _(HtmlTemplater.class).writePage("forums/myQuestion", this, params, out);
        }
    }

    public ForumPost getQuestion() {
        return forumPost;
    }
    
    public String getTitle() {
        return forumPost.getTitle();
    }
    
    public String getNotes() {
        return forumPost.getNotes();
    }
    
    public Date getPostDate() {
        return forumPost.getPostDate();
    }
    
    public String getPosterName() {
        Profile p = forumPost.getPoster();
        if( p != null ) {
            if( p.getNickName() != null ) {
                return p.getNickName();
            } else {
                return p.getName();
            }
        } else {
            return "";
        }
    }
        
    public List<ForumReply> getReplies() {
        if( forumPost.getForumReplys() != null ) {
            return forumPost.getForumReplys();
        } else {
            return Collections.EMPTY_LIST;
        }            
    }
    
    /**
     * Get the profile of the person who posted this question
     * 
     * @return 
     */
    public Profile getPoster() {
        return forumPost.getPoster();
    }
    
    

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            children = new ResourceList();
 
        }
        return children;
    }

    @Override
    public Resource child(String childName) throws NotAuthorizedException, BadRequestException {
        return super.child(childName);
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
        return forumPost.getName();
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

    private ForumReply createComment(String newComment, Session session) {                
        Profile currentUser = _(SpliffySecurityManager.class).getCurrentUser();
        Date now = _(CurrentDateService.class).getNow();       
        return forumPost.addComment(newComment, currentUser, now, session);
    }
}
