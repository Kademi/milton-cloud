/*
 * Copyright 2012 McEvoy Software Ltd.
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
package io.milton.cloud.server.apps.admin;

import au.com.bytecode.opencsv.CSVWriter;
import io.milton.cloud.server.web.AbstractResource;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.GetableResource;
import io.milton.vfs.db.Group;
import io.milton.vfs.db.GroupMembership;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Returns a CSV view of the business units in the parent folder
 *
 * @author brad
 */
public class ManageGroupMembersCsv extends AbstractResource implements GetableResource {

    private static final Logger log = LoggerFactory.getLogger(ManageGroupMembersCsv.class);
    private final String name;
    private final CommonCollectionResource parent;
    private final Group group;

    public ManageGroupMembersCsv(String name, CommonCollectionResource parent, Group group) {
        this.parent = parent;
        this.name = name;
        this.group = group;
    }

    @Override
    public AccessControlledResource.Priviledge getRequiredPostPriviledge(Request request) {
        return AccessControlledResource.Priviledge.WRITE_CONTENT;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        try (PrintWriter pw = new PrintWriter(out)) {
            CSVWriter writer = new CSVWriter(pw);
            toCsv(parent.getOrganisation(), writer);
            pw.flush();
        }
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
    public Long getMaxAgeSeconds(Auth auth) {
        return null;
    }

    @Override
    public String getContentType(String accepts) {
        return "text/csv";
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
    public boolean is(String type) {
        if (type.equals("csv")) {
            return true;
        }
        return super.is(type);
    }

    private void toCsv(Organisation rootOrg, CSVWriter writer) {
        List<String> values = new ArrayList<>();
        values.add("MemberOfOrg");

        values.add("UserName"); // unique profile name
        values.add("NickName"); // nickname
        values.add("Email");
        values.add("FirstName");
        values.add("SurName");
        values.add("Phone");

        String[] arr = new String[values.size()];
        values.toArray(arr);
        writer.writeNext(arr);

        writeUsers( writer);
    }

    private void writeUsers(CSVWriter writer) {
        if (group.getGroupMemberships() != null) {
            for (GroupMembership m : group.getGroupMemberships()) {
                List<String> values = buildLineOfValues(m);
                String[] arr = new String[values.size()];
                values.toArray(arr);
                writer.writeNext(arr);
            }
        }
    }

    private List<String> buildLineOfValues(GroupMembership m) {
        List<String> values = new ArrayList<>();

        Organisation org = m.getWithinOrg();
        values.add(org.getOrgId()); // org unique ID

        Profile p = m.getMember();
        values.add(p.getName()); // unique profile name
        values.add(p.getNickName()); // nickname
        values.add(p.getEmail());
        values.add(p.getFirstName());
        values.add(p.getSurName());
        values.add(p.getPhone());

        return values;
    }
}
