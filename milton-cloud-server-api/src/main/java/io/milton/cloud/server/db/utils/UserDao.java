package io.milton.cloud.server.db.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Order;
import io.milton.vfs.db.Credential;
import io.milton.vfs.db.PasswordCredential;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;

/**
 *
 * @author brad
 */
public class UserDao {

    public PasswordCredential getEmailCredential(String email, Session sess) {
        Criteria crit = sess.createCriteria(PasswordCredential.class);
        crit.add(Expression.eq("email", email));
        return (PasswordCredential) crit.uniqueResult();
    }
    

    public List<Profile> listProfiles(Organisation org, Session sess) {        
        Criteria crit = sess.createCriteria(Profile.class);
        crit.addOrder(Order.asc("surName"));
        crit.addOrder(Order.asc("firstName"));
        crit.addOrder(Order.asc("email"));
        List list = crit.list();
        if( list == null || list.isEmpty()) {
            return Collections.EMPTY_LIST;
        } else {
            List<Profile> users = new ArrayList<>(list);
            return users;
        }
    }

    /**
     * Look for the given user profile in the given organisation
     * 
     * @param name
     * @param organisation
     * @param session
     * @return 
     */
    public Profile getProfile(String name, Organisation organisation, Session session) {
        Criteria crit = session.createCriteria(Profile.class);
        crit.add(Expression.and(Expression.eq("organisation", organisation), Expression.eq("name", name)));
        return (Profile)crit.uniqueResult();        
    }
    
    /**
     * Look for the given profile in the given organisation or any of its ancestors
     * 
     * @param name
     * @param organisation
     * @param session
     * @return 
     */
    public Profile findProfile(String name, Organisation organisation, Session session) {
        while( organisation != null ) {
            Profile p = getProfile(name, organisation, session);
            if( p != null ) {
                return p;
            }
            organisation = organisation.getOrganisation();
        }
        return null;
    }
    
    
}
