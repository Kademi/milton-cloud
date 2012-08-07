package io.milton.cloud.server.apps.calendar;


import java.io.*;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.CalEvent;
import io.milton.vfs.db.Profile;
import io.milton.cloud.server.web.AbstractResource;
import io.milton.cloud.server.web.CommonCollectionResource;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import org.apache.commons.io.IOUtils;
import org.hibernate.Transaction;
import io.milton.vfs.db.Organisation;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.Response.Status;
import io.milton.principal.Principal;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.values.ValueAndType;
import io.milton.http.webdav.PropFindResponse.NameAndError;
import io.milton.http.webdav.PropertySourcePatchSetter;
import io.milton.property.BeanPropertyResource;
import io.milton.resource.*;
import io.milton.vfs.db.utils.SessionManager;

/**
 *
 * @author brad
 */
@BeanPropertyResource(value="spliffy")
public class CalEventResource extends AbstractResource implements ICalResource, DeletableResource, MoveableResource, CopyableResource, ReplaceableResource, PropertySourcePatchSetter.CommitableResource, GetableResource {

    private static final Logger log = LoggerFactory.getLogger(CalEventResource.class);
    private final CalEvent event;
    private final CalendarFolder parent;
    private final CalendarManager calendarManager;

    private Transaction tx; // for proppatch setting
    
    public CalEventResource(CalendarFolder parent, CalEvent event, CalendarManager calendarManager) {
        
        this.event = event;
        this.parent = parent;
        this.calendarManager = calendarManager;
    }

    @Override
    public String getUniqueId() {
        return event.getCtag() + "";
    }

    
    
    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException {
        writeData(out);
        out.flush();
    }


    @Override
    public void replaceContent(InputStream in, Long length) throws BadRequestException, ConflictException, NotAuthorizedException {
        System.out.println("replaceContent: " + getName());
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            IOUtils.copy(in, bout);
            String data = bout.toString();
            setiCalData(data);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

    }
    
    @Override
    public String getContentType(String accepts) {
        return "text/calendar";
    }

    @Override
    public String getICalData() {
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            writeData(bout);
            return bout.toString("UTF-8");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void setiCalData(String iCalData) {
        ByteArrayInputStream fin = null;
        try {
            fin = new ByteArrayInputStream(iCalData.getBytes("UTF-8"));
            CalendarBuilder builder = new CalendarBuilder();
            Calendar calendar = builder.build(fin);
            calendarManager.setCalendar(calendar, event);
            
        } catch (IOException | ParserException ex) {
            throw new RuntimeException(ex);
        } finally {
            IOUtils.closeQuietly(fin);
        }
    }

    public void writeData(OutputStream out) {
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            String s = calendarManager.getCalendar(event);
            out.write(s.getBytes());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public Date getStartDate() {
        return CalendarUtils.plainDate(event.getStartDate());
    }
    
    public String getStart() {
        return CalendarUtils.formatDate(getStartDate());
    }    

    public void setStartDate(Date d) {
        checkTx();
        event.setStartDate(d);
    }

    public String getTimezone() {
        return event.getTimezone();
    }

    public void setTimezone(String s) {
        checkTx();
        event.setTimezone(s);
    }


    public Date getEndDate() {
        return  CalendarUtils.plainDate(event.getEndDate());
    }

    public String getEnd() {
        return CalendarUtils.formatDate(getEndDate());
    }    
    
    public void setEndDate(Date d) {
        checkTx();
        event.setEndDate(d);
    }

    public String getDescription() {
        return event.getDescription();
    }

    public void setDescription(String d) {
        checkTx();
        event.setDescription(d);
    }

    public String getSummary() {
        return event.getSummary();
    }

    public void setSummary(String d) {
        checkTx();
        event.setSummary(d);
    }

    @Override
    public String getName() {
        return event.getName();
    }

    @Override
    public Date getModifiedDate() {
        return event.getModifiedDate();
    }

    @Override
    public Date getCreateDate() {
        return event.getCreatedDate();
    }

    @Override
    public boolean isDir() {
        return false;
    }

    @Override
    public CommonCollectionResource getParent() {
        return parent;
    }

    @Override
    public Map<Principal, List<Priviledge>> getAccessControlList() {
        return null;
    }

    @Override
    public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
        calendarManager.delete(event);
    }

    @Override
    public void moveTo(CollectionResource rDest, String name) throws ConflictException, NotAuthorizedException, BadRequestException {
        if( rDest instanceof CalendarFolder) {
            CalendarFolder calFolder = (CalendarFolder) rDest;
            calendarManager.move(event, calFolder.getCalendar(), name);
        } else {
            throw new BadRequestException(rDest, "The destination resource is not a calendar");
        }
    }

    @Override
    public void copyTo(CollectionResource rDest, String name) throws NotAuthorizedException, BadRequestException, ConflictException {
        if( rDest instanceof CalendarFolder) {
            CalendarFolder calFolder = (CalendarFolder) rDest;
            calendarManager.copy(event, calFolder.getCalendar(), name);
        } else {
            throw new BadRequestException(rDest, "The destination resource is not a calendar");
        }
    }

    @Override
    public void doCommit(Map<QName, ValueAndType> knownProps, Map<Status, List<NameAndError>> errorProps) {        
        if( tx == null ) {
            log.warn("doCommit: Transaction not started");
        } else {
            log.trace("doCommit: commiting");
            SessionManager.session().save(this.event);
            tx.commit();
        }
    }

    /**
     * Called from setters used by proppatch
     */
    private void checkTx() {
        if( tx == null ) {
            tx = SessionManager.session().beginTransaction();
        }
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return null;
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
