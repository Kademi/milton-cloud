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

import io.milton.cloud.util.ScriptExecutor;
import io.milton.http.DateUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

/**
 *
 * @author brad
 */
public class MediaInfoRunner {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(MediaInfoRunner.class);
    private String process = "mediainfo";

    public MediaInfo getInfo(File mediaFile) throws IOException {
        List<String> args = new ArrayList<>();
        args.add(mediaFile.getCanonicalPath());
        ScriptExecutor script = new ScriptExecutor(process, args, 0);
        try {
            System.out.println("exec: " + script);
            script.exec();
            System.out.println("finished running script");
        } catch (Exception ex) {
            log.error("Exception running: " + process, ex);
        }
        System.out.println("parse output");
        return parseOutput(script.getOutput());
    }

    public String getProcess() {
        return process;
    }

    public void setProcess(String process) {
        this.process = process;
    }

    public MediaInfo parseOutput(String output) throws IOException {
        if (output == null) {
            return null;
        }
        StringReader sr = new StringReader(output);
        BufferedReader br = new BufferedReader(sr);
        String line = br.readLine();
        MediaInfo info = new MediaInfo();
        while (line != null) {
            //System.out.println("line: " + line);
            process(info, line);
            line = br.readLine();
        }
        //System.out.println("done parseoutput");
        return info;
    }

    private void process(MediaInfo info, String line) {
        int pos = line.indexOf(":");
        if (pos < 0) {
            return;
        }
        String key = line.substring(0, pos - 1).trim();
        String value = line.substring(pos + 1).trim();
        if ("Duration".equals(key)) {
            Double durationSecs = parseDuration(value);
            info.setDurationSecs(durationSecs);
        } else if ("Encoded date".equals(key)) {
            Date dt = parseDate(value);
            info.setRecordedDate(dt);
        } else if ("Width".equals(key)) {
            Integer i = parsePixels(value);
            info.setWidth(i);
        } else if ("Height".equals(key)) {
            Integer i = parsePixels(value);
            info.setHeight(i);            
        }

    }

    /**
     * example input: 13s 959ms or 1mn 41s
     *
     * @param value
     * @return
     */
    public Double parseDuration(String value) {
        String[] arr = value.split(" ");
        double d = 0;
        for (String s : arr) {
            if (s.endsWith("ms")) {
                s = s.replace("ms", "");
                d += Double.valueOf(s)/1000;
            } else if (s.endsWith("s")) {
                s = s.substring(0, s.length() - 1);
                int secs = Integer.parseInt(s);
                d += secs;
            } else if (s.endsWith("mn") || s.endsWith("minutes")) {
                s = s.substring(0, s.indexOf("m"));
                int mins = Integer.parseInt(s);
                d += mins * 60;
            } else {
                log.error("Unsupported duration component: " + s + " in " + value);
                return null;
            }
        }
        return d;
    }

    /**
     * example input -UTC 2011-01-09 23:00:14 (9th Jan, 2011)
     *
     * @param value
     * @return
     */
    public Date parseDate(String value) {
        try {
            String[] arr = value.split(" ");
            String sTimezone = arr[0];
            String sDate = arr[1];
            String sTime = arr[2];
            arr = sDate.split("-");
            int year = Integer.parseInt(arr[0]);
            int month = Integer.parseInt(arr[1]);
            int day = Integer.parseInt(arr[2]);
            arr = sTime.split(":");
            int hour = Integer.parseInt(arr[0]);
            int minute = Integer.parseInt(arr[1]);
            int second = Integer.parseInt(arr[2]);

            Calendar cal = Calendar.getInstance();
            TimeZone tz = TimeZone.getTimeZone(sTimezone);
            cal.setTimeZone(tz);

            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month - 1);
            cal.set(Calendar.DAY_OF_MONTH, day);
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, minute);
            cal.set(Calendar.SECOND, second);

            return cal.getTime();
        } catch (Exception e) {
            log.warn("Exception parsing date: " + value, e);
            return null;
        }
    }

    public Integer parsePixels(String value) {
        if (value == null || !value.contains(" ")) {
            return null;
        }
        int pos = value.lastIndexOf(" ");
        String s = value.substring(0, pos);
        s = s.replace(" ", "");
        if (s.length() > 0) {
            return Integer.parseInt(s);
        } else {
            return null;
        }
    }
}
