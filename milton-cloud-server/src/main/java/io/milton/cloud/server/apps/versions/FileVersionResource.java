package io.milton.cloud.server.apps.versions;

import io.milton.common.ContentTypeUtils;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import org.hashsplit4j.api.Combiner;
import org.hashsplit4j.api.Fanout;
import io.milton.cloud.server.db.DirectoryMember;
import io.milton.cloud.server.db.ItemVersion;
import io.milton.cloud.server.db.Organisation;
import io.milton.resource.GetableResource;

/**
 *
 * @author brad
 */
public class FileVersionResource extends AbstractVersionResource implements GetableResource{

    private Fanout fanout;
    
    public FileVersionResource(VersionCollectionResource parent, DirectoryMember directoryMember) {
        super(parent, directoryMember);
    }

    public ItemVersion getItemVersion() {
        return directoryMember.getMemberItem();
    }

    @Override
    public boolean isDir() {
        return false;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        Combiner combiner = new Combiner();
        List<Long> fanoutCrcs = getFanout().getHashes();
        combiner.combine(fanoutCrcs, getHashStore(), getBlobStore(), out);
        out.flush();
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return 60 * 60 * 24 * 365 * 10l;  // is immutable, so 10 years
    }

    @Override
    public String getContentType(String accepts) {
        String acceptable = ContentTypeUtils.findContentTypes(getName());
        return ContentTypeUtils.findAcceptableContentType(acceptable, accepts);
    }

    @Override
    public Long getContentLength() {
        return getFanout().getActualContentLength();
    }
    
    private long getHash() {
        return getItemVersion().getItemHash();
    }
    
    private Fanout getFanout() {
        if (fanout == null) {
            fanout = getHashStore().getFanout(getHash());
            if (fanout == null) {
                throw new RuntimeException("Fanout not found: " + getHash());
            }
        }
        return fanout;
    }

    @Override
    public Organisation getOrganisation() {
        return parent.getOrganisation();
    }    
}
