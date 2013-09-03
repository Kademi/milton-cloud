/*
 * Copyright 2013 McEvoy Software Ltd.
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
package io.milton.cloud.server.db;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author brad
 */
public class ScheduledEmailTest {
    
    private static final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    
    public ScheduledEmailTest() {
    }

    @Test
    public void testNextRun_Hourly() throws ParseException {
        Date lastRun = sdf.parse("1/1/2000 13:00");
        ScheduledEmail e = new ScheduledEmail();
        e.setFrequency(ScheduledEmail.Frequency.HOURLY);
        e.setPeriodMultiples(3);
        Date nextRun = e.nextRun(lastRun);
        Date expectedNextRun = DateUtils.addHours(lastRun, 3);
        assertEquals(expectedNextRun, nextRun);
    }
    
    @Test
    public void testNextRun_Daily() throws ParseException {
        Date lastRun = sdf.parse("1/1/2000 13:00");
        ScheduledEmail e = new ScheduledEmail();
        e.setFrequency(ScheduledEmail.Frequency.DAILY);
        e.setPeriodMultiples(3);
        e.setRunHour(16);
        Date nextRun = e.nextRun(lastRun);
        Date expectedNextRun = DateUtils.addDays(lastRun, 3); // period multpls
        expectedNextRun = DateUtils.addHours(expectedNextRun, 3); // run hour
        assertEquals(expectedNextRun, nextRun);
    }    
    
    @Test
    public void testNextRun_Weekly() throws ParseException {
        Date lastRun = sdf.parse("1/1/2000 13:00");
        ScheduledEmail e = new ScheduledEmail();
        e.setFrequency(ScheduledEmail.Frequency.WEEKLY);
        e.setPeriodMultiples(2); // 2 weeks
        e.setRunHour(16);
        Date nextRun = e.nextRun(lastRun);
        Date expectedNextRun = DateUtils.addDays(lastRun, 14); // 2 weeks = 14 days
        expectedNextRun = DateUtils.addHours(expectedNextRun, 3); // run hour
        assertEquals(expectedNextRun, nextRun);
    }        
    
    @Test
    public void testNextRun_Monthly() throws ParseException {
        Date lastRun = sdf.parse("1/1/2000 13:00");
        Date expectedNextRun = sdf.parse("1/2/2000 16:00");
        ScheduledEmail e = new ScheduledEmail();
        e.setFrequency(ScheduledEmail.Frequency.MONTHLY);
        e.setPeriodMultiples(1);
        e.setRunHour(16);
        Date nextRun = e.nextRun(lastRun);
        assertEquals(expectedNextRun, nextRun);
    }   
    
    @Test
    public void testNextRun_Yearly() throws ParseException {
        Date lastRun = sdf.parse("1/1/2000 13:00");
        Date expectedNextRun = sdf.parse("1/1/2001 16:00");
        ScheduledEmail e = new ScheduledEmail();
        e.setFrequency(ScheduledEmail.Frequency.ANNUAL);
        e.setPeriodMultiples(1);
        e.setRunHour(16);
        Date nextRun = e.nextRun(lastRun);
        assertEquals(expectedNextRun, nextRun);
    }       
}