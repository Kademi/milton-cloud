/*
 * Copyright 2013 McEvoy Software Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.apps.signup;

import io.milton.cloud.server.web.GroupInWebsiteFolder;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.web.AbstractResource;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.RootFolder;
import io.milton.cloud.server.web.WebUtils;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.GetableResource;
import io.milton.vfs.db.Organisation;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import static io.milton.context.RequestContext._;
import io.milton.http.Request.Method;
import io.milton.resource.Resource;
import io.milton.vfs.db.Group;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author brad
 */
public class RegisterOrLoginPage extends AbstractResource implements GetableResource {

    private CommonCollectionResource parent;
    private String name;

    public RegisterOrLoginPage(CommonCollectionResource parent, String name) {
        this.parent = parent;
        this.name = name;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        _(HtmlTemplater.class).writePage("signup/registerOrLogin", this, params, out);
    }

    public GroupRegistrationPage getGroupRegoPage() throws NotAuthorizedException, BadRequestException {
        SignupApp signupApp = _(SignupApp.class);
        GroupInWebsiteFolder giwf = (GroupInWebsiteFolder) parent.closest("group");
        if (giwf != null) {
            if (giwf.getGroup().getRegistrationMode().equals(Group.REGO_MODE_OPEN)) {
                GroupRegistrationPage p = new GroupRegistrationPage(signupApp.getSignupPageName(), giwf, signupApp);
                return p;
            }
        }
        List<GroupRegistrationPage> list = getGroupRegoPages();
        if( !list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    public List<GroupRegistrationPage> getGroupRegoPages() throws NotAuthorizedException, BadRequestException {
        SignupApp signupApp = _(SignupApp.class);

        List<GroupRegistrationPage> pages = new ArrayList<>();
        RootFolder rf = WebUtils.findRootFolder(this);
        for (Resource r : rf.getChildren()) {
            if (r instanceof GroupInWebsiteFolder) {
                GroupInWebsiteFolder giwf = (GroupInWebsiteFolder) r;
                if (giwf.getGroup().getRegistrationMode().equals(Group.REGO_MODE_OPEN)) {
                    GroupRegistrationPage p = new GroupRegistrationPage(signupApp.getSignupPageName(), giwf, signupApp);
                    pages.add(p);
                }
            }
        }
        return pages;
    }

    public boolean isHasOrgs() {
        List<Organisation> childOrgs = getOrganisation().getChildOrgs();
        return childOrgs != null && !childOrgs.isEmpty();
    }

    @Override
    public boolean authorise(Request request, Method method, Auth auth) {
        return true;
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
    public Priviledge getRequiredPostPriviledge(Request request) {
        return Priviledge.READ_CONTENT;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return null;
    }

    @Override
    public Long getContentLength() {
        return null;
    }
}
