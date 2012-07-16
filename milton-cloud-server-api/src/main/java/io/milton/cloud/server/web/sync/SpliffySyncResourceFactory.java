package io.milton.cloud.server.web.sync;

import io.milton.common.Path;
import io.milton.resource.Resource;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import java.util.Date;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.Fanout;
import org.hashsplit4j.api.HashStore;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Website;
import io.milton.cloud.server.db.utils.OrganisationDao;
import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.http.ResourceFactory;
import io.milton.vfs.db.utils.SessionManager;

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
public class SpliffySyncResourceFactory implements ResourceFactory {

    public static final Date LONG_LONG_AGO = new Date(0);
    private String basePath = "/_hashes";
    private final SpliffySecurityManager securityManager;
    private final HashStore hashStore;
    private final BlobStore blobStore;
    
    public SpliffySyncResourceFactory(HashStore hashStore, BlobStore blobStore, SpliffySecurityManager securityManager) {
        this.hashStore = hashStore;
        this.blobStore = blobStore;
        this.securityManager = securityManager;
    }

    @Override
    public Resource getResource(String host, String path) throws NotAuthorizedException, BadRequestException {
        if (host.contains(":")) {
            host = host.substring(0, host.indexOf(":"));
        }
        Organisation org;
        Website website = Website.findByDomainName(host, SessionManager.session());
        if (website == null) {
            org = OrganisationDao.getRootOrg(SessionManager.session());
            if (org == null) {
                throw new RuntimeException("Not in a website and no root organisation. Host=" + host);
            }            
        } else {
            org = (Organisation) website.getOrganisation();
        }

        if (path.startsWith(basePath)) {
            path = path.substring(basePath.length()); // strip the base path
            Path p = Path.path(path);
            int numPathParts = p.getParts().length;
            if (numPathParts == 0) {
                return null; // we don't have a meaningful root folder
            } else if (numPathParts > 2) {
                return null; // not a recognised depth
            }
            String first = p.getFirst();
            switch (first) {
                case "fanouts":
                    if (numPathParts == 1) {
                        return new FanoutFolder(hashStore, "fanouts", securityManager, org);
                    } else {
                        String sHash = p.getName();
                        long hash = Long.parseLong(sHash);
                        return findFanout(hash, org);
                    }
                case "blobs":
                    if (numPathParts == 1) {
                        return new BlobFolder(blobStore, "blobs", securityManager, org);
                    } else {
                        String sHash = p.getName();
                        long hash = Long.parseLong(sHash);
                        return findBlob(hash, org);
                    }
                case "get":
                    if (numPathParts == 1) {
                        return null;
                    } else {
                        String sHash = p.getName();
                        long hash = Long.parseLong(sHash);
                        return findGetResource(hash, org);
                    }
                default:
                    return null;
            }
        } else {
            return null;
        }
    }

    private Resource findBlob(long hash, Organisation org) {
        byte[] bytes = blobStore.getBlob(hash);
        if (bytes == null) {
            return null;
        } else {
            return new BlobResource(bytes, hash, securityManager, org);
        }
    }

    private Resource findFanout(long hash, Organisation org) {
        Fanout fanout = hashStore.getFanout(hash);
        if (fanout == null) {
            System.out.println("fanout not found");
            return null;
        } else {
            return new FanoutResource(fanout, hash, securityManager, org);
        }
    }
    
    private Resource findGetResource(long hash, Organisation org) {
        Fanout fanout = hashStore.getFanout(hash);
        if (fanout == null) {
            System.out.println("fanout not found");
            return null;
        } else {
            return new GetResource(fanout, hash, securityManager, org, blobStore, hashStore);
        }
        
    }
            
}
