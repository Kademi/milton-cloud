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

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.common.With;
import io.milton.cloud.server.apps.signup.SignupApp;
import io.milton.cloud.server.db.SignupLog;
import io.milton.cloud.server.manager.PasswordManager;
import io.milton.cloud.server.web.AbstractResource;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.JsonResult;
import io.milton.cloud.server.web.WebUtils;
import io.milton.http.Auth;
import io.milton.http.FileItem;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.GetableResource;
import io.milton.resource.PostableResource;
import io.milton.vfs.db.Group;
import io.milton.vfs.db.GroupMembership;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.utils.SessionManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import static io.milton.context.RequestContext._;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;

/**
 * Returns a CSV view of the business units in the parent folder
 *
 * @author brad
 */
public class ManageUsersCsv extends AbstractResource implements GetableResource, PostableResource {

    private static final Logger log = LoggerFactory.getLogger(ManageUsersCsv.class);
    private final String name;
    private final ManageUsersFolder parent;
    private JsonResult jsonResult;
    private List<List<String>> unmatched = new ArrayList<>();
    private int numUpdated;
    private int numInserted;
    private PasswordManager passwordManager; // lazy looked up

    public ManageUsersCsv(String name, ManageUsersFolder parent) {
        this.parent = parent;
        this.name = name;
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        log.info("processForm: " + parameters.size() + " files:" + files.size());
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();

        if (!files.isEmpty()) {
            try {
                session.setFlushMode(FlushMode.MANUAL);
                Boolean insertMode = WebUtils.getParamAsBool(parameters, "insertMode");
                insertMode = (insertMode == null ? false : insertMode);
                FileItem file = files.values().iterator().next();
                doUpload(file, insertMode, session);
                jsonResult.setData(new UploadResult());
                if (unmatched.isEmpty()) {
                    log.info("No unmatched entries, so commit");
                    tx.commit();
                } else {
                    //log.info("Found unmatched entries: " + unmatched.size() + " - rollback");
                    //tx.rollback();
                    log.info("Found unmatched entries: " + unmatched.size() + " - commit the good ones");
                    tx.commit();
                }

            } catch (Exception ex) {
                log.warn("Exception processing", ex);
                tx.rollback();
                jsonResult = new JsonResult(false);
                jsonResult.setMessages(Arrays.asList("Failed to update"));
                jsonResult.setData(new UploadResult());
            }
        } else {
        }
        return null;
    }

    @Override
    public AccessControlledResource.Priviledge getRequiredPostPriviledge(Request request) {
        return AccessControlledResource.Priviledge.WRITE_CONTENT;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        if (jsonResult != null) {
            log.info("send json");
            jsonResult.write(out);
            log.info("finished send json");
        } else {
            try (PrintWriter pw = new PrintWriter(out)) {
                CSVWriter writer = new CSVWriter(pw);
                toCsv(parent.getOrganisation(), writer);
                pw.flush();
            }
        }
    }

    public String getTitle() {
        return "Business units CSV";
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
        if (jsonResult == null) {
            return "text/csv";
        } else {
            return JsonResult.CONTENT_TYPE;
        }
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
        values.add("Group"); // group member

        values.add("UserName"); // unique profile name
        values.add("NickName"); // nickname
        values.add("Email");
        values.add("FirstName");
        values.add("SurName");
        values.add("Phone");

        String[] arr = new String[values.size()];
        values.toArray(arr);
        writer.writeNext(arr);


        _toCsv(rootOrg, writer);
    }

    private void _toCsv(Organisation rootOrg, CSVWriter writer) {
        writeUsers(rootOrg, writer);
        if (rootOrg.getChildOrgs() == null) {
            return;
        }
        for (Organisation org : rootOrg.getChildOrgs()) {
            _toCsv(org, writer);
        }
    }

