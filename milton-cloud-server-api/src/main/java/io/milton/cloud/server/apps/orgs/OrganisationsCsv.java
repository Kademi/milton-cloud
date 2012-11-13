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
package io.milton.cloud.server.apps.orgs;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import io.milton.cloud.server.web.AbstractResource;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.JsonResult;
import io.milton.cloud.server.web.WebUtils;
import io.milton.common.Path;
import io.milton.http.Auth;
import io.milton.http.FileItem;
import io.milton.http.HttpManager;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.GetableResource;
import io.milton.resource.PostableResource;
import io.milton.vfs.db.Organisation;
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
import java.util.List;
import org.apache.commons.io.IOUtils;

/**
 * Returns a CSV view of the business units in the parent folder
 *
 * @author brad
 */
public class OrganisationsCsv extends AbstractResource implements GetableResource, PostableResource {

    private static final Logger log = LoggerFactory.getLogger(OrganisationsCsv.class);
    private final String name;
    private final OrganisationsFolder parent;
    private JsonResult jsonResult;
    private List<List<String>> unmatched = new ArrayList<>();
    private int numUpdated;

    public OrganisationsCsv(String name, OrganisationsFolder parent) {
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
                Boolean insertMode = WebUtils.getParamAsBool(parameters, "insertMode");
                insertMode = (insertMode == null ? false : insertMode);
                FileItem file = files.values().iterator().next();
                doUpload(file, insertMode, session);
                jsonResult.setData(new UploadResult());
                tx.commit();

            } catch (Exception ex) {
                log.warn("Exception processing", ex);
                tx.rollback();
                jsonResult = new JsonResult(false);
                jsonResult.setMessages(Arrays.asList("Failed to update"));
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
            jsonResult.write(out);
        } else {
            try (PrintWriter pw = new PrintWriter(out)) {
                CSVWriter writer = new CSVWriter(pw);
                toCsv(parent.getOrganisation(), parent.getOrganisation().childOrgs(), writer);
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

    private void toCsv(Organisation rootOrg, List<Organisation> orgs, CSVWriter writer) {
        if (orgs == null) {
            return;
        }
        for (Organisation org : orgs) {
            List<String> values;
            values = buildLineOfValues(rootOrg, org);
            String[] arr = new String[values.size()];
            values.toArray(arr);
            writer.writeNext(arr);
            toCsv(rootOrg, org.childOrgs(), writer);
        }
    }

    private List<String> buildLineOfValues(Organisation rootOrg, Organisation org) {
        List<String> values = new ArrayList<>();
        Path path = toOrgPath(rootOrg, org);
        if (!path.isRoot()) {
            path = path.getParent();
        }
        values.add(org.getOrgId()); // unique ID
        values.add(path.toString()); // path to org        
        values.add(org.getTitle()); // user friendly name
        values.add(org.getAddress());
        values.add(org.getAddressLine2());
        values.add(org.getAddressState());
        values.add(org.getPhone());
        values.add(org.getPostcode());
        return values;
    }

    private Path toOrgPath(Organisation rootOrg, Organisation org) {
        if (org == null || rootOrg == org) {
            return Path.root;
        } else {
            return toOrgPath(rootOrg, org.getOrganisation()).child(org.getName());
        }
    }

    private void doUpload(FileItem file, Boolean insertMode, Session session) throws IOException {
        log.info("doUpload: " + insertMode);
        InputStream in = null;
        try {
            in = file.getInputStream();
            Request req = HttpManager.request();
            if (insertMode) {
                fromCsv(in, session);

            } else {
                updateOnlyFromCsv(in, session);
            }
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    public void fromCsv(InputStream in, Session session) throws IOException {
        InputStreamReader r = new InputStreamReader(in);
        CSVReader reader = new CSVReader(r);

        String[] lineParts;
        int line = 0;
        while ((lineParts = reader.readNext()) != null) {
            if (lineParts.length > 0) {
                line++;
                System.out.println("line: " + line);
                if (log.isTraceEnabled()) {
                    log.trace("process line: " + line + " : " + Arrays.toString(lineParts));
                }
                List<String> lineList = new ArrayList<>();
                lineList.addAll(Arrays.asList(lineParts));
                if (lineList.size() > 0 && lineList.get(0).length() > 0) {
                    doProcess(getOrganisation(), lineList, line, true, session);
                }
            }
        }
        // TODO: find all recs not updated and delete them
        jsonResult = new JsonResult(true, "Done insert and updates");
    }

    private void doProcess(Organisation rootOrg, List<String> lineList, int line, boolean allowInserts, Session session) {
        log.trace("doProcess: ");
        String orgId = lineList.get(0);
        if (orgId == null || orgId.length() == 0) {
            //throw new RuntimeException("Cant save record with an empty name: column" + pos + " line: " + line);
            unmatched.add(lineList);
            return;
        }
        Organisation child = Organisation.findByOrgId(orgId, session);
        if (child == null) {
            log.trace("Create child called: " + orgId);
            if (allowInserts) {
                child = createOrg(rootOrg, session, lineList);
            } else {
                unmatched.add(lineList);
                return;
            }
            log.info("created new record: " + child.getName());
        } else {
            log.trace("found record to update: " + child.getName());
        }
        updateRecord(child, lineList, line, rootOrg, session);
    }

    private void updateRecord(Organisation child, List<String> lineList, int line, Organisation rootOrg, Session session) {
        numUpdated++;
        String sPath = lineList.get(1);
        Path path = Path.path(sPath);
        checkPath(child, path, rootOrg, session);
        child.setTitle(get(lineList, 2));
        //System.out.println("new title: " + child.getTitle() + ", " + numUpdated);
        child.setAddress(get(lineList, 3));
        child.setAddressLine2(get(lineList, 4));
        child.setAddressState(get(lineList, 5));
        child.setPhone(get(lineList, 6));
        child.setPostcode(get(lineList, 7));
        session.save(child);
    }

    private void updateOnlyFromCsv(InputStream in, Session session) throws IOException {
        InputStreamReader r = new InputStreamReader(in);
        CSVReader reader = new CSVReader(r);

        String[] lineParts;
        int line = 0;

        while ((lineParts = reader.readNext()) != null) {
            if (lineParts.length > 0) {
                line++;
                if (log.isTraceEnabled()) {
                    log.trace("process line: " + line + " : " + Arrays.toString(lineParts));
                }
                List<String> lineList = new ArrayList<>();
                lineList.addAll(Arrays.asList(lineParts));
                if (lineList.size() > 0 && lineList.get(0).length() > 0) {
                    doProcess(getOrganisation(), lineList, line, false, session);
                }
            }
        }
        jsonResult = new JsonResult(true, "Done updates");
        jsonResult.setData(unmatched);

    }

    private String get(List<String> lineList, int i) {
        if (i > lineList.size() - 1) {
            return null;
        }
        String s = lineList.get(i);
        if (s != null) {
            s = s.trim();
        }
        return s;
    }

    private Organisation createOrg(Organisation rootOrg, Session session, List<String> lineList) {
        String orgId = lineList.get(0);
        String sPath = lineList.get(1);
        log.info("Create org: " + sPath);
        Path path = Path.path(sPath);
        path = path.child(orgId);
        Organisation org = rootOrg;
        for (String childName : path.getParts()) {
            Organisation child = org.childOrg(childName);
            if (child == null) {
                child = org.createChildOrg(childName, session);
            }
            org = child;
        }
        return org;
    }

    /**
     * Check that the org has the given path, or move it if required
     *
     * @param child
     * @param path - path to the parent org
     */
    private void checkPath(Organisation childToCheck, Path path, Organisation rootOrg, Session session) {
        Organisation org = rootOrg;
        if (!path.isRoot()) {
            for (String childName : path.getParts()) {
                System.out.println("  - " + childName);
                Organisation child = org.childOrg(childName);
                if (child == null) {
                    child = org.createChildOrg(childName, session);
                }
                org = child;
            }
        }
        // The org that we're left with needs to be the parent of childToCheck
        if (childToCheck.getOrganisation() != org) {
            log.info("org has moved: " + childToCheck.getOrgId() + " - from: " + childToCheck.getOrganisation().getOrgId() + " to " + org.getOrgId());
            childToCheck.setOrganisation(org, session);
        }

    }

    public class UploadResult {

        private List<List<String>> unmatched = new ArrayList<>();
        private int numUpdated;

        public UploadResult() {
            this.numUpdated = OrganisationsCsv.this.numUpdated;
            this.unmatched = OrganisationsCsv.this.unmatched;
        }

        public int getNumUpdated() {
            return numUpdated;
        }

        public List<List<String>> getUnmatched() {
            return unmatched;
        }
    }
}
