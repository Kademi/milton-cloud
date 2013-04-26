/*
 * Copyright (C) 2012 McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.apps.signup;

import io.milton.cloud.server.web.GroupInWebsiteFolder;
import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.process.*;
import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;
import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.BrowsableApplication;
import io.milton.cloud.server.apps.ChildPageApplication;
import io.milton.cloud.server.apps.PortletApplication;
import io.milton.cloud.server.apps.ReportingApplication;
import io.milton.cloud.server.apps.SettingsApplication;
import io.milton.cloud.server.apps.orgs.OrganisationFolder;
import io.milton.cloud.server.apps.reporting.ReportingApp;
import io.milton.cloud.server.apps.user.DashboardPage;
import io.milton.cloud.server.apps.user.UserDashboardApp;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.db.GroupMembershipApplication;
import io.milton.cloud.server.db.SignupLog;
import io.milton.cloud.server.event.SubscriptionEvent;
import io.milton.cloud.server.event.SubscriptionEvent.SubscriptionAction;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.CommonResource;
import io.milton.cloud.server.web.JsonResult;
import io.milton.cloud.server.web.ResourceList;
import io.milton.cloud.server.web.RootFolder;
import io.milton.cloud.server.web.Utils;
import io.milton.cloud.server.web.WebUtils;
import io.milton.cloud.server.web.reporting.JsonReport;
import io.milton.cloud.server.web.templating.Formatter;
import io.milton.cloud.server.web.templating.TextTemplater;
import io.milton.event.Event;
import io.milton.event.EventListener;
import io.milton.event.EventManager;
import io.milton.http.FileItem;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.vfs.db.*;
import io.milton.vfs.db.utils.SessionManager;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.velocity.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.milton.context.RequestContext._;
import java.util.Date;
import org.hibernate.Session;

/**
 *
 * @author brad
 */
public class SignupApp implements ChildPageApplication, BrowsableApplication, EventListener, ReportingApplication, SettingsApplication, PortletApplication {

    private static final Logger log = LoggerFactory.getLogger(SignupApp.class);
    public static final String NEXT_HREF = "signup.next.href";
    public static final String REDIRECT_WEBSITE = "signup.reidrect.website";
    private String signupPageName = "signup";
    private StateProcess userManagementProcess;
    private TimerService timerService;
    private CurrentDateService currentDateService;
    private EventManager eventManager;
    private List<JsonReport> reports;
    private AppConfig config;

    public SignupApp() {
        reports = new ArrayList<>();
        reports.add(new GroupSignupsReport());

    }

    @Override
    public String getInstanceId() {
        return "signup";
    }

    @Override
    public String getTitle(Organisation organisation, Branch websiteBranch) {
        return "User registration and signup";
    }

