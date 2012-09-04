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

import info.ineighborhood.cardme.vcard.VCard;
import info.ineighborhood.cardme.vcard.types.UIDType;
import io.milton.cloud.server.web.BranchFolder;
import io.milton.vfs.db.Contact;
import io.milton.vfs.db.AddressBook;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.ContentDirectoryResource;
import io.milton.cloud.server.web.JsonResult;
import io.milton.cloud.server.web.NewPageResource;
import io.milton.cloud.server.web.PersonalResource;
import io.milton.cloud.server.web.UploadUtils;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.common.InternationalizedString;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.http.values.Pair;
import io.milton.resource.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import static io.milton.context.RequestContext._;
import io.milton.http.FileItem;
import io.milton.vfs.data.DataSession;
import io.milton.vfs.data.DataSession.FileNode;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Commit;
import io.milton.vfs.db.utils.SessionManager;
import java.io.ByteArrayInputStream;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an Address Book
 *
 * @author brad
 */
public class ContactsFolder extends BranchFolder implements AddressBookResource, ReportableResource, GetableResource, DeletableResource, PutableResource, PersonalResource {

    private static final Logger log = LoggerFactory.getLogger(ContactsFolder.class);
    private final AddressBook addressBook;
    private final ContactManager contactManager;
    private JsonResult jsonResult;

    public ContactsFolder(String name, CommonCollectionResource parent, AddressBook addressBook, Branch branch, ContactManager contactManager) {
        super(name, parent, branch, false);
        this.addressBook = addressBook;
        this.contactManager = contactManager;
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        if (!files.isEmpty()) {
            doImport(files);
        }
        return null;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        if (jsonResult != null) {
            jsonResult.write(out);
        } else {
            _(HtmlTemplater.class).writePage("contacts/contactsHome", this, params, out);
        }
    }

    @Override
    public ContactResource newFileResource(FileNode dm, ContentDirectoryResource parent, boolean renderMode) {
        //return super.newFileResource(dm, parent, renderMode);
        Contact c = addressBook.contact(dm.getName());
        return new ContactResource(dm, this, c, contactManager);
    }

    @Override
    public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
        contactManager.delete(addressBook);
    }

    @Override
    public String getCTag() {
        Commit head = branch.getHead();
        if (head == null) {
            return "na";
        }
        return head.getItemHash();
    }

    @Override
    public Resource createNew(String newName, InputStream inputStream, Long length, String contentType) throws IOException, ConflictException, NotAuthorizedException, BadRequestException {
        log.info("createNew: " + newName);
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        try {
            ContactResource r = (ContactResource) UploadUtils.createNew(session, this, newName, inputStream, length, contentType);
            String data = r.getAddressData();
            contactManager.createContact(addressBook, newName, data, session);

            return r;
        } finally {
            save();
            tx.commit();
        }
    }

    public AddressBook getAddressBook() {
        return addressBook;
    }

    @Override
    public String getContentType(String accepts) {
        return "text/html";
    }

    @Override
    public Long getContentLength() {
        return null;
    }

    @Override
    public InternationalizedString getDescription() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setDescription(InternationalizedString description) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Pair<String, String>> getSupportedAddressData() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Long getMaxResourceSize() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private void doImport(Map<String, FileItem> files) {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();

        try {
            for (FileItem fi : files.values()) {
                doImport(fi.getInputStream(), session);
            }
            this.save();
            tx.commit();
            jsonResult = new JsonResult(true);
        } catch (IOException ex) {
            tx.rollback();
            jsonResult = new JsonResult(false, ex.getMessage());
        }

    }

    private void doImport(InputStream inputStream, Session session) throws IOException {
        List<VCard> vcards = contactManager.parseMultiple(inputStream);
        log.info("doImport: vcards: " + vcards.size());
        for (VCard vc : vcards) {
            DataSession.DirectoryNode thisNode = this.dataSession.getRootDataNode();
            
            if (vc.getUID() == null) {
                vc.setUID(new UIDType());
                vc.getUID().setUID(UUID.randomUUID().toString());
            }
            String newName = vc.getUID().getUID();
            newName += ".vcf";
            newName = NewPageResource.getUniqueName(this, newName);
            DataSession.FileNode newFileNode = thisNode.addFile(newName);
            ContactResource fileResource = this.newFileResource(newFileNode, this, false);
            String vcardText = contactManager.format(vc);
            ByteArrayInputStream bin = new ByteArrayInputStream(vcardText.getBytes("UTF-8"));
            newFileNode.setContent(bin);
            String data = fileResource.getAddressData();
            contactManager.createContact(addressBook, newName, data, session);

            this.onAddedChild(fileResource);
            fileResource.updateModDate();
            log.info("created card: " + fileResource.getHref());
        }
    }
}