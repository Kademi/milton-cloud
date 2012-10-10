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
package io.milton.cloud.server.apps.admin;

import io.milton.cloud.server.apps.reporting.TimeDataPointBean;
import io.milton.cloud.server.db.AccessLog;
import io.milton.cloud.server.web.JsonResult;
import io.milton.cloud.server.web.reporting.GraphData;
import io.milton.cloud.server.web.reporting.JsonReport;
import io.milton.cloud.server.web.templating.Formatter;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Website;
import io.milton.vfs.db.utils.SessionManager;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import static io.milton.context.RequestContext._;

/**
 *
 * @author brad
 */
public class WebsiteAccessReport implements JsonReport{

    @Override
    public String getReportId() {
        return "websiteAccess";
    }

    @Override
    public String getTitle(Organisation organisation, Website website) {
        if( website != null ) {
            return "Web activity report: " + website.getName();
        } else {
            return "Web activity report: " + (organisation.getTitle() == null ? organisation.getName() : organisation.getTitle());
        }
    }
    
    
    

    @Override
    public GraphData runReport(Organisation org, Website website, Date start, Date finish, JsonResult jsonResult) {
        System.out.println("runReport: " + start + " - " + finish);
        Session session = SessionManager.session();
        Criteria crit = session.createCriteria(AccessLog.class)
                .setProjection(Projections.projectionList()
                .add(Projections.min("reqDate"))
                .add(Projections.rowCount())
                .add(Projections.groupProperty("reqYear"))
                .add(Projections.groupProperty("reqMonth"))
                .add(Projections.groupProperty("reqDay")));
        if (start != null) {
            crit.add(Restrictions.ge("reqDate", start));
        }
        if (finish != null) {
            crit.add(Restrictions.le("reqDate", finish));
        }
        if( website != null ) {
            crit.add(Restrictions.eq("website", website));
        }
        if( org != null ) {
            crit.add(Restrictions.eq("organisation", org));
        }
        crit.add(Restrictions.eq("contentType", "text/html")); // might need to include application/html at some point...
        List list = crit.list();
        List<TimeDataPointBean> dataPoints = new ArrayList<>();
        Formatter f = _(Formatter.class);
        for (Object oRow : list) {
            Object[] arr = (Object[]) oRow;
            Date date = (Date) arr[0];            
            Long count = f.toLong(arr[1]);
            TimeDataPointBean b = new TimeDataPointBean();
            b.setDate(date.getTime());
            b.setValue(count);
            dataPoints.add(b);
        }
        GraphData graphData = new GraphData();
        graphData.setData(dataPoints);
        String[] labels = {"Hits"};
        graphData.setLabels(labels);
        graphData.setXkey("date");
        String[] ykeys = {"value"};
        graphData.setYkeys(ykeys);
        return graphData;
    }
    
}
