package io.milton.cloud.server.web;

import io.milton.http.Request;
import io.milton.http.http11.auth.LoginResponseHandler;
import io.milton.resource.GetableResource;
import io.milton.resource.Resource;

/**
 *
 * @author brad
 */
public class SpliffyLoginTypeHandler implements LoginResponseHandler.LoginPageTypeHandler {

    @Override
    public boolean canLogin(Resource resource, Request request) {
        if(resource instanceof FileResource) {
            return false; // definitely not for content resources, they're strictly raw data due to syncronisation
        }
        if (resource instanceof GetableResource) {
            String ctHeader = request.getAcceptHeader();
            GetableResource gr = (GetableResource) resource;
            String ctResource = gr.getContentType("text/html");
            if (ctResource == null) {
                if (ctHeader != null) {
                    boolean b = ctHeader.contains("html");
                    return b;
                } else {
                    return false;
                }
            } else {
                boolean b = ctResource.contains("html");
                return b;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean isAjax(Resource r, Request request) {
        String acceptHeader = request.getAcceptHeader();
        return acceptHeader != null && (acceptHeader.contains("application/json") || acceptHeader.contains("text/javascript"));
    }
}
