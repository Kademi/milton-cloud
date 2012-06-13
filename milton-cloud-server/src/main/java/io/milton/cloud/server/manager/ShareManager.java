package io.milton.cloud.server.manager;

import io.milton.cloud.server.db.utils.SessionManager;
import com.ettrema.mail.MailboxAddress;
import com.ettrema.mail.StandardMessage;
import com.ettrema.mail.StandardMessageImpl;
import com.ettrema.mail.send.MailSender;
import io.milton.cloud.server.db.*;
import io.milton.http.AccessControlledResource;
import io.milton.http.HttpManager;
import java.util.*;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 * @author brad
 */
public class ShareManager {

    private final MailSender mailSender;
    private final ResourceManager resourceManager;

    public ShareManager(MailSender mailSender, ResourceManager resourceManager) {
        this.mailSender = mailSender;
        this.resourceManager = resourceManager;
    }
    
    

    public void sendShareInvites(Profile curUser, Repository repo, String shareWith, AccessControlledResource.Priviledge priv, String message) {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        List <StandardMessage> toSend = createShares(curUser, repo, shareWith, priv, message, session);
        tx.commit();
        sendMail(toSend);
    }

    private List<StandardMessage> createShares(Profile curUser, Repository repo, String shareWith, AccessControlledResource.Priviledge p, String message, Session session) {
        String[] arr = shareWith.split(",");
        MailboxAddress from = MailboxAddress.parse(curUser.getEmail());
        List<StandardMessage> list = new ArrayList<>();
        String inviteBaseHref = HttpManager.request().getAbsoluteUrl(); // http://asdada/asd/ad
        inviteBaseHref = inviteBaseHref.substring(0, inviteBaseHref.indexOf("/", 7) + 1);// http://asdada/
        inviteBaseHref += "shares/";
        for (String recip : arr) {
            StandardMessageImpl sm = createEmailShare(repo, from, recip, p, message, inviteBaseHref, session);
            list.add(sm);
        }
        return list;
    }

    private void sendMail(List<StandardMessage> list) {
        for (StandardMessage sm : list) {
            mailSender.sendMail(sm);
        }

    }

    private StandardMessageImpl createEmailShare(Repository repo, MailboxAddress from, String sRecip, AccessControlledResource.Priviledge p, String message, String inviteBaseHref, Session session) {

        Share share = new Share();
        share.setId(UUID.randomUUID());
        share.setSharedFrom(repo.trunk(session)); // path relative to sharing user
        share.setShareRecip(sRecip);
        share.setPriviledge(p);
        share.setCreatedDate(new Date());
        session.save(share);

        String inviteHref = inviteBaseHref + share.getId();

        MailboxAddress recip = MailboxAddress.parse(sRecip);

        StandardMessageImpl sm = new StandardMessageImpl();
        sm.setFrom(from);
        sm.setTo(Arrays.asList(recip));
        sm.setSubject("Share folder");
        System.out.println("Share invitation href: " + inviteHref);
        sm.setText(message + "\n\nTo see the files please click here: " + inviteHref);
        
        return sm;
    }

    /**
     * 
     * Updates the share so it is connected to the given entity's root folder
     * 
     * @param curUser
     * @param share
     * @param entityName
     * @param sharedAsName
     * @return
     * @throws Exception 
     */
    public void acceptShare(Profile curUser, Share share, BaseEntity sharedTo, String sharedAsName) throws Exception{
        
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();        
        share.setAcceptedDate(new Date());
        session.save(share);
        
        Repository newRepo = new Repository();
        newRepo.setBaseEntity(sharedTo);
        newRepo.setCreatedDate(new Date());
        newRepo.setName(sharedAsName);
        session.save(newRepo);
        Branch newTrunk = newRepo.trunk(session);
        newTrunk.setLinkedTo(share.getSharedFrom());
        
        tx.commit();
        
    }
}
