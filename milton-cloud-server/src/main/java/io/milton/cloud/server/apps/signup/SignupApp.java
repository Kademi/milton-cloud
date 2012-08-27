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

import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.process.*;
import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;
import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.BrowsableApplication;
import io.milton.cloud.server.apps.ChildPageApplication;
import io.milton.cloud.server.apps.ReportingApplication;
import io.milton.cloud.server.apps.admin.WebsiteAccessReport;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.db.SignupLog;
import io.milton.cloud.server.event.SubscriptionEvent;
import io.milton.cloud.server.event.SubscriptionEvent.SubscriptionAction;
import io.milton.cloud.server.web.ResourceList;
import io.milton.cloud.server.web.RootFolder;
import io.milton.cloud.server.web.reporting.JsonReport;
import io.milton.event.Event;
import io.milton.event.EventListener;
import io.milton.event.EventManager;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.vfs.db.*;
import io.milton.vfs.db.utils.SessionManager;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class SignupApp implements ChildPageApplication, BrowsableApplication, EventListener, ReportingApplication {

    private static final Logger log = LoggerFactory.getLogger(SignupApp.class);
    
    private String signupPageName = "signup";
    private StateProcess userManagementProcess;
    private TimerService timerService;
    private CurrentDateService currentDateService;
    private EventManager eventManager;
    private List<JsonReport> reports;

    public SignupApp() {
        reports = new ArrayList<>();
        reports.add(new GroupSignupsReport());
        
    }

    @Override
    public String getInstanceId() {
        return "signup";
    }

    @Override
    public String getTitle(Organisation organisation, Website website) {
        return "User registration and signup";
    }

    @Override
    public String getSummary(Organisation organisation, Website website) {
        return "Provides signup pages, which allows people to join your websites and groups";
    }

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
        userManagementProcess = buildProcess();
        eventManager = resourceFactory.getEventManager();
        currentDateService = config.getContext().get(CurrentDateService.class);
        timerService = config.getContext().get(TimerService.class);
    }

    @Override
    public Resource getPage(Resource parent, String requestedName) {
        if (parent instanceof GroupInWebsiteFolder) {
            GroupInWebsiteFolder wrf = (GroupInWebsiteFolder) parent;
            if (requestedName.equals(signupPageName)) {
                return new GroupRegistrationPage(requestedName, wrf);
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
            System.out.println("addBrowseablePages: " + groupsInWebsite.size());
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
        b.from("start").transition("started").to("active").when(new TrueRule()).then(signupEvent(SubscriptionEvent.SubscriptionAction.ACCEPTED));;
        // TODO: more stuff here eventually
        return b.getProcess();
    }



    private ActionHandler signupEvent(SubscriptionEvent.SubscriptionAction a) {
        return new SubscriptionEventActionHandler(a);
    }

    public void onNewMembership(GroupMembership gm, RootFolder rf) {
        MembershipProcess pp = new MembershipProcess();
        pp.setMembership(gm);
        pp.setProcessName(userManagementProcess.getName());
        pp.setProcessVersion(1);

        ProcessContext context = new ProcessContext(pp, userManagementProcess, timerService, currentDateService);
        if (rf instanceof WebsiteRootFolder) {
            RootFolder wrf = rf;
            context.addAttribute("website", wrf);
        }
        context.scan();
        log.info("Final state: " + pp.getStateName());
    }

    @Override
    public void onEvent(Event e) {
    }

    public class SubscriptionEventActionHandler implements ActionHandler {

        private final SubscriptionEvent.SubscriptionAction action;

        public SubscriptionEventActionHandler(SubscriptionAction action) {
            this.action = action;
        }

        @Override
        public void process(ProcessContext context) {
            MembershipProcess pi = (MembershipProcess) context.getProcessInstance();
            GroupMembership gm = pi.getMembership();
            WebsiteRootFolder wrf = (WebsiteRootFolder) context.getAttribute("website");
            Website website = null;
            if (wrf != null) {
                website = wrf.getWebsite();
            }
            try {
                
                SubscriptionEvent e = new SubscriptionEvent(gm, website, action);
                eventManager.fireEvent(e);
            } catch (ConflictException | BadRequestException | NotAuthorizedException ex) {
                throw new RuntimeException(ex);
            }

        }
    }
}
