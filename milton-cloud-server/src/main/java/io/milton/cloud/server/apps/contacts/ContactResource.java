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
import info.ineighborhood.cardme.vcard.VCard;
import java.io.*;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.milton.cloud.server.apps.calendar.CalEventResource;
import io.milton.cloud.server.web.JsonResult;
import io.milton.cloud.server.web.UploadUtils;
import io.milton.http.FileItem;
import io.milton.vfs.db.Contact;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.property.BeanPropertyResource;
import io.milton.resource.*;
import io.milton.vfs.data.DataSession.FileNode;
import io.milton.vfs.db.utils.SessionManager;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.hibernate.Session;

/**
 *
 * @author brad
 */
@BeanPropertyResource(value = "ldap")
public class ContactResource extends io.milton.cloud.server.web.FileResource implements AddressResource, LdapContact, PostableResource {

    private static final Logger log = LoggerFactory.getLogger(CalEventResource.class);
    private Contact contact;
    private final ContactsFolder parent;
    private final ContactManager contactManager;
    private Transaction tx; // for proppatch setting
    private VCard vcard;

    public ContactResource(FileNode fileNode, ContactsFolder parent, Contact contact, ContactManager contactManager) {
        super(fileNode, parent);
        this.contact = contact;
        this.parent = parent;
        this.contactManager = contactManager;
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {        
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();

        try {
            vcard();
            if (contact == null) {
                contact = parent.getAddressBook().add(getName());
            }
            contactManager.setPhone(vcard, parameters.get("telephonenumber"), contact);
            contactManager.setMail(vcard, parameters.get("mail"), contact);
            contactManager.setSurName(vcard, parameters.get("surName"), contact);
            contactManager.setGivenName(vcard, parameters.get("givenName"), contact);
            String data = contactManager.format(vcard);
            ByteArrayInputStream bin = null;
            bin = new ByteArrayInputStream(data.getBytes("UTF-8"));
            UploadUtils.setContent(this, bin);
            getParent().save();
            session.save(contact);
            System.out.println("surname: " + contact.getSurName());
            jsonResult = new JsonResult(true);
            tx.commit();
        } catch (IOException ex) {
            jsonResult = new JsonResult(false, ex.getMessage());            
        }
        return null;
    }

    
    @Override
    public void replaceContent(InputStream in, Long length) throws BadRequestException, ConflictException, NotAuthorizedException {
        super.replaceContent(in, length);
        contactManager.update(contact, getAddressData());
    }

    @Override
    public String getContentType(String accepts) {
        return "text/vcard";
    }

    @Override
    public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
        super.delete();
        if( contact != null ) {
            contactManager.delete(contact);
        }
    }

    @Override
    public String getAddressData() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            sendContent(out, null, null, null);
            return out.toString("UTF-8");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public String getFormattedName() {
        return contact.getGivenName() + " " + contact.getSurName();
    }

    public String getCommonName() {
        return getFormattedName();
    }

    public String getTelephone() {
        return contact.getTelephonenumber();
    }

    public String getEmail() {
        return contact.getMail();
    }

    public String getTelephonenumber() {
        return contact.getTelephonenumber();
    }

    public List<String> getPhoneNumbers() {
        return contactManager.getPhoneNumbers(vcard());
    }

    public String getMail() {
        return contact.getMail();
    }

    public List<String> getEmailAddresses() {
        return contactManager.getEmailAddresses(vcard());
    }

    public String getGivenName() {
        return contact.getGivenName();
    }

    public void setGivenName(String s) {
        contact.setGivenName(s);
    }

    public String getSurName() {
        return contact.getSurName();
    }

    private VCard vcard() {
        if (vcard == null) {
            VCardEngine cardEngine = new VCardEngine();
            try {
                vcard = cardEngine.parse(getAddressData());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        return vcard;
    }
}