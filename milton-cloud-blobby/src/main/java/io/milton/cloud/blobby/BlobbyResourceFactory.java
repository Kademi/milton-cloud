package io.milton.cloud.blobby;

import io.milton.cloud.common.store.CachingBlobStore;
import io.milton.cloud.common.store.FileSystemBlobStore;
import io.milton.common.Path;
import io.milton.resource.Resource;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import java.util.Date;
import org.hashsplit4j.api.BlobStore;
import io.milton.http.ResourceFactory;
import io.milton.http.SecurityManager;
import io.milton.http.fs.SimpleSecurityManager;
import io.milton.resource.CollectionResource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.String;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.log4j.Logger;

/**
 * Implements a URL scheme for handling HTTP interactions with a file sync
 * client
 *
 * where - XXX is some hash value - basePath is the configured base for this
 * resource factory
 *
 * /{basePath}/fanouts/XXX - returns a list of chunk hashs within a fanout,
 * which makes up a segment of a file
 *
 * /{basePath}/blobs/XXX - returns a stream of bytes which is the file content
 * for a single chunk
 *
 * Note that each file will have a single top level fanout hash which will be
 * linked to the fanout hashes containing chunk hashes. This top level fanout is
 * handled the same as the second level fanouts
 *
 * FileHash -> Fanout hashes -> Chunk hashes -> blobs (Actual file data)
 *
 * @author brad
 */
public class BlobbyResourceFactory implements ResourceFactory {

    private static final Logger log = Logger.getLogger(BlobbyResourceFactory.class);
    public static final Date LONG_LONG_AGO = new Date(0);
    private final String rootFolderName = "blobs";
    private final io.milton.http.SecurityManager securityManager;
    private final BlobStore blobStore;
    private final RootFolder rootFolder;

    public BlobbyResourceFactory() throws IOException {
        Properties defaultProps = new Properties();
        defaultProps.setProperty("root", "/blobs");
        defaultProps.setProperty("realm", "blobby");
        defaultProps.setProperty("user", "admin");
        defaultProps.setProperty("password", "password8");
        Properties props = new Properties(defaultProps);
        InputStream in = BlobbyResourceFactory.class.getResourceAsStream("/blobby.properties");
        if (in == null) {
            log.error("Could not find blobby properties file in classpath: /blobby.properties");
            log.error("Listing default values: ");
            for (String key : defaultProps.stringPropertyNames()) {
                log.error("  " + key + "=" + defaultProps.getProperty(key));
            }
        } else {
            props.load(in);
        }
        File root = new File(props.getProperty("root"));
        if (!root.exists()) {
            log.warn("Root blob path does not exist: " + root.getAbsolutePath());
            log.warn("Attempting to create...");
            if (root.mkdir()) {
                log.info("Created root path ok");
            } else {
                throw new RuntimeException("Could not create root blob path: " + root.getAbsolutePath());
            }
        }
        String realm = props.getProperty("realm");
        String user = props.getProperty("user");
        String password = props.getProperty("password");

        BlobStore fsBlobStore = new FileSystemBlobStore(root);
        blobStore = new CachingBlobStore(fsBlobStore, 1000);
        Map<String, String> map = new HashMap<>();
        map.put(user, password);
        this.securityManager = new SimpleSecurityManager(realm, map);
        rootFolder = new RootFolder(this);
        
    }

    @Override
    public Resource getResource(String host, final String path) throws NotAuthorizedException, BadRequestException {
        Path p = Path.path(path);
        if( p.getFirst().equals("dirs") ) {
            p = p.getStripFirst();
            return findDir(p);
        }
        return find(p, rootFolder);        
    }

    public static Resource find(Path p, CollectionResource col) throws NotAuthorizedException, BadRequestException {
        Resource r = col;
        for( String s : p.getParts()) {
            if( r instanceof CollectionResource) {
                r = ((CollectionResource)r).child(s);
            } else {
                return null;
            }
        }
        return r;
    }    
    
    public SecurityManager getSecurityManager() {
        return securityManager;
    }

    public BlobStore getBlobStore() {
        return blobStore;
    }

    /**
     * Find a resource representing the hashes of a directory. The given path
     * will identify a physical directory in the blobs dir
     * 
     * @param p
     * @return 
     */
    private Resource findDir(Path p) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    
}
