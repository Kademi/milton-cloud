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
package io.milton.cloud.server.db;

import io.milton.vfs.db.utils.DbUtils;
import java.util.List;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

/**
 * This is a comment on a content item. It is keyed on the meta UUID of the content
 * item
 *
 * @author brad
 */
@Entity
@DiscriminatorValue("C")
public class Comment extends Post {
    
    public static List<Comment> findByContentId(UUID contentId, Session session) {
        Criteria crit = session.createCriteria(Comment.class);
        crit.setCacheable(true);
        crit.add(Restrictions.eq("contentId", contentId));
        crit.addOrder(Order.asc("postDate")); // hmm, might need a join or something
        return DbUtils.toList(crit, Comment.class);        
    }
    
    private UUID contentId;
    private String contentHref;
    private String contentTitle;

    @Column(nullable=false)
    public UUID getContentId() {
        return contentId;
    }

    public void setContentId(UUID contentId) {
        this.contentId = contentId;
    }
    
    @Column(nullable=false)
    public String getContentHref() {
        return contentHref;
    }

    public void setContentHref(String contentHref) {
        this.contentHref = contentHref;
    }

    @Column(nullable=false)
    public String getContentTitle() {
        return contentTitle;
    }

    public void setContentTitle(String contentTitle) {
        this.contentTitle = contentTitle;
    }

    @Override
    public void accept(PostVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void delete(Session session) {
        session.delete(this);
    }
}
