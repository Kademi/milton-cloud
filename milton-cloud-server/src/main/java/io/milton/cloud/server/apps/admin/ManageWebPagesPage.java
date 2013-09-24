package io.milton.cloud.server.apps.admin;

import com.fuselms.apps.learning.ModuleFolder;
import io.milton.cloud.server.web.BranchFolder;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.ContentRedirectorPage;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.GetableResource;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class ManageWebPagesPage extends ContentRedirectorPage implements GetableResource {

    private static final Logger log = LoggerFactory.getLogger(ManageWebPagesPage.class);
    
    public ManageWebPagesPage(String name, CommonCollectionResource parent, String title) {
        super(name, parent, null, title);
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
    }

    
    @Override
    public String checkRedirect(Request request) throws NotAuthorizedException, BadRequestException {
        log.info("checkRedirect");
        String s = super.checkRedirect(request);
        if( s != null ) {
            return s;
        }

        BranchFolder bf = getBranch();
        if( bf == null ) {
            log.info("checkRedirect - couldnt find a branch");
            return "/websites";
        }
        return "/repositories/" + bf.getRepository().getName() + "/" + bf.getBranch().getName() + "/";
    }
    
}
