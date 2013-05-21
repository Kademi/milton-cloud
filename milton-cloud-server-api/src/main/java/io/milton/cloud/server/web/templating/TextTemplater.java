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
package io.milton.cloud.server.web.templating;

import io.milton.cloud.server.web.RootFolder;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.resource.Resource;
import java.io.*;
import java.util.*;
import org.apache.commons.collections.ExtendedProperties;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.ResourceNotFoundException;
import io.milton.vfs.db.Profile;
import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.cloud.server.web.WebUtils;
import io.milton.common.Path;
import javax.servlet.ServletContext;

import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.GetableResource;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;

/**
 * Templater for flat text files, such as css. Will locate templates in either
 * classpath or configured file roots.
 *
 * @author brad
 */
public class TextTemplater implements Templater {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TextTemplater.class);
    public static final String INJECTED_PATH = "/injected";
    private static ThreadLocal<byte[]> tlInjectedTemplate = new ThreadLocal<>();
    private final SpliffyResourceFactory resourceFactory;
    private final ThemeAwareResourceLoader templateLoader;
    private final VelocityEngine engine;
    private final SpliffySecurityManager securityManager;
    private final long loadedTime = System.currentTimeMillis();

    public static void setInjectedTemplate(byte[] bytes) {
        tlInjectedTemplate.set(bytes);
    }

    public TextTemplater(SpliffySecurityManager securityManager, SpliffyResourceFactory resourceFactory) {
        this.resourceFactory = resourceFactory;
        this.securityManager = securityManager;
        templateLoader = new ThemeAwareResourceLoader();
        java.util.Properties p = new java.util.Properties();
        p.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogSystem");
        engine = new VelocityEngine(p);
        engine.setProperty("resource.loader", "mine");
        engine.setProperty("mine.resource.loader.instance", templateLoader);
        engine.setProperty("mine.resource.loader.cache", "true");
        engine.setProperty("mine.resource.loader.modificationCheckInterval", "5");
    }

    @Override
    public void writePage(String templatePath, Resource aThis, Map<String, String> params, OutputStream out) throws IOException {
        if (!templatePath.startsWith("/")) {
            templatePath = "/templates/apps/" + templatePath;
        }
        RootFolder rootFolder = WebUtils.findRootFolder(aThis);
        templatePath = rootFolder.getDomainName() + ":" + templatePath;
        Template template = engine.getTemplate(templatePath);

        Context datamodel = new VelocityContext();
        datamodel.put("rootFolder", rootFolder);
        datamodel.put("page", aThis);
        datamodel.put("params", params);
        Profile user = securityManager.getCurrentUser();
        if (user != null) {
            datamodel.put("user", user);
        }
        PrintWriter pw = new PrintWriter(out);
        template.merge(datamodel, pw);
        pw.flush();
    }

    public void writePage(String templatePath, Profile currentUser, RootFolder rootFolder, Context context, Writer writer) throws IOException {
        if (!templatePath.startsWith("/")) {
            templatePath = "/templates/apps/" + templatePath;
        }
        templatePath = rootFolder.getDomainName() + ":" + templatePath;
        Template template = engine.getTemplate(templatePath);
        Context datamodel = new VelocityContext(context);
        datamodel.put("rootFolder", rootFolder);
        Profile user = securityManager.getCurrentUser();
        if (user != null) {
            datamodel.put("user", user);
        }
        log.info("writePage: " + templatePath);
        template.merge(datamodel, writer);
        writer.flush();
    }

    public VelocityEngine getEngine() {
        return engine;
    }

    public class ThemeAwareResourceLoader extends ResourceLoader {

        @Override
        public void init(ExtendedProperties configuration) {
        }

        @Override
        public synchronized InputStream getResourceStream(String source) throws ResourceNotFoundException {
            if (TextTemplater.log.isTraceEnabled()) {
                TextTemplater.log.trace("getResourceStream( " + source + ")");
            }
            byte[] bytes;
            if (source.endsWith(INJECTED_PATH)) {
                bytes = tlInjectedTemplate.get();
            } else {
                GetableResource r = getResource(source);
                if (r == null) {
                    return null;
                }

                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                try {
                    r.sendContent(bout, null, null, null);
                } catch (Throwable e) {
                    throw new RuntimeException("Couldnt parse: " + r.getClass(), e);
                }
                bytes = bout.toByteArray();
            }
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public boolean isSourceModified(org.apache.velocity.runtime.resource.Resource resource) {
            if (TextTemplater.log.isTraceEnabled()) {
                TextTemplater.log.info("isSourceModified( " + resource.getName() + ")");
            }
            if (resource.getName().endsWith(INJECTED_PATH)) {
                return true;
            }
            long lastMod = getLastModified(resource);
            return lastMod != resource.getLastModified();
        }

        @Override
        public long getLastModified(org.apache.velocity.runtime.resource.Resource resource) {
            Resource r = getResource(resource.getName());
            if (r == null) {
                return loadedTime;
            } else {
                Date dt = r.getModifiedDate();
                if (dt == null) {
                    if (TextTemplater.log.isTraceEnabled()) {
                        TextTemplater.log.trace("getLastModified( " + resource.getName() + ") = default (mod date is null)");
                    }
                    return loadedTime;
                } else {
                    if (TextTemplater.log.isTraceEnabled()) {
                        TextTemplater.log.trace("getLastModified( " + resource.getName() + ") = " + dt.getTime());
                    }
                    return dt.getTime();
                }
            }
        }

        public GetableResource getResource(String source) {
            if (!source.contains(":")) {
                return null;
            }
            try {
                log.info("getResource source: " + source);
                String[] arr = source.split(":");
                Resource r = resourceFactory.findResource(arr[0], Path.path(arr[1]));
                if (r instanceof GetableResource) {
                    return (GetableResource) r;
                } else {
                    return null;
                }
            } catch (NotAuthorizedException | BadRequestException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
