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
import java.util.Arrays;
import java.util.Map;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Returns a CSV which is the same as the one given, but with the first column
 * matched against organisations.
 *
 * The given CSV is repeated back, but with the first column resolved (if
 * possible) to an orgId
 *
 * If a value in the first column is an orgId within the current org then it is
 * unchanged. If it is an exact match on the title of an organisation within the
 * org then that orgId is returned.
 *
 * @author brad
 */
public class OrganisationsMatchHelperCsv extends AbstractResource implements GetableResource, PostableResource {

    private static final Logger log = LoggerFactory.getLogger(OrganisationsMatchHelperCsv.class);
    private final String name;
    private final CommonCollectionResource parent;
    private List<String[]> results = new ArrayList<>();

    public OrganisationsMatchHelperCsv(String name, CommonCollectionResource parent) {
        this.parent = parent;
        this.name = name;
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        log.info("processForm: " + parameters.size() + " files:" + files.size());
        Session session = SessionManager.session();

        if (!files.isEmpty()) {
            try {
                FileItem file = files.values().iterator().next();
                doUpload(file, session);

            } catch (Exception ex) {
                log.warn("Exception processing", ex);
            }
        } else {
            log.warn("No files uploaded for processing");
        }
        return null;
    }

    @Override
    public AccessControlledResource.Priviledge getRequiredPostPriviledge(Request request) {
        return AccessControlledResource.Priviledge.WRITE_CONTENT;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        try (PrintWriter pw = new PrintWriter(out)) {
            CSVWriter writer = new CSVWriter(pw);
            writer.writeAll(results);
            pw.flush();
        }
    }

    public String getTitle() {
        return "Organisation match helper CSV";
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

    private void doUpload(FileItem file, Session session) throws IOException {
        log.info("doUpload: ");
        InputStream in = null;
        try {
            in = file.getInputStream();
            fromCsv(in, session);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    public void fromCsv(InputStream in, Session session) throws IOException {
        InputStreamReader r = new InputStreamReader(in);
        CSVReader reader = new CSVReader(r);

        String[] lineParts;
        int line = 0;
        Organisation rootOrg = getOrganisation();
        log.info("fromCsv: rootOrg=" + rootOrg.getOrgId());
        while ((lineParts = reader.readNext()) != null) {
            if (lineParts.length > 0) {
                line++;
                if (log.isTraceEnabled()) {
                    log.trace("process line: " + line + " : " + Arrays.toString(lineParts));
                }
                List<String> lineList = new ArrayList<>();
                lineList.addAll(Arrays.asList(lineParts));
                if (lineList.size() > 0 && lineList.get(0).length() > 0) {
                    String result = doProcess(rootOrg, lineList, line, session);
                    if (result == null) {
                        result = "";
                    }
                    lineList.add(0, result); // add validation result to head of list
                    String[] arr = new String[lineList.size()];
                    lineList.toArray(arr);
                    results.add(arr);

                }
            }
        }
    }

    /**
     * Attempt to match the organisation in the first column
     *
     * @param rootOrg
     * @param lineList
     * @param line
     * @param session
     * @return - the result of the match successful if there is a message for
     * the user, or null for an exact match
     */
    private String doProcess(Organisation rootOrg, List<String> lineList, int line, Session session) {
        String orgIdOrTitle = lineList.get(0);
        if (StringUtils.isBlank(orgIdOrTitle)) {
            // Nothing to do
            return null;
        }
        log.info("doProcess: orgId=" + orgIdOrTitle);
        Organisation child = Organisation.findByOrgId(orgIdOrTitle, session);
        if (child != null) {
            // exact match on orgId, perfect
            return null;
        }
        List<Organisation> orgs = Organisation.search(orgIdOrTitle, rootOrg, null, session);
        if (orgs.isEmpty()) {
            // if contains commas then it could be that the title is concatenated with other stuff, like user name. So iterate
            if( orgIdOrTitle.contains(",")) {
                String[] arr = orgIdOrTitle.split(",");
                orgs = new ArrayList<>();
                for( String s : arr ) {
                    List<Organisation> found = Organisation.search(s, rootOrg, null, session);
                    orgs.addAll(found);
                }
            }
        }
        
        if (orgs.isEmpty()) {            
            lineList.remove(0);
            lineList.add(0, "unk001"); // TODO: make parameter
            return "Couldnt find: " + orgIdOrTitle;
        } else if (orgs.size() == 1) {
            Organisation found = orgs.get(0);
            lineList.remove(0);
            lineList.add(0, found.getOrgId());
            return "Found: " + found.getTitle();
        } else {
            // select the first one as default
            Organisation found = orgs.get(0);
            lineList.remove(0);
            lineList.add(0, found.getOrgId());
            
            String matches = "";
            for (Organisation org : orgs) {
                matches += org.getTitle() + " - " + org.getOrgId() + "\n";
            }
            return "Found more then one potential match for" + orgIdOrTitle + "\n" + matches;
        }
    }
}
