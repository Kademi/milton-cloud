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
package io.milton.cloud.server.apps.dynamiccss;

import com.asual.lesscss.LessEngine;
import com.asual.lesscss.LessException;
import com.asual.lesscss.LessOptions;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.ResourceApplication;
import io.milton.cloud.server.web.*;
import io.milton.cloud.server.web.templating.HtmlTemplateRenderer;
import io.milton.common.Path;
import io.milton.common.RangeUtils;
import io.milton.http.Auth;
import io.milton.http.HttpManager;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.Request.Method;
import io.milton.http.ResourceFactory;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.http.http11.auth.DigestResponse;
import io.milton.resource.CollectionResource;
import io.milton.resource.DigestResource;
import io.milton.resource.GetableResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Organisation;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import java.io.ByteArrayInputStream;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * @author brad
 */
public class DynamicCssApp implements ResourceApplication {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(DynamicCssApp.class);
    private ResourceFactory resourceFactory;
    private Date modDate;
    private LessEngine engine;
    private final ConcurrentMap<String, String> cache; // cache of ParsedResources, keyed on hash, for FileResource's

    public DynamicCssApp() {
        cache = new ConcurrentLinkedHashMap.Builder()
                .maximumWeightedCapacity(1000)
                .build();
    }

    @Override
    public String getTitle(Organisation organisation, Branch websiteBranch) {
        return "Dynamic CSS files";
    }

    @Override
    public String getSummary(Organisation organisation, Branch websiteBranch) {
        return "Allows CSS files to have parameterised which can be configured through the website";
    }

    @Override
    public Resource getResource(RootFolder webRoot, String path) throws NotAuthorizedException, BadRequestException {
        if (path.endsWith(".dyn.css")) {
            Path p = Path.path(path);

            TemplatedTextPage t = new TemplatedTextPage(p.getName(), webRoot, "text/css", path);
            t.setModifiedDate(modDate);
            return t;
        } else if (path.endsWith(HtmlTemplateRenderer.EXT_COMPILE_LESS)) {
            // Give a request path like this: /themes/x/\a\b.css,\x\y.css.less
            // 1. get just the name part => \a\b.css,\x\y.css.less
            // 2. chop off the extension => \a\b.css,\x\y.css
            // 3. split it and swap folder seperators =>  ["/a/b.css","/x/y.css"]
            // 4. then locate each resource, merge the content of all resources, then do LESS compilation
            // 5. Note the current path for resources resolved from css ends up being /themes/x

            Path p = Path.path(path);
            String reqName = p.getName();
            if (!reqName.contains(",")) {
                String actualResName = reqName.replace(HtmlTemplateRenderer.EXT_COMPILE_LESS, "");
                String host = HttpManager.request().getHostHeader();
                Resource rBase = resourceFactory.getResource(host, p.getParent().child(actualResName).toString());
                if (rBase instanceof GetableResource) {
                    String parentUrl = "http://" + host + "/" + p.getParent().toString();
                    return new SingleLessCssResource((GetableResource) rBase, actualResName, parentUrl);
                } else {
                    return null;
                }
            } else {
                // Note we ignore the folder, only interested in resource names encoded into resource name
                String[] paths = reqName.substring(0, reqName.length() - HtmlTemplateRenderer.EXT_COMPILE_LESS.length()).split(","); // need to chop off extension, then split
                List<GetableResource> resources = new ArrayList<>();
                List<String> notFound = new ArrayList<>();
                String host = HttpManager.request().getHostHeader();
                String parentUrl = null;
                for (String s : paths) {
                    s = s.replace(HtmlTemplateRenderer.COMBINED_RESOURCE_SEPERATOR, "/"); // HtmlTemplater changes path characters to avoid changing relative path
                    Resource rBase = resourceFactory.getResource(host, s);
                    if (rBase != null) {
                        if (rBase instanceof CollectionResource) {
                            CollectionResource col = (CollectionResource) rBase;
                            for (Resource r : col.getChildren()) {
                                if (r instanceof GetableResource) {
                                    GetableResource gr = (GetableResource) r;
                                    if (gr.getName().endsWith(".css") || gr.getName().endsWith(".less")) {
                                        resources.add(gr);
                                    }
                                }
                            }
                        } else if (rBase instanceof GetableResource) {
                            GetableResource fr = (GetableResource) rBase;
                            if (parentUrl == null) {
                                String parentPath = Path.path(s).getParent().toString();
                                parentUrl = "http://" + host + "/" + parentPath + "/";
                            }
                            resources.add(fr);
                        } else {
                            notFound.add("Not GET'able: " + s);
                            log.warn(" css resource is not getable: " + s + " - " + rBase.getClass());
                        }
                    } else {
                        notFound.add("Not found: " + s);
                        log.warn("CSS resource is not found: " + s + " in host: " + host);
                    }
                }
                if (resources.isEmpty()) {
                    log.warn("No paths found: " + path);
                    return null;
                } else {
                    return new MultiLessCssResource(resources, notFound, reqName, parentUrl);
                }
            }
        } else {
            return null;
        }
    }

    @Override
    public String getInstanceId() {
        return "dynamicCss";
    }

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
        this.resourceFactory = config.getContext().get(HttpManager.class).getResourceFactory();
        LessOptions options = new LessOptions();
        //options.setCss(true);
        options.setCharset("UTF-8");
        engine = new LessEngine(options);

