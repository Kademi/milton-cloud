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
import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.ResourceApplication;
import io.milton.cloud.server.apps.email.EmailApp;
import io.milton.cloud.server.web.*;
import io.milton.common.Path;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.Request.Method;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.http.http11.auth.DigestResponse;
import io.milton.resource.DigestResource;
import io.milton.resource.GetableResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Website;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 *
 * @author brad
 */
public class DynamicCssApp implements ResourceApplication {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(DynamicCssApp.class);
    
    private SpliffyResourceFactory resourceFactory;
    private Date modDate;
    private LessEngine engine;

    public DynamicCssApp() {
        LessOptions options = new LessOptions();
        options.setCss(true);
        options.setCharset("UTF-8");
        engine = new LessEngine(options);

    }

    @Override
    public String getTitle(Organisation organisation, Website website) {
        return "Dynamic CSS files";
    }

    @Override
    public String getSummary(Organisation organisation, Website website) {
        return "Allows CSS files to have parameterised which can be configured through the website";
    }

    @Override
    public Resource getResource(RootFolder webRoot, String path) throws NotAuthorizedException, BadRequestException {
        System.out.println("gerResource: " + path);
        if (path.endsWith(".dyn.css")) {
            Path p = Path.path(path);

            TemplatedTextPage t = new TemplatedTextPage(p.getName(), webRoot, "text/css", path);
            t.setModifiedDate(modDate);
            return t;
        } else if (path.endsWith(".less.css")) {
            Path p = Path.path(path);
            String reqName = p.getName();
            String physName = reqName.replace(".less.css", ".css");
            p = p.getParent().child(physName);
            Resource rBase = resourceFactory.findFromRoot(webRoot, p);
            if (rBase != null) {
                if (rBase instanceof GetableResource) {
                    GetableResource fr = (GetableResource) rBase;
                    System.out.println("less res");
                    return new LessCssResource(reqName, fr);
                }
            }
            return null;
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
        this.resourceFactory = resourceFactory;
        modDate = config.getContext().get(CurrentDateService.class).getNow();
    }

    public class LessCssResource implements GetableResource, DigestResource {

        private final GetableResource fileResource;
        private final String name;

        public LessCssResource(String name, GetableResource fileResource) {
            this.fileResource = fileResource;
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
            try {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                fileResource.sendContent(bout, range, params, contentType);
                String rawCss = bout.toString("UTF-8");
                String text = engine.compile(rawCss);
                out.write(text.getBytes("UTF-8"));
            } catch (LessException ex) {
                log.warn("LESS compilation exception", ex);
                sendLessError(ex.getLine(), ex.getColumn(), ex.getExtract(), ex.getMessage(), out);
                //throw new RuntimeException("Bad LESS from:" + fileResource.getName() + " - " + fileResource.getClass() ,ex);
            }

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
        public Date getModifiedDate() {
            return fileResource.getModifiedDate();
        }

        @Override
        public String getContentType(String accepts) {
            return fileResource.getContentType(accepts);
        }

        @Override
        public String getUniqueId() {
            return fileResource.getUniqueId();
        }

        @Override
        public Object authenticate(String user, String password) {
            return fileResource.authenticate(user, password);
        }

        @Override
        public boolean authorise(Request request, Method method, Auth auth) {
            return true;
        }

        @Override
        public String getRealm() {
            return fileResource.getRealm();
        }

        @Override
        public String checkRedirect(Request request) throws NotAuthorizedException, BadRequestException {
            return null;
        }

        @Override
        public Object authenticate(DigestResponse digestRequest) {
            if (fileResource instanceof DigestResource) {
                DigestResource dr = (DigestResource) fileResource;
                return dr.authenticate(digestRequest);
            } else {
                return null;
            }

        }

        @Override
        public boolean isDigestAllowed() {
            if (fileResource instanceof DigestResource) {
                DigestResource dr = (DigestResource) fileResource;
                return dr.isDigestAllowed();
            } else {
                return false;
            }
        }

        private void sendLessError(int line, int column, List<String> extract, String message, OutputStream out) {
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
            pw.flush();
        }
    }
}
