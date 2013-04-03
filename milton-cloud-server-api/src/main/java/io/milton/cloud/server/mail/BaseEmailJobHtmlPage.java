/*
 * Copyright 2012 McEvoy Software Ltd.
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
package io.milton.cloud.server.mail;

import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.cloud.server.db.BaseEmailJob;
import io.milton.cloud.server.web.AbstractResource;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.CommonResource;
import io.milton.http.Request;
import io.milton.resource.AccessControlledResource.Priviledge;
import io.milton.vfs.db.Organisation;

import static io.milton.context.RequestContext._;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Profile;
import org.mvel2.templates.TemplateRuntime;

/**
 *
 * @author brad
 */
public class BaseEmailJobHtmlPage extends AbstractResource implements CommonResource {

    private final BaseEmailJob job;
    private WebsiteRootFolder websiteRootFolder;
    private Profile profile;

    public BaseEmailJobHtmlPage(BaseEmailJob job, String id) {
        this.job = job;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    public String getBodyHtml() {
        TemplateRuntime.eval(job.getHtml(), this);
        return job.getHtml();
    }

    @Override
    public String getUniqueId() {
        return null;
    }

    @Override
    public String getName() {
        return job.getName();
    }

    @Override
    public CommonCollectionResource getParent() {
        if (websiteRootFolder == null) {
            if (job.getThemeSite() != null) {
                Branch live = job.getThemeSite().liveBranch();
                if (live != null) {
                    websiteRootFolder = new WebsiteRootFolder(_(ApplicationManager.class), job.getThemeSite(), live);
                }
            }
        }
        return websiteRootFolder;
    }

    @Override
    public Organisation getOrganisation() {
        return job.getOrganisation();
    }

    @Override
    public Priviledge getRequiredPostPriviledge(Request request) {
        return null;
    }
}
