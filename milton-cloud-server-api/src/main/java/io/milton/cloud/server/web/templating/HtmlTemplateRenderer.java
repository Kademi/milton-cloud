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

import io.milton.resource.Resource;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.*;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.apps.orgs.OrganisationFolder;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.CommonResource;
import io.milton.vfs.db.Profile;
import io.milton.cloud.server.web.RootFolder;

/**
 *
 * @author brad
 */
public class HtmlTemplateRenderer {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(HtmlTemplateRenderer.class);
    private final ApplicationManager applicationManager;
    private final Formatter formatter;

    public HtmlTemplateRenderer(ApplicationManager applicationManager, Formatter formatter) {
        this.applicationManager = applicationManager;
        this.formatter = formatter;
    }

    public void renderHtml(RootFolder rootFolder, Resource page, Map<String, String> params, Profile user, Template themeTemplate, TemplateHtmlPage themeTemplateTemplateMeta, Template contentTemplate, TemplateHtmlPage bodyTemplateMeta, String themeName, OutputStream out) throws IOException {
        Context datamodel = new VelocityContext();
        CommonCollectionResource folder = null;
        if( page instanceof CommonResource) {
            CommonResource cr = (CommonResource) page;
            folder = cr.getParent();
            datamodel.put("folder", folder);
        }
        datamodel.put("page", page);
        datamodel.put("params", params);
        if (user != null) {
            datamodel.put("user", user);
        }
        List<MenuItem> menu = applicationManager.getMenu(page, user, rootFolder);
        datamodel.put("menu", menu);
        datamodel.put("formatter", formatter);

        OrganisationFolder orgFolder = findParentOrg(page);
        if( orgFolder != null ) {
            datamodel.put("parentOrg", orgFolder);
        }
        
        PrintWriter pw = new PrintWriter(out);
        pw.write("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
        pw.write("<html>\n");
        pw.write("<head>\n");

        List<WebResource> pageWebResources = null;
        List<String> pageBodyClasses = null;
        if (page instanceof HtmlPage) {
            HtmlPage htmlPage = (HtmlPage) page;
            pageWebResources = htmlPage.getWebResources();
            pageBodyClasses = htmlPage.getBodyClasses();
            BodyRenderer bodyRenderer = new BodyRenderer(htmlPage);
            datamodel.put("body", bodyRenderer);
        }

        List<WebResource> webResources = deDupe(themeTemplateTemplateMeta.getWebResources(), bodyTemplateMeta.getWebResources(), pageWebResources);
        for (WebResource wr : webResources) {
            String html = wr.toHtml(themeName);
            pw.write(html + "\n");
        }
        pw.write("</head>\n");
        pw.write("<body class=\"");
        List<String> bodyClasses = deDupeBodyClasses(themeTemplateTemplateMeta.getBodyClasses(), bodyTemplateMeta.getBodyClasses(), pageBodyClasses);
        writeBodyClasses(pw, bodyClasses);
        pw.write("\">\n");
        pw.flush();
        // do theme body (then content body)

        VelocityContentDirective.setContentTemplate(contentTemplate, datamodel);
        themeTemplate.merge(datamodel, pw);

        pw.write("</body>\n");
        pw.write("</html>");
        pw.flush();
        System.out.println("finished rendering page");
    }

    private List<WebResource> deDupe(List<WebResource>... webResourceLists) {
        Set<WebResource> set = new HashSet<>();
        List<WebResource> orderedList = new ArrayList<>();
        for (List<WebResource> list : webResourceLists) {
            if (list != null) {
                for (WebResource wr : list) {
                    if (!set.contains(wr)) {
                        set.add(wr);
                        orderedList.add(wr);
                    }
                }
            }
        }
        return orderedList;
    }

    private List<String> deDupeBodyClasses(List<String>... bodyClassLists) {
        Set<String> set = new HashSet<>();
        List<String> orderedList = new ArrayList<>();
        for (List<String> list : bodyClassLists) {
            if (list != null) {
                for (String s : list) {
                    if (!set.contains(s)) {
                        set.add(s);
                        orderedList.add(s);
                    }
                }
            }
        }
        return orderedList;
    }

    private void writeBodyClasses(PrintWriter pw, List<String> bodyClasses) {
        for (String s : bodyClasses) {
            pw.append(s).append(" ");
        }
    }

    private OrganisationFolder findParentOrg(Resource page) {
        if( page instanceof OrganisationFolder ) {
            return (OrganisationFolder) page;
        } else if( page instanceof CommonResource ) {
            CommonResource cr = (CommonResource) page;
            return findParentOrg(cr.getParent());
        } else {
            return null;
        }
    }
    
    public class BodyRenderer {
        private final HtmlPage htmlPage;

        public BodyRenderer(HtmlPage htmlPage) {
            this.htmlPage = htmlPage;
        }

        @Override
        public String toString() {
            return htmlPage.getBody();
        }                
    }
}
