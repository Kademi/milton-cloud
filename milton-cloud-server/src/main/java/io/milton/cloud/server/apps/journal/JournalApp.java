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
package io.milton.cloud.server.apps.journal;

import com.fuselms.apps.learning.ProgramsModel;
import com.fuselms.db.LearnerProcess;
import com.fuselms.db.ModuleStatus;
import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.Application;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.apps.PortletApplication;
import io.milton.cloud.server.apps.orgs.OrganisationFolder;
import io.milton.cloud.server.apps.orgs.OrganisationRootFolder;
import io.milton.cloud.server.event.SubscriptionEvent;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.CommonResource;
import io.milton.cloud.server.web.ProfileResource;
import io.milton.cloud.server.web.RenderFileResource;
import io.milton.cloud.server.web.RootFolder;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.cloud.server.web.Utils;
import io.milton.cloud.server.web.WebUtils;
import io.milton.cloud.server.web.templating.TextTemplater;
import static io.milton.context.RequestContext._;
import io.milton.event.Event;
import io.milton.event.EventListener;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.Resource;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Group;
import io.milton.vfs.db.GroupInWebsite;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Repository;
import io.milton.vfs.db.utils.SessionManager;
import java.io.IOException;
import java.io.Writer;
import java.util.Date;
import java.util.List;
import org.apache.velocity.context.Context;
import org.hibernate.HibernateException;
import org.hibernate.Session;

/**
 *
 * @author brad
 */
public class JournalApp implements Application, EventListener, PortletApplication {

    private ApplicationManager applicationManager;

    @Override
    public String getInstanceId() {
        return "journal";
    }

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
        this.applicationManager = resourceFactory.getApplicationManager();
        resourceFactory.getEventManager().registerEventListener(this, SubscriptionEvent.class);

    }

    @Override
    public String getTitle(Organisation organisation, Branch websiteBranch) {
        return "Journal and Notes";
    }

    @Override
    public String getSummary(Organisation organisation, Branch websiteBranch) {
        return "Provides a simple text journal, which allows users to keep notes";
    }

    @Override
    public void onEvent(Event e) {
        if (e instanceof SubscriptionEvent) {
            SubscriptionEvent joinEvent = (SubscriptionEvent) e;
            Group group = joinEvent.getMembership().getGroupEntity();
            List<GroupInWebsite> giws = GroupInWebsite.findByGroup(group, SessionManager.session());
            for (GroupInWebsite giw : giws) {
                Branch b = giw.getWebsite().liveBranch();
                if (applicationManager.isActive(this, b)) {
                    addJournalRepo("journal", joinEvent.getMembership().getMember(), SessionManager.session());
                }
            }
        }
    }

    private void addJournalRepo(String name, Profile u, Session session) throws HibernateException {
        Repository existing = u.repository(name);
        if (existing != null) {
            return;
        }
        Repository journalRepo = new Repository();
        journalRepo.setName(name);
        journalRepo.setTitle(name);
        journalRepo.setBaseEntity(u);
        journalRepo.setCreatedDate(new Date());
        Repository.initRepo(journalRepo, name, session, u, u);
        session.save(journalRepo);
    }

    @Override
    public void renderPortlets(String portletSection, Profile currentUser, RootFolder rootFolder, Context context, Writer writer) throws IOException {
        CommonResource r = (CommonResource) context.get("page");
        switch (portletSection) {
            case PortletApplication.PORTLET_SECTION_END_OF_PAGE:
                writeFooterResources(writer, r);
                break;


        }
    }

    private void writeFooterResources(Writer writer, CommonResource r) throws IOException {
        // If a module page, then include any js files in that module
        if (r instanceof RenderFileResource) {
            writer.write("<script type=\"text/javascript\" src='/theme/apps/journal/jquery.journal-1.0.0.js' >//</script>\n");
        }
    }
}
