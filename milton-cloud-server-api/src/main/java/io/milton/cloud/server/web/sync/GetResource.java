package io.milton.cloud.server.web.sync;

import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.HashResource;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import org.hashsplit4j.api.Fanout;
import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.common.ContentTypeUtils;
import io.milton.http.Request;
import io.milton.http.Request.Method;
import io.milton.resource.GetableResource;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.Combiner;
import org.hashsplit4j.api.HashStore;

/**
 * Writes actual file content for the given hash
 *
 * @author brad
 */
public class GetResource extends BaseResource implements GetableResource, HashResource {
    private final String name; // might be just hash, or hash plus an extension
    private final Fanout fanout;
    private final String hash;
    private final BlobStore blobStore;
    private final HashStore hashStore;

    public GetResource(String name, Fanout fanout, String hash, SpliffySecurityManager securityManager, BlobStore blobStore, HashStore hashStore, CommonCollectionResource parent) {
        super(securityManager, parent);
        this.name = name;
        this.fanout = fanout;
        this.hash = hash;
        this.blobStore = blobStore;
        this.hashStore = hashStore;
    }

    @Override
    public boolean authorise(Request rqst, Method method, Auth auth) {
        return true;
    }
        
    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> map, String string) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        //System.out.println("sendContent: " + this + " length=" + getContentLength());
        Combiner combiner = new Combiner();
        combiner.combine(fanout.getHashes(), hashStore, blobStore, out);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getHash() {
        return hash;
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return 60 * 60 * 24 * 365 * 10l; // 10 years
    }

    @Override
    public String getContentType(String accepts) {
        return ContentTypeUtils.findAcceptableContentTypeForName(name, accepts);
    }

    @Override
    public Long getContentLength() {
        return fanout.getActualContentLength();
    }

    
    
}
