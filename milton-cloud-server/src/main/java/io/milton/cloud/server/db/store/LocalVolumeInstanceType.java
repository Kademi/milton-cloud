package io.milton.cloud.server.db.store;

import io.milton.cloud.server.db.VolumeInstance;
import java.io.*;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

/**
 * A volume instance type for storing blobs in a local directory
 *
 * The location parameter (ie stored on VolumeInstance's of this type) should
 * simply be a path
 *
 * @author brad
 */
public class LocalVolumeInstanceType implements VolumeInstanceType {

    private final String id;
    
    /**
     * On startup the initialDirs will be checked to see if they exist in the
     * database (as VolumeInstance's) and created if not
     *
     * @param initialDirs
     */
    public LocalVolumeInstanceType(String id, List<File> initialDirs, SessionFactory sessionFactory) {
        this.id = id;
        initInitialDirs(initialDirs, sessionFactory);
    }

    public LocalVolumeInstanceType(String id) {
        this.id = id;
    }

    @Override
    public String getTypeId() {
        return id;
    }
    
    

    private void initInitialDirs(List<File> initialDirs, SessionFactory sessionFactory) {
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        for (File f : initialDirs) {
            VolumeInstance vi = VolumeInstance.find(session, f.getAbsolutePath(), getTypeId());
            if( vi == null ) {
                // autocreate it
                vi = new VolumeInstance();
                vi.setCapacity(10000000l); // 10M
                vi.setCost(10);
                vi.setInstanceType(id);
                vi.setLocation(f.getAbsolutePath());
                vi.setOnline(true);
                session.save(vi);
            }
        }
        tx.commit();
    }

    @Override
    public void setBlob(String location, long hash, byte[] bytes) {
        File root = new File(location);
        File blob = FsHashUtils.toFile(root, hash);
        if (blob.exists()) {
            return; // already exists, so dont overwrite
        }
        File dir = blob.getParentFile();
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("Couldnt create blob directory: " + dir.getAbsolutePath());
            }
        }
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(blob);
            fout.write(bytes);
            fout.flush();
        } catch (IOException ex) {
            throw new RuntimeException(blob.getAbsolutePath(), ex);
        } finally {
            IOUtils.closeQuietly(fout);
        }
    }

    @Override
    public byte[] getBlob(String location, long hash) {
        File root = new File(location);
        File blob = FsHashUtils.toFile(root, hash);
        if (!blob.exists()) {
            return null;
        }
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(blob);
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            IOUtils.copy(fin, bout);
            return bout.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException(blob.getAbsolutePath(), ex);
        } finally {
            IOUtils.closeQuietly(fin);
        }
    }
}
