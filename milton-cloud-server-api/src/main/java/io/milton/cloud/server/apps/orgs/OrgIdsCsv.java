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
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.utils.SessionManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Map;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.FlushMode;

/**
 * Returns an updateable CSV view of the orgIDs of the business units within
 * this organisation
 *
 * @author brad
 */
public class OrgIdsCsv extends AbstractResource implements GetableResource, PostableResource {

    private static final Logger log = LoggerFactory.getLogger(OrgIdsCsv.class);
    private final String name;
    private final OrganisationsFolder parent;
    private JsonResult jsonResult;
    private List<String> errors = new ArrayList<>();
    private int numUpdated;
    private int currentLine;

    public OrgIdsCsv(String name, OrganisationsFolder parent) {
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
                FileItem file = files.values().iterator().next();
                session.setFlushMode(FlushMode.MANUAL);
                doUpload(file, session);
                if (errors.isEmpty()) {
                    jsonResult = new JsonResult(true, "Done updates");
                } else {
                    jsonResult = new JsonResult(true, "Errors detected");
                }
                session.flush();
                tx.commit();

                jsonResult.setData(new UploadResult()); // sets return values automatically

            } catch (Exception ex) {
                log.warn("Exception processing", ex);
                tx.rollback();
                jsonResult = new JsonResult(false);
                jsonResult.setMessages(Arrays.asList("Error processing line: " + currentLine + " - " + ex.getMessage()));
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
            try (OutputStreamWriter pw = new OutputStreamWriter(out)) {
                CSVWriter writer = new CSVWriter(pw);
                String[] arr = {"OrgID", "New OrgID", "OrgTitle"};
                writer.writeNext(arr);

                toCsv(parent.getOrganisation(), parent.getOrganisation().childOrgs(), writer);
                pw.flush();
            }
        }
    }

    public String getTitle() {
        return "OrgIDs CSV";
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

        List<String> values;
        String[] arr;
        for (Organisation org : orgs) {
            values = buildLineOfValues(rootOrg, org);
            arr = new String[values.size()];
            values.toArray(arr);
            writer.writeNext(arr);
            toCsv(rootOrg, org.childOrgs(), writer);
        }
    }

    private List<String> buildLineOfValues(Organisation rootOrg, Organisation org) {
        List<String> values = new ArrayList<>();
        values.add(org.getOrgId()); // unique ID
        values.add(""); // place holder for new ID
        values.add(org.getTitle()); // user friendly name
        return values;
    }

    private void doUpload(FileItem file, Session session) throws IOException {
        log.info("doUpload");
        InputStream in = null;
        try {
            in = file.getInputStream();

            updateOnlyFromCsv(in, session);

        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    private void updateOnlyFromCsv(InputStream in, Session session) throws IOException {
        InputStreamReader r = new InputStreamReader(in);
        CSVReader reader = new CSVReader(r);

        String[] lineParts;
        currentLine = 1;
        reader.readNext(); // skip first row, column headings

        Organisation rootOrg = getOrganisation();
        long rootOrgId = rootOrg.getId();

        List<UpdatedLine> updated = new ArrayList<>();

        while ((lineParts = reader.readNext()) != null) {
            if (lineParts.length > 0) {
                currentLine++;
                if (log.isTraceEnabled()) {
                    log.trace("process line: " + currentLine + " : " + Arrays.toString(lineParts));
                }
                List<String> lineList = new ArrayList<>();
                lineList.addAll(Arrays.asList(lineParts));
                if (lineList.size() > 0 && lineList.get(0).length() > 0) {
                    doProcess(rootOrg, lineList, currentLine, updated, session);
                }
                if (currentLine % 20 == 0) {
                    log.info("flush");
                    session.flush();
                }
            }
        }

        log.info("Save updated records, and check for dups: " + updated.size());
        for (UpdatedLine updatedLine : updated) {
            log.info("Save - id=" + updatedLine.org.getId());
            session.save(updatedLine.org);
        }
        session.flush();
        for (UpdatedLine updatedLine : updated) {
            log.info("Check dup - id=" + updatedLine.org.getId());
            if (!updatedLine.org.isOrgIdUniqueWithinAdmin(session)) {
                errors.add("Duplicate orgId: " + updatedLine.org.getOrgId() + " line " + updatedLine.line);
            }
        }
        session.flush();
    }

    private void doProcess(Organisation rootOrg, List<String> lineList, int line, List<UpdatedLine> updated, Session session) {
        String orgId = lineList.get(0);
        String newOrgId = get(lineList, 1);
        if (StringUtils.isEmpty(orgId) || StringUtils.isEmpty(newOrgId)) {
            return;
        }

        Organisation child = rootOrg.childOrg(orgId, session);
        if (child == null) {
            log.trace("Did not find child called: " + orgId);
            errors.add("Existing orgID not found on line: " + line + " orgId=" + orgId);
        } else {
            log.info("found record to update: " + child.getOrgId() + " ID=" + child.getId());
            numUpdated++;
            String oldOrgId = child.getOrgId();
            child.setOrgId(newOrgId);
            log.info("  - set new orgid= " + newOrgId + "  from " + oldOrgId + "  for id=" + child.getId());
            session.save(child);
            updated.add(new UpdatedLine(child, line, oldOrgId));

        }
    }

    /**
     *
     *
     * @param lineList
     * @param i - column index, ie starts at zero
     * @return
     */
    private String get(List<String> lineList, int i) {
        if (i > lineList.size() - 1) {
            return null;
        }
        String s = lineList.get(i);
        if (s != null) {
            s = s.trim();
            if (s.length() == 0) {
                s = null;
            }
        }
        return s;
    }

    public class UploadResult {

        private List<String> errors;
        private int numUpdated;

        public UploadResult() {
            this.numUpdated = OrgIdsCsv.this.numUpdated;
            this.errors = OrgIdsCsv.this.errors;
        }

        public int getNumUpdated() {
            return numUpdated;
        }

        public List<String> getErrors() {
            return errors;
        }
    }

    public class UpdatedLine {

        private Organisation org;
        private int line;
        private String oldOrgId;

        public UpdatedLine(Organisation org, int line, String oldOrgId) {
            this.org = org;
            this.line = line;
            this.oldOrgId = oldOrgId;
        }
    }
}
