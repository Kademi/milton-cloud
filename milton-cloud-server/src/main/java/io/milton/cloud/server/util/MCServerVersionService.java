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
package io.milton.cloud.server.util;

import io.milton.cloud.util.ServerVersionService;
import io.milton.webdav.utils.LockUtils;
import java.io.InputStream;
import java.util.Properties;

/**
 *
 * @author brad
 */
public class MCServerVersionService implements ServerVersionService {

    private final String version;

    public MCServerVersionService() {
        String v;
        try {
            InputStream in = LockUtils.class.getResourceAsStream("/META-INF/maven/io.milton/milton-cloud-server/pom.properties");
            if (in != null) {
                Properties props = new Properties();
                props.load(in);
                in.close();
                v = props.getProperty("version");
            } else {
                v = "0.0.0";
            }
        } catch (Exception e) {
            v = "0.0.0";
        }
        version = v;
    }

    @Override
    public String getServerVersion() {
        return version;
    }
}
