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

import io.milton.cloud.server.apps.Application;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.vfs.db.*;
import io.milton.vfs.db.utils.SessionManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final Map<String, Object> attributes = new HashMap<>();

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

    @Override
    public Website getWebsite() {
        return website;
    }

    @Override
    public Profile getSourceProfile() {
        return membership.getMember();
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

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * Caches isactive result in this event for performance
     *
     * @return
     */
    public boolean isActive(ApplicationManager applicationManager, Application app, Branch branch) {
        List<Application> activeApps = (List<Application>) getAttributes().get("activeApps");
        if (activeApps == null) {
            activeApps = applicationManager.findActiveApps(branch);
            getAttributes().put("activeApps", activeApps);
        }
        return applicationManager.isActive(app, activeApps);
    }

    /**
     * Locally cached lookup for GroupInWebsite's, to assist with active
     * determination
     *
     * @param group
     * @return
     */
    public List<GroupInWebsite> getGroupInWebsites(Group group) {
        List<GroupInWebsite> giws = (List<GroupInWebsite>) getAttributes().get("giws");
        if (giws == null) {
            giws = GroupInWebsite.findByGroup(group, SessionManager.session());
            getAttributes().put("giws", giws);
        }
        return giws;
    }
}
