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
import io.milton.resource.Resource;
import java.io.*;
import java.net.URL;
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
import java.net.MalformedURLException;
import javax.servlet.ServletContext;

/**
 * Templater for flat text files, such as css. Will locate templates in either
 * classpath or configured file roots.
 *
 * @author brad
 */
public class TextTemplater implements Templater {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TextTemplater.class);
    private List<File> roots;
    private final SimpleTemplateLoader templateLoader;
    private final VelocityEngine engine;
    private final SpliffySecurityManager securityManager;
    private final ServletContext servletContext;

    public TextTemplater(SpliffySecurityManager securityManager, ServletContext servletContext) {
        this.securityManager = securityManager;
        this.servletContext = servletContext;
        templateLoader = new SimpleTemplateLoader();
        java.util.Properties p = new java.util.Properties();
        p.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogSystem");
        engine = new VelocityEngine(p);
        engine.setProperty("resource.loader", "mine");
        engine.setProperty("mine.resource.loader.instance", templateLoader);

        roots = new ArrayList<>();
        File fWebappRoot = new File(servletContext.getRealPath("/"));
        roots.add(fWebappRoot);
        log.info("Using webapp root dir: " + fWebappRoot.getAbsolutePath());


        String extraRoots = System.getProperty(HtmlTemplater.ROOTS_SYS_PROP_NAME);
        if (extraRoots != null && !extraRoots.isEmpty()) {
            String[] arr = extraRoots.split(",");
            for (String s : arr) {
                File root = new File(s);
                if (!root.exists()) {
                    throw new RuntimeException("Root template dir specified in system property does not exist: " + root.getAbsolutePath() + " from property value: " + extraRoots);
                }
                roots.add(root);
            }
        }
    }

    @Override
    public void writePage(String templatePath, Resource aThis, Map<String, String> params, OutputStream out) throws IOException {
        if (!templatePath.startsWith("/")) {
            templatePath = "/templates/apps/" + templatePath;
        }
        Template template = engine.getTemplate(templatePath);
        RootFolder rootFolder = WebUtils.findRootFolder(aThis);
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

    public List<File> getTemplateFileRoots() {
        return roots;
    }

    public void setTemplateFileRoots(List<File> roots) {
        this.roots = roots;
    }

    public VelocityEngine getEngine() {
        return engine;
    }
    
    

    class SimpleTemplateLoader extends org.apache.velocity.runtime.resource.loader.ResourceLoader {

        @Override
        public void init(ExtendedProperties ep) {
        }

        @Override
        public InputStream getResourceStream(String path) throws ResourceNotFoundException {
            log.info("SimpleTemplateLoader: getResourceStream: " + path);
            TemplateSource source = findTemplateSource(path);
            if (source != null) {
                try {
                    return source.getInputStream();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                throw new ResourceNotFoundException("Not found: " + path);
            }
        }

        @Override
        public boolean isSourceModified(org.apache.velocity.runtime.resource.Resource resource) {
            return getLastModified(resource) != resource.getLastModified();
        }

        @Override
        public long getLastModified(org.apache.velocity.runtime.resource.Resource resource) {
            TemplateSource source = findTemplateSource(resource.getName());
            if (source != null) {
                return source.getTimestamp();
            } else {
                throw new RuntimeException("Not found: " + resource.getName());
            }

        }

        public TemplateSource findTemplateSource(String path) {
            log.info("findTemplateSource: " + path);

            if (roots != null) {
                for (File root : roots) {
                    File templateFile = new File(root, path);
                    if (templateFile.exists()) {
                        return new FileTemplateSource(templateFile);
                    }
                }
            }
            URL resource;
            if (path.startsWith("/")) {                
                try {
                    resource = servletContext.getResource(path);
                } catch (MalformedURLException ex) {
                    throw new RuntimeException(ex);
                }
                if (resource != null) {
                    return new ClassPathTemplateSource(resource);
                }
            }

            resource = this.getClass().getResource(path);
            if (resource != null) {
                return new ClassPathTemplateSource(resource);
            }
            return null;
        }
    }

    private interface TemplateSource {

        InputStream getInputStream() throws IOException;

        long getTimestamp();
    }

    private class ClassPathTemplateSource implements TemplateSource {

        private final URL resource;
        private final long timestamp;

        public ClassPathTemplateSource(URL resource) {
            this.resource = resource;
            this.timestamp = System.currentTimeMillis();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return resource.openStream();
        }

        @Override
        public long getTimestamp() {
            return timestamp;
        }
    }

    private class FileTemplateSource implements TemplateSource {

        private final File file;

        public FileTemplateSource(File file) {
            this.file = file;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            FileInputStream in = new FileInputStream(file);
            return in;
        }

        @Override
        public long getTimestamp() {
            return file.lastModified();
        }
    }
}
