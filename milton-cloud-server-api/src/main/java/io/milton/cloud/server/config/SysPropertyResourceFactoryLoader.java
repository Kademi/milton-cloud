/*
 * Copyright (C) 2012 McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.config;

import io.milton.http.MultipleResourceFactory;
import io.milton.servlet.StaticResourceFactory;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class SysPropertyResourceFactoryLoader {
    private Logger log = LoggerFactory.getLogger( SysPropertyResourceFactoryLoader.class );

    public static final String EXTRA_ROOT_SYS_PROP_NAME = "extra.web.resources.location";
    
    private final MultipleResourceFactory multipleResourceFactory;

    private String extraFileRoots;

    public SysPropertyResourceFactoryLoader(MultipleResourceFactory multipleResourceFactory) {
        this.multipleResourceFactory = multipleResourceFactory;
        log.info("Check system property: " + EXTRA_ROOT_SYS_PROP_NAME);
        extraFileRoots = System.getProperty(EXTRA_ROOT_SYS_PROP_NAME);
        if( extraFileRoots != null ) {
            log.info("Got value from system property: " + extraFileRoots);
        }
    }
    

    public void init() {
        if( extraFileRoots == null || extraFileRoots.length() == 0 ) {
            log.info("No configured extra file roots");
            return ;
        }
        log.info("Injecting extra file roots: " + extraFileRoots);
        for(String s : extraFileRoots.split(",")) {
            s = s.trim();
            if( s.length() > 0 ) {
                File root = new File(s);
                if( root.exists() ) {
                    if( root.isDirectory() ) {
                        StaticResourceFactory rf = new StaticResourceFactory("/", root);
                        multipleResourceFactory.addAsFirst(rf);
                    } else {
                        log.warn("Extra file root is not a directory: " + root.getAbsolutePath());
                    }
                } else {
                    log.warn("Extra file root does not exist: " + root.getAbsolutePath());
                }
            }
        }
    }
    
    public String getExtraFileRoots() {
        return extraFileRoots;
    }

    public void setExtraFileRoots(String extraFileRoots) {
        this.extraFileRoots = extraFileRoots;
    }
    
    
    
    
}
