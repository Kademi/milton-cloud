/*
 * Copyright (C) 2012 McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.web;

import io.milton.cloud.server.web.templating.HtmlTemplateParser;
import io.milton.common.ContentTypeService;
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
import io.milton.http.Request;
import java.util.HashMap;

/**
 *
 *
 * @author brad
 */
@BeanPropertyResource(value = "milton")
public class FileResource extends AbstractContentResource implements ReplaceableResource, ParameterisedResource, ContentResource, HashResource {

    private static final Logger log = LoggerFactory.getLogger(FileResource.class);
    private static final Range docTypeRange = new Range(0, 30);
    protected final FileNode fileNode;
    private RenderFileResource htmlPage; // for parsing html pages
    protected JsonResult jsonResult;

    public FileResource(FileNode fileNode, ContentDirectoryResource parent) {
        super(fileNode, parent);
        this.fileNode = fileNode;
    }

    @Override
    public void replaceContent(InputStream in, Long length) throws BadRequestException, ConflictException, NotAuthorizedException {
        UploadUtils.replaceContent(this, in, length);
    }

    /**
     * Just updates content, does not save on parent or do any transaction
     * handling
     *
     * @param in
     * @throws BadRequestException
     */
    public void setContent(InputStream in) throws BadRequestException {
        UploadUtils.setContent(this, in);
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException {
        if (jsonResult != null) {
            jsonResult.write(out);
            return;
        }
        if (params != null && params.containsKey("type") && "hash".equals(params.get("type"))) {
            String s = fileNode.getHash() + "";
            out.write(s.getBytes());
        } else {
            boolean asJson = (contentType != null && contentType.contains("application/json")) || (params != null && "json".equals(params.get("type")));
            if (asJson) {
                sendContentAsJson(out);
            } else {
                if (fileNode == null || fileNode.getHash() == null) {
                    log.info("Cant generate content for file, it has no hash");
                } else {
                    if (fileNode.getHash() != null) {
                        if (range == null) {
                            fileNode.writeContent(out);
                        } else {
                            fileNode.writeContent(out, range.getStart(), range.getFinish());
                        }
                    }
                }
            }
        }
    }

    private void sendContentAsJson(OutputStream out) throws IOException {
        RenderFileResource page = getHtml();
        Map<String, String> map = new HashMap<>();
        map.put("title", page.getTitle());
        map.put("body", page.getBody());
        for (String s : page.getParamNames()) {
            map.put(s, page.getParam(s));
        }
        jsonResult = new JsonResult(true);
        jsonResult.setData(map);
        jsonResult.write(out);
    }

    /**
     * Calculate content type based on file name
     *
     * @param accepts
     * @return
     */
    @Override
    public String getContentType(String accepts) {
        Request req = HttpManager.request();
        if (req.getParams() != null && "json".equals(req.getParams().get("type"))) {
            return JsonResult.CONTENT_TYPE;
        }
        if (getName() == null) {
            throw new RuntimeException("no name");
        }
        return getUnderlyingContentType();
    }

    /**
     * getContentType will return the content type of the data which will be
     * generated for this request, which might be a transformation of the
     * underlying data (eg transformed to JSON)
     *
     * getUnderlyingContentType returns the content type of the underlying data,
     * which is invariant with respect to request information
     *
     * @param accepts
     * @return
     */
    public String getUnderlyingContentType() {
        String acceptable = ContentTypeUtils.findContentTypes(getName().toLowerCase());
        String ct = ContentTypeUtils.findAcceptableContentType(acceptable, null);
        return ct;

    }

    @Override
    public Long getContentLength() {
        Request req = HttpManager.request();
        if (req != null) {
            Map<String, String> params = req.getParams();
            if (params != null && params.containsKey("type") && "hash".equals(params.get("type"))) {
                String s = fileNode.getHash() + "";
                return (long) s.length();
            }
        }
        if (fileNode == null || fileNode.getHash() == null) {
            return null;
        }
        try {
            return fileNode.getContentLength();
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Override
    public boolean isDir() {
        return false;
    }

    /**
     * Create an instance of a HTML renderable object to use with this file, if
     * this file is a suitable type (ie is html)
     *
     * Note the ContentApp is responsible for locating these resources for the
     * resource factory at the appropriate times. It will use a DocType to
     * determine if a html file should be rendered with a template or raw
     *
     * @return
     */
    public RenderFileResource getHtml() {
        if (htmlPage == null) {
            if (getName() == null || NodeChildUtils.isHtml(this)) { // name will be null when editing new html pages                
                if (!hasDocType()) { // only use a RFR if no doctype, this provides a means to have plain, unrendered, html files
                    htmlPage = new RenderFileResource(this);
                }
            }
        }
        return htmlPage;
    }

    /**
     * Attempts to parse no matter what the doctype
     * 
     * @return 
     */
    public RenderFileResource parseHtml() {
        if (htmlPage == null) {
            htmlPage = new RenderFileResource(this);
        }
        return htmlPage;
    }    
    
    private boolean hasDocType() {
        ByteArrayOutputStream bout = new ByteArrayOutputStream(10);
        try {
            sendContent(bout, docTypeRange, null, null);
            String s = bout.toString("UTF-8");
            return s.startsWith("<!DOCTYPE");
        } catch (Throwable ex) {
            log.warn("ioexception checking for doc type", ex);
            return false;
        }
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

    public String getBody() {
        RenderFileResource r = getHtml();
        if (r != null) {
            return r.getBody();
        } else {
            return "";
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
        try {
            parent.save();
        } catch (IOException ex) {
            throw new BadRequestException("ioex", ex);
        }

        tx.commit();
    }

    /**
     * Writes any parsed data in the htmlPage to this file's content. Runs JTidy
     * (or similar) to ensure html is parseable
     *
     * @throws BadRequestException
     * @throws NotAuthorizedException
     */
    public void doSaveHtml() throws BadRequestException, NotAuthorizedException {
        doSaveHtml(true);
    }
    
    public void doSaveHtml(boolean runTidy) throws BadRequestException, NotAuthorizedException {
        // htmlPage will only have been set if html content fields have been set, in which
        // case we need to generate and persist html content
        if (htmlPage != null) {
            try {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                _(HtmlTemplateParser.class).update(htmlPage, bout);
                String rawHtml = bout.toString("UTF-8");
                String tidyHtml;
                if( runTidy ) {
                    tidyHtml = WebUtils.tidyHtml2(rawHtml);
                    //String tidyHtml = WebUtils.tidyHtml(rawHtml);
                } else {
                    tidyHtml = rawHtml;
                }
                ByteArrayInputStream bin = new ByteArrayInputStream(tidyHtml.getBytes("UTF-8"));
                setContent(bin);
            } catch (UnsupportedEncodingException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            log.warn("No htmlPage, so nothing to save");
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

    public FileNode getFileNode() {
        return fileNode;
    }

    @Override
    public boolean is(String type) {
        return is(type, getName());
    }

    public boolean is(String type, String name) {
        if (type.equals("file")) {
            return true;
        }
        boolean b = super.is(type);
        if (b) {
            return true;
        }

        // will return a non-null value if type is contained in any content type
        List<String> list = _(ContentTypeService.class).findContentTypes(name);
        if (list != null) {
            for (String ct : list) {
                if (ct.contains(type)) {
                    return true;
                }
            }
        }

        // If the resource is a html page we can check if its template matches the type
        // Eg if html page has template learner/modulePage then page.is("modulePage) should return true
        if (name.endsWith(".html")) {
            RenderFileResource html = getHtml();
            if (html != null) {
                if (html.isTemplate(type)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void setHash(String s) {
        if (htmlPage != null) {
            log.warn("Set hash, but htmlpage representation exists and will not be updated"); // TODO: should we flush the htmlpage?
        }
        super.setHash(s);
    }

    @Override
    public Priviledge getRequiredPostPriviledge(Request request) {
        return null;
    }
    
    public String getTextContent() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            fileNode.writeContent(out);
            return out.toString("UTF-8");
        } catch (IOException ex) {
            log.error("Exception", ex);
            return null;
        }
    }
}
