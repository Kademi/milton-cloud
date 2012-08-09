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

import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.LifecycleApplication;
import io.milton.cloud.server.apps.MenuApplication;
import io.milton.cloud.server.apps.orgs.OrganisationFolder;
import io.milton.cloud.server.db.AccessLog;
import io.milton.cloud.server.web.*;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.context.Context;
import io.milton.context.Executable2;
import io.milton.context.RootContext;
import io.milton.event.Event;
import io.milton.event.EventListener;
import io.milton.event.ResponseEvent;
import io.milton.http.Request;
import io.milton.http.Response;
import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Website;
import io.milton.vfs.db.utils.SessionManager;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class ReportingApp implements MenuApplication, EventListener, LifecycleApplication {

    private static final Logger log = LoggerFactory.getLogger(ReportingApp.class);
    
    private final java.util.concurrent.LinkedBlockingQueue<Access> queue = new LinkedBlockingQueue<>();

    private RootContext rootContext;
    
    private Thread threadInserter;
    
    private SessionManager sessionManager;

    private SpliffySecurityManager securityManager;


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
    public String getSummary(Organisation organisation, Website website) {
        return "Provides reporting and analytics of user behaviour";
    }

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {        
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
        if( parent instanceof OrganisationFolder) {
            OrganisationFolder p = (OrganisationFolder) parent;
            if( requestedName.equals("siteActivity")) {
                return new SiteActivityReportPage(requestedName, p);
            }
        }
        return null;
    }

    @Override
    public void addBrowseablePages(CollectionResource parent, ResourceList children) {
    }

    @Override
    public void appendMenu(MenuItem parent) {
        String parentId = parent.getId();
        if (parentId.equals("menuRoot")) {
            OrganisationFolder parentOrg = WebUtils.findParentOrg(parent.getResource());
            if (parentOrg != null) {
                parent.getOrCreate("menuReporting", "Reporting", parentOrg.getPath().child("reporting"));
            }
        }
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
        if( profile != null ) {
            user = profile.getName();
        }
        Access a = new Access(h, path, referrerUrl, result, duration, size, method, response.getContentTypeHeader(), from, user);
        queue.add(a);

    }


    public class Access {

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

        public Access(String host, String url, String referrerUrl, int result, long duration, Long size, String method, String contentType, String fromAddress, String user) {
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
                        AccessLog al = new AccessLog();
                        al.setReqHost(access.host);
                        al.setUrl(access.url);
                        al.setReferrer(access.referrerUrl);
                        Date dt = new Date();
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(dt);
                        al.setReqDate(new java.sql.Date(dt.getTime()));
                        al.setReqYear(cal.get(Calendar.YEAR));                        
                        al.setReqMonth(cal.get(Calendar.MONTH) );
                        al.setReqDay( cal.get(Calendar.DAY_OF_MONTH));
                        al.setReqHour( cal.get(Calendar.HOUR_OF_DAY));
                        al.setResultCode( access.result);
                        al.setDurationMs( access.duration);
                        al.setNumBytes( access.size);
                        al.setReqMethod( access.method);
                        al.setContentType(access.contentType);
                        al.setReqFrom( access.fromAddress);
                        al.setReqUser(access.user);
                        session.save(al);
                        tx.commit();
                    } catch (Exception ex) {
                        log.error("Exception logging access",ex);
                        tx.rollback();
                    } finally {
                        sessionManager.close();
                    }
                }
            });
        }        
    }    
}
