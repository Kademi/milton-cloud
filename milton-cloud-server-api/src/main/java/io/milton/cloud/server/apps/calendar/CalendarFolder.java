package io.milton.cloud.server.apps.calendar;


import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.CalEvent;
import io.milton.vfs.db.Calendar;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import io.milton.cloud.server.web.AbstractCollectionResource;
import io.milton.cloud.server.web.Services;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.Utils;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.principal.Principal;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.*;

/**
 *
 * @author brad
 */
public class CalendarFolder extends AbstractCollectionResource implements CalendarResource, ReportableResource, DeletableResource, PutableResource, GetableResource {
    private final CalendarHomeFolder parent;
    private final Calendar calendar;
    private final CalendarManager calendarManager;
    
    private List<CalEventResource> children;

    public CalendarFolder(CalendarHomeFolder parent, Services services, Calendar calendar, CalendarManager calendarManager) {
        super(services);
        this.parent = parent;
        this.calendar = calendar;
        this.calendarManager = calendarManager;
    }
    
    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        services.getHtmlTemplater().writePage("calendar/calendar", this, params, out);
    }
    
    
    @Override
    public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
        calendarManager.delete(calendar);
    }    
    
    @Override
    public CommonCollectionResource getParent() {
        return parent;
    }

    @Override
    public BaseEntity getOwner() {
        return getParent().getOwner();
    }

    @Override
    public void addPrivs(List<Priviledge> list, Profile user) {
        getParent().addPrivs(list, user);
    }

    @Override
    public String getName() {
        return calendar.getName();
    }

    @Override
    public Date getModifiedDate() {
        return calendar.getModifiedDate();
    }

    @Override
    public Date getCreateDate() {
        return calendar.getCreatedDate();
    }

    @Override
    public Resource child(String childName) throws NotAuthorizedException, BadRequestException {
        return Utils.childOf(getChildren(), childName);
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if( children == null ) {
            children = new ArrayList<>();
            if( calendar.getEvents() != null ) {
                for( CalEvent event : calendar.getEvents() ) {
                    CalEventResource r = new CalEventResource(this, event, calendarManager);
                    children.add(r);
                }
            }
        }
        return children;
    }

    @Override
    public String getCalendarDescription() {
        return getName();
    }

    @Override
    public String getColor() {
        return calendar.getColor();
    }

    @Override
    public void setColor(String s) {
        this.calendar.setColor(s);
    }

    @Override
    public String getCTag() {
        return calendar.getCtag() + "";
    }
    
    @Override
    public Map<Principal, List<Priviledge>> getAccessControlList() {
        return null;
    }

    @Override
    public Resource createNew(String newName, InputStream inputStream, Long length, String contentType) throws IOException, ConflictException, NotAuthorizedException, BadRequestException {
        System.out.println("createNew: " + newName);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        IOUtils.copy(inputStream, bout);
        String data = bout.toString();
        CalEvent event= calendarManager.createEvent(calendar, newName, data, contentType);
        return new CalEventResource(this, event, calendarManager);
    }

    public Calendar getCalendar() {
        return calendar;
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
