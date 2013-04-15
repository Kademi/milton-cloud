package io.milton.cloud.server.apps.calendar;

import io.milton.cloud.server.apps.contacts.ContactsFolder;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Calendar;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.cloud.server.web.AbstractCollectionResource;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.NodeChildUtils;
import io.milton.cloud.server.web.PersonalResource;
import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.cloud.server.web.UserResource;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.cloud.server.web.templating.TitledPage;
import static io.milton.context.RequestContext._;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.principal.Principal;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.CollectionResource;
import io.milton.resource.GetableResource;
import io.milton.resource.MakeCollectionableResource;
import io.milton.resource.Resource;

import io.milton.vfs.db.AddressBook;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.utils.SessionManager;

/**
 *
 * @author brad
 */
public class CalendarHomeFolder extends AbstractCollectionResource implements MakeCollectionableResource, GetableResource, PersonalResource, TitledPage {

    private final String name;
    private final UserResource parent;
    private final CalendarManager calendarManager;
    private List<CalendarFolder> children;

    public CalendarHomeFolder(UserResource parent, String name, CalendarManager calendarManager) {
        this.parent = parent;
        this.name = name;
        this.calendarManager = calendarManager;
    }

    @Override
    public CollectionResource createCollection(String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
        Calendar calendar = calendarManager.createCalendar(parent.getThisUser(), newName);
        Branch branch = calendar.getTrunk();
        Profile curUser = _(SpliffySecurityManager.class).getCurrentUser();
        if (branch == null) {
            branch = calendar.createBranch(Branch.TRUNK, curUser, SessionManager.session());
        }
        return new CalendarFolder(newName, this, calendar, branch, calendarManager);
    }

    @Override
    public CommonCollectionResource getParent() {
        return parent;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Date getModifiedDate() {
        return null;
    }

    @Override
    public Date getCreateDate() {
        return null;
    }

    @Override
    public Resource child(String childName) throws NotAuthorizedException, BadRequestException {
        return NodeChildUtils.childOf(getChildren(), childName);
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            List<Calendar> calendarList = this.parent.getThisUser().getCalendars();
            children = new ArrayList<>();
            if (calendarList != null) {
                for (Calendar cal : calendarList) {
                    Branch branch = cal.getTrunk();
                    CalendarFolder f = new CalendarFolder(cal.getName(), this, cal, branch, calendarManager);
                    children.add(f);
                }
            }
        }
        return children;
    }

    @Override
    public Map<Principal, List<Priviledge>> getAccessControlList() {
        return null;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        _(HtmlTemplater.class).writePage("calendar/calendarsHome", this, params, out);
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return null;
    }

    @Override
    public String getContentType(String accepts) {
        return "text/html";
    }

    @Override
    public Long getContentLength() {
        return null;
    }

    @Override
    public Organisation getOrganisation() {
        return parent.getOrganisation();
    }

    @Override
    public Profile getOwnerProfile() {
        return parent.getThisUser();
    }

    @Override
    public String getTitle() {
        return "Calendars";
    }
}
