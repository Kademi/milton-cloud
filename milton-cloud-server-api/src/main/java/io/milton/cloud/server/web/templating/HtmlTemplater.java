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
import io.milton.cloud.server.web.*;
import io.milton.context.RequestContext;

import static io.milton.context.RequestContext._;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import javax.servlet.ServletContext;
import javax.xml.stream.XMLStreamException;

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
public class HtmlTemplater {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(HtmlTemplater.class);
    public static final String ROOTS_SYS_PROP_NAME = "template.file.roots";
    private List<File> roots;
    private final ServletContext servletContext;
    private final VelocityEngine engine;
    private final SpliffySecurityManager securityManager;
    private final HtmlTemplateLoader templateLoader;
    private final HtmlTemplateParser templateParser;
    private final HtmlTemplateRenderer templateRenderer;
    private final Map<String, TemplateHtmlPage> cachedTemplateMetaData = new HashMap<>();
    private String defaultPublicTheme = "fuse";
    private String defaultAdminTheme = "admin";
    private Path webRoot = Path.path("/");

    public HtmlTemplater(ApplicationManager applicationManager, Formatter formatter, SpliffySecurityManager securityManager, ServletContext servletContext) {
        this.servletContext = servletContext;
        this.securityManager = securityManager;
        templateLoader = new HtmlTemplateLoader();
        java.util.Properties p = new java.util.Properties();
        p.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogSystem");
        engine = new VelocityEngine(p);
        engine.setProperty("resource.loader", "mine");
        engine.setProperty("mine.resource.loader.instance", new HtmlTemplateLoaderResourceLoader());
        engine.setProperty("userdirective", VelocityContentDirective.class.getName() + "," + PortletsDirective.class.getName());

        templateParser = new HtmlTemplateParser();
        templateRenderer = new HtmlTemplateRenderer(applicationManager, formatter);

        roots = new ArrayList<>();
        File fWebappRoot = new File(servletContext.getRealPath("/"));
        roots.add(fWebappRoot);
        log.info("Using webapp root dir: " + fWebappRoot.getAbsolutePath());

        String extraRoots = System.getProperty(ROOTS_SYS_PROP_NAME);
        if (extraRoots != null && !extraRoots.isEmpty()) {
            String[] arr = extraRoots.split(",");
            for (String s : arr) {
                File root = new File(s);
                if (!root.exists()) {
                    throw new RuntimeException("Root template dir specified in system property does not exist: " + root.getAbsolutePath() + " from property value: " + extraRoots);
                }
                roots.add(root);
                log.info("Using file template root: " + root.getAbsolutePath());
            }
        }
    }

    /**
     * Tries to find if the page isPublic by checkign to see if it implements
     * CommonResource, and finds an appropriate theme if possible from the root
     * folder
     *
     * @param templatePath
     * @param aThis
     * @param params
     * @param out
     * @throws IOException
     */
    public void writePage(String templatePath, Resource aThis, Map<String, String> params, OutputStream out) throws IOException {
        boolean isPublic = false;
        if (aThis instanceof CommonResource) {
            CommonResource cr = (CommonResource) aThis;
            isPublic = cr.isPublic();
        }
        String theme = findTheme(aThis, isPublic);
        writePage(theme, templatePath, aThis, params, out);
    }

    /**
     * Finds an appropriate theme by looking up the root folder to find if there
     * is a website, and if so it uses the isPublic flag to decide if to use the
     * public or internal theme from the website.
     */
    public void writePage(boolean isPublic, String templatePath, Resource aThis, Map<String, String> params, OutputStream out) throws IOException {
        String theme = findTheme(aThis, isPublic);
        writePage(theme, templatePath, aThis, params, out);
    }

