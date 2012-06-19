package io.milton.cloud.server.apps.calendar;


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
import io.milton.cloud.server.web.Services;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.UserResource;
import io.milton.cloud.server.web.Utils;
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

/**
 *
 * @author brad
 */
public class CalendarHomeFolder extends AbstractCollectionResource implements MakeCollectionableResource, GetableResource {

    private final String name;
    private final UserResource parent;
    private final CalendarManager calendarManager;
    private List<CalendarFolder> children;

    public CalendarHomeFolder(UserResource parent, Services services, String name, CalendarManager calendarManager) {
        super(services);
        this.parent = parent;
        this.name = name;
        this.calendarManager = calendarManager;
    }

    public String getHref() {
        return parent.getHref() + name + "/";
    }    
    
    @Override
    public CollectionResource createCollection(String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
        Calendar calendar = calendarManager.createCalendar(parent.getOwner(), newName);
        return new CalendarFolder(this, services, calendar, calendarManager);
    }

    @Override
    public CommonCollectionResource getParent() {
        return parent;
    }

    @Override
    public BaseEntity getOwner() {
        return parent.getOwner();
    }

    @Override
    public void addPrivs(List<Priviledge> list, Profile user) {
        parent.addPrivs(list, user);
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
        return Utils.childOf(getChildren(), childName);
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            List<Calendar> calendarList = this.getOwner().getCalendars();
            children = new ArrayList<>();
            if (calendarList != null) {
                for (Calendar cal : calendarList) {
                    CalendarFolder f = new CalendarFolder(this, services, cal, calendarManager);
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
        getServices().getHtmlTemplater().writePage("calendar/calendarsHome", this, params, out);
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
}
