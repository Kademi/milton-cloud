package io.milton.cloud.server.queue;

import io.milton.context.Context;
import java.io.Serializable;

/**
 * MUST be serializable. Please remember to set your version id
 *
 * @author brad
 */
public interface Processable extends Serializable{
    /**
     * Implement the process to be executed. Must commit any transactions.
     *
     * @param context
     */
    public void doProcess(Context context);

    /**
     * A reminder to implementors to implement the Serializable interface
     * AND set a serialVersionUID
     */
    public void pleaseImplementSerializable();

}
