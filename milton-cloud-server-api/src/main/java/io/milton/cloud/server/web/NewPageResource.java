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

import io.milton.cloud.server.db.NamedCounter;
import io.milton.common.FileUtils;
import io.milton.http.Auth;
import io.milton.http.FileItem;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.Request.Method;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.http.http11.auth.DigestResponse;
import io.milton.resource.*;
import io.milton.vfs.data.DataSession;
import io.milton.vfs.db.utils.SessionManager;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

/**
 * This is instantiated when a html page is requested that doesnt exist. It will
 * create an actual page on POST, but otherwise behaves the same as if it was an
 * existing empty page
 *
 * @author brad
 */
public class NewPageResource implements GetableResource, PostableResource, DigestResource {

    public static String getDateAsName() {
        return getDateAsName(false);
    }

    public static String getDateAsName(boolean seconds) {
        Date dt = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(dt);
        String name = cal.get(Calendar.YEAR) + "_" + cal.get(Calendar.MONTH) + "_" + cal.get(Calendar.DAY_OF_MONTH) + "_" + cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE);
        if (seconds) {
            name += "_" + cal.get(Calendar.SECOND);
        }
        return name;
    }

    public static String getDateAsNameUnique(CollectionResource col) {
        String name = getDateAsName();
        return getUniqueName(col, name);
    }

    public static String getUniqueName(CollectionResource col, final String baseName) {
        try {
            String name = baseName;
            Resource r = col.child(name);
            int cnt = 0;
            boolean isFirst = true;
            while (r != null) {
                cnt++;
                name = FileUtils.incrementFileName(name, isFirst);
                isFirst = false;
                r = col.child(name);
            }
            return name;
        } catch (NotAuthorizedException | BadRequestException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Just like findAutoName, but does not add .html
     *
     * @param baseName
     * @param folder
     * @param parameters
     * @return
     */
    public static String findAutoCollectionName(String baseName, CollectionResource folder, Map<String, String> parameters) {
        return findAutoName(baseName, folder, parameters, false);
    }

    public static String findAutoName(String baseName, CollectionResource folder, Map<String, String> parameters) {
        return findAutoName(baseName, folder, parameters, true);
    }

    public static String findAutoName(String baseName, CollectionResource folder, Map<String, String> parameters, boolean isHtml) {
        String nameToUse = getImpliedName(baseName, parameters, folder, isHtml);
        if (nameToUse != null) {
            nameToUse = nameToUse.toLowerCase().replace("/", "");
            nameToUse = nameToUse.replace("'", "");
            nameToUse = nameToUse.replace("\"", "");
            nameToUse = nameToUse.replace("@", "-");
            nameToUse = nameToUse.replace(" ", "-");
            nameToUse = nameToUse.replace("?", "-");
            nameToUse = nameToUse.replace(":", "-");
            nameToUse = nameToUse.replace("--", "-");
            nameToUse = nameToUse.replace("--", "-");
            nameToUse = NewPageResource.getUniqueName(folder, nameToUse);
        } else {
            nameToUse = NewPageResource.getDateAsNameUnique(folder);
        }
        return nameToUse;
    }

    /**
     *
     * @param baseName
     * @param parameters
     * @param folder
     * @return
     */
    public static String getImpliedName(String baseName, Map<String, String> parameters, CollectionResource folder, boolean isHtml) {
        String nameToCreate = baseName;
        if (nameToCreate.equals("_autoname.html")) {
            if (parameters.containsKey("name")) {
                nameToCreate = parameters.get("name");
            } else if (parameters.containsKey("nickName")) {
                nameToCreate = parameters.get("nickName");
            } else if (parameters.containsKey("fullName")) {
                nameToCreate = parameters.get("fullName");
            } else if (parameters.containsKey("firstName")) {
                String fullName = parameters.get("firstName");
                if (parameters.containsKey("surName")) {
                    fullName = fullName + "." + parameters.get("surName");
                }
                nameToCreate = fullName;
            } else if (parameters.containsKey("title")) {
                nameToCreate = parameters.get("title");
            } else {
                nameToCreate = "$[counter]";
            }
        }

        if (nameToCreate.contains("$[counter]")) {
            String folderId = folder.getUniqueId();
            if (folderId == null) {
                throw new RuntimeException("Cant calc counter for folder which has null uniqueId: " + folder.getClass());
            }
            Long l = NamedCounter.increment(folderId, SessionManager.session());
            nameToCreate = nameToCreate.replace("$[counter]", l.toString());
        }
        if (isHtml) {
            if (!nameToCreate.endsWith(".html")) {
                nameToCreate += ".html";
            }
        }
        return nameToCreate;
    }
    private final ContentDirectoryResource parent;
    private final String name;
    private RenderFileResource created;
    private String createdName;

    public NewPageResource(ContentDirectoryResource parent, String name) {
        this.parent = parent;
        this.name = name;
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        return getCreated(true, parameters).processForm(parameters, files);
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        getCreated(false, params).sendContent(out, range, params, contentType);
    }

    private RenderFileResource getCreated(boolean autocreate, Map<String, String> params) {
        if (created == null) {
            if (autocreate) {
                createdName = findAutoName(params);
                DataSession.FileNode newNode = parent.getDirectoryNode().addFile(createdName);
                FileResource newFr = new FileResource(newNode, parent);
                created = newFr.getHtml();
            } else {
                FileResource tempFr = new FileResource(null, parent);
                created = tempFr.getHtml();
            }
            created.setParsed(true);
            created.setNewPage(true);
            String t = params.get("template");
            if (t != null) {
                created.setTemplate(t);
            } else {
                created.setTemplate("content/page");
            }
        }
        return created;
    }

    public String findAutoName(Map<String, String> parameters) {
        String baseName = getName().replace(".new", "");
        return findAutoName(baseName, parent, parameters);
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return null;
    }

    @Override
    public String getContentType(String accepts) {
        return "text/html";
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
        return parent.authenticate(user, password);
    }

    @Override
    public Object authenticate(DigestResponse digestRequest) {
        return parent.authenticate(digestRequest);
    }

    @Override
    public boolean authorise(Request request, Method method, Auth auth) {
        if (auth == null || auth.getTag() == null) {
            return false;
        }
        return parent.authorise(request, method, auth);
    }

    @Override
    public String getRealm() {
        return parent.getRealm();
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
    public boolean isDigestAllowed() {
        return parent.isDigestAllowed();
    }
}
