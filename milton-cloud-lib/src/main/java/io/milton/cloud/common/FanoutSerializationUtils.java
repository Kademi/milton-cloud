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
package io.milton.cloud.common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import org.hashsplit4j.api.Fanout;
import org.hashsplit4j.api.FanoutImpl;

/**
 *
 * @author brad
 */
public class FanoutSerializationUtils {

    public static void writeFanout(List<String> childCrcs, long actualContentLength, OutputStream bout) throws IOException {
        bout.write((actualContentLength + "").getBytes());
        for (String l : childCrcs) {
            bout.write("\n".getBytes());
            bout.write(l.getBytes());
        }
        bout.flush();
    }

    public static Fanout readFanout(InputStream bin) throws IOException {
        List<String> list = new ArrayList<>();
        InputStreamReader r = new InputStreamReader(bin);
        BufferedReader br = new BufferedReader(r);
        String line = br.readLine();
        if (line == null) {
            throw new RuntimeException("First line should have actual content length");
        }
        Long actualContentLength = Long.parseLong(line);
        line = br.readLine();
        while (line != null) {
            list.add(line);
            line = br.readLine();
        }

        return new FanoutImpl(list, actualContentLength);

    }
}
