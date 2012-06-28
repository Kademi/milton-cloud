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
import io.milton.http.Response.Status;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.http.values.ValueAndType;
import io.milton.http.webdav.PropFindResponse.NameAndError;
import io.milton.property.BeanPropertyResource;
import io.milton.resource.ReplaceableResource;
import io.milton.vfs.data.DataSession.FileNode;
import io.milton.vfs.db.utils.SessionManager;
import java.io.*;
import java.util.List;
import javax.xml.namespace.QName;

/**
 *
 *
 * @author brad
 */
@BeanPropertyResource(value = "milton")
public class FileResource extends AbstractContentResource implements ReplaceableResource, ParameterisedResource {

    private static final Logger log = LoggerFactory.getLogger(FileResource.class);
    private final FileNode fileNode;
    private RenderFileResource htmlPage; // for parsing html pages

    public FileResource(FileNode fileNode, ContentDirectoryResource parent) {
        super(fileNode, parent);
        this.fileNode = fileNode;
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
                updateModDate();
            } catch (IOException ex) {
                throw new BadRequestException("Couldnt read the new hash", ex);
            }

        } else {
            setContent(in);
        }
        parent.save();
        tx.commit();
    }

    /**
     * Just updates content, does not save on parent or do any transaction
     * handling
     *
     * @param in
     * @throws BadRequestException
     */
    public void setContent(InputStream in) throws BadRequestException {
        log.info("replaceContent: set content");
        try {
            // parse data and persist to stores
            fileNode.setContent(in);
            updateModDate();
        } catch (IOException ex) {
            throw new BadRequestException("exception", ex);
        }

    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        System.out.println("sendContent: fileresource: " + getName());
        if (params != null && params.containsKey("type") && "hash".equals(params.get("type"))) {
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
        if (htmlPage == null) {
            if (NodeChildUtils.isHtml(this)) {
                htmlPage = new RenderFileResource(this);
            }
        }
        return htmlPage;
    }

    @Override
    public String getTitle() {
        RenderFileResource r = getHtml();
        if (r != null) {
            return r.getTitle();
        } else {
            return getName();
        }
    }

    @Override
    public String getParam(String name) {
        return getHtml().getParam(name);
    }

    @Override
    public void setParam(String name, String value) {
        getHtml().setParam(name, value);
    }

    @Override
    public List<String> getParamNames() {
        return getHtml().getParamNames();
    }

    @Override
    public void doCommit(Map<QName, ValueAndType> knownProps, Map<Status, List<NameAndError>> errorProps) throws BadRequestException, NotAuthorizedException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();

        doSave();

        tx.commit();
    }

    public void doSave() throws BadRequestException, NotAuthorizedException {
        // htmlPage will only have been set if html content fields have been set, in which
        // case we need to generate and persist html content
        if (htmlPage != null) {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            services.getTemplateParser().update(htmlPage, bout);
            byte[] arr = bout.toByteArray();
            ByteArrayInputStream bin = new ByteArrayInputStream(arr);
            setContent(bin);
            parent.save();
        }

    }
}