    /**
     * Generate a templated page with the given theme and template. The theme
     * controls the "chrome", ie the menu, header, footer, etc. The template
     * controls the layout of this specific page, ie contacts, calendar, file
     * list, etc
     *
     * @param theme
     * @param templatePath
     * @param aThis
     * @param params
     * @param out
     * @throws IOException
     */
    public void writePage(String theme, String templatePath, Resource aThis, Map<String, String> params, OutputStream out) throws IOException {
        System.out.println("HtmlTempater: writePage: " + theme + ", " + templatePath);
        RootFolder rootFolder = WebUtils.findRootFolder(aThis);
        RequestContext.getCurrent().put(rootFolder);
        if (theme.equals("custom")) {
            RequestContext.getCurrent().put("isCustom", Boolean.TRUE); // can't pass params to velocity template loader, so have to workaround
        }
        String themePath; // path that contains the theme templates Eg /templates/themes/admin/ or /content/theme/
        if (theme.equals("custom")) {
            themePath = "/content/theme/";
        } else {
            themePath = "/templates/themes/" + theme + "/";
        }        
        
        UserResource user = securityManager.getCurrentPrincipal();        
        if (!templatePath.startsWith("/")) {
            if( templatePath.startsWith("theme/")) {
                templatePath = templatePath.replace("theme/", themePath); // Eg change theme/page to /content/theme/page
            } else {
                templatePath = "/templates/apps/" + templatePath; // Eg change admin/manageUsers to /templates/apps/admin/manageUsers
            }
        }
        if( !templatePath.endsWith(".html")) {
            templatePath = templatePath + ".html";
        }

        Template bodyTemplate = getTemplate(templatePath);
        System.out.println("templatePath: " + templatePath);
        TemplateHtmlPage bodyTemplateMeta = cachedTemplateMetaData.get(templatePath);

        String themeTemplateName = findThemeTemplateName(bodyTemplateMeta);
        String themeTemplatePath; // if the given themeTemplateName is an absolute path then use it as is, other prefix with themePath
        if (themeTemplateName.startsWith("/")) {
            themeTemplatePath = themeTemplateName;
        } else {
            themeTemplatePath = themePath + themeTemplateName;
        }
        if( !themeTemplatePath.endsWith(".html")) {
            themeTemplatePath += ".html"; // this class only does html templates
        }
        Template themeTemplate = getTemplate(themeTemplatePath);
        System.out.println("themeTemplatePath: " + themeTemplatePath);
        if (themeTemplate == null) {
            throw new RuntimeException("Couldnt find themeTemplate: " + themeTemplatePath);
        }
        TemplateHtmlPage themeTemplateTemplateMeta = cachedTemplateMetaData.get(themeTemplatePath);
        if (themeTemplateTemplateMeta == null) {
            throw new RuntimeException("Couldnt find meta for template: " + themeTemplatePath);
        }

        templateRenderer.renderHtml(rootFolder, aThis, params, user, themeTemplate, themeTemplateTemplateMeta, bodyTemplate, bodyTemplateMeta, theme, out);
    }

    public String findTheme(Resource r, boolean isPublic) {
        RootFolder rootFolder = WebUtils.findRootFolder(r);
        String theme;
        if (rootFolder instanceof OrganisationRootFolder) {
            theme = defaultAdminTheme;
        } else if (rootFolder instanceof WebsiteRootFolder) {
            WebsiteRootFolder websiteFolder = (WebsiteRootFolder) rootFolder;
            if (isPublic) {
                theme = websiteFolder.getWebsite().getPublicTheme();
            } else {
                theme = websiteFolder.getWebsite().getInternalTheme();
            }
            if (theme == null) {
                theme = defaultPublicTheme;
            }
        } else {
            theme = defaultPublicTheme;
        }
        return theme;
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
        return defaultPublicTheme;
    }

