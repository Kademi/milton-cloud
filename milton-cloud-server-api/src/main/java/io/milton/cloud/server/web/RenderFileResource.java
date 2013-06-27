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

import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import io.milton.cloud.server.web.templating.HtmlPage;
import io.milton.cloud.server.web.templating.HtmlTemplateParser;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.cloud.server.web.templating.WebResource;
import io.milton.common.Path;
import io.milton.http.Auth;
import io.milton.http.FileItem;
import io.milton.http.Range;
import io.milton.http.Response.Status;
import io.milton.principal.Principal;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.http.values.ValueAndType;
import io.milton.http.webdav.PropFindResponse.NameAndError;
import io.milton.property.BeanPropertyResource;
import io.milton.resource.*;
import io.milton.vfs.db.utils.SessionManager;
import javax.xml.namespace.QName;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.milton.context.RequestContext._;
import io.milton.http.Request;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Repository;
import java.util.HashMap;
import java.util.Iterator;
import javax.xml.stream.XMLStreamException;

/**
 *
 * This class is for rendering HTML pages.
 *
 * It wraps a normal FileResource and parses its content, expecting it to be a
 * well formed HTML page. It extracts a template, if present in a link tag, and
 * if not present defaults it to theme/page, so it will use the page.html
 * template of the current theme
 *
 * Example:
 *
 * <html> <head> <title>home page</title> <link rel="template" href="theme/home"
 * /> </head> <body class="home">
 *
 * The title, template and body can be updated with a POST to this
 *
 * Also supports read/write parameters embedded in the html, such as:
 *
 * <html> <head> <title>module 1</title> <script title="learningTimeMins"
 * type="data/parameter">220</script>
 *
 * Parameters can be accessed via milton/ajax integration with the milton
 * namespace
 *
 * Eg: module1/_DAV/PROPFIND?fields=milton:learningTimeMins
 *
 * And those parameters can be updated via PROPPATCH in a similar manner
 *
 * Creating new html pages is supported by integration with NewPageResource,
 * which looks for a .new suffix, and creates an instance of RenderFileResource
 * on the fly
 *
 * @author brad
 */
@BeanPropertyResource(value = "milton")
public class RenderFileResource extends AbstractResource implements GetableResource, MoveableResource, CopyableResource, DeletableResource, HtmlPage, PostableResource, ParameterisedResource, ReplaceableResource, ContentResource {

    private static final Logger log = LoggerFactory.getLogger(RenderFileResource.class);
    public static final String DEFAULT_TEMPLATE = "theme/page";
    private final FileResource fileResource;
    private final List<String> bodyClasses = new ArrayList<>();
    private final List<WebResource> webResources = new ArrayList<>();
    private boolean parsed;
    private String template;
    private String title;
    private String body;
    private JsonResult jsonResult;
    private boolean isNewPage; // set to true by NewPageResource
    private String nameOverride;

    public RenderFileResource(FileResource fileResource) {
        this.fileResource = fileResource;
    }

    /**
     * Call when the underlying FileResource has changed. This will cause this
     * RFR to flush any cached representation of the file
     */
    public void reset() {
        parsed = false;
        bodyClasses.clear();
        webResources.clear();
        body = null;
        title = null;
    }
    
    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        log.info("processform: " + getName());

        // First ensure existing content is parsed
        checkParse();

        // Now bind new data to the parsed fields
        if (parameters.containsKey("template")) {
            template = parameters.get("template");
        }
        if (parameters.containsKey("title")) {
            title = parameters.get("title");
        }
        if (parameters.containsKey("body")) {
            body = parameters.get("body");
            setBody(parameters.get("body"));
        }
        boolean didNew = isNewPage();

        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        try {
            doSaveHtml();
            tx.commit();
            if (didNew) {
                jsonResult = new JsonResult(true, "Created page", fileResource.getHref());
            } else {
                jsonResult = new JsonResult(true);
            }
        } catch (IOException ex) {
            log.error("io ex", ex);
            jsonResult = new JsonResult(false, "IO Exception");
        }

