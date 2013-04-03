package io.milton.cloud.server.apps.scheduler;

import edu.emory.mathcs.backport.java.util.Arrays;
import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.server.apps.Application;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.apps.AttachmentApplication;
import io.milton.cloud.server.apps.email.*;
import io.milton.cloud.server.apps.orgs.OrganisationRootFolder;
import io.milton.cloud.server.db.GroupRecipient;
import io.milton.cloud.server.db.ScheduledEmail;
import io.milton.cloud.server.db.ScheduledEmailResult;
import io.milton.cloud.server.web.AbstractCollectionResource;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.CommonResource;
import io.milton.cloud.server.web.JsonResult;
import io.milton.cloud.server.web.ResourceList;
import io.milton.cloud.server.web.RootFolder;
import io.milton.cloud.server.web.WebUtils;
import io.milton.cloud.server.web.templating.DataBinder;
import io.milton.cloud.server.web.templating.Formatter;
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
import io.milton.vfs.db.Group;
import io.milton.vfs.db.utils.SessionManager;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 * @author brad
 */
public class ManageScheduledEmailFolder extends AbstractCollectionResource implements GetableResource, PostableResource {

    private static final Logger log = LoggerFactory.getLogger(ManageAutoEmailsFolder.class);
    private final ScheduledEmail job;
    private final CommonCollectionResource parent;
    private ResourceList children;
    private JsonResult jsonResult;

    public ManageScheduledEmailFolder(ScheduledEmail email, CommonCollectionResource parent) {
        this.job = email;
        this.parent = parent;
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();

        if (parameters.containsKey("group")) {
            String groupName = parameters.get("group");
            String sIsRecip = parameters.get("isRecip");
            if ("true".equals(sIsRecip)) {
                addGroup(groupName);
            } else {
                removeGroup(groupName);
            }
            tx.commit();
            jsonResult = new JsonResult(true);
        } else if (parameters.containsKey("test")) {
            try {
                Date toDate = _(CurrentDateService.class).getNow();
                Date fromDate = _(Formatter.class).addDays(toDate, -7);
                _(SchedulerApp.class).sendScheduledEmail(job, fromDate, toDate, session);
                session.flush();
                tx.commit();
                System.out.println("--- saved and commited");
                jsonResult = new JsonResult(true);
            } catch (IOException iOException) {
                log.warn("exception", iOException);
                jsonResult = new JsonResult(false, "Exception");
            }
        } else {
            try {
                String s = parameters.get("sFrequency");
                if (s != null) {
                    ScheduledEmail.Frequency f = ScheduledEmail.Frequency.valueOf(s);
                    job.setFrequency(f);
                }

                _(DataBinder.class).populate(job, parameters);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
            log.info("Saving. Subject=" + job.getSubject());
            session.save(job);
            tx.commit();
            jsonResult = new JsonResult(true);
        }
        return null;
    }

    public String getTitle() {
        return "Manage scheduled email: " + job.getName();
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
            _(HtmlTemplater.class).writePage("admin", "scheduler/manageScheduledEmail", this, params, out);
        }
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            children = new ResourceList();
            for (ScheduledEmail e : ScheduledEmail.findByOrg(getOrganisation(), SessionManager.session())) {
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
        return job.getName();
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

    public List<Group> getAllGroups() {
        return job.getOrganisation().groups(SessionManager.session());
    }

    private void removeGroup(String groupName) {
        if (job.getGroupRecipients() == null) {
            return;
        }
        Iterator<GroupRecipient> it = job.getGroupRecipients().iterator();
        Session session = SessionManager.session();
        while (it.hasNext()) {
            GroupRecipient gr = it.next();
            Group g = gr.getRecipient();
            if (g.getName().equals(groupName)) {
                it.remove();
                session.delete(gr);
            }
        }
    }

    private void addGroup(String groupName) {
        System.out.println("addgroup: " + groupName);
        if (job.getGroupRecipients() == null) {
            job.setGroupRecipients(new ArrayList<GroupRecipient>());
        }
        for (GroupRecipient gr : job.getGroupRecipients()) {
            if (gr.getRecipient().getName().equals(groupName)) {
                log.info("Already has group");
                return;
            }
        }
        Session session = SessionManager.session();
        Group g = job.getOrganisation().group(groupName, session);
        if (g == null) {
            log.warn("group not found: " + groupName);
            return;
        }
        GroupRecipient gr = new GroupRecipient();
        gr.setJob(job);
        gr.setRecipient(g);
        session.save(gr);
        job.getGroupRecipients().add(gr);
        System.out.println("added group");
    }

    public List<Group> getGroupRecipients() {
        List<Group> list = new ArrayList<>();
        if (job.getGroupRecipients() != null) {
            for (GroupRecipient gr : job.getGroupRecipients()) {
                list.add(gr.getRecipient());
            }
        }
        return list;
    }

    public String getSubject() {
        return job.getSubject();
    }

    public String getFromAddress() {
        return job.getFromAddress();
    }

    public String getNotes() {
        return job.getNotes();
    }

    public ScheduledEmail getJob() {
        return job;
    }

    public List<ScheduledEmail.Frequency> getAllFrequencies() {
        return Arrays.asList(ScheduledEmail.Frequency.values());
    }

    public Date getLastRunDate() {
        ScheduledEmailResult result = job.getLastResult(SessionManager.session());
        if (result == null) {
            return null;
        } else {
            return result.getStartDate();
        }
    }

    public ResourceList getAttachmentChoices() {
        ResourceList list = new ResourceList();
        RootFolder rf = WebUtils.findRootFolder(this);
        if (rf instanceof OrganisationRootFolder) {
            OrganisationRootFolder orf = (OrganisationRootFolder) rf;
            for (Application app : _(ApplicationManager.class).getActiveApps(rf)) {
                if (app instanceof AttachmentApplication) {
                    AttachmentApplication attachmentApplication = (AttachmentApplication) app;
                    list.addAll(attachmentApplication.getAvailableAttachments(orf)); 
                }
            }
        }
        return list;
    }
}
