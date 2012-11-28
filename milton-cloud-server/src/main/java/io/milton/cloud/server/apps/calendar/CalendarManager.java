/*
 * Copyright (C) 2012 McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.apps.calendar;

import io.milton.cloud.common.HashCalc;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.CalEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.Checksum;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.parameter.TzId;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;
import org.apache.commons.io.output.NullOutputStream;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.milton.vfs.db.Calendar;
import io.milton.vfs.db.utils.SessionManager;
import java.util.logging.Level;

/**
 *
 * @author brad
 */
public class CalendarManager {

    private static final Logger log = LoggerFactory.getLogger(CalendarManager.class);
    private String defaultColor = "blue";

    public Calendar createCalendar(BaseEntity owner, String newName) {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();

        Calendar c = new Calendar();
        c.setColor(defaultColor);
        c.setCreatedDate(new Date());
        c.setModifiedDate(new Date());
        c.setName(newName);
        c.setOwner(owner);
        c.setCtag(System.currentTimeMillis());

        session.save(c);

        tx.commit();

        return c;
    }

    public void delete(CalEvent event) {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();

        session.delete(event);

        tx.commit();
    }

    public void move(CalEvent event, Calendar destCalendar, String name) {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();

        if (!name.equals(event.getName())) {
            event.setName(name);
        }

        Calendar sourceCal = event.getCalendar();
        if (destCalendar != sourceCal) {
            sourceCal.getEvents().remove(event);
            event.setCalendar(destCalendar);
            if (destCalendar.getEvents() == null) {
                destCalendar.setEvents(new ArrayList<CalEvent>());
            }
            destCalendar.getEvents().add(event);
            updateCtag(sourceCal);
            updateCtag(destCalendar);
            session.save(sourceCal);
            session.save(destCalendar);
        }

        tx.commit();
    }

    public void copy(CalEvent event, Calendar destCalendar, String name) {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();

        if (destCalendar.getEvents() == null) {
            destCalendar.setEvents(new ArrayList<CalEvent>());
        }
        CalEvent newEvent = new CalEvent();
        newEvent.setCalendar(destCalendar);
        destCalendar.getEvents().add(newEvent);

        newEvent.setCreatedDate(new Date());
        newEvent.setDescription(event.getDescription());
        newEvent.setEndDate(event.getEndDate());
        newEvent.setModifiedDate(new Date());
        newEvent.setName(name);
        newEvent.setStartDate(event.getStartDate());
        newEvent.setSummary(event.getSummary());
        newEvent.setTimezone(event.getTimezone());
        updateCtag(newEvent);
        session.save(newEvent);

        tx.commit();
    }

    public void delete(Calendar calendar) {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();

        session.delete(calendar);
        tx.commit();
    }

    public CalEvent createEvent(Calendar calendar, String newName, String icalData, String contentType) throws UnsupportedEncodingException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        CalEvent e = new CalEvent();
        e.setName(newName);
        e.setCalendar(calendar);
        e.setCreatedDate(new Date());
        e.setModifiedDate(new Date());

        ByteArrayInputStream fin = new ByteArrayInputStream(icalData.getBytes("UTF-8"));
        CalendarBuilder builder = new CalendarBuilder();
        net.fortuna.ical4j.model.Calendar cal4jCalendar;
        try {
            cal4jCalendar = builder.build(fin);
        } catch (IOException | ParserException ex) {
            throw new RuntimeException(ex);
        }
        _setCalendar(cal4jCalendar, e);
        updateCtag(e);
        session.save(e);
        tx.commit();
        