        modDate = config.getContext().get(CurrentDateService.class).getNow();
    }

    public class SingleLessCssResource extends LessCssResource {

        private final GetableResource singleResource;
        private final String parentUrl;

        public SingleLessCssResource(GetableResource singleResource, String name, String parentUrl) {
            super(name);
            this.singleResource = singleResource;
            this.parentUrl = parentUrl;
        }

        @Override
        protected GetableResource getResource() {
            return singleResource;
        }

        @Override
        public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            // Lets be nice and say if we didnt find something
            String rawCss = bout.toString("UTF-8");
            try {
                String text = engine.compile(rawCss, parentUrl);
                out.write(text.getBytes("UTF-8"));
            } catch (LessException ex) {
                log.warn("LESS compilation exception", ex);
                sendLessError(rawCss, ex.getLine(), ex.getColumn(), ex.getExtract(), ex.getMessage(), out);
                //throw new RuntimeException("Bad LESS from:" + fileResource.getName() + " - " + fileResource.getClass() ,ex);
            }
        }
    }

    public class MultiLessCssResource extends LessCssResource {

        private final List<GetableResource> fileResources;
        private final GetableResource mostRecentModResource;
        private final List<String> notFound;
        private final String parentUrl;

        public MultiLessCssResource(List<GetableResource> fileResources, List<String> notFound, String name, String parentUrl) {
            super(name);
            this.fileResources = fileResources;
            this.notFound = notFound;
            this.parentUrl = parentUrl;
            GetableResource mr = null;
            Date mostRecentDate = null;
            for( GetableResource gr : fileResources) {
                if( mr == null ) {
                    mr = gr;
                    mostRecentDate = gr.getModifiedDate();
                } else {
                    Date dt = gr.getModifiedDate();
                    if( dt != null && mostRecentDate != null ) {
                        if( dt.after(mostRecentDate)) {
                            mr = gr;
                            mostRecentDate = gr.getModifiedDate();
                        }
                    }
                }
            }
            this.mostRecentModResource = mr;
        }

        @Override
        protected GetableResource getResource() {
            return mostRecentModResource;
        }

        @Override
        public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
            String cacheKey = getName() + getUniqueId() + getModifiedDate();
            String lessText = cache.get(cacheKey);
            if (lessText == null) {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                // Lets be nice and say if we didnt find something
                for (String s : notFound) {
                    String msg = "/** " + s + " */\n";
                    bout.write(msg.getBytes());
                }
                StringBuilder sb = new StringBuilder();
                for (GetableResource gr : fileResources) {
                    String header = "/** " + gr.getName() + " */\n";
                    bout.write(header.getBytes());
                    gr.sendContent(bout, range, params, contentType);
                    bout.write("\n".getBytes());
                }
                String rawCss = bout.toString("UTF-8");
                try {
                    lessText = engine.compile(rawCss, parentUrl);                    
                    cache.put(cacheKey, lessText);
                } catch (LessException ex) {
                    log.warn("LESS compilation exception", ex);
                    sendLessError(rawCss, ex.getLine(), ex.getColumn(), ex.getExtract(), ex.getMessage(), out);
                    return;
                    //throw new RuntimeException("Bad LESS from:" + fileResource.getName() + " - " + fileResource.getClass() ,ex);
                }
            }
            ByteArrayInputStream bin = new ByteArrayInputStream(lessText.getBytes("UTF-8"));
            RangeUtils.writeRange(bin, range, out);
        }
    }

    public abstract class LessCssResource implements GetableResource, DigestResource {

        private final String name;

        protected abstract GetableResource getResource();

        public LessCssResource(String name) {
            this.name = name;
        }

        @Override
        public Date getModifiedDate() {
            return getResource().getModifiedDate();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Long getMaxAgeSeconds(Auth auth) {
            return 60 * 60 * 24 * 7l;
        }

        @Override
        public Long getContentLength() {
            return null;
        }

        @Override
        public String getContentType(String accepts) {
            return "text/css";
        }

        @Override
        public String getUniqueId() {
            return getResource().getUniqueId();
        }

        @Override
        public Object authenticate(String user, String password) {
            return getResource().authenticate(user, password);
        }

        @Override
        public boolean authorise(Request request, Method method, Auth auth) {
            return true;
        }

        @Override
        public String getRealm() {
            return getResource().getRealm();
        }

        @Override
        public String checkRedirect(Request request) throws NotAuthorizedException, BadRequestException {
            return null;
        }

        @Override
        public Object authenticate(DigestResponse digestRequest) {
            return ((DigestResource) getResource()).authenticate(digestRequest);

        }

        @Override
        public boolean isDigestAllowed() {
            if (getResource() instanceof DigestResource) {
                return ((DigestResource) getResource()).isDigestAllowed();
            } else {
                return false;
            }

        }

        protected void sendLessError(String rawCss, int line, int column, List<String> extract, String message, OutputStream out) {
            try {
                PrintWriter pw = new PrintWriter(out);
                pw.println("LESS Compile error");
                pw.println("Line: " + line);
                pw.println("Column: " + column);
                pw.println("Reason: " + message);
                if (extract != null && extract.size() > 0) {
                    pw.println("-------");
                    pw.println("Extract: ");
                    for (String extractLine : extract) {
                        System.out.println(extractLine);
                    }
                }
                pw.println("---- Raw CSS follows ---");
                LineNumberReader reader = new LineNumberReader(new StringReader(rawCss));
                String cssLine = reader.readLine();
                while (cssLine != null) {
                    pw.println("Line:" + reader.getLineNumber() + "   " + cssLine);
                    cssLine = reader.readLine();
                }
                pw.print(rawCss);
                pw.flush();
            } catch (IOException ex) {
                log.error("Exception generating error content for Less CSS", ex);
            }
        }
    }
}
