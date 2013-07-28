package io.milton.cloud.server.apps.calendar;

import io.milton.cloud.server.web.JsonResult;
import io.milton.cloud.server.web.UploadUtils;
import io.milton.cloud.server.web.templating.DataBinder;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.context.ClassNotInContextException;
import static io.milton.context.RequestContext._;
import io.milton.http.FileItem;
import io.milton.http.Range;
import java.io.*;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.milton.vfs.db.CalEvent;
import org.hibernate.Transaction;
import io.milton.http.Response.Status;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.values.ValueAndType;
import io.milton.http.webdav.PropFindResponse.NameAndError;
import io.milton.http.webdav.PropertySourcePatchSetter;
import io.milton.annotations.BeanPropertyResource;
import io.milton.resource.*;
import io.milton.vfs.data.DataSession;
import io.milton.vfs.db.Calendar;
import io.milton.vfs.db.utils.SessionManager;
import org.hibernate.Session;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

/**
 *
 * @author brad
 */
@BeanPropertyResource(value = "spliffy")
public class CalEventResource extends io.milton.cloud.server.web.FileResource implements ICalResource, DeletableResource, ReplaceableResource, PropertySourcePatchSetter.CommitableResource, GetableResource, PostableResource {

    private static final Logger log = LoggerFactory.getLogger(CalEventResource.class);
    private final CalEvent event;
    private final CalendarManager calendarManager;
    private final Calendar calendar;
    private Transaction tx; // for proppatch setting

    public CalEventResource(DataSession.FileNode fileNode, CalendarFolder parent, CalEvent event, CalendarManager calendarManager) {
        super(fileNode, parent);
        this.event = event;
        this.parent = parent;
        this.calendarManager = calendarManager;
        this.calendar = parent.getCalendar();
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        Session session = SessionManager.session();
        tx = SessionManager.beginTx();
        String newName = UUID.randomUUID().toString() + ".ics";        
        try {
            CalEvent newEvent = calendarManager.createEvent(calendar, newName, null);
            _(DataBinder.class).populate(newEvent, parameters);
            session.save(newEvent);
            String icalData = calendarManager.getCalendar(newEvent);
            ByteArrayInputStream bin = new ByteArrayInputStream(icalData.getBytes("UTF-8"));
            DataSession.FileNode newFileNode = parent.getDirectoryNode().addFile(newName);
            newFileNode.setContent(bin);                                    
            CalEventResource newRes = new CalEventResource(newFileNode, (CalendarFolder) getParent(), newEvent, calendarManager);
            newRes.save();
            jsonResult = new JsonResult(true, "Created calendar resource", newRes.getHref());
            tx.commit();

        } catch (IOException | ClassNotInContextException | IllegalAccessException | InvocationTargetException ex) {
            log.error("Exception creating event", ex);
            tx.rollback();
            jsonResult = new JsonResult(false, "Exception occured: " + ex.getMessage());
        }

        return null;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException {
        if (params.containsKey("edit")) {
            _(HtmlTemplater.class).writePage("calendar/editEvent", this, params, out);
        } else {
            super.sendContent(out, range, params, contentType);
        }
    }

    @Override
    public void replaceContent(InputStream in, Long length) throws BadRequestException, ConflictException, NotAuthorizedException {
        super.replaceContent(in, length);
        calendarManager.update(event, getICalData());
    }

    @Override
    public String getContentType(String accepts) {
        return "text/calendar";
    }

    @Override
    public boolean is(String type) {
        if (type.equals("event") || type.equals("calendarResource")) {
            return true;
        }
        return super.is(type);
    }

    @Override
    public String getICalData() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            sendContent(out, null, null, null);
            return out.toString("UTF-8");
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
        return CalendarUtils.plainDate(event.getEndDate());
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
    public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
        super.delete();
        if (event != null) {
            calendarManager.delete(event);
        }
    }

    @Override
    public void doCommit(Map<QName, ValueAndType> knownProps, Map<Status, List<NameAndError>> errorProps) {
        if (tx == null) {
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
        if (tx == null) {
            tx = SessionManager.session().beginTransaction();
        }
    }
}
