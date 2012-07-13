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
import java.util.Date;
import org.hibernate.Session;
import org.hibernate.Transaction;
 
/**
 *
 * @author brad
 */
public class MyTopicFolder extends AbstractCollectionResource implements GetableResource, PostableResource {

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
            createQuestion(newQuestion, comment, session);
            tx.commit();
            jsonResult = new JsonResult(true);
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
        return forumTopic.getForumPosts();
    }
    
    

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            children = new ResourceList();            
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

    private void createQuestion(String newQuestion, String comment, Session session) {
        
        
        ForumPost p = new ForumPost();
        p.setTopic(forumTopic);
        p.setName("p" + System.currentTimeMillis()); // HACK
        p.setTitle(newQuestion);
        p.setNotes(comment);
        Profile currentUser = _(SpliffySecurityManager.class).getCurrentUser();
        p.setPoster(currentUser);
        Date now = _(CurrentDateService.class).getNow();
        p.setPostDate(now);        
        p.setWebsite(website());
        session.save(p);
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
