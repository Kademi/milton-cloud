/*
 * Copyright 2013 McEvoy Software Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Temporal;
import org.hibernate.Session;

/**
 * Represents an authentication access. The nonce is used for authenticating 
 * Digest requests and signed cookies
 *
 * @author brad
 */
@javax.persistence.Entity
public class LoginNonce {
    
    public static LoginNonce get(String nonce, Session session) {
        return (LoginNonce) session.get(LoginNonce.class, nonce);
    }
    
    private String nonce;
    private Date createdAt;

    /**
     * Natural ID, usually will be a UUID
     * 
     * @return 
     */
    @Id
    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }
    
    @Column(nullable=false)
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)       
    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    
    
}