        return null;
    }

    @Override
    public Priviledge getRequiredPostPriviledge(Request request) {
        return Priviledge.WRITE_CONTENT;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        if (jsonResult != null) {
            jsonResult.write(out);
        } else {
            checkParse();
            WebUtils.setActiveMenu(getHref(), WebUtils.findRootFolder(this));
            _(HtmlTemplater.class).writePage(template, this, params, out);
        }
    }

    private void checkParse() {
        if (parsed) {
            return;
        }
        parsed = true;
        try {
            _(HtmlTemplateParser.class).parse(this);
        } catch (XMLStreamException ex) {
            throw new RuntimeException(getHref(), ex);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        for (WebResource wr : webResources) {
            if (wr.getTag().equals("link")) {
                String rel = wr.getAtts().get("rel");
                if (rel != null && rel.equals("template")) {
                    template = wr.getAtts().get("href");
                }
            }
        }
        // If no page template is given defaul to theme/page, this is equivalent to:
        // <link rel="template" href="theme/page" />
        if (template == null) {
            template = DEFAULT_TEMPLATE;
        }
    }

    @Override
    public InputStream getInputStream() {
        if (fileResource.getContentLength() == null) {
            return null;
        }
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try {
            fileResource.sendContent(bout, null, null, title);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return new ByteArrayInputStream(bout.toByteArray());
    }

    @Override
    public List<String> getBodyClasses() {
        return bodyClasses;
    }

    @Override
    public List<WebResource> getWebResources() {
        return webResources;
    }

    @Override
    public String getBody() {
        checkParse();
        return body;
    }

    @Override
    public String getSource() {
        return "fileRes-" + getFileResource().getHash();
    }

    @Override
    public Long getContentLength() {
        return null;
    }

    @Override
    public boolean isDir() {
        return false;
    }

    @Override
    public CommonCollectionResource getParent() {
        return fileResource.getParent();
    }

    @Override
    public Organisation getOrganisation() {
        return fileResource.getOrganisation();
    }

    @Override
    public String getName() {
        if (nameOverride != null) {
            return nameOverride;
        }
        String name = fileResource.getName();
        return name;
    }

    @Override
    public Date getModifiedDate() {
        return fileResource.getModifiedDate();
    }

    @Override
    public Date getCreateDate() {
        return fileResource.getCreateDate();
    }

    @Override
    public Map<Principal, List<Priviledge>> getAccessControlList() {
        return fileResource.getAccessControlList();
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return null;
    }

    @Override
    public void moveTo(CollectionResource rDest, String name) throws ConflictException, NotAuthorizedException, BadRequestException {
        fileResource.moveTo(rDest, name);
    }

    @Override
    public void copyTo(CollectionResource toCollection, String name) throws NotAuthorizedException, BadRequestException, ConflictException {
        fileResource.copyTo(toCollection, name);
    }

    @Override
    public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
        fileResource.delete();
    }

    @Override
    public void setBody(String b) {
        checkParse();
        this.body = b;
    }

    @Override
    public String getTitle() {
        checkParse();
        if (title == null) {
            return "";
        }
        return title;
    }

    @Override
    public void setTitle(String t) {
        checkParse();
        this.title = t;
    }

    @Override
    public boolean is(String type) {
        if (isTemplate(type)) {
            return true;
        }
        return fileResource.is(type, getName());
    }

    public boolean isTemplate(String type) {
        checkParse();
        // eg, if template is learner/modulePage, then is("modulePage") returns true
        if (this.template != null && template.endsWith(type)) {
            return true;
        }
        return false;
    }

    @Override
    public String getParam(String name) {
        checkParse();
        WebResource wr = param(name);
        if (wr == null) {
            return null;
        } else {
            return wr.getBody();
        }
    }

    @Override
    public void setParam(String name, String value) {
        checkParse();
        WebResource wr = param(name);
        if (wr == null) {
            wr = new WebResource();
            wr.setTag("script");
            wr.getAtts().put("type", "data/parameter");
            wr.getAtts().put("title", name);
            webResources.add(wr);
        }
        wr.setBody(value);
    }

    private WebResource param(String name) {
        checkParse();
        WebResource wr = WebResource.param(webResources, name);
        return wr;
    }

    @Override
    public List<String> getParamNames() {
        checkParse();
        List<String> names = new ArrayList<>();
        for (WebResource wr : this.webResources) {
            if (wr.getTag().equals("script")) {
                String type = wr.getAtts().get("type");
                if ("data/parameter".equals(type)) {
                    String paramTitle = wr.getAtts().get("title");
                    names.add(paramTitle);
                }
            }
        }
        return names;
    }

    public void removeParam(String paramToRemove) {
        Iterator<WebResource> it = this.webResources.iterator();
        while (it.hasNext()) {
            WebResource wr = it.next();
            if (wr.getTag().equals("script")) {
                String type = wr.getAtts().get("type");
                if ("data/parameter".equals(type)) {
                    String paramTitle = wr.getAtts().get("title");
                    if (paramTitle.equals(paramToRemove)) {
                        it.remove();
                    }
                }
            }
        }
    }

    public List<CommentBean> getComments() {
        return fileResource.getComments();
    }

    public int getNumComments() {
        return fileResource.getNumComments();
    }

    public void setNewComment(String s) throws NotAuthorizedException {
        fileResource.setNewComment(s);
    }

    /**
     * This is just here to make newComment a bean property
     *
     * @return
     */
    public String getNewComment() {
        return fileResource.getNewComment();
    }

    @Override
    public void doCommit(Map<QName, ValueAndType> knownProps, Map<Status, List<NameAndError>> errorProps) throws BadRequestException, NotAuthorizedException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        try {
            doSaveHtml();
            tx.commit();
        } catch (IOException ex) {
            tx.rollback();
            throw new BadRequestException("io ex", ex);
        }
    }

    /**
     * Call this after setting parameters or properties like title and body.
     * This will generate new HTML and write it to the blob store.
     *
     * It will then call save to the repository is updated
     *
     * @throws BadRequestException
     * @throws NotAuthorizedException
     * @throws IOException
     */
    public void doSaveHtml() throws BadRequestException, NotAuthorizedException, IOException {
        log.info("doSaveHtml: " + getName());
        fileResource.doSaveHtml();
        fileResource.getParent().save();
    }

    @Override
    public boolean isPublic() {
        return getParent().isPublic();
    }

    public io.milton.cloud.server.web.FileResource getFileResource() {
        return fileResource;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
        for (WebResource wr : webResources) {
            if (wr.getTag().equals("link")) {
                String rel = wr.getAtts().get("rel");
                if (rel != null && rel.equals("template")) {
                    wr.getAtts().put("href", template);
                    return;
                }
            }
        }
        WebResource wrTemplate = new WebResource();
        wrTemplate.setTag("link");
        wrTemplate.getAtts().put("rel", "template");
        wrTemplate.getAtts().put("href", template);
        webResources.add(wrTemplate);
    }

    public boolean isNewPage() {
        return isNewPage;
    }

    public void setNewPage(boolean b) {
        isNewPage = b;
    }

    public boolean isParsed() {
        return parsed;
    }

    public void setParsed(boolean parsed) {
        this.parsed = parsed;
    }

    @Override
    public void replaceContent(InputStream in, Long length) throws BadRequestException, ConflictException, NotAuthorizedException {
        this.fileResource.replaceContent(in, length);
    }

    /**
     * For public repositories we allow all READ operations
     *
     * TODO: should limit this to not include PROPFIND TODO: a POST is often
     * available to anonymous users but will be rejected
     *
     * @param request
     * @param method
     * @param auth
     * @return
     */
    @Override
    public boolean authorise(Request request, Request.Method method, Auth auth) {
//        if (!method.isWrite) {
//            if (isPublic()) {
//                return true;
//            }
//        }
        return super.authorise(request, method, auth);
    }

    @Override
    public String getHash() {
        return fileResource.getHash();
    }

    @Override
    public Profile getModifiedBy() {
        return fileResource.getModifiedBy();
    }

    @Override
    public void save() throws IOException {
        fileResource.save();
    }

    @Override
    public void setHash(String s) {
        fileResource.setHash(s);
    }

    @Override
    public String getLink() {
        return "<a href='" + getHref() + "'>" + getTitle() + "</a>";
    }

    @Override
    public Branch getBranch() {
        return fileResource.getBranch();
    }

    @Override
    public Repository getRepository() {
        return getBranch().getRepository();
    }

    @Override
    public Profile getOwnerProfile() {
        return fileResource.getOwnerProfile();
    }

    public void setNameOverride(String name) {
        this.nameOverride = name;
    }

    public String getNameOverride() {
        return nameOverride;
    }
}
