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

import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Website;
import io.milton.vfs.db.utils.DbUtils;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Order;

/**
 *
 * @author brad
 */
@Entity
@DiscriminatorColumn(name = "TYPE", discriminatorType = DiscriminatorType.STRING, length = 2)
@DiscriminatorValue("P")
@Inheritance(strategy = InheritanceType.JOINED)
public class Post implements Serializable{
    
    public static List<Post> findByWebsite(Website website, Session session) {
        Criteria crit = session.createCriteria(Post.class);
        crit.add(Expression.eq("website", website));
        crit.addOrder(Order.desc("postDate"));
        List<Post> list = DbUtils.toList(crit, Post.class);
        return list;
    }    
    
    private long id;
    private Website website;
    private Profile poster;
    private Date postDate;
    private String notes;

    @Id
    @GeneratedValue    
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    /**
     * Provides fast, de-normalised access to lookup all posts for a given website
     * 
     * @return 
     */
    @ManyToOne(optional=false)
    public Website getWebsite() {
        return website;
    }

    public void setWebsite(Website website) {
        this.website = website;
    }
        


    @ManyToOne(optional=false)
    public Profile getPoster() {
        return poster;
    }

    public void setPoster(Profile poster) {
        this.poster = poster;
    }


    @Column(nullable=false)
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)    
    public Date getPostDate() {
        return postDate;
    }

    public void setPostDate(Date postDate) {
        this.postDate = postDate;
    }

    @Column(nullable=true)
    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void accept(PostVisitor visitor) {
        // do nothing, will be overridden
    }
    
}