    @Override
    public String getSummary(Organisation organisation, Branch websiteBranch) {
        return "Provides signup pages, which allows people to join your websites and groups";
    }

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
        userManagementProcess = buildProcess();
        eventManager = resourceFactory.getEventManager();
        currentDateService = config.getContext().get(CurrentDateService.class);
        timerService = config.getContext().get(TimerService.class);
        this.config = config;
    }

    @Override
    public Resource getPage(Resource parent, String requestedName) {
        if (parent instanceof GroupInWebsiteFolder) {
            GroupInWebsiteFolder giwf = (GroupInWebsiteFolder) parent;
            if (requestedName.equals(signupPageName)) {
                if (Group.REGO_MODE_CLOSED.equals(giwf.getGroup().getRegistrationMode())) {
                    log.warn("Attempt to access CLOSED rego page for group: " + giwf.getGroup().getName());
                } else {
                    return new GroupRegistrationPage(requestedName, giwf, this);
                }
            } else if (requestedName.equals("dash")) {
                boolean openGroup = Group.REGO_MODE_OPEN.equals(giwf.getGroup().getRegistrationMode());
                return new DashboardPage(requestedName, giwf, !openGroup); // dont require login if group is open. Although will challenge when they try to access a page
            }
        }
        if (parent instanceof OrganisationFolder) {
            OrganisationFolder orgFolder = (OrganisationFolder) parent;
            if (requestedName.equals("pendingApps")) {
                return new ProcessPendingPage("pendingApps", orgFolder, this);
            } else if (requestedName.equals("signupSearch")) {
                return new SignupsSearchPage(orgFolder, requestedName);
            }
        }

        if (parent instanceof CommonCollectionResource) {
            if (requestedName.equals("registerOrLogin")) {
                CommonCollectionResource wrf = (CommonCollectionResource) parent;
                return new RegisterOrLoginPage(wrf, requestedName);
            }
        }

        return null;
    }

    @Override
    public void addBrowseablePages(CollectionResource parent, ResourceList children) {
        if (parent instanceof WebsiteRootFolder) {
            WebsiteRootFolder wrf = (WebsiteRootFolder) parent;
            Website website = wrf.getWebsite();
            List<GroupInWebsite> groupsInWebsite = website.groups(SessionManager.session());
            for (GroupInWebsite giw : groupsInWebsite) {
                GroupInWebsiteFolder f = new GroupInWebsiteFolder(giw, wrf);
                children.add(f);
            }
        }
    }

    @Override
    public List<JsonReport> getReports(Organisation org, Website website) {
        return reports;
    }

    private StateProcess buildProcess() {
        StateProcessBuilder b = new StateProcessBuilder("userSubscription", "start");
        b.from("start").transition("started").to("active").when(new TrueRule()).then(signupEvent(SubscriptionEvent.SubscriptionAction.ACCEPTED));
        // TODO: more stuff here eventually
        return b.getProcess();
    }

    private ActionHandler signupEvent(SubscriptionEvent.SubscriptionAction a) {
        return new SubscriptionEventActionHandler(a);
    }

    public void onNewMembership(GroupMembership gm, RootFolder rf) {
        long tm = System.currentTimeMillis();
        MembershipProcess pp = new MembershipProcess();
        pp.setMembership(gm);
        pp.setProcessName(userManagementProcess.getName());
        pp.setProcessVersion(1);

        ProcessContext context = new ProcessContext(pp, userManagementProcess, timerService, currentDateService);
        context.addAttribute("organisation", gm.getGroupEntity().getOrganisation());
        if (rf instanceof WebsiteRootFolder) {
            RootFolder wrf = rf;
            context.addAttribute("website", wrf);
        }        
        context.scan();
        if (log.isDebugEnabled()) {
            log.debug("Final state: " + pp.getStateName() + " processed in: " + (System.currentTimeMillis() - tm) + "ms");
        }
    }

    /**
     * Get the URL to redirect the new user to
     *
     * @param newUser
     * @return
     */
    public String getNextHref(Profile newUser, Branch websiteBranch) {
        String s = config.get(NEXT_HREF, websiteBranch);
        return s;
    }

    @Override
    public void onEvent(Event e) {
    }

    @Override
    public void renderSettings(Profile currentUser, Organisation org, Branch websiteBranch, Context context, Writer writer) throws IOException {
        String href; // = findSetting("gaAccountNumber", rootFolder);
        if (websiteBranch != null) {
            href = config.get(NEXT_HREF, websiteBranch);
        } else {
            href = config.get(NEXT_HREF, org);
        }
        if (href == null) {
            href = "";
        }
        writer.write("<label for='signupNextHref'>First page after signup</label>");
        writer.write("<input type='text' id='signupNextHref' name='signupNextHref' value='" + href + "' />");
        writer.write("<br/>");
        writer.write("<label for='signupNextHref'>Redirect to website</label>");
        Formatter formatter = _(Formatter.class);
        String redirWebsite;
        if (websiteBranch != null) {
            redirWebsite = config.get(REDIRECT_WEBSITE, websiteBranch);
        } else {
            redirWebsite = config.get(REDIRECT_WEBSITE, org);
        }
        writer.write("<select name='" + REDIRECT_WEBSITE + "'>\n");
        writer.write("  <option>[Please select]</option>");
        System.out.println("websites: " + org.websites().size());
        for (Website w : org.websites()) {
            writer.write(formatter.option(w.getName(), w.getName(), redirWebsite));
        }
        writer.write("</select>");
        writer.flush();
    }

    @Override
    public JsonResult processForm(Map<String, String> parameters, Map<String, FileItem> files, Organisation org, Branch websiteBranch) throws BadRequestException, NotAuthorizedException, ConflictException {
        String signupNextHref = parameters.get("signupNextHref");
        String redirWebsite = parameters.get(REDIRECT_WEBSITE);
        if (websiteBranch != null) {
            config.set(NEXT_HREF, websiteBranch, signupNextHref);
            config.set(REDIRECT_WEBSITE, websiteBranch, redirWebsite);
        } else {
            config.set(NEXT_HREF, org, signupNextHref);
            config.set(REDIRECT_WEBSITE, org, redirWebsite);
        }
        return new JsonResult(true);
    }

    @Override
    public void renderPortlets(String portletSection, Profile currentUser, RootFolder rootFolder, Context context, Writer writer) throws IOException {
        if (portletSection.equals("adminDashboardPrimary")) {
            CommonResource r = (CommonResource) context.get("page");
            Organisation org = r.getOrganisation();
            if (!Utils.isEmpty(org.getWebsites())) {

                // Generate recent signups link
                Date minus7Days = _(Formatter.class).addDays(_(Formatter.class).getNow(), -14);
                long count = SignupLog.countOfSignups(org, minus7Days, null);
                writer.append("<div class='alert'>\n");
                writer.append("<h3>Signups in last 14 days: <a href='signupSearch'><strong>" + count + "</strong></a><a class='Btn' href='signupSearch'>review</a></h3>\n");
                writer.append("</div>\n");


                List<GroupMembershipApplication> applications = GroupMembershipApplication.findByAdminOrg(r.getOrganisation(), SessionManager.session());
                context.put("applications", applications);
                _(TextTemplater.class).writePage("signup/pendingAccountsPortlet.html", currentUser, rootFolder, context, writer);


                writer.append("<div class='report'>\n");
                writer.append("<h3>Signup activity</h3>\n");
                writer.append("<div class='signupReport'></div>\n");
                writer.append("<script type='text/javascript' >\n");
                writer.append("jQuery(function() {\n");
                //17/09/2012 - 24/09/2012
                String range = ReportingApp.getDashboardDateRange();
                OrganisationFolder orgFolder = WebUtils.findParentOrg(r);
                if (orgFolder != null) {
                    //http://localhost:8080/organisations/3dn/reporting/org-learningProgress?startDate=Choose+a+date+range&finishDate=
                    String href = orgFolder.getHref() + "reporting/org-groupSignups";
                    writer.append(" runReport(\"" + range + "\", jQuery('.report .signupReport'), null, \"" + href + "\");\n");
                    writer.append("});\n");
                    writer.append("</script>\n");
                }
                writer.append("</div>\n");
            }

        }
    }

    private boolean isNewOrg(Organisation org) {
        Date now = currentDateService.getNow();
        Date endGettingStartedDate = _(Formatter.class).addDays(org.getCreatedDate(), 7);
        return now.before(endGettingStartedDate);
    }

    /**
     * accept or reject the given app
     *
     * @param gma
     * @param b
     */
    public void processPending(GroupMembershipApplication gma, Boolean b, Session session) {
        if (b) {
            Profile p = gma.getMember();
            Organisation org = gma.getWithinOrg();
            Group group = gma.getGroupEntity();
            p.addToGroup(group, org, session);
            SignupLog.logSignup(gma.getWebsite(), p, org, group, SessionManager.session());
        } else {
            // TODO: send rejected email
        }
        session.delete(gma);
    }

    public class SubscriptionEventActionHandler implements ActionHandler {

        private final SubscriptionEvent.SubscriptionAction action;

        public SubscriptionEventActionHandler(SubscriptionAction action) {
            this.action = action;
        }

        @Override
        public void process(ProcessContext context) {
            long tm = System.currentTimeMillis();
            MembershipProcess pi = (MembershipProcess) context.getProcessInstance();
            GroupMembership gm = pi.getMembership();
            Organisation org = (Organisation) context.getAttribute("organisation");
            WebsiteRootFolder wrf = (WebsiteRootFolder) context.getAttribute("website");
            Website website = null;
            if (wrf != null) {
                website = wrf.getWebsite();
            }
            try {
                SubscriptionEvent e = new SubscriptionEvent(gm, website, org, action);
                eventManager.fireEvent(e);
                if(log.isDebugEnabled()) {
                    log.debug("Fired " + e + " Duration=" + (System.currentTimeMillis() - tm) + "ms");
                }
            } catch (ConflictException | BadRequestException | NotAuthorizedException ex) {
                throw new RuntimeException(ex);
            }

        }
    }

    public String getSignupPageName() {
        return signupPageName;
    }
}
