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

import io.milton.cloud.server.apps.orgs.OrganisationRootFolder;
import io.milton.cloud.server.apps.website.WebsiteRootFolder;
import io.milton.common.Path;
import io.milton.resource.Resource;
import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import java.io.*;
import java.net.URL;
import java.util.*;
import org.apache.commons.collections.ExtendedProperties;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.web.RootFolder;
import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.cloud.server.web.UserResource;
import io.milton.cloud.server.web.WebUtils;

/**
 * Builds pages by plugging a few things in together: - a static skeleton for a
 * HTML page, ie the html, header and body tags - a Theme, which defines web
 * resources, such as js and css files, to include in the header. And which
 * defines one or more layouts for the body. - the template itself, which
 * provides more web resources to be included in the header, and the body layout
 * which will be injected into the theme template. Also defines a parameter
 * indicating which theme template to use
 *
 * @author brad
 */
public class HtmlTemplater implements Templater {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(HtmlTemplater.class);
    public static final String ROOTS_SYS_PROP_NAME = "template.file.roots";
    private List<File> roots;
    private final VelocityEngine engine;
    private final SpliffySecurityManager securityManager;
    private final HtmlTemplateLoader templateLoader;
    private final HtmlTemplateParser templateParser;
    private final HtmlTemplateRenderer templateRenderer;
    private final Map<String, TemplateHtmlPage> cachedTemplateMetaData = new HashMap<>();
    private String defaultTheme = "fuse";
    private Path webRoot = Path.path("/"); 

