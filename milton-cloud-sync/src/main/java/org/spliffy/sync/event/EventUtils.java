package org.spliffy.sync.event;

import io.milton.event.Event;
import io.milton.event.EventManager;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;

/**
 *
 * @author brad
 */
public class EventUtils {

    public static void fireQuietly(EventManager eventManager, Event e) {
        try {
            eventManager.fireEvent( e );
        } catch( ConflictException | BadRequestException | NotAuthorizedException ex ) {
            throw new RuntimeException( ex );
        }
    }
}
