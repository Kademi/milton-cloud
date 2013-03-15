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
import java.util.*;
import org.apache.commons.collections.ExtendedProperties;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.web.*;
import io.milton.context.RequestContext;

import io.milton.http.HttpManager;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.GetableResource;
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
    private final SpliffyResourceFactory resourceFactory;
    private final VelocityEngine engine;
    private final SpliffySecurityManager securityManager;
    private final HtmlTemplateParser templateParser;
    private final HtmlTemplateRenderer templateRenderer;
    private final Map<String, GetableResourcePathTemplateHtmlPage> cachedTemplateMetaData = new HashMap<>();
    private String defaultPublicTheme = "fuse";
    private String defaultAdminTheme = "admin";
    private Path webRoot = Path.path("/");
    private final long loadedTime = System.currentTimeMillis();

    public HtmlTemplater(SpliffyResourceFactory resourceFactory, ApplicationManager applicationManager, Formatter formatter, SpliffySecurityManager securityManager) {
        this.resourceFactory = resourceFactory;
        this.securityManager = securityManager;
        java.util.Properties p = new java.util.Properties();
        p.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogSystem");
        engine = new VelocityEngine(p);
        engine.setProperty("resource.loader", "mine");
        engine.setProperty("mine.resource.loader.instance", new HtmlTemplateLoaderResourceLoader());
        engine.setProperty("mine.resource.loader.cache", "true");
        engine.setProperty("mine.resource.loader.modificationCheckInterval", "5");
        engine.setProperty("userdirective", VelocityContentDirective.class.getName() + "," + PortletsDirective.class.getName() + "," + RenderAppSettingsDirective.class.getName());

        templateParser = new HtmlTemplateParser();
        templateRenderer = new HtmlTemplateRenderer(applicationManager, formatter);
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
        String theme = findTheme(aThis);
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
        RootFolder rootFolder = WebUtils.findRootFolder(aThis);
        RequestContext.getCurrent().put(rootFolder);
        if (theme.equals("custom")) {
            RequestContext.getCurrent().put("isCustom", Boolean.TRUE); // can't pass params to velocity template loader, so have to workaround
        }
        String themePath; // path that contains the theme templates Eg /templates/themes/admin/ or /content/theme/
        if (theme.equals("custom")) {
            //Need to resolve theme to distinct identifier - ie if custom then the branch
            themePath = "/theme/";
        } else {
            themePath = "/templates/themes/" + theme + "/";
        }

        UserResource user = securityManager.getCurrentPrincipal();
        if (!templatePath.startsWith("/")) {
            if (templatePath.startsWith("theme/")) {
                templatePath = "/" + templatePath;
            } else {
                templatePath = "/theme/apps/" + templatePath; // Eg change admin/manageUsers to /theme/apps/admin/manageUsers
            }
        }

        if (!templatePath.endsWith(".html")) {
            templatePath = templatePath + ".html";
        }
        templatePath = rootFolder.getDomainName() + ":" + templatePath;
        Template bodyTemplate = getTemplate(templatePath);
        if( bodyTemplate == null ) {
            throw new RuntimeException("Couldnt find template: " + templatePath);
        }
        TemplateHtmlPage bodyTemplateMeta = getTemplateResource(templatePath);
        if (bodyTemplateMeta == null) {
            throw new RuntimeException("Didnt find cached meta, which is weird because i did find the template that it comes from. Maybe check cache key format");
        }

        String themeTemplateName = findThemeTemplateName(bodyTemplateMeta);
        String themeTemplatePath; // if the given themeTemplateName is an absolute path then use it as is, other prefix with themePath
        if (themeTemplateName.startsWith("/")) {
            themeTemplatePath = themeTemplateName;
        } else {
            themeTemplatePath = "/theme/" + themeTemplateName;
        }
        if (!themeTemplatePath.endsWith(".html")) {
            themeTemplatePath += ".html"; // this class only does html templates
        }
        themeTemplatePath = rootFolder.getDomainName() + ":" + themeTemplatePath;
        Template themeTemplate = getTemplate(themeTemplatePath);
        if (themeTemplate == null) {
            throw new RuntimeException("Couldnt find themeTemplate: " + themeTemplatePath);
        }
        TemplateHtmlPage themeTemplateTemplateMeta = getTemplateResource(themeTemplatePath);
        if (themeTemplateTemplateMeta == null) {
            throw new RuntimeException("Couldnt find meta for template: " + themeTemplatePath);
        }

        templateRenderer.renderHtml(rootFolder, aThis, params, user, themeTemplate, themeTemplateTemplateMeta, bodyTemplate, bodyTemplateMeta, theme, themePath, out);
    }

    public String findTheme(Resource r) {
        RootFolder rootFolder = WebUtils.findRootFolder(r);
        String theme;
        if (rootFolder instanceof OrganisationRootFolder) {
            theme = defaultAdminTheme;
        } else if (rootFolder instanceof WebsiteRootFolder) {
            WebsiteRootFolder websiteFolder = (WebsiteRootFolder) rootFolder;
            theme = websiteFolder.getBranch().getPublicTheme();
            if (theme == null) {
                theme = defaultPublicTheme;
            }
        } else {
            theme = defaultPublicTheme;
        }
        return theme;
    }

    public String findThemePath(CommonResource r) {
        String theme = findTheme(r);
        return findThemePath(theme);
    }

    public String findThemePath(String theme) {
        String themePath; // path that contains the theme templates Eg /templates/themes/admin/ or /content/theme/
        if (theme.equals("custom")) {
            themePath = "/theme/";
        } else {
            themePath = "/templates/themes/" + theme + "/";
        }
        return themePath;
    }

    private String findThemeTemplateName(TemplateHtmlPage bodyTemplateMeta) {
        if (bodyTemplateMeta == null) {
            throw new NullPointerException("bodyTemplateMeta is null");
        }
        if (bodyTemplateMeta.getWebResources() == null) {
            throw new NullPointerException("bodyTemplateMeta.getWebResources() is null");
        }

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

    private Template getTemplate(String templatePath) {
        if (templatePath == null) {
            throw new RuntimeException("templatePath is null");
        }
        return engine.getTemplate(templatePath);
    }

    public GetableResourcePathTemplateHtmlPage getTemplateResource(String source) {
        if (!source.contains(":")) {
            System.out.println("no semicolon: " + source);
            return null;
        }        
        synchronized (this) {
            GetableResourcePathTemplateHtmlPage meta = cachedTemplateMetaData.get(source);
            //System.out.println("meta: " + meta + " source=" + source);
            if (meta == null) {
                String[] arr = source.split(":");
                String host = arr[0];
                String sPath = arr[1];

                try {
                    Resource r = resourceFactory.findResource(host, Path.path(sPath));
                    if (r instanceof GetableResource) {
                        meta = new GetableResourcePathTemplateHtmlPage(host, sPath, loadedTime, templateParser);
                        System.out.println("put to cache: " + source);
                        cachedTemplateMetaData.put(source, meta);
                    } else {
                        log.warn("Null or incompatible resource: " + r + " for source: " + source);
                    }
                } catch (NotAuthorizedException | BadRequestException e) {
                    throw new RuntimeException(e);
                }
            }
            return meta;            
        }
    }

    public class HtmlTemplateLoaderResourceLoader extends ResourceLoader {

        @Override
        public void init(ExtendedProperties configuration) {
        }

        @Override
        public synchronized InputStream getResourceStream(String source) throws ResourceNotFoundException {            
            if (HtmlTemplater.log.isTraceEnabled()) {
                HtmlTemplater.log.trace("getResourceStream( " + source + ") ");
            }
            GetableResourcePathTemplateHtmlPage r = getTemplateResource(source);
            if (r == null) {
                throw new ResourceNotFoundException(source);
            }
            try {
                r.parse();
                return new ByteArrayInputStream(r.getBody().getBytes("UTF-8"));
            } catch (IOException | XMLStreamException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean isSourceModified(org.apache.velocity.runtime.resource.Resource resource) {
            long lastMod = getLastModified(resource);
            boolean isMod = lastMod != resource.getLastModified();
            if (HtmlTemplater.log.isTraceEnabled()) {
                HtmlTemplater.log.trace("isSourceModified( " + resource.getName() + ") = " + isMod);
            }
            return isMod;
        }

        @Override
        public long getLastModified(org.apache.velocity.runtime.resource.Resource resource) {
            if (HtmlTemplater.log.isTraceEnabled()) {
                HtmlTemplater.log.trace("getLastModified( " + resource.getName() + ")");
            }
            GetableResourcePathTemplateHtmlPage r = getTemplateResource(resource.getName());
            if (r == null) {
                throw new ResourceNotFoundException(resource.getName());
            }
            return r.getTimestamp();
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

    public VelocityEngine getEngine() {
        return engine;
    }
}
