package io.milton.cloud.server.apps.email;

import io.milton.cloud.server.event.SubscriptionEvent;
import io.milton.cloud.server.event.SubscriptionEvent.SubscriptionAction;
import io.milton.cloud.server.mail.EmailTriggerType;
import io.milton.cloud.server.mail.Option;
import io.milton.vfs.db.Group;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Website;
import io.milton.vfs.db.utils.SessionManager;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author brad
 */
public class SubscriptionEventTriggerType implements EmailTriggerType {

    @Override
    public String getEventId() {
        return SubscriptionEvent.ID;
    }
    
    @Override
    public String label(String optionCode) {
        switch (optionCode) {
            case "triggerCondition1":
                return "Group";
            case "triggerCondition2":
                return "Website";
            case "triggerCondition3":
                return "Action";
                
        }
        return null;
    }    

    @Override
    public List<Option> options1(Organisation o) {
        List<Option> list = new ArrayList<>();
        for (Group g : o.groups(SessionManager.session())) {
            Option.add(list, g.getId(), g.getName());
        }
        return list;
    }

    @Override
    public List<Option> options2(Organisation o) {
        List<Option> list = new ArrayList<>();
        if (o.getWebsites() != null) {
            for (Website g : o.getWebsites()) {
                Option.add(list, g.getId(), g.getDomainName());
            }
        }
        return list;

    }

    @Override
    public List<Option> options3(Organisation o) {
        List<Option> list = new ArrayList<>();
        for( SubscriptionAction a : SubscriptionEvent.SubscriptionAction.values()) {
            Option.add(list, a.name());
        }
        return list;
    }

    @Override
    public List<Option> options4(Organisation o) {
        return null;
    }

    @Override
    public List<Option> options5(Organisation o) {
        return null;
    }
}
