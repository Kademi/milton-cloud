/*
 * Copyright 2012 McEvoy Software Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.apps.reporting;

import au.com.bytecode.opencsv.CSVWriter;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.TemplatedHtmlPage;
import io.milton.cloud.server.web.WebUtils;
import io.milton.cloud.server.web.reporting.JsonReport;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.vfs.db.Website;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;

/**
 *
 * @author brad
 */
public class ReportCsvPage extends TemplatedHtmlPage implements IReportPage {

    private final JsonReport jsonReport;
    private final Website website;

    public ReportCsvPage(String name, CommonCollectionResource parent, String title, JsonReport jsonReport, Website website) {
        super(name, parent, "reporting/timeReport", title);
        this.website = website;
        this.jsonReport = jsonReport;
        setForceLogin(true);
    }    

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {

        Date start;
        Date finish;
        try {
            // Note that the scheduled app uses fromDate/toDate parameters
            String sStart = WebUtils.getParam(params, "startDate");
            if( sStart == null ) {
                sStart = WebUtils.getParam(params, "fromDate");                
            }
            start = parseDate(sStart);
            String sFinish = WebUtils.getParam(params, "finishDate"); 
            if( sFinish == null ) {
                sFinish = WebUtils.getParam(params, "toDate"); 
            }
            finish = parseDate(sFinish);

            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(out);
            CSVWriter writer = new CSVWriter(outputStreamWriter);
            jsonReport.runReportCsv(getOrganisation(), website, start, finish, writer);
            writer.flush();
            outputStreamWriter.flush();
        } catch (ParseException parseException) {
            throw new RuntimeException(parseException);
        }
    }

    private Date parseDate(String s) throws ParseException {
        if (s == null || s.trim().length() == 0) {
            return null;
        }
        Date dt = ReportingApp.sdf().parse(s);
        return dt;
    }

    @Override
    public String getContentType(String accepts) {
        return "text/csv";
    }
    
    @Override
    public boolean isAttachable() {
        return true;
    }
    
}
