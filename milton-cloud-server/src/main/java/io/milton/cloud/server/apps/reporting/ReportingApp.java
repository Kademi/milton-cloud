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
package io.milton.cloud.server.apps.reporting;

import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.Application;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.apps.ChildPageApplication;
import io.milton.cloud.server.apps.LifecycleApplication;
import io.milton.cloud.server.apps.MenuApplication;
import io.milton.cloud.server.apps.PortletApplication;
import io.milton.cloud.server.apps.ReportingApplication;
import io.milton.cloud.server.apps.orgs.OrganisationFolder;
import io.milton.cloud.server.apps.orgs.OrganisationRootFolder;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.db.AccessLog;
import io.milton.cloud.server.manager.CurrentRootFolderService;
import io.milton.cloud.server.web.*;
import io.milton.cloud.server.web.reporting.JsonReport;
import io.milton.cloud.server.web.templating.Formatter;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.common.Path;
import io.milton.context.Context;
import io.milton.context.Executable2;
import io.milton.context.RootContext;
import io.milton.event.Event;
import io.milton.event.EventListener;
import io.milton.event.ResponseEvent;
import io.milton.http.Request;
import io.milton.http.Response;
import io.milton.resource.Resource;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Website;
import io.milton.vfs.db.utils.SessionManager;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.milton.context.RequestContext._;
import io.milton.http.HttpManager;
import io.milton.vfs.db.Branch;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author brad
 */
public class ReportingApp implements MenuApplication, EventListener, LifecycleApplication, ChildPageApplication, PortletApplication {

    private static final Logger log = LoggerFactory.getLogger(ReportingApp.class);
    
    public static String getDashboardDateRange() {
        Date now = _(CurrentDateService.class).getNow();
        now = _(Formatter.class).addDays(now, -7);
        return formatDate(now);
    }
    
