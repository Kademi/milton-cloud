package io.milton.cloud.server.web.sync;

import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import org.hashsplit4j.api.Fanout;
import io.milton.vfs.db.Organisation;
import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.resource.GetableResource;

/**
 * Note that this will write the hash values which make up the fanout hash
 * - it will NOT write the actual file content!
 *
 * @author brad
 */
public class FanoutResource extends BaseResource implements GetableResource {

    private final Fanout fanout;
    private final long hash;

    public FanoutResource(Fanout fanout, long hash, SpliffySecurityManager securityManager, Organisation org) {
        super(securityManager, org);
        this.fanout = fanout;
        this.hash = hash;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> map, String string) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        DataOutputStream dos = new DataOutputStream(out);
        dos.writeLong(fanout.getActualContentLength());
        for (Long l : fanout.getHashes()) {
            dos.writeLong(l);
        }
        dos.flush();
        out.flush();
    }

    @Override
    public String getName() {
        return hash + "";
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return 60 * 60 * 24 * 365 * 10l; // 10 years
    }

    @Override
    public String getContentType(String string) {
        return "application/octet-stream";
    }

    @Override
    public Long getContentLength() {
        return 8 + fanout.getHashes().size() * 8l; // 8 bytes for each hash, plus 8 for the actualContentLength
    }
}
