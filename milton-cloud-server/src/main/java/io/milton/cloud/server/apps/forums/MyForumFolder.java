package io.milton.cloud.server.apps.forums;

import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.db.Forum;
import io.milton.cloud.server.db.ForumPost;
import io.milton.cloud.server.web.AbstractCollectionResource;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.JsonResult;
import io.milton.cloud.server.web.NewPageResource;
import io.milton.cloud.server.web.ResourceList;
import io.milton.cloud.server.web.RootFolder;
import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.cloud.server.web.WebUtils;
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
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import static io.milton.context.RequestContext._;
import io.milton.http.FileItem;
import io.milton.http.Request;
import io.milton.http.exceptions.ConflictException;
import io.milton.resource.PostableResource;
import io.milton.vfs.db.Website;
import io.milton.vfs.db.utils.SessionManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class MyForumFolder extends AbstractCollectionResource implements GetableResource, IForumResource, PostableResource {

    private static final Logger log = LoggerFactory.getLogger(MyForumFolder.class);
    
    private final Forum forum;
    private final CommonCollectionResource parent;
    private ResourceList children;
    
    private JsonResult jsonResult;

    public MyForumFolder(Forum forum, CommonCollectionResource parent) {
        this.forum = forum;
        this.parent = parent;
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        if (parameters.containsKey("newQuestion")) {
            String newQuestion = parameters.get("newQuestion");
            String comment = parameters.get("comment");
            ForumPost p = createQuestion(newQuestion, comment, session);
            log.info("Created post: " + p.getId() + " - " + p.getName());
            tx.commit();
            jsonResult = new JsonResult(true, "Created", p.getName());
        }
        return null;
    }

    @Override
    public Priviledge getRequiredPostPriviledge(Request request) {
        return Priviledge.READ_CONTENT;
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
        if( forum.getForumPosts() != null ) {
            return forum.getForumPosts();
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

    private ForumPost createQuestion(String newQuestion, String comment, Session session) {
        String newName = NewPageResource.findAutoCollectionName(newQuestion, this, null);
        ForumPost p = new ForumPost();
        p.setForum(forum);
        if( forum.getForumPosts() == null ) {
            forum.setForumPosts(new ArrayList<ForumPost>());
        }
        forum.getForumPosts().add(p);        
        log.info("createQuestion: newName=" + newName);
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
