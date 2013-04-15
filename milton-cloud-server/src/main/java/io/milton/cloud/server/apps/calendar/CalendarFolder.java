package io.milton.cloud.server.apps.calendar;


import io.milton.vfs.db.Calendar;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import io.milton.cloud.server.web.BranchFolder;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.*;

import static io.milton.context.RequestContext._;
import io.milton.http.values.SupportedCalendarComponentList;
import io.milton.vfs.db.Branch;

/**
 *
 * @author brad
 */
public class CalendarFolder extends BranchFolder implements CalendarResource, ReportableResource, DeletableResource, PutableResource, GetableResource {
    private final Calendar calendar;
    private final CalendarManager calendarManager;
    
    public CalendarFolder(String name, CalendarHomeFolder parent,  Calendar calendar, Branch branch, CalendarManager calendarManager) {
        super(name, parent, branch);
        this.calendar = calendar;
        this.calendarManager = calendarManager;
    }
    
    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        _(HtmlTemplater.class).writePage("calendar/calendar", this, params, out);
    }
        
    
    @Override
    public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
        calendarManager.delete(calendar);
    }    
    
    public Calendar getCalendar() {
        return (Calendar) this.getRepository();
    }
    
    @Override
    public String getCalendarDescription() {
        return calendar.getDescription();
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
        return getHash();
    }
    
    @Override
    public SupportedCalendarComponentList getSupportedComponentSet() {
        return SupportedCalendarComponentList.asList(ComponentType.VEVENT, ComponentType.VTODO);
    }

    @Override
    public boolean is(String type) {
        if( type.equals("calendar")) {
            return true;
        }
        return  super.is(type);        
    }

    

}
