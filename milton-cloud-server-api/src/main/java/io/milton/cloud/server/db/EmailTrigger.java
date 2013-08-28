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
import io.milton.vfs.db.Website;
import io.milton.vfs.db.utils.DbUtils;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class EmailTrigger extends BaseEmailJob implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(EmailTrigger.class);
    
    public static List<EmailTrigger> findByOrg(Organisation org, Session session) {
        Criteria crit = session.createCriteria(EmailTrigger.class);
        crit.add(Restrictions.eq("organisation", org));
        Disjunction notDeleted = Restrictions.disjunction();
        notDeleted.add(Restrictions.isNull("deleted"));
        notDeleted.add(Restrictions.eq("deleted", Boolean.FALSE));
        crit.add(notDeleted);
        
        return DbUtils.toList(crit, EmailTrigger.class);
    }

    public static List<EmailTrigger> find(Session session, String eventId, Website website, String trigger1, String trigger2, String trigger3, String trigger4, String trigger5) {
        log.info("find triggers: " + eventId);
        Criteria crit = session.createCriteria(EmailTrigger.class);
        crit.setCacheable(true);
        crit.add(Restrictions.eq("themeSite", website));
        crit.add(Restrictions.eq("eventId", eventId));
        Disjunction notDeleted = Restrictions.disjunction();
        notDeleted.add(Restrictions.isNull("deleted"));
        notDeleted.add(Restrictions.eq("deleted", Boolean.FALSE));
        crit.add(notDeleted);
        
        List<EmailTrigger> rawList = DbUtils.toList(crit, EmailTrigger.class);
        log.info("triggers raw: " + rawList.size());
        List<EmailTrigger> finalList = new ArrayList<>();
        for (EmailTrigger trigger : rawList) {
            if (trigger.isEnabled()) {
                if (matches(trigger, trigger1, trigger2, trigger3, trigger4, trigger5)) {
                    log.info("Found matching trigger", trigger.eventId);
                    finalList.add(trigger);
                } else {
                    log.info("trigger does not match this event: " + trigger1 + ", " + trigger2 + ", " + trigger3);
                }
            } else {
                log.info("trigger not enabled");
            }
        }
        return finalList;
    }

    private static boolean matches(EmailTrigger trigger, String trigger1, String trigger2, String trigger3, String trigger4, String trigger5) {
        if (!matches(trigger.getTriggerCondition1(), trigger1)) {
            return false;
        }
        if (!matches(trigger.getTriggerCondition2(), trigger2)) {
            return false;
        }
        if (!matches(trigger.getTriggerCondition3(), trigger3)) {
            return false;
        }
        if (!matches(trigger.getTriggerCondition4(), trigger4)) {
            return false;
        }
        if (!matches(trigger.getTriggerCondition5(), trigger5)) {
            return false;
        }
        return true;
    }

    private static boolean matches(String triggerCondition, String eventValue) {
        //System.out.println("matches: " + triggerCondition + " - " + eventValue);
        if (triggerCondition == null) {            
//            System.out.println("  - yes");
            return true; // null trigger condition means match anything or nothing
        } else {
            if (eventValue == null) {
//                System.out.println("  - no, event value is null");
                return false; // not null trigger condition means match specific value, but no value given
            }
        }
        boolean  b = eventValue.startsWith(triggerCondition); // the trigger condition can be an initial portion of the value
//        System.out.println("  - " + b + " " + eventValue + " starts with " + triggerCondition);
        return b;
    }
    
    private String eventId;
    private String triggerCondition1;
    private String triggerCondition2;
    private String triggerCondition3;
    private String triggerCondition4;
    private String triggerCondition5;
    private boolean enabled;
    private boolean includeUser; // whether the email should go to the user associated with the event which fires this trigger
    private String conditionScriptXml; // if present will be checked on triggers, and will only fire if returns true

    public EmailTrigger() {
    }

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

    @Column(length = 2000)
    public String getConditionScriptXml() {
        return conditionScriptXml;
    }

    public void setConditionScriptXml(String conditionScriptXml) {
        this.conditionScriptXml = conditionScriptXml;
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
        if (triggerCondition3 != null && triggerCondition3.trim().length() == 0) {
            triggerCondition3 = null;
        }
        if (triggerCondition4 != null && triggerCondition4.trim().length() == 0) {
            triggerCondition4 = null;
        }
        if (triggerCondition5 != null && triggerCondition5.trim().length() == 0) {
            triggerCondition5 = null;
        }
    }

    @Override
    public void accept(EmailJobVisitor visitor) {
        visitor.visit(this);
    }
    
    public List<EmailItem> history(Date from, Date to, boolean reverseOrder, Session session) {
        return EmailItem.findByJobAndDate(this, from, to,reverseOrder, session);
    }

    @Transient
    @Override
    public boolean isActive() {
        return enabled && !deleted();
    }
    
    
}
