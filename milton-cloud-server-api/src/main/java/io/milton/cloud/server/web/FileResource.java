package io.milton.cloud.server.web;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.milton.common.ContentTypeUtils;
import io.milton.http.HttpManager;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.ReplaceableResource;
import io.milton.vfs.content.ContentSession.FileNode;
import io.milton.vfs.db.utils.SessionManager;

/**
 *
 *
 * @author brad
 */
public class FileResource extends AbstractContentResource implements ReplaceableResource {

    private static final Logger log = LoggerFactory.getLogger(FileResource.class);
    private final FileNode fileNode;
    private RenderFileResource htmlPage; // for parsing html pages

    public FileResource(FileNode fileNode, ContentDirectoryResource parent, Services services) {
        super(fileNode, parent, services);
        this.fileNode = fileNode;
        System.out.println("loaed file: " + getName() + " with hash: " + fileNode.getHash());
    }

    @Override
    public void replaceContent(InputStream in, Long length) throws BadRequestException, ConflictException, NotAuthorizedException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();

        String ct = HttpManager.request().getContentTypeHeader();
        if (ct != null && ct.equals("spliffy/hash")) {
            // read the new hash and set it on this            
            DataInputStream din = new DataInputStream(in);
            try {
                long hash = din.readLong();
                fileNode.setHash(hash);
                System.out.println("setHash: " + hash + " - on " + getName());
            } catch (IOException ex) {
                throw new BadRequestException("Couldnt read the new hash", ex);
            }

        } else {
            log.info("replaceContent: set content");
            try {
                // parse data and persist to stores
                fileNode.setContent(in);
            } catch (IOException ex) {
                throw new BadRequestException("exception", ex);
            }
        }
        parent.save();
        tx.commit();
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        System.out.println("sendContent: fileresource: " + getName());
        if( params != null && params.containsKey("type") && "hash".equals(params.get("type") )) {
            String s = fileNode.getHash() + "";
            out.write(s.getBytes());
        } else {
            fileNode.writeContent(out);
        }
    }

    /**
     * Calculate content type based on file name
     *
     * @param accepts
     * @return
     */
    @Override
    public String getContentType(String accepts) {
        String acceptable = ContentTypeUtils.findContentTypes(getName());
        return ContentTypeUtils.findAcceptableContentType(acceptable, accepts);
    }

    @Override
    public Long getContentLength() {
        return fileNode.getContentLength();
    }


    @Override
    public boolean isDir() {
        return false;
    }
    
    public RenderFileResource getHtml() {
        if( htmlPage == null ) {
            htmlPage = new RenderFileResource(services, this);
        }
        return htmlPage;                
    }
}
