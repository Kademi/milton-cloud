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
package io.milton.cloud.server.apps;

import io.milton.cloud.server.web.reporting.JsonReport;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Website;
import java.util.List;
import org.hibernate.Criteria;

/**
 * A specialisation of Application which allows it to provide reports to the
 * Reporting application
 *
 * @author brad
 */
public interface ReportingApplication {
    /**
     * Get the reports available for the given org and website
     * 
     * @param org
     * @param website - optional, null for organisation wide context
     * @return 
     */
    List<JsonReport> getReports(Organisation org, Website website);
    
    List<CustomReportDataSource> getDataSources();
    
    public interface CustomReportDataSource {
        public Criteria buildBaseCriteria(Organisation org, Website website);
        
        public List<String> getFieldNames(Organisation org, Website website);
        
        public String getTitle();
        
        public String getId();
    }
    
}
