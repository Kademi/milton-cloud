package io.milton.cloud.server.web;

import io.milton.cloud.server.web.templating.HtmlTemplateParser;
import io.milton.common.ContentTypeService;
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
import io.milton.http.values.ValueAndType;
import io.milton.http.webdav.PropFindResponse.NameAndError;
import io.milton.property.BeanPropertyResource;
import io.milton.resource.ReplaceableResource;
import io.milton.vfs.data.DataSession.FileNode;
import io.milton.vfs.db.utils.SessionManager;
import java.io.*;
import java.util.List;
import javax.xml.namespace.QName;

import static io.milton.context.RequestContext._;
import io.milton.http.Auth;

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
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException {
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
        if( fileNode == null || fileNode.getHash() == 0 ) {
            return null;
        }        
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
        if (getHtml() != null) {
            return getHtml().getParam(name);
        } else {
            return null;
        }
    }

    @Override
    public void setParam(String name, String value) {
        getHtml().setParam(name, value);
    }

    @Override
    public List<String> getParamNames() {
        if (getHtml() != null) {
            return getHtml().getParamNames();
        } else {
            return null;
        }
    }

    @Override
    public void doCommit(Map<QName, ValueAndType> knownProps, Map<Status, List<NameAndError>> errorProps) throws BadRequestException, NotAuthorizedException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();

        doSaveHtml();
        parent.save();

        tx.commit();
    }

    /**
     * Writes any parsed data in the htmlPage to this file's content
     * 
     * @throws BadRequestException
     * @throws NotAuthorizedException 
     */
    public void doSaveHtml() throws BadRequestException, NotAuthorizedException {
        // htmlPage will only have been set if html content fields have been set, in which
        // case we need to generate and persist html content
        if (htmlPage != null) {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            _(HtmlTemplateParser.class).update(htmlPage, bout);
            byte[] arr = bout.toByteArray();
            ByteArrayInputStream bin = new ByteArrayInputStream(arr);
            setContent(bin);            
        } else {
            System.out.println("no htmlPage, so no property changes");
        }
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        if (this.is("html")) {
            return null;
        } else {
            return 60 * 60 * 24 * 7 * 4l; // 1 month
        }

    }

    public long getHash() {
        return this.contentNode.getHash();
    }

    @Override
    public boolean is(String type) {
        if (type.equals("file")) {
            return true;
        }
        boolean b = super.is(type);
        if (b) {
            return true;
        }

        // will return a non-null value if type is contained in any content type
        List<String> list = _(ContentTypeService.class).findContentTypes(getName());
        if( list != null ) {
            for(String ct : list ) {
                if( ct.contains(type)) {
                    return true;
                }
            }
        }
        return false;
    }
}
