package io.milton.cloud.server.apps.calendar;

import io.milton.cloud.server.apps.contacts.*;
import io.milton.cloud.server.web.*;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;

import static  io.milton.context.RequestContext._;

/**
 * @author brad
 */
public class MyCalendarsPage extends TemplatedHtmlPage {

    public MyCalendarsPage(String name, CommonCollectionResource parent) {
        super(name, parent, "calendar/mycalendarsHome", "Calendars");
    }

    
}
