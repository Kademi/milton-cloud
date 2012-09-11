/*
 * Copyright 2012 McEvoy Software Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.db;

import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.utils.DbUtils;
import java.io.Serializable;
import java.util.List;
import javax.persistence.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

/**
 * This table defines the conditions which will trigger automatic emails. Where
 * a condition has a null trigger condition (eg group) it will match events
 * which have a null for that value (eg an event which is not relevant to a
 * group) , and it will match any value.
 *
 * @author brad
 */
@Entity
@DiscriminatorValue("T")
public class EmailTrigger extends BaseEmailJob implements Serializable {

    public static List<EmailTrigger> findByOrg(Organisation org, Session session) {
        Criteria crit = session.createCriteria(EmailTrigger.class);
        crit.add(Restrictions.eq("organisation", org));
        return DbUtils.toList(crit, EmailTrigger.class);
    }

    public static List<EmailTrigger> find(Session session, String eventId, Organisation org, String trigger1, String trigger2, String trigger3, String trigger4, String trigger5) {
        Criteria crit = session.createCriteria(EmailTrigger.class);
        if (trigger1 != null) {
            crit.add(Restrictions.or(Restrictions.eq("triggerCondition1", trigger1), Restrictions.isNull("triggerCondition1")));
        } else {
            crit.add(Restrictions.isNull("triggerCondition1"));
        }
        if (trigger2 != null) {
            crit.add(Restrictions.or(Restrictions.eq("triggerCondition2", trigger2), Restrictions.isNull("triggerCondition2")));
        } else {
            crit.add(Restrictions.isNull("triggerCondition2"));
        }
        if (trigger3 != null) {
            crit.add(Restrictions.or(Restrictions.eq("triggerCondition3", trigger3), Restrictions.isNull("triggerCondition3")));
        } else {
            crit.add(Restrictions.isNull("triggerCondition3"));
        }
        if (trigger4 != null) {
            crit.add(Restrictions.or(Restrictions.eq("triggerCondition4", trigger4), Restrictions.isNull("triggerCondition4")));
        } else {
            crit.add(Restrictions.isNull("triggerCondition4"));
        }
        if (trigger5 != null) {
            crit.add(Restrictions.or(Restrictions.eq("triggerCondition5", trigger5), Restrictions.isNull("triggerCondition5")));
        } else {
            crit.add(Restrictions.isNull("triggerCondition5"));
        }
        return DbUtils.toList(crit, EmailTrigger.class);
    }
    private String eventId;
    private String triggerCondition1;
    private String triggerCondition2;
    private String triggerCondition3;
    private String triggerCondition4;
    private String triggerCondition5;
    private boolean enabled;
    private boolean includeUser; // whether the email should go to the user associated with the event which fires this trigger

    /**
     * This is the eventId to trigger on. Required
     *
     * @return
     */
    @Column(nullable = false)
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getTriggerCondition1() {
        return triggerCondition1;
    }

    public void setTriggerCondition1(String triggerCondition1) {
        this.triggerCondition1 = triggerCondition1;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isIncludeUser() {
        return includeUser;
    }

    public void setIncludeUser(boolean includeUser) {
        this.includeUser = includeUser;
    }

    /**
     * @return the triggerCondition2
     */
    public String getTriggerCondition2() {
        return triggerCondition2;
    }

    /**
     * @param triggerCondition2 the triggerCondition2 to set
     */
    public void setTriggerCondition2(String triggerCondition2) {
        this.triggerCondition2 = triggerCondition2;
    }

    /**
     * @return the triggerCondition3
     */
    public String getTriggerCondition3() {
        return triggerCondition3;
    }

    /**
     * @param triggerCondition3 the triggerCondition3 to set
     */
    public void setTriggerCondition3(String triggerCondition3) {
        this.triggerCondition3 = triggerCondition3;
    }

    /**
     * @return the triggerCondition4
     */
    public String getTriggerCondition4() {
        return triggerCondition4;
    }

    /**
     * @param triggerCondition4 the triggerCondition4 to set
     */
    public void setTriggerCondition4(String triggerCondition4) {
        this.triggerCondition4 = triggerCondition4;
    }

    /**
     * @return the triggerCondition5
     */
    public String getTriggerCondition5() {
        return triggerCondition5;
    }

    /**
     * @param triggerCondition5 the triggerCondition5 to set
     */
    public void setTriggerCondition5(String triggerCondition5) {
        this.triggerCondition5 = triggerCondition5;
    }

    public void checkNulls() {
        if (triggerCondition1 != null && triggerCondition1.trim().length() == 0) {
            triggerCondition1 = null;
        }
        if (triggerCondition2 != null && triggerCondition2.trim().length() == 0) {
            triggerCondition2 = null;
        }
        if( triggerCondition3 != null && triggerCondition3.trim().length() == 0 ) {
            triggerCondition3 = null;
        }
        if( triggerCondition4 != null && triggerCondition4.trim().length() == 0 ) {
            triggerCondition4 = null;
        }
        if( triggerCondition5 != null && triggerCondition5.trim().length() == 0 ) {
            triggerCondition5 = null;
        }        
    }
    
    @Override
    public void accept(EmailJobVisitor visitor) {
        visitor.visit(this);
    }
    
}
