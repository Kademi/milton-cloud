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
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * An AddressBook is a Repository which can have linked contact records.
 *
 * The contact records provide a search optimised view of the CARDDAV data held
 * in the repository
 *
 * @author brad
 */
@Entity
@DiscriminatorValue("AB")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class AddressBook extends Repository {

    private List<Contact> contacts;

    @OneToMany(mappedBy = "addressBook")
    public List<Contact> getContacts() {
        return contacts;
    }

    public void setContacts(List<Contact> contacts) {
        this.contacts = contacts;
    }

    @Override
    public void delete(Session session) {
        if (getContacts() != null) {
            for (Contact c : getContacts()) {
                session.delete(c);
            }
            setContacts(null);
        }
        super.delete(session);
    }

    public Contact contact(String name) {
        if (getContacts() != null) {
            for (Contact c : getContacts()) {
                if (c.getName().equals(name)) {
                    return c;
                }
            }
        }
        return null;
    }

    @Override
    public String type() {
        return "AB";
    }

    public Contact add(String name) {
        Contact c = new Contact();
        c.setAddressBook(this);
        c.setName(name);
        return c;
    }
    
    
}
