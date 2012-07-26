package io.milton.cloud.server.web.sync;

import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.GetableResource;
import io.milton.vfs.data.HashCalc;
import io.milton.vfs.db.DataItem;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.utils.SessionManager;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 *
 * @author brad
 */
public class DirectoryHashResource  extends BaseResource implements GetableResource {

    private final long hash;

    public DirectoryHashResource(long hash, SpliffySecurityManager securityManager, Organisation org) {
        super(securityManager, org);
        this.hash = hash;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> map, String string) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        List<DataItem> list = DataItem.findByHash(hash, SessionManager.session());
        HashCalc.getInstance().calcHash(list, out);
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
        return null;
    }

    @Override
    public Long getContentLength() {
        return null;
    }
}

