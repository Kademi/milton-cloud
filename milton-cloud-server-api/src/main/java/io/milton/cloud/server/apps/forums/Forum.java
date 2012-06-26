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

import io.milton.vfs.db.Website;
import io.milton.vfs.db.utils.DbUtils;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;

/**
 *
 * @author brad
 */
@Entity
public class Forum implements Serializable{
    
    public static List<Forum>  findByOrg(Website website, Session session) {
        Criteria crit = session.createCriteria(Forum.class);
        crit.add(Expression.eq("website", website));
        return DbUtils.toList(crit, Forum.class);
    }    
    
    public static Forum addToWebsite(Website website, String name, String title, Session session ) {
        Forum f = new Forum();
        f.setName(name);
        f.setTitle(title);
        f.setWebsite(website);
        session.save(f);
        return f;
    }
    
    private List<ForumTopic> forumTopics;
    private long id;
    private Website website;
    private String name;
    private String title;
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
     * User entered title of the reward
     * 
     * @return 
     */
    @Column(nullable=false)
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
        

    @ManyToOne(optional=false)
    public Website getWebsite() {
        return website;
    }

    public void setWebsite(Website w) {
        this.website = w;
    }
    
    /**
     * The auto-assigned identifier for the reward. It should be derived from the
     * title, but is immutable from a user's perspective so that it can
     * map to a fixed directory in the rewards repository
     * 
     * @return 
     */
    @Column(nullable=false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column
    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @OneToMany(mappedBy = "forum")
    public List<ForumTopic> getForumTopics() {
        return forumTopics;
    }

    public void setForumTopics(List<ForumTopic> forumTopics) {
        this.forumTopics = forumTopics;
    }
    
    public ForumTopic addTopic(String name, String title, Session session) {
        ForumTopic ft = new ForumTopic();
        ft.setForum(this);
        ft.setName(name);
        ft.setTitle(title);
        if( this.getForumTopics() == null ) {
            setForumTopics(new ArrayList<ForumTopic>());
        }
        getForumTopics().add(ft);
        session.save(ft);
        return ft;
    }
}