    public static SimpleDateFormat sdf() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        sdf.setLenient(false);
        return sdf;
    }
    
    public static String formatDate(Date dt) {
        if (dt == null) {
            return "";
        }
        return sdf().format(dt);
    }    
        
    
    private final java.util.concurrent.LinkedBlockingQueue<Access> queue = new LinkedBlockingQueue<>();
    private RootContext rootContext;
    private Thread threadInserter;
    private SessionManager sessionManager;
    private SpliffySecurityManager securityManager;
    private CurrentRootFolderService currentRootFolderService;
    private ApplicationManager applicationManager;

    @Override
    public void initDefaultProperties(AppConfig config) {
    }

    @Override
    public void shutDown() {
        threadInserter.interrupt();
    }

    @Override
    public String getInstanceId() {
        return "Reporting";
    }

    @Override
    public String getTitle(Organisation organisation, Branch websiteBranch) {
        return "Reports";
    }

    @Override
    public String getSummary(Organisation organisation, Branch websiteBranch) {
        return "Provides reporting and analytics of user behaviour";
    }

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
        applicationManager = resourceFactory.getApplicationManager();
        currentRootFolderService = config.getContext().get(CurrentRootFolderService.class);
        sessionManager = resourceFactory.getSessionManager();
        securityManager = resourceFactory.getSecurityManager();
        resourceFactory.getEventManager().registerEventListener(this, ResponseEvent.class);
        this.rootContext = config.getContext();

        log.trace("starting stats logging daemon");
        threadInserter = new Thread(new Inserter());
        threadInserter.setDaemon(true);
        threadInserter.start();

    }

    @Override
    public Resource getPage(Resource parent, String requestedName) {
        if (parent instanceof OrganisationFolder) {
            OrganisationFolder p = (OrganisationFolder) parent;
            if (requestedName.equals("reporting")) {
                return new ReportsHomeFolder(requestedName, p);
            }
        }
        return null;
    }

    @Override
    public void appendMenu(MenuItem parent) {
        String parentId = parent.getId();
        if (parentId.equals("menuRoot")) {
            OrganisationFolder parentOrg = WebUtils.findParentOrg(parent.getResource());
            if (parentOrg != null) {
                parent.getOrCreate("menuReporting", "Reporting", parentOrg.getPath().child("reporting"));
            }
        } else if (parentId.equals("menuReporting")) {
            OrganisationFolder parentOrg = WebUtils.findParentOrg(parent.getResource());
            Path parentPath = parentOrg.getPath().child("reporting");
            // Add menu items for each website
            Organisation org = parentOrg.getOrganisation();
            if (org.getWebsites() != null) {
                for (Website w : org.getWebsites()) {
                    parent.getOrCreate("menuReportsWebsite" + w.getName(), w.getName(), parentPath.child(w.getName()));
                }
            }
        } else if (parentId.startsWith("menuReportsWebsite")) {
            String websiteName = parentId.replace("menuReportsWebsite", "");
            Website w = Website.findByName(websiteName, SessionManager.session());
            if (w != null) {
                // Get list of reports
                for (JsonReport report : findReports(w)) {
                    parent.getOrCreate("menuReport_" + report.getReportId(), report.getTitle(w.getOrganisation(), w), parent.getHref() + "/" + report.getReportId());
                }
            }
        }
    }

    private List<JsonReport> findReports(Website website) {
        List<JsonReport> reports = new ArrayList<>();
        Branch b = website.liveBranch();
        for (Application app : applicationManager.findActiveApps(b)) {
            if (app instanceof ReportingApplication) {
                ReportingApplication rapp = (ReportingApplication) app;
                for (JsonReport r : rapp.getReports(website.getOrganisation(), website)) {
                    reports.add(r);
                }
            }
        }
        return reports;
    }

    @Override
    public void onEvent(Event e) {
        if (e instanceof ResponseEvent) {
            ResponseEvent re = (ResponseEvent) e;
            log(re.getRequest(), re.getResponse(), re.getDuration());
        }
    }

    private void log(Request request, Response response, long duration) {
        System.out.println("adding to queue: " + queue.size());
        String host = request.getHostHeader();
        if (host == null) {
            host = "";
        }
        String h = host.replace("test.", "www."); // TODO: nasty little hack. need to bring statsfilter together with statsresourcefactory
        String path = request.getAbsolutePath();
        String referrerUrl = request.getRefererHeader();
        int result = response.getStatus() == null ? 500 : response.getStatus().code;
        String from = request.getFromAddress();
        Long size = response.getContentLength();
        String method = request.getMethod().code;
        String user = null;
        Profile profile = securityManager.getCurrentUser();
        if (profile != null) {
            user = profile.getName();
        }
        RootFolder rootFolder = currentRootFolderService.peekRootFolder();
        Long orgId = null;
        Long websiteId = null;
        if (rootFolder != null) {
            orgId = rootFolder.getOrganisation().getId();
            if (rootFolder instanceof WebsiteRootFolder) {
                WebsiteRootFolder wrf = (WebsiteRootFolder) rootFolder;
                websiteId = wrf.getWebsite().getId();
            }
        }
        Access a = new Access(orgId, websiteId, h, path, referrerUrl, result, duration, size, method, response.getContentTypeHeader(), from, user);
        queue.add(a);

    }

    @Override
    public void renderPortlets(String portletSection, Profile currentUser, RootFolder rootFolder, org.apache.velocity.context.Context context, Writer writer) throws IOException {
        if (rootFolder instanceof OrganisationRootFolder) { // only for admin console
            // add js resources to header, so that any page that displays reports has what it needs
            if (PortletApplication.PORTLET_SECTION_HEADER.equals(portletSection)) {
                if (currentUser != null) { // don't bother if no one logged in
                    writer.append("<script src='/static/js/raphael-2.0.2.min.js'>//</script>\n");
                    writer.append("<script src='/static/js/morris.js'>//</script>\n");
                    writer.append("<script type='text/javascript' src='/static/js/jquery-ui-1.8.20.custom.min.js'>//</script>\n");
                    writer.append("<script type='text/javascript' src='/static/daterange/daterangepicker.jQuery.js'>//</script>\n");
                    writer.append("<script type='text/javascript' src='/templates/apps/reporting/reports.js'>//</script>\n");
                    writer.append("<link rel='stylesheet' href='/static/daterange/ui.daterangepicker.css' type='text/css' />\n");
                    writer.append("<link rel='stylesheet' href='/static/common/jquery-ui-1.8.11.custom.css' type='text/css' title='ui-theme' />\n");
                    writer.append("<link href='/templates/apps/reporting/reports.css' rel='stylesheet' type='text/css' />\n");
                }
            }
        }
    }

    public class Access {

        private Long organisationId;
        private Long websiteId;
        private String host;
        private String url;
        private String referrerUrl;
        private int result;
        private long duration;
        private Long size;
        private String method;
        private String contentType;
        private String fromAddress;
        private String user;

        public Access(Long organisationId, Long websiteId, String host, String url, String referrerUrl, int result, long duration, Long size, String method, String contentType, String fromAddress, String user) {
            this.organisationId = organisationId;
            this.websiteId = websiteId;
            this.host = host;
            this.url = url;
            this.referrerUrl = referrerUrl;
            this.result = result;
            this.duration = duration;
            this.size = size;
            this.method = method;
            this.contentType = contentType;
            this.fromAddress = fromAddress;
            this.user = user;
        }
    }

    private class Inserter implements Runnable {

        @Override
        public void run() {
            boolean running = true;
            while (running) {
                try {
                    Access a = queue.take();
                    log.trace("insert log");
                    doInsert(a);
                } catch (InterruptedException ex) {
                    log.warn("inserter operation terminated", ex);
                    running = false;
                }
            }
            log.warn("inserter stopped");
        }

        private void doInsert(final Access access) {
            rootContext.execute(new Executable2() {
                @Override
                public void execute(Context context) {
                    Session session = sessionManager.open();
                    Transaction tx = session.beginTransaction();
                    try {
                        Organisation org = null;
                        if (access.organisationId != null) {
                            org = (Organisation) session.get(Organisation.class, access.organisationId);
                        }
                        Website website = null;
                        if (access.websiteId != null) {
                            website = (Website) session.get(Website.class, access.websiteId);
                        }

                        AccessLog.insert(org, website, access.host, access.url, access.referrerUrl, access.result, access.duration, access.size, access.method, access.contentType, access.fromAddress, access.user, session);
                        tx.commit();
                    } catch (Exception ex) {
                        log.error("Exception logging access", ex);
                        tx.rollback();
                    } finally {
                        sessionManager.close();
                    }
                }
            });
        }
    }
}