    public HtmlTemplater(ApplicationManager applicationManager, Formatter formatter, SpliffySecurityManager securityManager) {
        this.securityManager = securityManager;
        templateLoader = new HtmlTemplateLoader();
        engine = new VelocityEngine();
        engine.setProperty("resource.loader", "mine");
        engine.setProperty("mine.resource.loader.instance", new HtmlTemplateLoaderResourceLoader());
        engine.setProperty("userdirective", VelocityContentDirective.class.getName());

        templateParser = new HtmlTemplateParser();
        templateRenderer = new HtmlTemplateRenderer(applicationManager, formatter);
        String extraRoots = System.getProperty(ROOTS_SYS_PROP_NAME);
        if (extraRoots != null && !extraRoots.isEmpty()) {
            String[] arr = extraRoots.split(",");
            roots = new ArrayList<>();
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
        UserResource user = securityManager.getCurrentPrincipal();
        RootFolder rootFolder = WebUtils.findRootFolder(aThis);
        String theme;
        if (rootFolder instanceof OrganisationRootFolder) {
            theme = defaultTheme;
        } else if (rootFolder instanceof WebsiteRootFolder) {
            WebsiteRootFolder websiteFolder = (WebsiteRootFolder) rootFolder;
            theme = websiteFolder.getWebsite().getTheme();
            if (theme == null) {
                theme = defaultTheme;
            }
        } else {
            if( rootFolder == null) {
                throw new RuntimeException("Couldnt find root folder for: "  + aThis.getClass());
            } else {
                throw new RuntimeException("Unknown root folder type: " + rootFolder.getClass());
            }
        }
        if( !templatePath.startsWith("/")) {
            templatePath = "/templates/apps/" + templatePath;
        }        
        templatePath = templatePath + ".html";
        
        Template bodyTemplate = getTemplate(templatePath);
        TemplateHtmlPage bodyTemplateMeta = cachedTemplateMetaData.get(templatePath);
        
        String themeTemplateName = findThemeTemplateName(bodyTemplateMeta);
        templatePath = "/templates/themes/" + theme + "/" + themeTemplateName + ".html";
        Template themeTemplate = getTemplate(templatePath);
        TemplateHtmlPage themeTemplateTemplateMeta = cachedTemplateMetaData.get(templatePath);
        if (themeTemplateTemplateMeta == null) {
            throw new RuntimeException("Couldnt find meta for template: " + templatePath);
        }

        templateRenderer.renderHtml(rootFolder, aThis, params, user, themeTemplate, themeTemplateTemplateMeta, bodyTemplate, bodyTemplateMeta, theme, out);
    }

    private String findThemeTemplateName(TemplateHtmlPage bodyTemplateMeta) {
        for (WebResource wr : bodyTemplateMeta.getWebResources()) {
            if (wr.getTag().equals("link")) {
                String rel = wr.getAtts().get("rel");
                if (rel != null && rel.equals("template")) {
                    String templateName = wr.getAtts().get("href");
                    return templateName;
                }
            }
        }
        return "normal";
    }

    public String getDefaultTheme() {
        return defaultTheme;
    }

    public void setDefaultTheme(String defaultTheme) {
        this.defaultTheme = defaultTheme;
    }

    public String getWebRoot() {
        return webRoot.toString();
    }

    public void setWebRoot(String webRoot) {
        this.webRoot = Path.path(webRoot);
    }

    public List<File> getTemplateFileRoots() {
        return roots;
    }

    public void setTemplateFileRoots(List<File> roots) {
        this.roots = roots;
    }

    private Template getTemplate(String templatePath) {
        return engine.getTemplate(templatePath);
    }

    public class HtmlTemplateLoaderResourceLoader extends ResourceLoader {

        @Override
        public void init(ExtendedProperties configuration) {
            
        }

        @Override
        public InputStream getResourceStream(String source) throws ResourceNotFoundException {
            TemplateHtmlPage meta;
            try {
                meta = templateLoader.findTemplateSource(source);
            } catch (IOException ex) {
                throw new ResourceNotFoundException(source, ex);
            }
            if( meta == null ) {
                throw new ResourceNotFoundException(source);
            }
            return new ByteArrayInputStream(meta.getBody().getBytes());
        }

        @Override
        public boolean isSourceModified(org.apache.velocity.runtime.resource.Resource resource) {
            TemplateHtmlPage meta;
            try {
                meta = templateLoader.findTemplateSource(resource.getName());
                return meta.getTimestamp() != resource.getLastModified();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public long getLastModified(org.apache.velocity.runtime.resource.Resource resource) {
            TemplateHtmlPage meta;
            try {
                meta = templateLoader.findTemplateSource(resource.getName());
                return meta.getTimestamp();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
 
    }

    public class HtmlTemplateLoader {

        public TemplateHtmlPage findTemplateSource(String path) throws IOException {
            log.info("findTemplateSource: " + path);

            Path p = Path.path(path);
            Path webPath = webRoot.add(p).getParent(); // go to parent, because the path is the directory which contains the template

            // First look in the filesystem
            TemplateHtmlPage meta = null;
            if (roots != null) {
                for (File root : roots) {
                    File templateFile = new File(root, path);
                    if (!templateFile.exists()) {
                        log.warn("Template does not exist: " + templateFile.getAbsolutePath());
                    } else {
                        System.out.println("found file: " + templateFile.getAbsolutePath());
                        meta = loadFileMeta(path, templateFile, webPath);
                        break;
                    }
                }
            }
            // if not in filesystem, try classpath
            if (meta == null) {
                String cpPath = path;
                URL resource = this.getClass().getResource(cpPath);
                if (resource != null) {
                    meta = loadClassPathMeta(path, resource, webPath);
                } else {
                    meta = null;
                }
            }

            return meta;
        }

        public long getLastModified(Object o) {
            TemplateHtmlPage templateFile = (TemplateHtmlPage) o;
            return templateFile.getTimestamp();
        }

        public Reader getReader(Object o, String path) throws IOException {
            TemplateHtmlPage meta = (TemplateHtmlPage) o;
            if (meta.getBody() == null) {
                throw new RuntimeException("No template text for: " + meta.getId());
            }
            return new StringReader(meta.getBody());
        }

        private TemplateHtmlPage loadClassPathMeta(String path, URL resource, Path webPath) throws IOException {
            TemplateHtmlPage meta = cachedTemplateMetaData.get(path);
            if (meta == null) {
                meta = new ClassPathTemplateHtmlPage(resource);
                templateParser.parse(meta, webPath); // needs web path to evaluate resource paths in templates
                cachedTemplateMetaData.put(path, meta);
            }
            return meta;
        }

        private TemplateHtmlPage loadFileMeta(String path, File templateFile, Path webPath) throws IOException {
            TemplateHtmlPage meta = cachedTemplateMetaData.get(path);
            if (meta != null) {
                if (meta instanceof FileTemplateHtmlPage) {
                    FileTemplateHtmlPage fmeta = (FileTemplateHtmlPage) meta;
                    if (fmeta.getTimestamp() != templateFile.lastModified()) {
                        cachedTemplateMetaData.remove(path);
                        meta = null;
                    }
                }
            }
            if (meta == null) {
                meta = new FileTemplateHtmlPage(templateFile);
                templateParser.parse(meta, webPath); // needs web path to evaluate resource paths in templates
                cachedTemplateMetaData.put(path, meta);
            }
            return meta;
        }
    }
}
