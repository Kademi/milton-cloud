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
package io.milton.cloud.server.web.reporting;

import au.com.bytecode.opencsv.CSVWriter;
import io.milton.cloud.server.web.JsonResult;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Website;
import java.io.OutputStream;
import java.util.Date;

/**
 *
 * @author brad
 */
public interface JsonReport {
    
    /**
     * Just a simple unique identifier for this report
     * 
     * @return 
     */
    String getReportId();
    
    /**
     * Run the report and return the data in the GraphData object. If there
     * are problems you can set messages as appropriate in the jsonResult
     * 
     * @param website - the organisation this report is being run for
     * @param website - null for an organisation query, otherwise the website to limit data to
     * @param start - null if none given, otherwise start date of the period
     * @param finish - null if none given, otherwise finish date of the period
     * @param jsonResult - null if the report could not be run, otherwise the data to
     * graph
     * @return 
     */
    GraphData runReport(Organisation org, Website website, Date start, Date finish, JsonResult jsonResult);

    /**
     * Get the title for this report, given the organisation and website
     * 
     * @param organisation
     * @param website
     * @return 
     */
    String getTitle(Organisation organisation, Website website);
    
    /**
     * Run the report, but output in CSV format
     * 
     * @param org
     * @param website
     * @param start
     * @param finish
     * @param writer 
     */
    void runReportCsv(Organisation org, Website website, Date start, Date finish, CSVWriter writer);
    
    void writeChartAsPng(Organisation org, Website website, Date start, Date finish, OutputStream out);
}
