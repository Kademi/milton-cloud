package io.milton.cloud.server.web.sync;

import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import org.hashsplit4j.api.Fanout;
import io.milton.vfs.db.Organisation;
import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.resource.GetableResource;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.Combiner;
import org.hashsplit4j.api.HashStore;

/**
 * Writes actual file content for the given hash
 *
 * @author brad
 */
public class GetResource extends BaseResource implements GetableResource {

    private final Fanout fanout;
    private final String hash;
    private final BlobStore blobStore;
    private final HashStore hashStore;

    public GetResource(Fanout fanout, String hash, SpliffySecurityManager securityManager, Organisation org, BlobStore blobStore, HashStore hashStore) {
        super(securityManager, org);
        this.fanout = fanout;
        this.hash = hash;
        this.blobStore = blobStore;
        this.hashStore = hashStore;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> map, String string) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        Combiner combiner = new Combiner();
        combiner.combine(fanout.getHashes(), hashStore, blobStore, out);
    }

    @Override
    public String getName() {
        return hash;
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return 60 * 60 * 24 * 365 * 10l; // 10 years
    }

    @Override
    public String getContentType(String string) {
        return null;
    }

    @Override
    public Long getContentLength() {
        return fanout.getActualContentLength();
    }
}
