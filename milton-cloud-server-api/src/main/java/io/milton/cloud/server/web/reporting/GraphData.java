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

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Value object for holding the result of a report run via ajax.
 * 
 * This will be fed into the client side graph generator
 *
 * @author brad
 */
public class GraphData<T> {
    
    /**
     * Since most reports use a day time period, its useful to have a function
     * to strip away the time component of a date
     * 
     * @param date
     * @return 
     */
    public static Date stripTime(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();        
    }    
    
    public static  String[] toArray(List<String> line) {
        String[] arr = new String[line.size()];
        line.toArray(arr);
        return arr;
    }

    public static  String formatValue(Object x) {
        if( x == null ) {
            return "";
        }
        return x.toString();
    }    

    public static String formatDateValue(long date) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }    

    public static String formatDateValue(Object x) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    private String graphType = "Line";
    private List<T> data;
    private String xkey;
    private String[] ykeys;
    private String[] labels;

    private String itemsTitle; // the title displayed on the page for the items section
    private List items;
    private String[] itemFields;
    
    
    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public String getItemsTitle() {
        return itemsTitle;
    }

    public void setItemsTitle(String itemsTitle) {
        this.itemsTitle = itemsTitle;
    }

    public String getGraphType() {
        return graphType;
    }

    public void setGraphType(String graphType) {
        this.graphType = graphType;
    }       
    
    
    /**
     * When a report returns a list of items (eg top 10 urls) this will have
     * the field names
     * 
     * @return 
     */
    public String[] getItemFields() {
        return itemFields;
    }

    public void setItemFields(String[] itemFields) {
        this.itemFields = itemFields;
    }

    /**
     * For reports which want to display a list of items (eg top 10 URLS's)
     * under the graph, set the items here
     * 
     * @return 
     */
    public List getItems() {
        return items;
    }

    public void setItems(List items) {
        this.items = items;
    }        

    public String[] getLabels() {
        return labels;
    }

    public void setLabels(String[] labels) {
        this.labels = labels;
    }

    public String getXkey() {
        return xkey;
    }

    public void setXkey(String xkey) {
        this.xkey = xkey;
    }

    public String[] getYkeys() {
        return ykeys;
    }

    public void setYkeys(String[] ykeys) {
        this.ykeys = ykeys;
    }
    
    
    
    
}
