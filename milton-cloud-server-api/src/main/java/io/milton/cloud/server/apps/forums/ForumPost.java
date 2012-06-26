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

import io.milton.vfs.db.Profile;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.persistence.*;

/**
 *
 * @author brad
 */
@Entity
@DiscriminatorValue("FP")
public class ForumPost extends Post implements Serializable{
    private List<ForumReply> forumReplys;
    private ForumTopic topic;
    private Profile poster;
    private Date postDate;
    private String name;
    private String title;
    private String notes;


    @ManyToOne(optional=false)
    public ForumTopic getTopic() {
        return topic;
    }

    public void setTopic(ForumTopic topic) {
        this.topic = topic;
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
    
    
}
