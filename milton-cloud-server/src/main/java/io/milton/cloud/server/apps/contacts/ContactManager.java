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
import info.ineighborhood.cardme.vcard.features.EmailFeature;
import info.ineighborhood.cardme.vcard.features.TelephoneFeature;
import info.ineighborhood.cardme.vcard.types.EmailType;
import info.ineighborhood.cardme.vcard.types.TelephoneType;
import io.milton.vfs.db.AddressBook;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Contact;
import io.milton.vfs.db.utils.SessionManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.apache.commons.io.IOUtils;
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
        c.setName(newName);
        c.setTitle(newName);
        c.setBaseEntity(owner);
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

        session.save(contact.getAddressBook());
        tx.commit();
    }

    public Contact createContact(AddressBook addressBook, String newName, String icalData, Session session) throws UnsupportedEncodingException {
        Contact e = new Contact();
        e.setName(newName);
        e.setAddressBook(addressBook);
        _update(e, icalData);
        session.save(e);
        session.save(e.getAddressBook());
        return e;
    }

    public void update(Contact contact, String data) {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        _update(contact, data);
        session.save(contact);
        session.save(contact.getAddressBook());
        tx.commit();
    }

    public String format(VCard vcard) {
        VCardWriter writer = new VCardWriter();
        writer.setVCard(vcard);
        return writer.buildVCardString();
    }
    
    public VCard parse(String data) {
        VCardEngine cardEngine = new VCardEngine();
        try {
            return cardEngine.parse(data);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void _update(Contact contact, String data) {
        VCard vcard = parse(data);
        if (vcard.getUID() != null && vcard.getUID().hasUID()) {
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
        String ph = getPhone(vcard);
        contact.setTelephonenumber(ph);
    }

    public List<String> getEmailAddresses(VCard vcard) {
        List<String> list = new ArrayList<>();
        Iterator<EmailFeature> it = vcard.getEmails();
        while (it.hasNext()) {
            list.add(it.next().getEmail());
        }
        return list;
    }

    private String getPhone(VCard vcard) {
        Iterator<TelephoneFeature> itPhone = vcard.getTelephoneNumbers();
        while (itPhone.hasNext()) {
            return itPhone.next().getTelephone();
        }
        return null;
    }

    public List<String> getPhoneNumbers(VCard vcard) {
        List<String> list = new ArrayList<>();
        Iterator<TelephoneFeature> itPhone = vcard.getTelephoneNumbers();
        while (itPhone.hasNext()) {
            String s = itPhone.next().getTelephone();
            list.add(s);
        }
        return list;
    }

    private String buildFormattedName(Contact contact) {
        String s = "";
        if (contact.getGivenName() != null && contact.getGivenName().length() > 0) {
            s += contact.getGivenName();
        }
        if (contact.getSurName() != null && contact.getSurName().length() > 0) {
            s += " ";
            s += contact.getSurName();
        }
        return s;
    }

    public void setPhone(VCard vcard, String phone, Contact contact) {
        contact.setTelephonenumber(phone);
        Iterator<TelephoneFeature> it = vcard.getTelephoneNumbers();
        while( it.hasNext() ) {
            it.next().setTelephone(phone);
            return ;
        }
        TelephoneFeature t = new TelephoneType(phone);
        vcard.addTelephoneNumber(t);        
    }

    public void setMail(VCard vcard, String email, Contact contact) {
        contact.setMail(email);
        Iterator<EmailFeature> it = vcard.getEmails();
        while( it.hasNext() ) {
            it.next().setEmail(email);
            return ;
        }
        EmailFeature e = new EmailType(email);
        vcard.addEmail(e);
    }

    public void setSurName(VCard vcard, String surName, Contact contact) {
        vcard.getName().setFamilyName(surName);        
        contact.setSurName(surName);
    }

    public void setGivenName(VCard vcard, String givenName, Contact contact) {
        vcard.getName().setGivenName(givenName);
        contact.setGivenName(givenName);
    }

    public List<VCard> parseMultiple(InputStream inputStream) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        IOUtils.copy(inputStream, bout);
        String vcardText = bout.toString("UTF-8");
        VCardEngine cardEngine = new VCardEngine();
        List<VCard> vcards = cardEngine.parseManyInOneVCard(vcardText);
        return vcards;
    }
}
