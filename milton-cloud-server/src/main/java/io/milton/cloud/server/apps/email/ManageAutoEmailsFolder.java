package io.milton.cloud.server.apps.email;

import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.server.db.EmailTrigger;
import io.milton.cloud.server.db.GroupEmailJob;
import io.milton.cloud.server.web.AbstractCollectionResource;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.JsonResult;
import io.milton.cloud.server.web.ResourceList;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.http.Auth;
import io.milton.http.FileItem;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.principal.Principal;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.GetableResource;
import io.milton.resource.PostableResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.utils.SessionManager;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.hibernate.Session;
import org.hibernate.Transaction;

import static io.milton.context.RequestContext._;

/**
 * 
 *
 * @author brad
 */
public class ManageAutoEmailsFolder extends AbstractCollectionResource  implements GetableResource, PostableResource {

    private final String name;
    private final CommonCollectionResource parent;
    private final Organisation org;
    private ResourceList children;
    
    private JsonResult jsonResult;
    

    public ManageAutoEmailsFolder(String name, CommonCollectionResource parent, Organisation org) {        
        this.name = name;
        this.parent = parent;
        this.org = org;
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        // this form post is to create a shell group email job
        String nameToCreate = parameters.get("name");
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        Date now = _(CurrentDateService.class).getNow();
        
        
        
        tx.commit();
        
        jsonResult = new JsonResult(true);
        
        return null;
    }    
        
    
    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {               
        if( jsonResult != null ) {
            jsonResult.write(out);
        } else {
            MenuItem.setActiveIds("menuTalk", "menuEmails", "menuAutoEmail");
            _(HtmlTemplater.class).writePage("admin","email/manageAutoEmails", this, params, out);
        }
    }    

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            children = new ResourceList();
            List<EmailTrigger> jobs = EmailTrigger.findByOrg(org, SessionManager.session()); 
            System.out.println("jobs: " + jobs.size());
            for( EmailTrigger f : jobs ) {
                ManageAutoEmailPage page = new ManageAutoEmailPage(f, parent);
                children.add(page);
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
        
}
