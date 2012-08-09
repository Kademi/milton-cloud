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

import io.milton.cloud.server.db.AccessLog;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.JsonResult;
import io.milton.cloud.server.web.TemplatedHtmlPage;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.vfs.db.utils.SessionManager;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

/**
 *
 * @author brad
 */
public class SiteActivityReportPage extends TemplatedHtmlPage {

    private final SimpleDateFormat sdf;
    
    public SiteActivityReportPage(String name, CommonCollectionResource parent) {
        super(name, parent, "reporting/siteActivity", "Site Activity Report");
        setForceLogin(true);
        sdf = new SimpleDateFormat("dd/MM/yyyy");
        sdf.setLenient(false);
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        if (params.containsKey("startDate")) {
            searchDaily(params, out);
        } else {
            super.sendContent(out, range, params, contentType);
        }
    }

    private void searchDaily(Map<String, String> params, OutputStream out) throws IOException {
        Date start;
        Date finish;
        try {
            String sStart = params.get("startDate");
            start = parseDate(sStart);
            System.out.println("start: " + start);
            String sFinish = params.get("finishDate");
            finish = parseDate(sFinish);
            System.out.println("finish: " + finish);
        } catch (ParseException parseException) {
            JsonResult jsonResult = new JsonResult(false,"Invalid date: " + parseException.getMessage());
            jsonResult.write(out);
            return ;
        }
        Session session = SessionManager.session();
        Criteria crit = session.createCriteria(AccessLog.class)
                .setProjection(Projections.projectionList()
                .add(Projections.min("reqDate"))
                .add(Projections.rowCount())
                .add(Projections.groupProperty("reqYear"))
                .add(Projections.groupProperty("reqMonth"))
                .add(Projections.groupProperty("reqDay"))
                .add(Projections.groupProperty("reqHour")));
        if (start != null) {
            crit.add(Restrictions.ge("reqDate", start));
        }
        if (finish != null) {
            crit.add(Restrictions.le("reqDate", finish));
        }
        List list = crit.list();
        List<TimeDataPointBean> dataPoints = new ArrayList<>();
        for (Object oRow : list) {
            Object[] arr = (Object[]) oRow;
            Date date = (Date) arr[0];
            Integer count = (Integer) arr[1];
            TimeDataPointBean b = new TimeDataPointBean();
            b.setDate(date.getTime());
            b.setValue(count);
            dataPoints.add(b);
        }
        JsonResult jsonResult = new JsonResult(true);
        jsonResult.setData(dataPoints);
        jsonResult.write(out);
    }

    private Date parseDate(String s) throws ParseException {
        return sdf.parse(s);
    }
}
