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
import io.milton.cloud.server.manager.CurrentRootFolderService;
import io.milton.cloud.server.web.RootFolder;
import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.common.FileUtils;
import io.milton.common.Utils;
import io.milton.http.ResourceFactory;
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
public class SpliffySyncResourceFactory implements ResourceFactory {

    private static final Logger log = Logger.getLogger(SpliffySyncResourceFactory.class);
    
    public static final Date LONG_LONG_AGO = new Date(0);
    private String basePath = "/_hashes";
    private final SpliffySecurityManager securityManager;
    private final CurrentRootFolderService currentRootFolderService;
    private final HashStore hashStore;
    private final BlobStore blobStore;
    
    public SpliffySyncResourceFactory(HashStore hashStore, BlobStore blobStore, SpliffySecurityManager securityManager,CurrentRootFolderService currentRootFolderService) {
        this.hashStore = hashStore;
        this.blobStore = blobStore;
        this.securityManager = securityManager;
        this.currentRootFolderService = currentRootFolderService;
    }

    @Override
    public Resource getResource(String host, String path) throws NotAuthorizedException, BadRequestException {
        if (host.contains(":")) {
            host = host.substring(0, host.indexOf(":"));
        }

        if (path.startsWith(basePath)) {
            RootFolder rootFolder = currentRootFolderService.getRootFolder(host);
            return getSyncResource(path, rootFolder.getOrganisation());
        } else {
            return null;
        }
    }

    private Resource getSyncResource(String path, Organisation org) throws NumberFormatException {
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
            case "fileFanouts": // for getting the fanouts that make up a file
                if (numPathParts == 1) {
                    return new FanoutFolder(hashStore, first, securityManager, org, false);
                } else {
                    return findFanout(p.getName(), org, false);
                }
            case "chunkFanouts": // for getting the list of blobs within a fanout
                if (numPathParts == 1) {
                    return new FanoutFolder(hashStore, first, securityManager, org, true);
                } else {
                    return findFanout(p.getName(), org, true);
                }
            case "blobs": // For writing blobs, ie chunks of files
                if (numPathParts == 1) {
                    return new BlobFolder(blobStore, first, securityManager, org);
                } else {
                    return findBlob(p.getName(), org);
                }
            case "files": // For writing entire file contents, not just blobs
                if (numPathParts == 1) {
                    return new FilesFolder(blobStore, hashStore, path, securityManager, org);
                } else {
                    return findGetResource(p.getName(), org);
                }
            case "dirhashes": // gets the triplets within a directory, keyed by the hash of the directory
                if (numPathParts == 1) {
                    return new DirectoryHashesFolder(blobStore, hashStore, path, securityManager, org);
                } else {
                    return findDirectoryHashResource(p.getName(), org);
                }
            default:
                return null;
        }
    }

    private Resource findBlob(String hash, Organisation org) {
        byte[] bytes = blobStore.getBlob(hash);
        if (bytes == null) {
            return null;
        } else {
            return new BlobResource(bytes, hash, securityManager, org);
        }
    }

    private Resource findFanout(String hash, Organisation org, boolean isChunk) {
        Fanout fanout;
        if( isChunk ) {
            fanout = hashStore.getChunkFanout(hash);
        } else {
            fanout = hashStore.getFileFanout(hash);
        }
        if (fanout == null) {
            log.warn("fanout not found");
            return null;
        } else {
            return new FanoutResource(fanout, hash, securityManager, org);
        }
    }
    
    private Resource findGetResource(String fileName, Organisation org) {
        String hash = FileUtils.stripExtension(fileName);
        Fanout fanout = hashStore.getFileFanout(hash);
        if (fanout == null) {
            log.warn("fanout not found");
            return null;
        } else {
            return new GetResource(fileName, fanout, hash, securityManager, org, blobStore, hashStore);
        }
        
    }

    private Resource findDirectoryHashResource(String hash, Organisation org) {
        return new DirectoryHashResource(hash, securityManager, org);
    }
            
}
