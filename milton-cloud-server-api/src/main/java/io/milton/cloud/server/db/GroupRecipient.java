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

import io.milton.vfs.db.Group;
import java.io.Serializable;
import javax.persistence.*;

/**
 *
 * @author brad
 */
@Entity
public class GroupRecipient implements Serializable{
    private long id;
    private Group recipient;
    private GroupEmailJob job;

    @Id
    @GeneratedValue    
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @ManyToOne(optional=false)
    public GroupEmailJob getJob() {
        return job;
    }

    public void setJob(GroupEmailJob job) {
        this.job = job;
    }

    @ManyToOne(optional=false)
    public Group getRecipient() {
        return recipient;
    }

    public void setRecipient(Group recipient) {
        this.recipient = recipient;
    }

    
    
}
