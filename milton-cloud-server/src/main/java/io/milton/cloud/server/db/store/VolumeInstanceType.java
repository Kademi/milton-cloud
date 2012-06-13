package io.milton.cloud.server.db.store;

/**
 * Different implementations are for different means of persisting data:
 * 
 * Eg to a local file system, a remote web server, or a cloud service
 *
 * @author brad
 */
public interface VolumeInstanceType {
    
    /**
     * Identifies this type
     * 
     * @return 
     */
    String getTypeId();
    
    /**
     * Write the given bytes to the storage at the given location
     * 
     * @param location - a parameter which makes sense to this instance type
     * @param hash - hash of the given bytes
     * @param bytes - the data to store
     */
    public void setBlob(String location, long hash, byte[] bytes) throws VolumeInstanceException;


    /**
     * Read bytes from the given location
     * 
     * @param location
     * @param hash
     * @return 
     */
    public byte[] getBlob(String location, long hash) throws VolumeInstanceException;
}
