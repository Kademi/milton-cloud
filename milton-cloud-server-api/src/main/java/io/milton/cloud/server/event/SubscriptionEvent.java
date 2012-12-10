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

import io.milton.vfs.db.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Fired when a user joins a group
 *
 * @author brad
 */
public class SubscriptionEvent implements TriggerEvent {

    public static final String ID = "subscription";

    /**
     * The sorts of things that can happen in a signupevent
     */
    public enum SubscriptionAction {

        AUTOAPPROVED,
        PENDING,
        REJECTED,
        ACCEPTED,
        DISABLED,
        LAPSED,
        PAYMENT_OVERDUE,
        RE_ACTIVATED
    }
    private final Website website;
    private final Organisation organisation;
    private final GroupMembership membership;
    private final SubscriptionAction action;

    /**
     *
     * @param membership
     * @param website - nullable
     * @param org - must not be null
     * @param action
     */
    public SubscriptionEvent(GroupMembership membership, Website website, Organisation org, SubscriptionAction action) {
        this.website = website;
        this.organisation = org;
        this.membership = membership;
        this.action = action;
    }

    public SubscriptionAction getAction() {
        return action;
    }

    public GroupMembership getMembership() {
        return membership;
    }

    public Website getWebsite() {
        return website;
    }

    @Override
    public List<BaseEntity> getSourceEntities() {
        List<BaseEntity> list = new ArrayList<>();
        list.add(membership.getMember());
        return list;
    }

    @Override
    public String getEventId() {
        return ID;
    }

    @Override
    public Organisation getOrganisation() {
        return organisation;
    }

    @Override
    public String getTriggerItem1() {
        return membership.getGroupEntity().getId() + "";
    }

    @Override
    public String getTriggerItem2() {
        if (website != null) {
            return website.getId() + "";
        } else {
            return null;
        }
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
