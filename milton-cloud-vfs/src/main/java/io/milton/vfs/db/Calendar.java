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
package io.milton.vfs.db;

import java.util.List;
import javax.persistence.*;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Represents a user calendar, which can contain events
 *
 * @author brad
 */
@Entity
@DiscriminatorValue("CA")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Calendar extends Repository {

    private List<CalEvent> events;
    private String color;
    private String description;

    @OneToMany(mappedBy = "calendar")
    public List<CalEvent> getEvents() {
        return events;
    }

    public void setEvents(List<CalEvent> events) {
        this.events = events;
    }

    @Column
    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    

    @Override
    public String type() {
        return "CA";
    }

    @Override
    public void delete(Session session) {
        if (getEvents() != null) {
            for (CalEvent e : getEvents()) {
                session.delete(e);
            }
            setEvents(null);
        }
        super.delete(session);
    }

    public CalEvent event(String name) {
        if (getEvents() != null) {
            for (CalEvent e : getEvents()) {
                if (e.getName().equals(name)) {
                    return e;
                }
            }
        }
        return null;
    }

    public CalEvent add(String name) {
        CalEvent c = new CalEvent();
        c.setCalendar(this);
        c.setName(name);
        return c;
    }
}
