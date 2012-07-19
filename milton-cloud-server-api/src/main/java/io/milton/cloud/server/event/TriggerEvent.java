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
package io.milton.cloud.server.event;

import io.milton.event.Event;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Organisation;
import java.util.List;

/**
 * base class for events handled by the EmailTrigger system
 * 
 * The trigger item properties represent textual values that can be keyed 
 * on for event triggers. Typically these will be id's of objects in context
 * of the event, such as the website, group, etc.
 *
 * @author brad
 */
public interface TriggerEvent extends Event {


    /**
     * This identifies the type of event in a persistence friendly manner
     * 
     * @return 
     */
    String getEventId();
    
    /**
     * The administrative organisation which contains the source of the event
     * 
     * @return 
     */
    Organisation getOrganisation();
    
    /**
     * Returns the entities which caused the event. This will usually be a single
     * Profile object, such as when a user registers. However, in some cases there
     * might be multiple source entities
     * @return 
     */
    List<BaseEntity> getSourceEntities();
    
    String getTriggerItem1();
    
    String getTriggerItem2();
    
    String getTriggerItem3();
    
    String getTriggerItem4();
    
    String getTriggerItem5();
        
        
}
