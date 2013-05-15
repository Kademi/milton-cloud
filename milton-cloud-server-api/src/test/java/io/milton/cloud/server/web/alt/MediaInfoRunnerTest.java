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
package io.milton.cloud.server.web.alt;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author brad
 */
public class MediaInfoRunnerTest {

    MediaInfoRunner runner;

    @Before
    public void setup() {
        runner = new MediaInfoRunner();
    }

    @Test
    public void testGetInfo() throws Exception {
    }

    @Test
    public void testGetProcess() {
    }

    @Test
    public void testSetProcess() {
    }

    @Test
    public void testParseOutput() throws Exception {
        String s = "General\n"
                + "Complete name                            : VID_20110110_120007.3gp\n"
                + "Encoded date                             : UTC 2011-01-09 23:00:14\n"                
                + "Duration                                 : 13s 959ms\n"
                + "Tagged date                              : UTC 2011-01-09 23:00:14\n"
                + "Width                                    : 352 pixels\n"
                + "Height                                   : 1 280 pixels\n"                
                + "Classification                           : (empty)\n";
        MediaInfo info = runner.parseOutput(s);
        assertNotNull(info);
        assertEquals(13, info.getDurationSecs().intValue());
        assertNotNull(info.getRecordedDate());
        assertEquals(352, info.getWidth().intValue());
        assertEquals(1280, info.getHeight().intValue());
    }

    @Test
    public void testParseDuration_Seconds() {
        Double i = runner.parseDuration("13s 959ms   ");
        assertEquals(13.959, i, 0.00001);
    }
    
    @Test
    public void testParseDuration_Minutes() {
        Double i = runner.parseDuration("1mn 41s");
        assertEquals(101,i, 0.00001);
    }
    
    @Test
    public void testParseDate() {
        Date dt = runner.parseDate("UTC 2011-01-09 23:00:14");
        assertNotNull(dt);
        Calendar cal = Calendar.getInstance();
        cal.setTime(dt);
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        assertEquals(2011, cal.get(Calendar.YEAR));
        assertEquals(0, cal.get(Calendar.MONTH));
        assertEquals(9, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
    }
}
