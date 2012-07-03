package io.milton.cloud.server.web.alt;

import io.milton.cloud.server.db.AltFormat;
import io.milton.cloud.server.web.FileResource;
import io.milton.common.ContentTypeUtils;
import io.milton.common.Path;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.Request.Method;
import io.milton.http.ResourceFactory;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.http.http11.auth.DigestResponse;
import io.milton.resource.DigestResource;
import io.milton.resource.GetableResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.utils.SessionManager;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.Combiner;
import org.hashsplit4j.api.Fanout;
import org.hashsplit4j.api.HashStore;
import org.hibernate.Transaction;

/**
 * Provides access to alternative formats for a given file.
 *
 * for example
 *
 * @author brad
 */
public class AltFormatResourceFactory implements ResourceFactory {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AltFormatResourceFactory.class);
    private final ResourceFactory wrapped;
    private final HashStore hashStore;
    private final BlobStore blobStore;
    private final AltFormatGenerator altFormatGenerator;

    public AltFormatResourceFactory(ResourceFactory wrapped, HashStore hashStore, BlobStore blobStore, AltFormatGenerator altFormatGenerator) {
        this.wrapped = wrapped;
        this.hashStore = hashStore;
        this.blobStore = blobStore;
        this.altFormatGenerator = altFormatGenerator;
    }

    @Override
    public Resource getResource(String host, String sPath) throws NotAuthorizedException, BadRequestException {
        log.info("getResource: " + sPath + " ----------------------------");
        Path p = Path.path(sPath);
        if (p.getName().startsWith("alt-")) {
            Resource r = wrapped.getResource(host, p.getParent().toString());
            if (r instanceof FileResource) {
                FileResource fr = (FileResource) r;
                long sourceHash = fr.getHash();
                AltFormat f = AltFormat.find(sourceHash, p.getName(), SessionManager.session());
                if (f != null) {
                    return new AltFormatResource((FileResource) r, p.getName(), f);
                } else {
                    // if the format is valid then create a resource which will generate on demand
                    String formatName = p.getName().replace("alt-", "");
                    FormatSpec format = altFormatGenerator.findFormat(formatName);
                    if (format != null) {
                        return new AltFormatResource((FileResource) r, p.getName(), format);
                    }
                    System.out.println("format not found: " + formatName);
                    return null;
                }
            } else {
                return null;
            }
        } else {
            return wrapped.getResource(host, sPath);
        }
    }

    /**
     * Used the hash of the resource and the content type embedded in the name
     * to locate an alternative representation of the resource
     */
    public class AltFormatResource implements GetableResource, DigestResource {

        private final FileResource r;
        private final String name;
        private final FormatSpec formatSpec;
        private AltFormat altFormat;
        private Fanout fanout;

        public AltFormatResource(FileResource r, String name, AltFormat altFormat) {
            this.r = r;
            this.name = name;
            this.altFormat = altFormat;
            formatSpec = null;
        }

        public AltFormatResource(FileResource r, String name, FormatSpec formatSpec) {
            this.r = r;
            this.name = name;
            this.altFormat = null;
            this.formatSpec = formatSpec;
        }

        @Override
        public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
            Combiner combiner = new Combiner();
            List<Long> fanoutCrcs = getFanout().getHashes();
            combiner.combine(fanoutCrcs, hashStore, blobStore, out);
            out.flush();

        }

        private Fanout getFanout() {
            if (fanout == null) {
                fanout = hashStore.getFanout(getAltFormat().getAltHash());
            }
            return fanout;
        }

        private AltFormat getAltFormat() {
            if (altFormat == null) {
                Transaction tx = SessionManager.session().beginTransaction();
                try {
                    altFormat = altFormatGenerator.generate(formatSpec, r);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                tx.commit();
            }
            return altFormat;
        }

        @Override
        public Long getMaxAgeSeconds(Auth auth) {
            return 60 * 60 * 24l;
        }

        @Override
        public String getContentType(String accepts) {
            return ContentTypeUtils.findAcceptableContentType(name, accepts);
        }

        @Override
        public Long getContentLength() {
            return getFanout().getActualContentLength();
        }

        @Override
        public String getUniqueId() {
            if (altFormat != null) {
                return altFormat.getAltHash() + "";
            } else {
                return null;
            }
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object authenticate(String user, String password) {
            return r.authenticate(user, password);
        }

        @Override
        public Object authenticate(DigestResponse digestRequest) {
            return r.authenticate(digestRequest);
        }

        @Override
        public boolean authorise(Request request, Method method, Auth auth) {
            return r.authorise(request, method, auth);
        }

        @Override
        public String getRealm() {
            return r.getRealm();
        }

        @Override
        public Date getModifiedDate() {
            return r.getModifiedDate();
        }

        @Override
        public String checkRedirect(Request request) throws NotAuthorizedException, BadRequestException {
            return null;
        }

        @Override
        public boolean isDigestAllowed() {
            return r.isDigestAllowed();
        }
    }
}
