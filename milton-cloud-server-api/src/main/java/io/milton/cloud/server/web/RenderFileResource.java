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
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Profile;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import io.milton.cloud.server.web.templating.HtmlPage;
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

/**
 *
 * @author brad
 */
@BeanPropertyResource(value="milton")
public class RenderFileResource extends AbstractResource implements GetableResource, MoveableResource, CopyableResource, DeletableResource, HtmlPage, PostableResource, ParameterisedResource {

    private static final Logger log = LoggerFactory.getLogger(RenderFileResource.class);
    private final FileResource fileResource;
    private final List<String> bodyClasses = new ArrayList<>();
    private final List<WebResource> webResources = new ArrayList<>();
    private boolean parsed;
    private String template;
    private String title;
    private String body;
    private JsonResult jsonResult;

    public RenderFileResource(FileResource fileResource) {
        super(fileResource.getServices());
        this.fileResource = fileResource;
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
        }
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        services.getTemplateParser().update(this, bout);
        byte[] arr = bout.toByteArray();
        ByteArrayInputStream bin = new ByteArrayInputStream(arr);
        fileResource.replaceContent(bin, (long) arr.length);
        jsonResult = new JsonResult(true);
        return null;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        if (jsonResult != null) {
            jsonResult.write(out);
        } else {
            checkParse();
            services.getHtmlTemplater().writePage(template, this, params, out);
        }
    }

    private void checkParse() {
        if (parsed) {
            return;
        }
        try {
            services.getTemplateParser().parse(this, Path.root);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        template = "content/page";
        for (WebResource wr : webResources) {
            if (wr.getTag().equals("link")) {
                String rel = wr.getAtts().get("rel");
                if (rel != null && rel.equals("template")) {
                    template = wr.getAtts().get("href");
                }
            }
        }
        parsed = true;
    }

    @Override
    public InputStream getInputStream() {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try {
            fileResource.sendContent(bout, null, null, title);
        } catch (IOException | NotAuthorizedException | BadRequestException | NotFoundException ex) {
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
    public BaseEntity getOwner() {
        return fileResource.getOwner();
    }

    @Override
    public Organisation getOrganisation() {
        return fileResource.getOrganisation();
    }

    @Override
    public void addPrivs(List<Priviledge> list, Profile user) {
        fileResource.addPrivs(list, user);
    }

    @Override
    public String getName() {
        return fileResource.getName();
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
    public String getContentType(String accepts) {
        return fileResource.getContentType(accepts);
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
        this.body = b;
    }

    @Override
    public String getTitle() {
        checkParse();
        return title;
    }

    @Override
    public void setTitle(String t) {
        this.title = t;
    }

    @Override
    public boolean is(String type) {
        checkParse();
        // eg, if template is learner/modulePage, then is("modulePage") returns true
        if (this.template != null && template.endsWith(type)) {
            return true;
        }
        return fileResource.is(type);
    }
    
    @Override
    public String getParam(String name) {
        checkParse();
        WebResource wr = param(name);
        if( wr == null ) {
            return null;
        } else {
            return wr.getBody();
        }
    }
    
    @Override
    public void setParam(String name, String value) {
        System.out.println("setParam: " + getName() + " - " + name + " -> " + value);
        checkParse();
        WebResource wr = param(name);
        if(wr == null ) {
            wr = new WebResource(Path.root);
            wr.setTag("script");
            wr.getAtts().put("type", "data/parameter");
            wr.getAtts().put("title", name);
            webResources.add(wr);
        }
        wr.setBody(value);
    }
    
    private WebResource param(String name) {
        for( WebResource wr : this.webResources ) {
            if( wr.getTag().equals("script")) {
                String type = wr.getAtts().get("type");
                if( "data/parameter".equals(type) ) {
                    String title = wr.getAtts().get("title");
                    if( name.equals(title)) {
                        return wr;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public List<String> getParamNames() {
        List<String> names = new ArrayList<>();
        for( WebResource wr : this.webResources ) {
            if( wr.getTag().equals("script")) {
                String type = wr.getAtts().get("type");
                if( "data/parameter".equals(type) ) {
                    String paramTitle = wr.getAtts().get("title");
                    names.add(paramTitle);
                }
            }
        }
        return names;
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

        doSave();

        tx.commit();
    }
    
    public void doSave() throws BadRequestException, NotAuthorizedException {
        fileResource.doSave();
    }    
}
