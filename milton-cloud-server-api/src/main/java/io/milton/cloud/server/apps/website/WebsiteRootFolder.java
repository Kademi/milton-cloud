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
package io.milton.cloud.server.apps.website;

import io.milton.vfs.db.Website;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Permission;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Profile;
import io.milton.http.*;
import io.milton.http.Request.Method;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.principal.Principal;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.web.*;
import io.milton.resource.GetableResource;
import io.milton.resource.PropFindableResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.*;
import io.milton.vfs.db.utils.SessionManager;

/**
 *
 * @author brad
 */
public class WebsiteRootFolder extends AbstractResource implements RootFolder, CommonCollectionResource, GetableResource, PropFindableResource {

    private Map<String, PrincipalResource> childEntities = new HashMap<>();
    private final ApplicationManager applicationManager;
    private final Website website;
    private ResourceList children;

    public WebsiteRootFolder(Services services, ApplicationManager applicationManager, Website website) {
        super(services);
        this.website = website;
        this.applicationManager = applicationManager;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public boolean authorise(Request request, Request.Method method, Auth auth) {
        if (method.equals(Method.PROPFIND)) { // force login for webdav browsing
            return getCurrentUser() != null;
        }
        return true;
    }

    @Override
    public Date getModifiedDate() {
        return null;
    }

    @Override
    public Resource child(String childName) throws NotAuthorizedException, BadRequestException {
        Resource r = applicationManager.getPage(this, childName);
        if (r != null) {
            return r;
        }
        r = Utils.childOf(getChildren(), childName);
        if (r != null) {
            return r;
        }
        // Note that the current user might go to their own home page before they've logged in.
        // This would mean that they would not be present in the list of children, because
        // only the logged in user is added. So must look for them explicitly
        //r = findEntity(childName);
        return r;
    }

    @Override
    public PrincipalResource findEntity(Profile u) {
        PrincipalResource r = childEntities.get(u.getName());
        if (r != null) {
            return r;
        }
        if (u == null) {
            return null;
        } else {
            UserResource ur = new UserResource(this, u, applicationManager);
            childEntities.put(u.getName(), ur);
            return ur;
        }
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            children = new ResourceList();
            if (getCurrentUser() != null) {
                PrincipalResource r = findEntity(getCurrentUser());
                children.add(r);
            }
            Branch currentLive = website.currentBranch();
            if (currentLive != null) {
                BranchFolder rf = new BranchFolder("content", this, currentLive, true);
                children.add(rf);
            }
            if (website.getRepository().getBranches() != null) {
                for (Branch b : website.getRepository().getBranches()) {
                    BranchFolder rf = new BranchFolder(b.getName(), this, b, false);
                    children.add(rf);
                }
            }

            applicationManager.addBrowseablePages(this, children);
        }
        return children;
    }

    @Override
    public String checkRedirect(Request request) {
        if (request.getMethod().equals(Method.GET)) {
            return "/content/index.html";
        }
        return null;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
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
    public CommonCollectionResource getParent() {
        return null;
    }

    @Override
    public BaseEntity getOwner() {
        return null;
    }

    @Override
    public void addPrivs(List<Priviledge> list, Profile user) {
        // TODO: also include priviledges on the repo, eg:
        //List<Permission> perms = itemVersion.getItem().grantedPermissions(user);
        //SecurityUtils.addPermissions(perms, list);
        Set<Permission> perms = SecurityUtils.getPermissions(user, website.getRepository().getBaseEntity(), SessionManager.session());
        SecurityUtils.addPermissions(perms, list);
    }

    @Override
    public Date getCreateDate() {
        return null;
    }

    @Override
    public boolean isDir() {
        return true;
    }

    @Override
    public Map<Principal, List<Priviledge>> getAccessControlList() {
        return Collections.EMPTY_MAP;
    }

    @Override
    public Organisation getOrganisation() {
        return (Organisation) website.getOrganisation();
    }

    public Website getWebsite() {
        return website;
    }

    public SettingsMap getSettings() {
        return new SettingsMap();
    }

    public class SettingsMap implements Map<String, String> {

        @Override
        public String get(Object key) {
            String s = website.getRepository().getAttribute(key.toString());
            return s;
        }

        @Override
        public int size() {
            if (website.getRepository().getNvPairs() != null) {
                return website.getRepository().getNvPairs().size();
            } else {
                return 0;
            }
        }

        @Override
        public boolean isEmpty() {
            return size() == 0;
        }

        @Override
        public boolean containsKey(Object key) {
            return get(key) != null;
        }

        @Override
        public boolean containsValue(Object value) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String put(String key, String value) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String remove(Object key) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void putAll(Map<? extends String, ? extends String> m) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Set<String> keySet() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Collection<String> values() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Set<Entry<String, String>> entrySet() {
            Set<Entry<String, String>> set = new HashSet<>();
            if (website.getRepository().getNvPairs() != null) {
                for (NvPair nv : website.getRepository().getNvPairs()) {
                    final NvPair pair = nv;
                    Entry<String, String> e = new Entry<String, String>() {

                        @Override
                        public String getKey() {
                            return pair.getName();
                        }

                        @Override
                        public String getValue() {
                            return pair.getPropValue();
                        }

                        @Override
                        public String setValue(String value) {
                            throw new UnsupportedOperationException("Not supported yet.");
                        }
                    };
                    set.add(e);
                }
            }
            return set;
        }
    }
}
