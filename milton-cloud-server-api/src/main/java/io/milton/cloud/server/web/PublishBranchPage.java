package io.milton.cloud.server.web;

import io.milton.cloud.common.CurrentDateService;
import io.milton.http.FileItem;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.PostableResource;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Repository;
import io.milton.vfs.db.utils.SessionManager;
import java.util.Date;
import java.util.Map;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 * @author brad
 */
public class PublishBranchPage extends TemplatedHtmlPage implements PostableResource {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(PublishBranchPage.class);
    
    private final BranchFolder branchFolder;
        
    public PublishBranchPage(String name, BranchFolder parent) {
        super(name, parent, "admin/publish", "Publish " + parent.getName());
        this.branchFolder = parent;
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        log.info("processForm: ");        
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        Branch branch = branchFolder.getBranch();
        Repository repo = branch.getRepository();
        repo.setLiveBranch(branch.getName());
        session.save(repo);
        log.info("Published branch: " + branch.getId() + " with name: " + branch.getName());
        tx.commit();
        jsonResult = new JsonResult(true, "Published " + branch.getName());
        return null;
    }

    @Override
    public Priviledge getRequiredPostPriviledge(Request request) {
        return Priviledge.WRITE_CONTENT;
    }

    
    
}
