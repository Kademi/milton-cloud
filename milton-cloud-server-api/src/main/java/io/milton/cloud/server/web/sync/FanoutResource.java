package io.milton.cloud.server.web.sync;

import io.milton.cloud.common.FanoutSerializationUtils;
import io.milton.cloud.server.web.CommonCollectionResource;
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

/**
 * Note that this will write the hash values which make up the fanout hash
 * - it will NOT write the actual file content!
 *
 * @author brad
 */
public class FanoutResource extends BaseResource implements GetableResource {

    private final Fanout fanout;
    private final String hash;

    public FanoutResource(Fanout fanout, String hash, SpliffySecurityManager securityManager,CommonCollectionResource parent) {
        super(securityManager, parent);
        this.fanout = fanout;
        this.hash = hash;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> map, String string) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        FanoutSerializationUtils.writeFanout(fanout.getHashes(), fanout.getActualContentLength(), out);
        out.flush();
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
        return "application/octet-stream";
    }

    @Override
    public Long getContentLength() {
        return null;
//        String s = fanout.getActualContentLength() + "";
//        return s.length() + fanout.getHashes().size() * 40l;
    }
}
