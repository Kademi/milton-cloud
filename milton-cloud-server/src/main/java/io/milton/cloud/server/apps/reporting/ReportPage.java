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

import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.JsonResult;
import io.milton.cloud.server.web.TemplatedHtmlPage;
import io.milton.cloud.server.web.reporting.GraphData;
import io.milton.cloud.server.web.reporting.JsonReport;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.vfs.db.Website;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 *
 * @author brad
 */
public class ReportPage extends TemplatedHtmlPage {

    private final SimpleDateFormat sdf;
    private final JsonReport jsonReport;
    private final Website website;

    public ReportPage(String name, CommonCollectionResource parent, String title, JsonReport jsonReport, Website website) {
        super(name, parent, "reporting/timeReport", title);
        this.website = website;
        this.jsonReport = jsonReport;
        setForceLogin(true);
        sdf = new SimpleDateFormat("dd/MM/yyyy");
        sdf.setLenient(false);
    }
    

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        if (params.containsKey("startDate")) {
            Date start;
            Date finish;
            try {
                String sStart = params.get("startDate");
                start = parseDate(sStart);
                System.out.println("start: " + start);
                String sFinish = params.get("finishDate");
                finish = parseDate(sFinish);
                System.out.println("finish: " + finish);
                JsonResult jsonResult = new JsonResult(true);
                GraphData graphData = jsonReport.runReport(getOrganisation(), website, start, finish, jsonResult);
                if( jsonResult.isStatus() ) {
                    jsonResult.setData(graphData);
                }
                jsonResult.write(out);
            } catch (ParseException parseException) {
                JsonResult jsonResult = new JsonResult(false, "Invalid date: " + parseException.getMessage());
                jsonResult.write(out);
                return;
            }            
        } else {
            MenuItem.setActiveIds("menuReporting", "menuReportingHome", "menuReportsWebsite" + website.getName());
            super.sendContent(out, range, params, contentType);
        }
    }


    private Date parseDate(String s) throws ParseException {
        return sdf.parse(s);
    }
}