    private void writeUsers(Organisation org, CSVWriter writer) {
        if (org.getMembers() != null) {
            for (GroupMembership m : org.getMembers()) {
                List<String> values = buildLineOfValues(m);
                String[] arr = new String[values.size()];
                values.toArray(arr);
                writer.writeNext(arr);
            }
        }
    }

    private void doUpload(FileItem file, Boolean insertMode, Session session) throws IOException {
        log.info("doUpload: " + insertMode);
        InputStream in = null;
        try {
            in = file.getInputStream();
            boolean allowinserts = insertMode;
            fromCsv(in, session, allowinserts);

        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    public void fromCsv(InputStream in, Session session, boolean allowInserts) throws IOException {
        InputStreamReader r = new InputStreamReader(in);
        CSVReader reader = new CSVReader(r);

        String[] lineParts;
        int line = 0;
        Organisation rootOrg = getOrganisation();
        long rootOrgId = rootOrg.getId();

        reader.readNext(); // skip headers        
        while ((lineParts = reader.readNext()) != null) {
            if (lineParts.length > 0) {
                line++;
                //if (log.isTraceEnabled()) {
                log.info("process line: " + line + " : " + Arrays.toString(lineParts));
                //}
                List<String> lineList = new ArrayList<>();
                lineList.addAll(Arrays.asList(lineParts));
                if (lineList.size() > 0 && lineList.get(0).length() > 0) {
                    long tm = System.currentTimeMillis();
                    try {
                        doProcess(rootOrg, lineList, line, allowInserts, session);
                        tm = System.currentTimeMillis() - tm;
                        session.flush();
                        log.info("  fromCsv: line processed in " + tm + "ms");
                    } catch (Exception e) {
                        log.error("Exception processing line: " + line, e);
                        rootOrg = null;
                    }
                    if (line % 20 == 0 || rootOrg == null) {
                        log.info("Clear session");
                        session.clear();
                        rootOrg = Organisation.get(rootOrgId, session);
                    }
                }
            }
        }
        if (unmatched.isEmpty()) {
            jsonResult = new JsonResult(true, "Done");
        } else {
            jsonResult = new JsonResult(false, "Found " + unmatched.size() + " records. Please review and correct");
        }
    }

    private void doProcess(Organisation rootOrg, List<String> lineList, int line, boolean allowInserts, Session session) {
        long tm = System.currentTimeMillis();
        String orgId = lineList.get(0);
        String groupName = lineList.get(1);
        String userName = lineList.get(2);
        String nickName = lineList.get(3);
        String email = lineList.get(4);
        String firstName = lineList.get(5);
        String surName = lineList.get(6);
        String phone = lineList.get(7);
        String newPassword = null;
        if (lineList.size() >= 9) {
            newPassword = lineList.get(8);
        }

        if (StringUtils.isBlank(orgId)) {
            lineList.add("blank orgId given for line: " + line);
            unmatched.add(lineList);
            return;
        }

        if (StringUtils.isBlank(email)) {
            lineList.add("blank email given for line: " + line);
            unmatched.add(lineList);
            return;
        }

        if (StringUtils.isBlank(groupName)) {
            lineList.add("blank groupName given for line: " + line);
            unmatched.add(lineList);
            return;
        }

        // sometimes we might have combined firstname+surname in one field, so we split it out
        if (StringUtils.isBlank(surName)) {
            if (!StringUtils.isBlank(firstName)) {
                if (firstName.contains(" ")) {
                    int pos = firstName.lastIndexOf(" ");
                    surName = firstName.substring(pos + 1);
                    firstName = firstName.substring(0, pos);
                }
            }
        }

        if (StringUtils.isBlank(nickName)) {
            nickName = firstName;
        }


        final Organisation withinOrg = getOrganisation().childOrg(orgId, session);
        if (withinOrg == null) {
            // Try to find the org
            unmatched.add(lineList);
            lineList.add("Couldnt find organisation: " + orgId + " on line " + line);
            return;
        } else {
            if (!withinOrg.isWithin(rootOrg)) {
                unmatched.add(lineList);
                lineList.add("Couldnt find organisation: " + orgId + " on line " + line + " (is not within root organisation)");
                return;
            }
        }
        if (withinOrg == null) {
            return;
        }

        Profile p;
        if (StringUtils.isBlank(userName)) {
            if (!StringUtils.isBlank(email)) {
                p = Profile.findByEmail(email, session);
            } else {
                p = null;
            }
        } else {
            p = Profile.find(userName, session);
        }        

        Date now = _(CurrentDateService.class).getNow();

        boolean isNew = false;
        if (p == null) {
            if (allowInserts) {
                log.info("Creating user: " + nickName);
                p = new Profile();
                isNew = true;
                if (StringUtils.isBlank(userName)) {
                    userName = Profile.findAutoName(nickName, session);
                }
                p.setName(userName);
                p.setCreatedDate(now);
                p.setModifiedDate(now);
                numInserted++;
            } else {
                if (StringUtils.isBlank(userName)) {
                    lineList.add("Username is blank, but allow inserts is false. Line: " + line);
                } else {
                    lineList.add("Couldnt find username " + userName + " . I would insert but allow inserts is false. Line: " + line);
                }
                unmatched.add(lineList);
                return;
            }
        } else {
            numUpdated++;
        }
        final Profile profile = p;
        p.setNickName(nickName);
        p.setEmail(email);
        p.setFirstName(firstName);
        p.setSurName(surName);
        p.setPhone(phone);

        session.save(p);
        if (isNew) {
            if (newPassword != null) {
                passwordManager().setPassword(p, newPassword);
            }
        }


        if (p.isInGroup(groupName, withinOrg)) {
            // great, do nothing
        } else {
            final Group g = rootOrg.group(groupName, session);
            if (g == null) {
                lineList.add("Couldnt find group " + groupName + " on line: " + line);
                unmatched.add(lineList);
                return;
            } else {
                // If user is a member of the same group on another org we should remove that membership
                if (g.isMember(p)) {
                    p.removeMembership(g, session);
                }
                p.getOrCreateGroupMembership(g, withinOrg, session, new With<GroupMembership, Object>() {

                    @Override
                    public Object use(GroupMembership t) throws Exception {
                        _(SignupApp.class).onNewMembership(profile.membership(g), null);
                        SignupLog.logSignup(null, getOrganisation(), profile, withinOrg, g, SessionManager.session());
                        return null;
                    }
                });
                tm = System.currentTimeMillis();                
                session.flush();

            }
        }
    }

    private PasswordManager passwordManager() {
        if (passwordManager == null) {
            passwordManager = _(PasswordManager.class);
        }
        return passwordManager;
    }

    private List<String> buildLineOfValues(GroupMembership m) {
        List<String> values = new ArrayList<>();

        Organisation org = m.getWithinOrg();
        values.add(org.getOrgId()); // org unique ID
        values.add(m.getGroupEntity().getName()); // group member

        Profile p = m.getMember();
        values.add(p.getName()); // unique profile name
        values.add(p.getNickName()); // nickname
        values.add(p.getEmail());
        values.add(p.getFirstName());
        values.add(p.getSurName());
        values.add(p.getPhone());

        return values;
    }

    public class UploadResult {

        private List<List<String>> unmatched = new ArrayList<>();
        private int numUpdated;
        private int numInserted;

        public UploadResult() {
            this.numUpdated = ManageUsersCsv.this.numUpdated;
            this.numInserted = ManageUsersCsv.this.numInserted;
            this.unmatched = ManageUsersCsv.this.unmatched;
        }

        public int getNumUpdated() {
            return numUpdated;
        }

        public List<List<String>> getUnmatched() {
            return unmatched;
        }

        public int getNumInserted() {
            return numInserted;
        }
    }
}
