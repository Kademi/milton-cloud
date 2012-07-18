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
package io.milton.vfs.db;

import io.milton.cloud.process.ProcessInstance;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Base class for entities which represent ProcessInstance's
 *
 * @author brad
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "TYPE", discriminatorType = DiscriminatorType.STRING, length = 20)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public abstract class BaseProcess implements Serializable, ProcessInstance {
    private Long id;
    private String type;
    private Date timeEntered;
    private String processName;
    private int processVersion;
    private String stateName;
    
    @Id
    @GeneratedValue
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }    
    
    @Override
    public void setTimeEntered(Date dateTime) {
        this.timeEntered = dateTime;
    }

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    @Column
    @Override
    public Date getTimeEntered() {
        return timeEntered;
    }
    
    
    @Override
    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public int getProcessVersion() {
        return processVersion;
    }

    public void setProcessVersion(int processVersion) {
        this.processVersion = processVersion;
    }
    
    

    @Override
    public String getStateName() {
        return stateName;
    }

    @Override
    public void setStateName(String name) {
        this.stateName = name;
    }
    
    
}
