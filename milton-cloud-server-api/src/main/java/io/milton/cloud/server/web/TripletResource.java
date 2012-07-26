package io.milton.cloud.server.web;

import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.Request.Method;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.http.http11.auth.DigestResponse;
import io.milton.resource.DigestResource;
import io.milton.resource.GetableResource;
import io.milton.vfs.data.HashCalc;
import io.milton.vfs.db.DataItem;
import io.milton.vfs.db.utils.SessionManager;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Used by HttpTripletStore to get triplets for a directory
 *
 * @author brad
 */
public class TripletResource implements GetableResource, DigestResource {

    private final String name;
    private final ContentDirectoryResource dr;
    
    public TripletResource(String name, ContentDirectoryResource dr) {
        this.name = name;
        this.dr = dr;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {        
        List<DataItem> list = DataItem.findByHash(dr.getDirectoryNode().getHash(), SessionManager.session());        
        HashCalc.getInstance().calcHash(list, out);
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return null;
    }

    @Override
    public String getContentType(String accepts) {
        return "text/triplets";
    }

    @Override
    public Long getContentLength() {
        return null;
    }

    @Override
    public String getUniqueId() {
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object authenticate(String user, String password) {
        return dr.authenticate(user, password);
    }

    @Override
    public boolean authorise(Request request, Method method, Auth auth) {
        return dr.authorise(request, method, auth);
    }

    @Override
    public String getRealm() {
        return dr.getRealm();
    }

    @Override
    public Date getModifiedDate() {
        return null;
    }

    @Override
    public String checkRedirect(Request request) throws NotAuthorizedException, BadRequestException {
        return null;
    }

    @Override
    public Object authenticate(DigestResponse digestRequest) {
        return dr.authenticate(digestRequest);
    }

    @Override
    public boolean isDigestAllowed() {
        return dr.isDigestAllowed();
    }
    
}