        return e;
    }

    public String getCalendar(CalEvent calEvent) {

        net.fortuna.ical4j.model.Calendar calendar = new net.fortuna.ical4j.model.Calendar();
        calendar.getProperties().add(new ProdId("-//spliffy.org//iCal4j 1.0//EN"));
        calendar.getProperties().add(Version.VERSION_2_0);
        //calendar.getProperties().add(CalScale.GREGORIAN);
        TimeZoneRegistry registry = TimeZoneRegistryFactory.getInstance().createRegistry();
        String sTimezone = calEvent.getTimezone();
        TimeZone timezone = null;
        if (sTimezone != null && sTimezone.length() > 0) {
            timezone = registry.getTimeZone(sTimezone); // Eg Pacific/Auckland
        }
        if (timezone == null) {
            timezone = registry.getTimeZone("Pacific/Auckland");
            log.warn("Couldnt find timezone: " + sTimezone + ", using default: " + timezone);
        }
        VTimeZone tz = timezone.getVTimeZone();
        calendar.getComponents().add(tz);
        net.fortuna.ical4j.model.DateTime start = CalUtils.toCalDateTime(calEvent.getStartDate(), timezone);
        net.fortuna.ical4j.model.DateTime finish = CalUtils.toCalDateTime(calEvent.getEndDate(), timezone);
        String summary = calEvent.getSummary();
        VEvent vevent = new VEvent(start, finish, summary);
        //vevent.getProperties().add(new Uid(UUID.randomUUID().toString()));
        vevent.getProperties().add(new Uid(calEvent.getId().toString()));
        vevent.getProperties().add(tz.getTimeZoneId());
        TzId tzParam = new TzId(tz.getProperties().getProperty(Property.TZID).getValue());
        vevent.getProperties().getProperty(Property.DTSTART).getParameters().add(tzParam);

        calendar.getComponents().add(vevent);


        CalendarOutputter outputter = new CalendarOutputter();
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try {
            outputter.output(calendar, bout);
        } catch (IOException | ValidationException ex) {
            throw new RuntimeException(ex);
        }
        return bout.toString();

    }

    public void setCalendar(net.fortuna.ical4j.model.Calendar calendar, CalEvent calEvent) {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        
        _setCalendar(calendar, calEvent);
        
        updateCtag(calEvent);
        session.save(calEvent);
        tx.commit();        
    }
    
    private void _setCalendar(net.fortuna.ical4j.model.Calendar calendar, CalEvent calEvent) {
        VEvent ev = event(calendar);
        calEvent.setStartDate(ev.getStartDate().getDate());
        Date endDate = null;
        if (ev.getEndDate() != null) {
            endDate = ev.getEndDate().getDate();
        }
        calEvent.setEndDate(endDate);
        String summary = null;
        if (ev.getSummary() != null) {
            summary = ev.getSummary().getValue();
        }
        calEvent.setSummary(summary);
    }

    private VEvent event(net.fortuna.ical4j.model.Calendar cal) {
        return (VEvent) cal.getComponent("VEVENT");
    }

    private void updateCtag(CalEvent event) {
        OutputStream nulOut = new NullOutputStream();
        CheckedOutputStream cout = new CheckedOutputStream(nulOut, new Adler32());
        appendLine(event.getDescription(), cout);
        appendLine(event.getSummary(), cout);
        appendLine(event.getTimezone(), cout);
        appendLine(event.getStartDate(), cout);
        appendLine(event.getEndDate(), cout);
        Checksum check = cout.getChecksum();
        long crc = check.getValue();
        event.setCtag(crc);
        updateCtag(event.getCalendar());
    }

    private void updateCtag(Calendar sourceCal) {
        OutputStream nulOut = new NullOutputStream();
        CheckedOutputStream cout = new CheckedOutputStream(nulOut, new Adler32());

        appendLine(sourceCal.getColor(), cout);
        if (sourceCal.getEvents() != null) {
            for (CalEvent r : sourceCal.getEvents()) {
                String name = r.getName();
                String line = HashCalc.getInstance().toHashableText(name, r.getCtag()+"", "");
                appendLine(line, cout);
            }
        }
        Checksum check = cout.getChecksum();
        long crc = check.getValue();
        sourceCal.setCtag(crc);
    }
    
    private void appendLine(String text, OutputStream out) {
        try {
            out.write(text.getBytes());
            out.write("\n".getBytes());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private void appendLine(Date d, OutputStream out) {
        try {
            out.write(d.toString().getBytes());
            out.write("\n".getBytes());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }    

    public String getDefaultColor() {
        return defaultColor;
    }

    public void setDefaultColor(String defaultColor) {
        this.defaultColor = defaultColor;
    }
}
