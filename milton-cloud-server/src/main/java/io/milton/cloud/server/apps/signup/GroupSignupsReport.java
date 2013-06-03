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
package io.milton.cloud.server.apps.signup;

import au.com.bytecode.opencsv.CSVWriter;
import io.milton.cloud.util.TimeDataPointBean;
import io.milton.cloud.server.db.SignupLog;
import io.milton.cloud.server.web.JsonResult;
import io.milton.cloud.server.web.reporting.GraphData;
import io.milton.cloud.server.web.reporting.JsonReport;
import io.milton.cloud.server.web.templating.Formatter;
import io.milton.vfs.db.Group;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Website;
import io.milton.vfs.db.utils.SessionManager;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.milton.context.RequestContext._;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import org.hibernate.criterion.Restrictions;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 *
 * @author brad
 */
public class GroupSignupsReport implements JsonReport {

    private static final Logger log = LoggerFactory.getLogger(GroupSignupsReport.class);

    @Override
    public String getReportId() {
        return "groupSignups";
    }

    @Override
    public String getTitle(Organisation organisation, Website website) {
        if (website != null) {
            return "Group signups report: " + website.getName();
        } else {
            return "Group signups report: " + (organisation.getTitle() == null ? organisation.getOrgId() : organisation.getTitle());
        }
    }

    @Override
    public GraphData runReport(Organisation org, Website website, Date start, Date finish, JsonResult jsonResult) {
        log.info("runReport: " + start + " - " + finish);
        Session session = SessionManager.session();
        Formatter f = _(Formatter.class);
        Criteria crit = session.createCriteria(SignupLog.class)
                .setProjection(Projections.projectionList()
                .add(Projections.min("reqDate"))
                .add(Projections.rowCount())
                .add(Projections.groupProperty("groupEntity"))
                .add(Projections.groupProperty("reqYear"))
                .add(Projections.groupProperty("reqMonth"))
                .add(Projections.groupProperty("reqDay")));
        if (start != null) {
            crit.add(Restrictions.ge("reqDate", start));
        }
        if (finish != null) {
            crit.add(Restrictions.le("reqDate", finish));
        }
        if (website != null) {
            crit.add(Restrictions.eq("website", website));
        }
        if (org != null) {
            crit.add(Restrictions.eq("organisation", org));
        }
        List list = crit.list();
        Set<String> groupsInSeries = new HashSet<>();
        List<Map<String, Object>> dataPoints = new ArrayList<>();
        Map<Long, Map<String, Object>> mapOfDataPointsByTime = new HashMap<>();
        log.info("results: " + list.size());
        for (Object oRow : list) {
            Object[] arr = (Object[]) oRow;
            Date date = (Date) arr[0];
            date = GraphData.stripTime(date);
            Long time = date.getTime();
            Long count = f.toLong(arr[1]);
            Group group = (Group) arr[2];

            groupsInSeries.add(group.getName()); // keep a set of group names for labels

            Map<String, Object> dataPoint = mapOfDataPointsByTime.get(time);
            if (dataPoint == null) {
                dataPoint = new HashMap<>();
                dataPoint.put("date", time);
                mapOfDataPointsByTime.put(time, dataPoint);
                dataPoints.add(dataPoint);
            }
            dataPoint.put(group.getName(), count);

        }
        GraphData graphData = new GraphData();
        graphData.setData(dataPoints);
        String[] ykeys = new String[groupsInSeries.size()];
        graphData.setLabels(ykeys);
        graphData.setXkey("date");
        groupsInSeries.toArray(ykeys);
        graphData.setYkeys(ykeys);
        log.info("data points: " + dataPoints.size());
        return graphData;
    }

    @Override
    public void runReportCsv(Organisation org, Website website, Date start, Date finish, CSVWriter writer) {
        JsonResult jsonResult = new JsonResult();
        GraphData<Map<String, Object>> graphData = runReport(org, website, start, finish, jsonResult);
        List<Map<String, Object>> data = graphData.getData();
        List<String> line = new ArrayList<>();
        line.add(graphData.getXkey());
        for (String s : graphData.getLabels()) {
            line.add(s);
        }
        writer.writeNext(GraphData.toArray(line));

        for (Map<String, Object> row : data) {
            line = new ArrayList<>();
            Object x = row.get(graphData.getXkey());
            line.add(GraphData.formatDateValue(x));

            for (String s : graphData.getLabels()) {
                Object y = row.get(s);
                line.add(GraphData.formatValue(y));
            }
            writer.writeNext(GraphData.toArray(line));
        }
    }

    @Override
    public void writeChartAsPng(Organisation org, Website website, Date start, Date finish, OutputStream out) {
        String title = "Signups for";
        if (website != null) {
            title += " " + website.getName();
        } else {
            title += " all of " + org.getFormattedName();
        }
        XYSeriesCollection xyDataset = new XYSeriesCollection();

        JsonResult jsonResult = new JsonResult();
        GraphData<Map<String, Object>> graphData = runReport(org, website, start, finish, jsonResult);
        List<Map<String, Object>> data = graphData.getData();

        int i = 0;
        for (String seriesKey : graphData.getYkeys()) {
            String seriesTitle = graphData.getLabels()[i++];
            XYSeries dataSeries = new XYSeries(seriesTitle);
            xyDataset.addSeries(dataSeries);
            for (Map<String, Object> row : data) {
                Long val = (Long) row.get(seriesKey);
                if (val != null) {
                    Long date = (Long) row.get(graphData.getXkey());
                    dataSeries.add(date, val);
                }
            }
        }

        JFreeChart chart = ChartFactory.createXYLineChart(title, "Date", "Number of signups", xyDataset, PlotOrientation.VERTICAL, true, false, false);
        
        XYPlot plot = chart.getXYPlot();
        DateAxis dateAxis = new DateAxis("Date");
        DateTickUnit unit = new DateTickUnit(DateTickUnitType.DAY, 1);
        DateFormat chartFormatter = new SimpleDateFormat("dd/MM/yyyy");
        dateAxis.setDateFormatOverride(chartFormatter);
        dateAxis.setTickUnit(unit);
        plot.setDomainAxis(dateAxis);
        
        //JFreeChart chart = new JFreeChart("Hits", JFreeChart.DEFAULT_TITLE_FONT, plot, false);
        //chart.setBackgroundPaint(java.awt.Color.WHITE);
        try {
            ChartUtilities.writeChartAsPNG(out, chart, 600, 400);
            out.flush();
        } catch (IOException e) {
            log.error("Exception rendering chart", e);
        }
    }
}
