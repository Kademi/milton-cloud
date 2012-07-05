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
        engine = new VelocityEngine();
        engine.setProperty("resource.loader", "mine");
        engine.setProperty("mine.resource.loader.instance", new HtmlTemplateLoaderResourceLoader());
        engine.setProperty("userdirective", VelocityContentDirective.class.getName());

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
            System.out.println("isPublic: " + isPublic + " - from res with parent " + cr.getParent().getClass());
        }
        String theme = findTheme(aThis, isPublic);
        System.out.println("theme: " + theme);
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
        System.out.println("writePage: theme: " + theme);
        UserResource user = securityManager.getCurrentPrincipal();
        RootFolder rootFolder = WebUtils.findRootFolder(aThis);
        if (!templatePath.startsWith("/")) {
            templatePath = "/templates/apps/" + templatePath;
        }
        templatePath = templatePath + ".html";

        Template bodyTemplate = getTemplate(templatePath);
        TemplateHtmlPage bodyTemplateMeta = cachedTemplateMetaData.get(templatePath);

        String themeTemplateName = findThemeTemplateName(bodyTemplateMeta);
        String themeTemplatePath;
        if (theme.equals("custom")) {
            themeTemplatePath = "/content/theme/" + themeTemplateName + ".html"; // TODO: need branch to be configurable
            RequestContext.getCurrent().put("isCustom", Boolean.TRUE); // can't pass params to velocity template loader, so have to workaround
            RequestContext.getCurrent().put(rootFolder);
        } else if (themeTemplateName.startsWith("/")) {
            themeTemplatePath = themeTemplateName + ".html";
        } else {
            themeTemplatePath = "/templates/themes/" + theme + "/" + themeTemplateName + ".html";
        }
        Template themeTemplate = getTemplate(themeTemplatePath);
        if( themeTemplate == null ) {
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
                    return meta;
                }
            }                

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
                System.out.println("isCustom, load from: " + wrf.getWebsite().getName());
                try {
                    Resource r = NodeChildUtils.find(p, wrf);
                    FileResource fr = NodeChildUtils.toFileResource(r);
                    if (fr == null) {
                        throw new RuntimeException("Couldnt find template: " + path + " in website: " + wrf.getWebsite().getName());                        
                    }
                    meta = loadContentMeta(fr, wrf.getWebsite().getName(), webPath);
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
                        System.out.println("found file: " + templateFile.getAbsolutePath());
                        meta = loadFileMeta(templateFile, webPath);
                        break;
                    }
                }
            }
            // if not in filesystem, try classpath
            if (meta == null) {
                URL resource = this.getClass().getResource(path);
                if (resource != null) {
                    meta = loadClassPathMeta(resource, webPath);
                }
            }

            if( meta != null ) {
                cachedTemplateMetaData.put(path, meta);
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
            templateParser.parse(meta, webPath); // needs web path to evaluate resource paths in templates
            return meta;
        }

        private TemplateHtmlPage loadFileMeta(File templateFile, Path webPath) throws IOException {
            FileTemplateHtmlPage meta = new FileTemplateHtmlPage(templateFile);
            templateParser.parse(meta, webPath); // needs web path to evaluate resource paths in templates
            return meta;
        }

        private TemplateHtmlPage loadContentMeta( FileResource fr, String websiteName, Path webPath) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
            ContentTemplateHtmlPage meta = new ContentTemplateHtmlPage(fr, websiteName, webPath);
            templateParser.parse(meta, webPath); // needs web path to evaluate resource paths in templates
            return meta;

        }
    }
}
