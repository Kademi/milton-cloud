package io.milton.cloud.server.mail;

import io.milton.vfs.db.Organisation;
import java.util.List;

/**
 * Defines the configuration options for an email trigger type.
 * 
 * For example, the subscription event type allows the user to select the
 * group, website and action
 * 
 * These configuration choices must correspond to the properties stored on the 
 * EmailTrigger object
 *
 * @author brad
 */
public interface EmailTriggerType {
    
    /**
     * Must be same as on corresponding Event implementation
     * 
     * @return 
     */
    String getEventId();
    
    List<Option> options1(Organisation o);
    
    List<Option> options2(Organisation o);
    
    List<Option> options3(Organisation o);
    
    List<Option> options4(Organisation o);
    
    List<Option> options5(Organisation o);
    
    String label(String optionCode);
}
