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
package io.milton.cloud.server.apps.website;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Processes a LESS css file to find parameters and their values
 *
 * @author brad
 */
public class LessParameterParser {

    public void findParams(InputStream in, Map<String, String> map) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line = reader.readLine();
        while (line != null) {
            line = line.trim();
            if (line.startsWith("@") && line.contains(":")) {
                String[] arr = line.split(":");
                String key = arr[0].trim();
                key = key.substring(1); // drop the @
                String val = arr[1];
                val = val.substring(0, val.length() - 1); // drop the ;
                val = val.trim();
                map.put(key, val);
            }
            line = reader.readLine();
        }
    }

    public void setParams(Map<String, String> params, InputStream in, OutputStream out) throws IOException {
        params = new HashMap<>(params); // make a copy that we can change
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
        if (in != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line = reader.readLine();
            while (line != null) {
                line = line.trim();
                if (line.startsWith("@") && line.contains(":")) {
                    String[] arr = line.split(":");
                    String key = arr[0].trim();
                    key = key.substring(1); // drop the @                
                    if (params.containsKey(key)) {
                        String val = params.get(key);
                        line = "@" + key + ": " + val + ";";
                        params.remove(key); // remove processed entries so we know whats left to add at end
                    }
                    writer.write(line);
                    writer.newLine();
                }
                line = reader.readLine();
            }
        }
        // Now add params not already written
        for (String key : params.keySet()) {
            String val = params.get(key);
            String line = "@" + key + ": " + val + ";";
            writer.write(line);
            writer.newLine();
        }
        writer.flush();
        writer.close();
    }
}
