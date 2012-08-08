 package io.milton.cloud.server.apps.forums;

import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.db.ForumPost;
import io.milton.cloud.server.db.ForumTopic;
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
import io.milton.vfs.db.Website;
import io.milton.vfs.db.utils.SessionManager;
import java.util.Collections;
import java.util.Date;
import org.hibernate.Session;
import org.hibernate.Transaction;
 
/**
 *
 * @author brad
 */
public class MyTopicFolder extends AbstractCollectionResource implements GetableResource, PostableResource, IForumResource {

    private final ForumTopic forumTopic;
    private final CommonCollectionResource parent;
    private ResourceList children;

    private JsonResult jsonResult;
    
    public MyTopicFolder(ForumTopic forumTopic, CommonCollectionResource parent) {        
        this.forumTopic = forumTopic;
        this.parent = parent;
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        if( parameters.containsKey("newQuestion")) {
            String newQuestion = parameters.get("newQuestion");
            String comment = parameters.get("comment");
            ForumPost p = createQuestion(newQuestion, comment, session);
            tx.commit();
            jsonResult = new JsonResult(true, "Created", p.getName());
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
            _(HtmlTemplater.class).writePage("forums/myTopic", this, params, out);
        }
    }
    
    public List<ForumPost> getQuestions() {
        if( forumTopic.getForumPosts() != null ) {
            return forumTopic.getForumPosts();
        } else {
            return Collections.EMPTY_LIST;
        }
    }
    
    

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            children = new ResourceList();
            for( ForumPost p : getQuestions()) {
                MyQuestionFolder f = new MyQuestionFolder(p, this);
                children.add(f);
            }
        }
        return children;
    }

    @Override
    public Resource child(String childName) throws NotAuthorizedException, BadRequestException {
        // if children collection is loaded then find it from there
        if( children != null ) {
            return super.child(childName);
        }
        // otherwise attempt to locate without instantiating the whole collection
        ForumPost p = ForumPost.findByName(childName, forumTopic, SessionManager.session()); 
        if( p == null ) {
            return null;
        } else {
            MyQuestionFolder f = new MyQuestionFolder(p, this);
            return f;
        }
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
        return forumTopic.getName();
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

    private ForumPost createQuestion(String newQuestion, String comment, Session session) {                
        ForumPost p = new ForumPost();
        p.setTopic(forumTopic);
        String newName = NewPageResource.findAutoName(newQuestion, this, null);
        newName = newName.replace(".html", ""); // hack
        p.setName(newName);
        p.setTitle(newQuestion);
        p.setNotes(comment);
        Profile currentUser = _(SpliffySecurityManager.class).getCurrentUser();
        p.setPoster(currentUser);
        Date now = _(CurrentDateService.class).getNow();
        p.setPostDate(now);        
        p.setWebsite(website());
        session.save(p);
        return p;
    }
    
    private Website website() {
        RootFolder rootFolder = WebUtils.findRootFolder(this);
        if( rootFolder instanceof WebsiteRootFolder) {
            WebsiteRootFolder wrf = (WebsiteRootFolder) rootFolder;
            return wrf.getWebsite();
        } else {
            return null;
        }
    }
}