    public void setDefaultTheme(String defaultTheme) {
        this.defaultPublicTheme = defaultTheme;
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
        if (templatePath == null) {
            throw new RuntimeException("templatePath is null");
        }
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
            } catch (Exception ex) {
                System.out.println("Exception loading template");
                ex.printStackTrace();
                log.error("exception loading template: " + source, ex);
                return null;
            }
            if (meta == null) {
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

    public String getDefaultAdminTheme() {
        return defaultAdminTheme;
    }

    public void setDefaultAdminTheme(String defaultAdminTheme) {
        this.defaultAdminTheme = defaultAdminTheme;
    }

    public String getDefaultPublicTheme() {
        return defaultPublicTheme;
    }

    public void setDefaultPublicTheme(String defaultPublicTheme) {
        this.defaultPublicTheme = defaultPublicTheme;
    }

    public class HtmlTemplateLoader {

        /**
         * We'll look for the template in the website's repository (if using
         * custom theme), then in the webapp filesystem, then in the regular
         * filesystem, then finally in the classpath
         */
        public TemplateHtmlPage findTemplateSource(String path) throws IOException {
            log.info("findTemplateSource: " + path);

            TemplateHtmlPage meta = cachedTemplateMetaData.get(path);
            if (meta != null) {
                if (!meta.isValid()) {
                    cachedTemplateMetaData.remove(path);
                    meta = null;
                } else {
                    log.info("cache hit: " + meta.getSource() + " - " + meta.getClass());
                    return meta;
                }
            }
            long tm = System.currentTimeMillis();

            Boolean isCustom = RequestContext.getCurrent().get("isCustom");
            if (isCustom == null) {
                isCustom = false;
            }

            Path p = Path.path(path);
            Path webPath = webRoot.add(p).getParent(); // go to parent, because the path is the directory which contains the template            
            if (isCustom) {
                // load from website repo
                RootFolder rootFolder = _(RootFolder.class);
                WebsiteRootFolder wrf = (WebsiteRootFolder) rootFolder;
                try {
                    Resource r = NodeChildUtils.find(p, wrf);
                    FileResource fr = NodeChildUtils.toFileResource(r);
                    if (fr == null) {
                        if( !"VM_global_library.vm".equals(path )) {
                            log.info("Couldnt find template: " + path + " in website: " + wrf.getWebsite().getName());
                        }                        
                    } else {
                        meta = loadContentMeta(fr, wrf.getWebsite().getName(), webPath);
                    }
                } catch (NotAuthorizedException | BadRequestException | NotFoundException ex) {
                    throw new IOException(ex);
                }
            }

            if (meta == null && roots != null) {
                for (File root : roots) {
                    File templateFile = new File(root, path);
                    if (!templateFile.exists()) {
                        log.warn("Template does not exist: " + templateFile.getAbsolutePath());
                    } else {
                        log.info("found file: " + templateFile.getAbsolutePath());
                        meta = loadFileMeta(templateFile, webPath);
                        break;
                    }
                }
            }

            // Not in filesystem, try a servlet resource
            if (meta == null && path.startsWith("/")) {
                // try to locate a physical file path, so we can detect file changes
                String realPath = servletContext.getRealPath(path);
                if (realPath != null) {
                    File templateFile = new File(realPath);
                    if (templateFile.exists() && templateFile.isFile()) {
                        meta = loadFileMeta(templateFile, webPath);
                    }
                }
                // if we couldnt get a real path might be because its a WAR packaged resource, or overlay, so
                // try to get url.
                if (meta == null) {
                    URL resource = servletContext.getResource(path);
                    if (resource != null) {
                        meta = loadClassPathMeta(resource, webPath);
                    }
                }

//                String localWebPath = servletContext.getRealPath(path);
//                InputStream r = servletContext.getResourceAsStream(path);
//                System.out.println("res as str: " + r);
//                System.out.println("localwebpath: " + localWebPath);
//                if (localWebPath != null) {
//                    File templateFile = new File(localWebPath);
//                    System.out.println("f: " + templateFile.getCanonicalPath() + " - " + templateFile.exists());
//                    if (templateFile.exists()) {
//                        log.info("found web resource: " + templateFile.getAbsolutePath());
//                        meta = loadFileMeta(templateFile, webPath);
//                    }
//                }
            }

            // if not in filesystem, try classpath
            if (meta == null) {
                URL resource = this.getClass().getResource(path);
                if (resource != null) {
                    meta = loadClassPathMeta(resource, webPath);
                }
            }

            if (meta != null) {
                tm = System.currentTimeMillis() - tm;
                log.info("cache miss: " + meta.getSource() + " - " + meta.getClass() + " parsed in " + tm + "ms");
                cachedTemplateMetaData.put(path, meta);
            } else {
                log.warn("Failed to find: " + path);
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

        private TemplateHtmlPage loadClassPathMeta(URL resource, Path webPath) throws IOException {
            ClassPathTemplateHtmlPage meta = new ClassPathTemplateHtmlPage(resource);
            try {
                templateParser.parse(meta, webPath); // needs web path to evaluate resource paths in templates
            } catch (XMLStreamException ex) {
                throw new IOException(resource.toString(), ex);
            }
            return meta;
        }

        private TemplateHtmlPage loadFileMeta(File templateFile, Path webPath) throws IOException {
            FileTemplateHtmlPage meta = new FileTemplateHtmlPage(templateFile);
            try {
                templateParser.parse(meta, webPath); // needs web path to evaluate resource paths in templates
            } catch (XMLStreamException ex) {
                throw new IOException(templateFile.getCanonicalPath(), ex);
            }
            return meta;
        }

        private TemplateHtmlPage loadContentMeta(FileResource fr, String websiteName, Path webPath) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
            ContentTemplateHtmlPage meta = new ContentTemplateHtmlPage(fr, websiteName, webPath);
            try {
                templateParser.parse(meta, webPath); // needs web path to evaluate resource paths in templates
            } catch (XMLStreamException ex) {
                throw new IOException(fr.getHref(), ex);
            }
            return meta;

        }
    }
}
