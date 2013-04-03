package io.milton.cloud.server.apps.scheduler;

import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.server.apps.email.*;
import io.milton.cloud.server.db.ScheduledEmail;
import io.milton.cloud.server.web.AbstractCollectionResource;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.JsonResult;
import io.milton.cloud.server.web.NewPageResource;
import io.milton.cloud.server.web.ResourceList;
import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.cloud.server.web.templating.MenuItem;
import static io.milton.context.RequestContext._;
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
import io.milton.vfs.db.Organisation;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import io.milton.http.Request;
import io.milton.vfs.data.DataSession;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.utils.SessionManager;
import java.util.Date;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 * @author brad
 */
public class ManageScheduledEmailsFolder extends AbstractCollectionResource implements GetableResource, PostableResource {

    private static final Logger log = LoggerFactory.getLogger(ManageAutoEmailsFolder.class);
    
    private final String name;
    private final CommonCollectionResource parent;
    private ResourceList children;
    private JsonResult jsonResult;

    public ManageScheduledEmailsFolder(String name, CommonCollectionResource parent) {
        this.name = name;
        this.parent = parent;
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        String origName = parameters.get("name");
        String nameToCreate = NewPageResource.findAutoCollectionName(origName, this, parameters);
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        Date now = _(CurrentDateService.class).getNow();

        Profile thisUser = _(SpliffySecurityManager.class).getCurrentUser();
        
        ScheduledEmail t = new ScheduledEmail();
        t.setEnabled(false);
        t.setName(nameToCreate);
        t.setTitle(origName);
        t.setOrganisation(getOrganisation());
        t.setStartDate(now);
        t.setFrequency(ScheduledEmail.Frequency.WEEKLY);
        t.setPeriodMultiples(1);
        t.setRunHour(3);
        t.setRunas(thisUser);
        session.save(t);

        tx.commit();

        jsonResult = new JsonResult(true);

        return null;
    }

    public String getTitle() {
        return "Manage scheduled emails";
    }

    @Override
    public Priviledge getRequiredPostPriviledge(Request request) {
        return Priviledge.WRITE_CONTENT;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        if (jsonResult != null) {
            jsonResult.write(out);
        } else {
            MenuItem.setActiveIds("menuTalk", "menuScheduler", "menuSchedulerEmail");
            _(HtmlTemplater.class).writePage("admin", "scheduler/manageScheduledEmails", this, params, out);
        }
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            children = new ResourceList();
            for( ScheduledEmail e : ScheduledEmail.findByOrg(getOrganisation(), SessionManager.session()) ) {
                ManageScheduledEmailFolder f = new ManageScheduledEmailFolder(e, this);
                children.add(f);
            }
        }
        return children;
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
}
