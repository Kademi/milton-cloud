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
import io.milton.vfs.db.utils.DbUtils;
import java.io.Serializable;
import java.util.ArrayList;
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
@DiscriminatorValue("FP")
public class ForumPost extends Post implements Serializable{

    public static ForumPost findByName(String childName, Forum forum, Session session) {
        Criteria c = session.createCriteria(ForumPost.class);
        c.add(Expression.eq("name", childName));
        c.add(Expression.eq("forum", forum));
        return DbUtils.unique(c);
    }
    
    /**
     * Find up to 'limit' results, when ordered by date, for the given forum
     * 
     * @param forum
     * @param limit
     * @param session
     * @return 
     */
    public static List<ForumPost> findRecentByForum(Forum forum, Integer limit, Session session) {
        Criteria crit = session.createCriteria(Post.class);
        crit.add(Expression.eq("forum", forum));
        crit.addOrder(Order.desc("postDate"));
        if( limit != null ) {
            crit.setMaxResults(limit);
        }
        List<ForumPost> list = DbUtils.toList(crit, ForumPost.class);
        return list;
    }       
    
    private List<ForumReply> forumReplys;
    private Forum forum;
    private String name;
    private String title;


    @ManyToOne(optional=false)
    public Forum getForum() {
        return forum;
    }

    public void setForum(Forum forum) {
        this.forum = forum;
    }

    @Column(nullable=false)
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    @Column(nullable=false)
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @OneToMany(mappedBy = "post")
    public List<ForumReply> getForumReplys() {
        return forumReplys;
    }

    public void setForumReplys(List<ForumReply> forumReplys) {
        this.forumReplys = forumReplys;
    }
    
    @Override
    public void accept(PostVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void delete(Session session) {
        if( getForumReplys() != null ) {
            for( ForumReply r : getForumReplys()) {
                r.delete(session);
            }
        }
        session.delete(this);
    }

    public ForumReply addComment(String newComment, Profile currentUser, Date now, Session session) {
        ForumReply r = new ForumReply();
        r.setNotes(newComment);
        r.setPost(this);
        r.setPostDate(now);
        r.setPoster(currentUser);
        r.setWebsite(this.getWebsite());
        if( this.getForumReplys() != null ) {
            this.setForumReplys(new ArrayList<ForumReply>());
        }
        this.getForumReplys().add(r);
        session.save(r);
        return r;
    }
    
    @Transient
    public long getNumReplies() {
        if( getForumReplys() == null ) {
            return 0;
        } else {
            return getForumReplys().size();
        }
    }
}
