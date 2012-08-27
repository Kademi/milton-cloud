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
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.db.SignupLog;
import io.milton.cloud.server.event.SubscriptionEvent;
import io.milton.cloud.server.event.SubscriptionEvent.SignupAction;
import io.milton.cloud.server.web.ResourceList;
import io.milton.cloud.server.web.RootFolder;
import io.milton.event.Event;
import io.milton.event.EventListener;
import io.milton.event.EventManager;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.vfs.db.*;
import io.milton.vfs.db.utils.SessionManager;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class SignupApp implements ChildPageApplication, BrowsableApplication, EventListener {

    private static final Logger log = LoggerFactory.getLogger(SignupApp.class);
    
    private String signupPageName = "signup";
    private StateProcess userManagementProcess;
    private TimerService timerService;
    private CurrentDateService currentDateService;
    private EventManager eventManager;

    public SignupApp() {
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

    private StateProcess buildProcess() {
        StateProcessBuilder b = new StateProcessBuilder("userSubscription", "start");
        b.from("start").transition("autoApproved").to("active").when(autoApproved()).then(signupEvent(SubscriptionEvent.SignupAction.AUTOAPPROVED));
        b.from("start").transition("review").to("pending").when(new TrueRule());

        b.from("pending").transition("accepted").to("active").when(accountEnabled()).then(signupEvent(SubscriptionEvent.SignupAction.ACCEPTED));
        b.from("pending").transition("rejected").to("disabled").when(rejected()).then(signupEvent(SubscriptionEvent.SignupAction.REJECTED));

        b.from("active").transition("disabled").to("disabled").when(accountDisabled()).then(signupEvent(SubscriptionEvent.SignupAction.DISABLED));
        b.from("active").transition("lapsed").to("disabled").when(membershipLapsed()).then(signupEvent(SubscriptionEvent.SignupAction.LAPSED));
        b.from("active").transition("paymentOverdue").to("disabled").when(membershipLapsed()).then(signupEvent(SubscriptionEvent.SignupAction.PAYMENT_OVERDUE));

        b.from("disabled").transition("reActivated").to("active").when(accountEnabled()).then(signupEvent(SubscriptionEvent.SignupAction.RE_ACTIVATED));

        return b.getProcess();
    }

    private Rule autoApproved() {
        return new AutoApproved();
    }

    private Rule accountDisabled() {
        return new AccountEnabledRule(false);
    }

    private Rule accountEnabled() {
        return new AccountEnabledRule(true);
    }

    private Rule rejected() {
        return new RejectedRule();
    }

    private Rule membershipLapsed() {
        return null;
        //throw new UnsupportedOperationException("Not yet implemented");
    }

    private ActionHandler signupEvent(SubscriptionEvent.SignupAction a) {
        return new SubscriptionEventActionHandler(a);
    }

    public void onNewProfile(Profile u, Group groupToJoin, RootFolder rf) {
        ProfileProcess pp = new ProfileProcess();
        pp.setProfile(u);
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
        log.info("onEvent" + e);
        if (e instanceof SubscriptionEvent) {
            // although these events are raised from within this app, they can be
            // raised from anywhere, eg other apps might also do signups
            SubscriptionEvent se = (SubscriptionEvent) e;
            log.info("process subscription event: group=" + se.getGroup().getName());
            SignupLog.logSignup(se.getWebsite(), se.getProfile(), se.getMemberOfOrg(), se.getGroup(), SessionManager.session());
        }
    }

    public class SubscriptionEventActionHandler implements ActionHandler {

        private final SubscriptionEvent.SignupAction action;

        public SubscriptionEventActionHandler(SignupAction action) {
            this.action = action;
        }

        @Override
        public void process(ProcessContext context) {
            ProfileProcess pi = (ProfileProcess) context.getProcessInstance();
            Profile p = pi.getProfile();
            WebsiteRootFolder wrf = (WebsiteRootFolder) context.getAttribute("website");
            Website website = null;
            if (wrf != null) {
                website = wrf.getWebsite();
            }
            try {
                if (p.getMemberships() == null || p.getMemberships().isEmpty()) {
                    SubscriptionEvent e = new SubscriptionEvent(p, null, website, null, action);
                    eventManager.fireEvent(e);
                } else {
                    for (GroupMembership m : p.getMemberships()) {
                        SubscriptionEvent e = new SubscriptionEvent(p, m.getGroupEntity(), website, m.getWithinOrg(), action);
                        eventManager.fireEvent(e);
                    }
                }
            } catch (ConflictException | BadRequestException | NotAuthorizedException ex) {
                throw new RuntimeException(ex);
            }

        }
    }

    public class RejectedRule implements Rule {

        @Override
        public boolean eval(ProcessContext context) {
            ProfileProcess pi = (ProfileProcess) context.getProcessInstance();
            Profile p = pi.getProfile();
            return p.isRejected();
        }
    }

    public class AutoApproved implements Rule {

        @Override
        public boolean eval(ProcessContext context) {
            log.info("AutoApproved: eval");
            ProfileProcess pi = (ProfileProcess) context.getProcessInstance();
            // Check that all groups are open
            Profile p = pi.getProfile();
            if (p.getMemberships() == null) {
                // can't be open if there are no groups
                log.info("AutoApproved: not auto approved because has no memberships");
                return false;
            }
            for (GroupMembership gm : p.getMemberships()) {
                if (!Group.REGO_MODE_OPEN.equals(gm.getGroupEntity().getRegistrationMode())) {
                    System.out.println("Not open group: " + gm.getGroupEntity().getName());
                    return false;
                }
            }
            log.info("AutoApproved: eval: no closed groups so return true");
            return true;
        }
    }

    /**
     * This rule is true when the user's account has the given value
     */
    public class AccountEnabledRule implements Rule {

        private final boolean whenEnabled;

        public AccountEnabledRule(boolean whenEnabled) {
            this.whenEnabled = whenEnabled;
        }

        @Override
        public boolean eval(ProcessContext context) {
            ProfileProcess pi = (ProfileProcess) context.getProcessInstance();
            // Check that all groups are open
            Profile p = pi.getProfile();
            return p.isEnabled() == whenEnabled;
        }
    }
}
