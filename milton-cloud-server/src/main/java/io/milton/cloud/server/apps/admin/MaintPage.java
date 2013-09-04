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
package io.milton.cloud.server.apps.admin;

import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.common.MutableCurrentDateService;
import io.milton.cloud.server.apps.orgs.OrganisationRootFolder;
import io.milton.cloud.server.queue.AsynchProcessor;
import io.milton.cloud.server.web.AbstractResource;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.JsonResult;
import io.milton.cloud.server.web.templating.DataBinder;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.http.Auth;
import io.milton.http.FileItem;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.PostableResource;
import io.milton.vfs.db.Organisation;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.milton.context.RequestContext._;
import io.milton.vfs.db.FanoutHash;
import io.milton.vfs.db.utils.SessionManager;
import java.util.Collections;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 *
 * @author brad
 */
public class MaintPage extends AbstractResource implements PostableResource {

    private static final Logger log = LoggerFactory.getLogger(MaintPage.class);
    private OrganisationRootFolder rootFolder;
    private CurrentDateService currentDateService;
    private JsonResult jsonResult;

    public MaintPage(OrganisationRootFolder rootFolder) {
        this.rootFolder = rootFolder;
        currentDateService = _(CurrentDateService.class);
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        try {
            if (parameters.containsKey("runScheduledTasks")) {
                _(AsynchProcessor.class).runScheduledJobs();
                jsonResult = new JsonResult(true, "Running..");
            } else {
                jsonResult = new JsonResult(true, "Updated");
                _(DataBinder.class).populate(this, parameters);
                jsonResult.setData(currentDateService.getNow().toString());
            }
        } catch (Exception e) {
            log.error("ex", e);
            jsonResult = new JsonResult(false, e.getMessage());
        }
        return null;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        if (jsonResult != null) {
            jsonResult.write(out);
        } else {
            _(HtmlTemplater.class).writePage("admin/maint", this, params, out);
        }
    }

    public List<String> getScheduledTaskHistory() {
        AsynchProcessor asyncProc = _(AsynchProcessor.class);
        return asyncProc.getHistory();
    }

    public Long getDateOffsetMinutes() {
        if (currentDateService instanceof MutableCurrentDateService) {
            MutableCurrentDateService m = (MutableCurrentDateService) currentDateService;
            Long v = m.getDateOffset();
            if (v != null) {
                v = v / 60000; // convert from millis to minutes
            }
            return v;
        } else {
            return null;
        }
    }

    public void setDateOffsetMinutes(Long v) {
        System.out.println("setDateOffsetSec: " + v);
        if (currentDateService instanceof MutableCurrentDateService) {
            MutableCurrentDateService m = (MutableCurrentDateService) currentDateService;
            if (v != null) {
                v = v * 1000 * 60;
            }
            m.setDateOffset(v);
        } else {
            throw new RuntimeException("Cant set date on non-mutable current date service");
        }
    }

    public CurrentDateService getCurrentDateService() {
        return currentDateService;
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return null;
    }

    @Override
    public Long getContentLength() {
        return null;
    }

    public String getTitle() {
        return "Maintenance page";
    }

    @Override
    public CommonCollectionResource getParent() {
        return rootFolder;
    }

    @Override
    public Organisation getOrganisation() {
        return rootFolder.getOrganisation();
    }

    @Override
    public Priviledge getRequiredPostPriviledge(Request request) {
        return Priviledge.WRITE_ACL;
    }

    @Override
    public String getName() {
        return "maint";
    }
}
