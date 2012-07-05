package io.milton.cloud.server.manager;

import java.util.Date;
import io.milton.vfs.db.Credential;
import io.milton.vfs.db.PasswordCredential;
import io.milton.vfs.db.Profile;
import io.milton.http.http11.auth.DigestGenerator;
import io.milton.http.http11.auth.DigestResponse;
import io.milton.vfs.db.utils.SessionManager;

/**
 *
 * @author brad
 */
public class PasswordManager {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(PasswordManager.class);
    private final DigestGenerator digestGenerator;
    private String realm = "spliffy";

    public PasswordManager(DigestGenerator digestGenerator) {
        this.digestGenerator = digestGenerator;
    }

    public PasswordManager() {
        this.digestGenerator = new DigestGenerator();
    }

    public void setPassword(Profile user, String newPassword) {
        // Attempt to locate a PasswordCredential
        PasswordCredential epc = null;
        if (user.getCredentials() != null) {
            for (Credential c : user.getCredentials()) {
                if (c instanceof PasswordCredential) {
                    epc = (PasswordCredential) c;
                    break;
                }
            }
        }
        if (epc == null) {
            epc = new PasswordCredential();
            epc.setCreatedDate(new Date());
            epc.setModifiedDate(new Date());
            epc.setProfile(user);
        }
        String hash = calcPasswordHash(user.getName(), newPassword);
        epc.setPassword(hash);
        SessionManager.session().save(epc);
    }

    public String calcPasswordHash(String userName, String password) {
        String a1md5 = digestGenerator.encodePasswordInA1Format(userName, realm, password);
        return a1md5;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public boolean verifyDigest(DigestResponse digest, Profile user) {
        PasswordCredential epc = null;
        if (user.getCredentials() != null) {
            for (Credential c : user.getCredentials()) {
                if (c instanceof PasswordCredential) {
                    epc = (PasswordCredential) c;
                    break;
                }
            }
        }
        if (epc == null) {
            log.warn("Profile is not associated with a password: profileId=" + user.getId());
            return false;
        }
        //String actualPassword = epc.getPassword();
        String a1Md5 = epc.getPassword();
        //String a1Md5 = digestGenerator.encodePasswordInA1Format(user.getName(), realm, actualPassword);
        String expectedResp = digestGenerator.generateDigestWithEncryptedPassword(digest, a1Md5);
        String actualResp = digest.getResponseDigest();
        if (expectedResp.equals(actualResp)) {
//            log.info("verifyDigest: ok");
            return true;
        } else {
            System.out.println("digests don't match!!!!");
            System.out.println(digest.getCnonce());
            System.out.println(digest.getMethod());
            System.out.println(digest.getNc());
            System.out.println("nonce:" + digest.getNonce());
            System.out.println(digest.getQop());
            System.out.println(digest.getRealm());
            System.out.println(digest.getUri());
            System.out.println(digest.getUser());
            return false;
        }
    }

    public boolean verifyPassword(Profile user, String requestPassword) {
        if (requestPassword != null) {
            System.out.println("veruify password: " + user.getName());
            if (user.getCredentials() != null && !user.getCredentials().isEmpty()) {
                for (Credential c : user.getCredentials()) {
                    if (c instanceof PasswordCredential) {
                        PasswordCredential pc = (PasswordCredential) c;
                        if (matches(user.getName(), pc, requestPassword)) {
                            return true;
                        } else {
                            log.warn("Non-matching password");
                        }
                    }
                }
            } else {
                log.warn("null or empty creds for: " + user.getId());
            }
        }
        log.warn("Basic login failed for user: " + user.getEmail());
        return false;
    }
    

    private boolean matches(String username, PasswordCredential pc, String requestPassword) {
        String a1Md5 = pc.getPassword();
        String reqHash = calcPasswordHash(username, requestPassword);
        if(a1Md5.equals(reqHash)) {
            return true;
        } else {
            log.warn("password hashes do not match: " + a1Md5 + " != " + reqHash);
            return false;
        }
    }
}
