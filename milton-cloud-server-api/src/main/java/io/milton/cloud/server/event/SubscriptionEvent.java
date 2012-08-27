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
import io.milton.vfs.db.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Fired when a user signs up
 *
 * @author brad
 */
public class SubscriptionEvent implements TriggerEvent {

    public static final String ID = "subscription";

    
    /**
     * The sorts of things that can happen in a signupevent
     */
    public enum SignupAction {

        AUTOAPPROVED,
        PENDING,
        REJECTED,
        ACCEPTED,
        DISABLED,
        LAPSED,
        PAYMENT_OVERDUE,
        RE_ACTIVATED
    }
    private final Profile profile;
    private final Group group;
    private final Website website;
    private final Organisation memberOfOrg;
    private final SignupAction action;

    public SubscriptionEvent(Profile profile, Group group, Website website, Organisation memberOfOrg, SignupAction action) {
        this.profile = profile;
        this.group = group;
        this.website = website;
        this.memberOfOrg = memberOfOrg;
        this.action = action;
    }

    public Profile getProfile() {
        return profile;
    }

    public Website getWebsite() {
        return website;
    }

    public Group getGroup() {
        return group;
    }

    public Organisation getMemberOfOrg() {
        return memberOfOrg;
    }
        

    @Override
    public List<BaseEntity> getSourceEntities() {
        List<BaseEntity> list = new ArrayList<>();
        list.add(profile);
        return list;
    }    
    
    @Override
    public String getEventId() {
        return ID;
    }

    @Override
    public Organisation getOrganisation() {
        return website.getOrganisation();
    }

    @Override
    public String getTriggerItem1() {
        return group.getId() + "";
    }

    @Override
    public String getTriggerItem2() {
        return website.getId() + "";
    }

    @Override
    public String getTriggerItem3() {
        return action.name();
    }

    @Override
    public String getTriggerItem4() {
        return null;
    }

    @Override
    public String getTriggerItem5() {
        return null;
    }
}
