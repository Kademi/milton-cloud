/*
 * Copyright (C) 2012 McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.vfs.db;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.*;

/**
 * TODO: store photo from PHOTO field
 *
 * @author brad
 */
@Entity
public class Contact implements Serializable {

    private Long id;
    private String uid;
    private AddressBook addressBook;
    private String name;
    private String givenName;
    private String surName;
    private String mail;
    private String organizationName;
    private String telephonenumber;
    private String skypeId;
    private Date createdDate;   
    private Date modifiedDate;    
    private List<ContactExtendedProperty> extendedProperties;

    /**
     * @return the id
     */
    @Id
    @GeneratedValue
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    @ManyToOne(optional=false)
    public AddressBook getAddressBook() {
        return addressBook;
    }

    public void setAddressBook(AddressBook addressBook) {
        this.addressBook = addressBook;
    }

    
    /**
     * @return the name
     */
    @Column(nullable=false)
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the givenName
     */
    public String getGivenName() {
        return givenName;
    }

    /**
     * @param givenName the givenName to set
     */
    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    /**
     * @return the surName
     */
    public String getSurName() {
        return surName;
    }

    /**
     * @param surName the surName to set
     */
    public void setSurName(String surName) {
        this.surName = surName;
    }

    /**
     * @return the mail
     */
    public String getMail() {
        return mail;
    }

    /**
     * @param mail the mail to set
     */
    public void setMail(String mail) {
        this.mail = mail;
    }

    /**
     * @return the organizationName
     */
    public String getOrganizationName() {
        return organizationName;
    }

    /**
     * @param organizationName the organizationName to set
     */
    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    /**
     * @return the telephonenumber
     */
    public String getTelephonenumber() {
        return telephonenumber;
    }

    /**
     * @param telephonenumber the telephonenumber to set
     */
    public void setTelephonenumber(String telephonenumber) {
        this.telephonenumber = telephonenumber;
    }
    
    @Column(nullable=false)
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    @Column(nullable=false)
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    @Column(nullable=false)
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setExtendedProperty(String extensionName, String extensionData) {
        if( extendedProperties == null ) {
            extendedProperties = new ArrayList<>();
        }
        for( ContactExtendedProperty ext : extendedProperties ) {
            if( ext.getName().equals(extensionName)) {
                ext.setPropValue(extensionData);
                return ;
            }
        }
        ContactExtendedProperty ext = new ContactExtendedProperty();
        ext.setContact(this);
        ext.setName(name);
        ext.setPropValue(extensionData);
        extendedProperties.add(ext);
    }

    @OneToMany
    public List<ContactExtendedProperty> getExtendedProperties() {
        return extendedProperties;
    }

    public void setExtendedProperties(List<ContactExtendedProperty> extendedProperties) {
        this.extendedProperties = extendedProperties;
    }
    
    
}
