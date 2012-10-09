/*
 * Copyright 2012 McEvoy Software Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.apps.contactus;

import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.SettingsApplication;
import io.milton.cloud.server.mail.SendMailProcessable;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.db.AppControl;
import io.milton.cloud.server.db.GroupEmailJob;
import io.milton.cloud.server.db.GroupRecipient;
import io.milton.cloud.server.manager.CurrentRootFolderService;
import io.milton.cloud.server.queue.AsynchProcessor;
import io.milton.cloud.server.web.AbstractResource;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.JsonResult;
import io.milton.cloud.server.web.RootFolder;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.cloud.server.web.WebUtils;
import io.milton.cloud.server.web.templating.DataBinder;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.http.Auth;
import io.milton.http.FileItem;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.principal.Principal;
import io.milton.resource.GetableResource;
import io.milton.resource.PostableResource;
import io.milton.vfs.db.Organisation;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static io.milton.context.RequestContext._;
import io.milton.http.Request;
import io.milton.http.Request.Method;
import io.milton.http.XmlWriter;
import io.milton.http.XmlWriter.Element;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Group;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Website;
import io.milton.vfs.db.utils.SessionManager;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import org.apache.velocity.context.Context;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 * @author brad
 */
public class ContactUsFormPage extends AbstractResource implements GetableResource, PostableResource {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ContactUsFormPage.class);
    private CommonCollectionResource parent;
    private String name;
    private JsonResult jsonResult;
    private AppControl _appControl;

    public ContactUsFormPage(CommonCollectionResource parent, String name) {
        this.parent = parent;
        this.name = name;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        if (jsonResult != null) {
            jsonResult.write(out);
        } else {
            _(HtmlTemplater.class).writePage("contactus/contactForm", this, params, out);
        }
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        log.info("processForm");
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        try {
            ContactRequest r = new ContactRequest();
            _(DataBinder.class).populate(r, parameters);
            log.info("processForm: customer email: " + r.getEmail());
            Long jobId = generateEmailItems(r, session);
            jsonResult = new JsonResult(true);
            tx.commit();

            SendMailProcessable processable = new SendMailProcessable(jobId);
            _(AsynchProcessor.class).enqueue(processable);
        } catch (Exception e) {
            log.error("Exeption processing contact email", e);
            tx.rollback();
            jsonResult = new JsonResult(false, "Couldnt send email: " + e.getMessage());
        }
        return null;
    }

    @Override
    public Priviledge getRequiredPostPriviledge(Request request) {
        return Priviledge.READ_CONTENT;
    }

    private AppControl appControl() {
        if (_appControl == null) {
            RootFolder rootFolder = WebUtils.findRootFolder(this);
            Website website;
            Branch branch;
            if (rootFolder instanceof WebsiteRootFolder) {
                WebsiteRootFolder wrf = (WebsiteRootFolder) rootFolder;
                website = wrf.getWebsite();
                branch = wrf.getBranch();
            } else {
                throw new RuntimeException("Cant proceess contact for an organisation");
            }

            _appControl = AppControl.find(branch, ContactUsApp.CONTACT_US_ID, SessionManager.session());
            if (_appControl == null) {
                throw new RuntimeException("Cant process contact request, no AppControl record");
            }
        }
        return _appControl;
    }

    private Long generateEmailItems(ContactRequest r, Session session) {

        Website website = (Website) appControl().getWebsiteBranch().getRepository();
        AppControl appControl = appControl();
        if (appControl == null) {
            throw new RuntimeException("Cant process contact request, no AppControl record");
        }

        String emailBody = toMessageHtml(r);
        Date now = _(CurrentDateService.class).getNow();
        Group group = findGroup(appControl, session);
        GroupEmailJob job = new GroupEmailJob();
        job.setName("contact");
        job.setOrganisation(getOrganisation());
        job.setStatusDate(now);
        job.setFromAddress(getFromAddress(appControl, website, group));
        job.setSubject(getSubject(appControl, website));
        job.setTitle(getTitle(appControl, website, r));
        job.setHtml(emailBody);
        job.setStatus(GroupEmailJob.STATUS_READY_TO_SEND);
        session.save(job);
        job.setGroupRecipients(getGroupRecipients(job, group, session));
        log.info("Created group email job: " + job.getId());
        return job.getId();
    }

    @Override
    public String getContentType(String accepts) {
        if (jsonResult != null) {
            return JsonResult.CONTENT_TYPE;
        }
        return "text/html";
    }

    @Override
    public boolean authorise(Request request, Method method, Auth auth) {
        if (method.equals(Method.GET) || method.equals(Method.POST)) {
            return true;
        }
        return super.authorise(request, method, auth);
    }

    /**
     * Content to appear above the form
     *
     * @return
     */
    public String getIntro() {
        String intro = appControl().getSetting("intro");
        return intro;
        //return "<p>The 3DN team is here to support you through your eLearning experience.  Whether you have a question regarding registering or want further information about the programs 3DN offer in the Intellectual Disability space, please feel free to use the form below and one of our friendly staff will get in touch as soon as possible.</p>";
    }

    public String getTitle() {
        RootFolder rf = WebUtils.findRootFolder(this);
        if (rf instanceof WebsiteRootFolder) {
            WebsiteRootFolder wrf = (WebsiteRootFolder) rf;
            return "<h1>Contact " + wrf.getWebsite().getName() + "</h1>";
        } else {
            return "<h1>Contact</h1>";
        }
    }

    public String getThankYouMessage() {
        String thankyou = appControl().getSetting("thankyou");
        if( thankyou == null || thankyou.trim().length() == 0 ) {
            thankyou = "Thank you for your message, we will reply shortly.";
        }
        return thankyou;
        //return "<p>Thank you for your enquiry. Our team will respond as soon as possible.</p>";
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
    public Date getModifiedDate() {
        return null;
    }

    @Override
    public Date getCreateDate() {
        return null;
    }

    @Override
    public Map<Principal, List<Priviledge>> getAccessControlList() {
        return null;
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return null;
    }

    @Override
    public Long getContentLength() {
        return null;
    }

    @Override
    public boolean isPublic() {
        return true;
    }

    private String toMessageHtml(ContactRequest r) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XmlWriter w = new XmlWriter(out);
        Element html = w.begin("html");
        Element body = html.begin("body");
        Element table = body.begin("table");
        addRow(table, "Name", r.getFirstName() + " " + r.getSurName());
        addRow(table, "Email", r.getEmail());
        addRow(table, "Phone", r.getPhone());
        addRow(table, "Company", r.getCompany());
        table.close();
        body.close();
        html.close();
        w.flush();
        try {
            return out.toString("UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    private List<GroupRecipient> getGroupRecipients(GroupEmailJob job, Group group, Session session) {
        List<GroupRecipient> list = new ArrayList<>();
        GroupRecipient gr = new GroupRecipient();
        gr.setJob(job);
        gr.setRecipient(group);
        session.save(gr);
        return list;
    }

    private String getSubject(AppControl appControl, Website website) {
        return "Website enquiry: " + website.getName();
    }

    private Group findGroup(AppControl appControl, Session session) {
        String sGroup = appControl.getSetting("group");
        if (sGroup == null) {
            sGroup = Group.ADMINISTRATORS;
        }
        Group g = getOrganisation().group(sGroup, session);
        if (g == null) {
            throw new RuntimeException("Cant find group: " + sGroup + " in org: " + getOrganisation().getName());
        }
        return g;
    }

    private String getFromAddress(AppControl appControl, Website website, Group group) {
        String s = group.getName() + "@";
        if (website.getDomainName() != null) {
            return s += website.getDomainName();
        } else {
            String domain = website.getName() + "." + _(CurrentRootFolderService.class).getPrimaryDomain();
            return s += domain;
        }
    }

    private void addRow(Element table, String name, String val) {
        Element tr = table.begin("tr");
        tr.begin("td").writeText(name).close();
        tr.begin("td").writeText(val).close();
        tr.close();
    }

    private String getTitle(AppControl appControl, Website website, ContactRequest r) {
        String name = r.getFirstName() + " " + r.getSurName();
        return "Contact from: " + name + " for " + website.getName();
    }

}
