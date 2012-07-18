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
import io.milton.cloud.server.apps.Application;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.event.SignupEvent;
import io.milton.cloud.server.web.ResourceList;
import io.milton.vfs.db.GroupInWebsite;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.ProfileProcess;
import io.milton.vfs.db.Website;
import io.milton.vfs.db.utils.SessionManager;
import java.util.List;

/**
 *
 * @author brad
 */
public class SignupApp implements Application {

    private String signupPageName = "signup";
    private StateProcess userManagementProcess;

    private TimerService timerService;
    private CurrentDateService currentDateService;
    
    public SignupApp() {
    }

    @Override
    public String getInstanceId() {
        return "signup";
    }

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
        userManagementProcess = buildProcess();
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
        b.from("start").transition("autoApproved").to("active").when(autoApproved()).then(signupEvent(SignupEvent.SignupAction.AUTOAPPROVED));
        b.from("start").transition("review").to("pending").when(new TrueRule());

        b.from("pending").transition("accepted").to("active").when(accountEnabled()).then(signupEvent(SignupEvent.SignupAction.ACCEPTED));
        b.from("pending").transition("rejected").to("disabled").when(hasVariable("rejected", "true")).then(signupEvent(SignupEvent.SignupAction.REJECTED));

        b.from("active").transition("disabled").to("disabled").when(accountDisabled()).then(signupEvent(SignupEvent.SignupAction.DISABLED));
        b.from("active").transition("lapsed").to("disabled").when(membershipLapsed()).then(signupEvent(SignupEvent.SignupAction.LAPSED));
        b.from("active").transition("paymentOverdue").to("disabled").when(membershipLapsed()).then(signupEvent(SignupEvent.SignupAction.PAYMENT_OVERDUE));

        b.from("disabled").transition("reActivated").to("active").when(accountEnabled()).then(signupEvent(SignupEvent.SignupAction.RE_ACTIVATED));



        return b.getProcess();
    }

    private Rule autoApproved() {
        return new AutoApproved();
    }

    private Rule accountDisabled() {
        return new AccountDisabled();
    }

    private Rule accountEnabled() {
        return new AccountEnabled();
    }

    private Rule hasVariable(String rejected, String atrue) {
        return null;
        //throw new UnsupportedOperationException("Not yet implemented");
    }

    private Rule membershipLapsed() {
        return null;
        //throw new UnsupportedOperationException("Not yet implemented");
    }

    private ActionHandler signupEvent(SignupEvent.SignupAction a) {
        return null;
    }

    public void onNewProfile(Profile u) {
        ProfileProcess pp = new ProfileProcess();
        pp.setProfile(u);
        pp.setProcessName(userManagementProcess.getName());
        pp.setProcessVersion(1);
        
        ProcessContext context = new ProcessContext(pp, userManagementProcess, timerService, currentDateService);
        context.scan();
        
    }

    public class AutoApproved implements Rule {

        @Override
        public boolean eval(ProcessContext context) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    public class AccountDisabled implements Rule {

        @Override
        public boolean eval(ProcessContext context) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    public class AccountEnabled implements Rule {

        @Override
        public boolean eval(ProcessContext context) {
            //context.getProcessInstance();
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
