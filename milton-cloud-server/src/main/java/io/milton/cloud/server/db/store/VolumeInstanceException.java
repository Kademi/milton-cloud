package io.milton.cloud.server.db.store;

/**
 * Indicates that there was a problem accessing a particular volume instance
 * 
 * Its possible that attempting the same operation on another instance
 * might be successful
 *
 * @author brad
 */
public class VolumeInstanceException extends Exception{

    public VolumeInstanceException(String message, Throwable cause) {
        super(message, cause);
    }

    public VolumeInstanceException(String message) {
        super(message);
    }

    public VolumeInstanceException(Throwable cause) {
        super(cause);
    }
    
}
