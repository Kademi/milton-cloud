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
package io.milton.cloud.server.apps.contacts;

import info.ineighborhood.cardme.engine.VCardEngine;
import info.ineighborhood.cardme.io.VCardWriter;
import info.ineighborhood.cardme.vcard.VCard;
import info.ineighborhood.cardme.vcard.VCardImpl;
import info.ineighborhood.cardme.vcard.features.EmailFeature;
import info.ineighborhood.cardme.vcard.features.ExtendedFeature;
import info.ineighborhood.cardme.vcard.features.NameFeature;
import info.ineighborhood.cardme.vcard.features.TelephoneFeature;
import info.ineighborhood.cardme.vcard.types.*;
import io.milton.vfs.db.AddressBook;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Contact;
import io.milton.vfs.db.ContactExtendedProperty;
import io.milton.vfs.db.utils.SessionManager;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class ContactManager {

    private static final Logger log = LoggerFactory.getLogger(ContactManager.class);

    public AddressBook createAddressBook(BaseEntity owner, String newName) {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();

        AddressBook c = new AddressBook();
        c.setCreatedDate(new Date());
        c.setModifiedDate(new Date());
        c.setName(newName);
        c.setDescription("Auto generated");
        c.setOwner(owner);

        session.save(c);

        tx.commit();

        return c;
    }

    public void delete(AddressBook event) {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();

        session.delete(event);

        tx.commit();
    }

    public void move(Contact contact, AddressBook dest, String name) {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();

        if (!name.equals(contact.getName())) {
            contact.setName(name);
        }

        AddressBook sourceCal = contact.getAddressBook();
        if (dest != sourceCal) {
            sourceCal.getContacts().remove(contact);
            contact.setAddressBook(dest);
            if (dest.getContacts() == null) {
                dest.setContacts(new ArrayList<Contact>());
            }
            dest.getContacts().add(contact);
            session.save(sourceCal);
            session.save(dest);
        }

        tx.commit();
    }

    public void copy(Contact contact, AddressBook dest, String name) {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();

        if (dest.getContacts() == null) {
            dest.setContacts(new ArrayList<Contact>());
        }
        Contact newContact = new Contact();
        newContact.setAddressBook(dest);
        dest.getContacts().add(newContact);

        newContact.setCreatedDate(new Date());
        newContact.setModifiedDate(new Date());
        newContact.setName(name);
        newContact.setGivenName(contact.getGivenName());
        newContact.setMail(contact.getMail());
        newContact.setOrganizationName(contact.getOrganizationName());
        newContact.setSurName(contact.getSurName());
        newContact.setTelephonenumber(contact.getTelephonenumber());
        session.save(newContact);

        tx.commit();
    }

    public void delete(Contact contact) {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();

        session.delete(contact);
        
        // update addressbook to force ctag change
        contact.getAddressBook().setModifiedDate(new Date());
        session.save(contact.getAddressBook());        
        tx.commit();
    }

    public Contact createContact(AddressBook addressBook, String newName, String icalData, String contentType) throws UnsupportedEncodingException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        Contact e = new Contact();
        e.setName(newName);
        e.setAddressBook(addressBook);
        e.setCreatedDate(new Date());
        _update(e, icalData);
        session.save(e);
        // update addressbook to force ctag change
        e.getAddressBook().setModifiedDate(new Date());
        session.save(e.getAddressBook());        
        tx.commit();
        return e;
    }

    public void update(Contact contact, String data) {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        _update(contact, data);
        session.save(contact);
        
        // update addressbook to force ctag change
        contact.getAddressBook().setModifiedDate(new Date());
        session.save(contact.getAddressBook());
        tx.commit();
    }

    public String getContactAsCarddav(Contact contact) {
        VCardImpl vc = new VCardImpl();
        vc.setBegin(new BeginType());
        vc.setUID(new UIDType(contact.getUid()));
        String formattedName = buildFormattedName(contact);
        vc.setFormattedName(new FormattedNameType(formattedName));
        NameFeature nf = new NameType(contact.getSurName(), contact.getGivenName());
        vc.setName(nf);
        if (contact.getMail() != null && contact.getMail().length() > 0) {
            vc.addEmail(new EmailType(contact.getMail()));
        }
        String o = contact.getOrganizationName();
        if (o != null && o.length() > 0) {
            OrganizationType orgType = new OrganizationType();
            orgType.addOrganization(o);
            vc.setOrganizations(orgType);
        }
        if (contact.getTelephonenumber() != null && contact.getTelephonenumber().length() > 0) {
            vc.addTelephoneNumber(new TelephoneType(contact.getTelephonenumber()));
        }
        if( contact.getExtendedProperties() != null ) {            
            for( ContactExtendedProperty ext : contact.getExtendedProperties() ) {
                vc.addExtendedType(new ExtendedType(ext.getName(), ext.getPropValue()));
            }
        }
        vc.setEnd(new EndType());
        VCardWriter writer = new VCardWriter();
        writer.setVCard(vc);
        return writer.buildVCardString();
    }

    private void _update(Contact contact, String data) {
        contact.setModifiedDate(new Date());
        VCardEngine cardEngine = new VCardEngine();
        VCard vcard;
        try {
            vcard = cardEngine.parse(data);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        if( vcard.getUID() != null && vcard.getUID().hasUID() ) {
            contact.setUid(vcard.getUID().getUID());
        } else {
            contact.setUid(UUID.randomUUID().toString());
        }
        if (vcard.getName() != null) {
            contact.setGivenName(vcard.getName().getGivenName());
            contact.setSurName(vcard.getName().getFamilyName());
        }
        contact.setMail(""); // reset in case none given
        Iterator<EmailFeature> it = vcard.getEmails();
        while (it.hasNext()) {
            contact.setMail(it.next().getEmail());
        }
        if (vcard.getOrganizations() != null) {
            contact.setOrganizationName("");
            Iterator<String> itOrg = vcard.getOrganizations().getOrganizations();
            while (itOrg.hasNext()) {
                contact.setOrganizationName(itOrg.next());
            }
        }
        Iterator<TelephoneFeature> itPhone = vcard.getTelephoneNumbers();
        while (itPhone.hasNext()) {
            contact.setTelephonenumber(itPhone.next().getTelephone());
        }
        Iterator<ExtendedFeature> itEx = vcard.getExtendedTypes();
        if( itEx != null ) {
            while( itEx.hasNext() ) {
                ExtendedFeature ext = itEx.next();
                contact.setExtendedProperty(ext.getExtensionName(), ext.getExtensionData());
            }
        }
    }

    private String buildFormattedName(Contact contact) {
        String s = "";
        if( contact.getGivenName() != null && contact.getGivenName().length() > 0 ) {
            s += contact.getGivenName();
        }
        if( contact.getSurName() != null && contact.getSurName().length() > 0 ) {
            s += " ";
            s += contact.getSurName();
        }
        return s;
    }
}
