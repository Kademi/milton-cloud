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

import io.milton.cloud.server.db.ForumReply;
import io.milton.cloud.server.db.ForumPost;
import io.milton.cloud.server.db.PostVisitor;
import io.milton.cloud.server.db.Post;
import io.milton.cloud.server.db.Comment;
import io.milton.cloud.server.manager.CurrentRootFolderService;
import io.milton.cloud.server.web.ProfileBean;

import static io.milton.context.RequestContext._;

/**
 *
 * @author brad
 */
public class PostBean {
    
    public static PostBean toBean(Post p) {
        final PostBean b = new PostBean();
        b.setNotes(p.getNotes());
        b.setDate(p.getPostDate().getTime());
        b.setUser(ProfileBean.toBean(p.getPoster()));
        String web = p.getWebsite().getDomainName();
        if( web == null ) {
            web = p.getWebsite().getName() + "." + _(CurrentRootFolderService.class).getPrimaryDomain();
        }
        b.setContentDomain(web);
        PostVisitor visitor = new PostVisitor() {

            @Override
            public void visit(Comment c) {
                b.setContentHref(c.getContentHref());
                b.setContentTitle(c.getContentTitle());
            }

            @Override
            public void visit(ForumPost p) {
                b.setContentTitle(p.getForum().getTitle());
                b.setContentHref(ForumsApp.toHref(p));
            }

            @Override
            public void visit(ForumReply r) {
                b.setContentTitle(r.getPost().getTitle());
                b.setContentHref(ForumsApp.toHref(r.getPost()));  
            }
        };
        p.accept(visitor);
        
        return b;
    }
    
    private ProfileBean user;
    private String notes;
    private String contentTitle;
    private String contentDomain;
    private String contentHref;
    private long date;

    public String getContentDomain() {
        return contentDomain;
    }

    public void setContentDomain(String contentDomain) {
        this.contentDomain = contentDomain;
    }

    
    
    public String getContentHref() {
        return contentHref;
    }

    public void setContentHref(String contentHref) {
        this.contentHref = contentHref;
    }

    public String getContentTitle() {
        return contentTitle;
    }

    public void setContentTitle(String contentTitle) {
        this.contentTitle = contentTitle;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public ProfileBean getUser() {
        return user;
    }

    public void setUser(ProfileBean user) {
        this.user = user;
    }

      
}
