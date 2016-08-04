package io.milton.cloud.process;

import java.util.Date;

/**
 *
 * @author brad
 */
public interface ProcessInstance {

    void setTimeEntered(Date dateTime);

    String getProcessName();

    String getStateName();
    
    void setStateName(String name);    

    Date getTimeEntered();


}
