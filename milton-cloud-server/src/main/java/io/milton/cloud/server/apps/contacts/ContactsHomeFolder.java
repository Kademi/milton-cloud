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

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import io.milton.vfs.db.AddressBook;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.cloud.server.web.AbstractCollectionResource;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.NodeChildUtils;
import io.milton.cloud.server.web.PersonalResource;
import io.milton.cloud.server.web.ResourceList;
import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.cloud.server.web.UserResource;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.cloud.server.web.templating.TitledPage;
import io.milton.resource.AccessControlledResource;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.principal.Principal;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.CollectionResource;
import io.milton.resource.GetableResource;
import io.milton.resource.MakeCollectionableResource;
import io.milton.resource.Resource;

import static io.milton.context.RequestContext._;
import io.milton.http.Request;
import io.milton.http.Request.Method;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.utils.SessionManager;
import java.util.Collections;
import java.util.Comparator;

import static io.milton.context.RequestContext._;

/**
 *
 * @author brad
 */
public class ContactsHomeFolder extends AbstractCollectionResource implements MakeCollectionableResource, GetableResource, PersonalResource, TitledPage {

    private final String name;
    private final UserResource parent;
    private final ContactManager contactManager;
    private ResourceList children;

    public ContactsHomeFolder(UserResource parent, String name, ContactManager contactManager) {
        this.parent = parent;
        this.name = name;
        this.contactManager = contactManager;
    }

    @Override
    public boolean authorise(Request request, Method method, Auth auth) {
        return super.authorise(request, method, auth);
    }

    @Override
    public CollectionResource createCollection(String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
        AddressBook addressBook = contactManager.createAddressBook(parent.getThisUser(), newName);
        Branch branch = addressBook.getTrunk();
        Profile curUser = _(SpliffySecurityManager.class).getCurrentUser();
        if( branch == null ) {
            branch = addressBook.createBranch(Branch.TRUNK, curUser, SessionManager.session());
        }
        return new ContactsFolder(newName, this, addressBook, branch, contactManager);
    }

    @Override
    public CommonCollectionResource getParent() {
        return parent;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Date getModifiedDate() {
        return null;
    }

    @Override
    public Date getCreateDate() {
        return null;
    }

    @Override
    public Resource child(String childName) throws NotAuthorizedException, BadRequestException {
        return NodeChildUtils.childOf(getChildren(), childName);
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            List<AddressBook> addressBooks = parent.getThisUser().getAddressBooks();
            children = new ResourceList();
            if (addressBooks != null) {
                for (AddressBook cal : addressBooks) {
                    Branch branch = cal.getTrunk();
                    ContactsFolder f = new ContactsFolder(cal.getName(), this, cal, branch, contactManager);
                    children.add(f);
                }
            }
        }
        return children;
    }
    
    public List<ContactResource> getContacts() throws NotAuthorizedException, BadRequestException {
        List<ContactResource> list = new ArrayList();
        for( Resource r : getChildren() ) {
            if( r instanceof ContactsFolder ) {
                ContactsFolder book = (ContactsFolder) r;
                for( Resource rContact : book.getChildren()) {
                    if( rContact instanceof ContactResource ) {
                        ContactResource c = (ContactResource) rContact;
                        list.add(c);
                    }
                }
            }
        }
        Collections.sort(list, new Comparator<ContactResource>() {

            @Override
            public int compare(ContactResource o1, ContactResource o2) {
                String n1 = o1.getCommonName();
                String n2 = o2.getCommonName();
                return n1.compareTo(n2);
            }
        });
        return list;
    }

    @Override
    public Map<Principal, List<AccessControlledResource.Priviledge>> getAccessControlList() {
        return null;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        _(HtmlTemplater.class).writePage("contacts/contactsHome", this, params, out);
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return null;
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
    public Organisation getOrganisation() {
        return parent.getOrganisation();
    }

    @Override
    public Profile getOwnerProfile() {
        return parent.getThisUser();
    }

    @Override
    public String getTitle() {
        return "Contacts";
    }
    
    
}
