/*
 * Copyright 2012 McEvoy Software Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.apps.forums;

import io.milton.cloud.server.apps.signup.SignupPage;
import io.milton.cloud.server.web.AbstractResource;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.JsonWriter;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.Request.Method;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.principal.Principal;
import io.milton.resource.GetableResource;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Website;
import io.milton.vfs.db.utils.SessionManager;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class PostSearchResource extends AbstractResource implements GetableResource {

    private static final Logger log = LoggerFactory.getLogger(SignupPage.class);
    
    private final String name;
    private final CommonCollectionResource parent;
    private final Website website;
    

    public PostSearchResource(String name, Website website, CommonCollectionResource parent) {
        super(parent.getServices());
        this.website = website;
        this.parent = parent;
        this.name = name;
        System.out.println("hello from PostSearchResource");
    }
        
    
    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {               
        System.out.println("PostSearchResource: sendContent");
        JsonWriter jsonWriter = new JsonWriter();
        List<PostBean> beans = new ArrayList<>();
        System.out.println("search posts: " + website.getName());
        for( Post p : Post.findByWebsite(website, SessionManager.session())) {
            PostBean b = PostBean.toBean(p);
            beans.add(b);
        }
        jsonWriter.write(beans, out);
    }

    @Override
    public boolean authorise(Request request, Method method, Auth auth) {
        return super.authorise(request, method, auth);
    }
    
    
        
    @Override
    public boolean isDir() {
        return false;
    }

    @Override
    public CommonCollectionResource getParent() {
        return parent;
    }

    @Override
    public BaseEntity getOwner() {
        return null;
    }

    @Override
    public void addPrivs(List<Priviledge> list, Profile user) {
        parent.addPrivs(list, user);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Date getModifiedDate() {
        return null;
    }

    @Override
    public Date getCreateDate() {
        return null;
    }

    @Override
    public Map<Principal, List<Priviledge>> getAccessControlList() {
        return null;
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
    public Organisation getOrganisation() {
        return website.getOrganisation();
    }
               
}
