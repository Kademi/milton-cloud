package io.milton.cloud.server.db.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Order;
import io.milton.vfs.db.PasswordCredential;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.utils.DbUtils;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Disjunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class UserDao {
    
    private static final Logger log = LoggerFactory.getLogger(UserDao.class);

    public PasswordCredential getEmailCredential(String email, Session sess) {
        Criteria crit = sess.createCriteria(PasswordCredential.class);
        crit.add(Expression.eq("email", email));
        return (PasswordCredential) crit.uniqueResult();
    }

    public List<Profile> listProfilesByAdminOrg(Organisation org, Session sess) {
        Criteria crit = sess.createCriteria(Profile.class);
        crit.add(Expression.eq("adminOrg", org));
        crit.addOrder(Order.asc("surName"));
        crit.addOrder(Order.asc("firstName"));
        crit.addOrder(Order.asc("email"));
        List list = crit.list();
        if (list == null || list.isEmpty()) {
            return Collections.EMPTY_LIST;
        } else {
            List<Profile> users = new ArrayList<>(list);
            return users;
        }
    }

    public List<Profile> listProfiles(Organisation org, Session sess) {
        Criteria crit = sess.createCriteria(Profile.class);
        crit.add(Expression.eq("organisation", org));
        crit.addOrder(Order.asc("surName"));
        crit.addOrder(Order.asc("firstName"));
        crit.addOrder(Order.asc("email"));
        List list = crit.list();
        if (list == null || list.isEmpty()) {
            return Collections.EMPTY_LIST;
        } else {
            List<Profile> users = new ArrayList<>(list);
            return users;
        }
    }

    /**
     * Look for the given user profile in the given organisation
     *
     * @param nameOrEmail
     * @param organisation
     * @param session
     * @return
     */
    public Profile getProfile(String nameOrEmail, Organisation organisation, Session session) {
        Criteria crit = session.createCriteria(Profile.class);
        crit.add(Expression.and(Expression.eq("organisation", organisation), 
                Expression.or(Expression.eq("name", nameOrEmail), Expression.eq("email", nameOrEmail))                
        ));
        List<Profile> list = DbUtils.toList(crit, Profile.class);
        if( list.isEmpty() ) {
            return null;
        } else {
            return list.get(0);
        }
    }

    /**
     * Look for the given profile in the given organisation or any of its
     * ancestors
     *
     * @param name
     * @param organisation
     * @param session
     * @return
     */
    public Profile findProfile(String name, Organisation organisation, Session session) {
        while (organisation != null) {
            Profile p = getProfile(name, organisation, session);
            if (p != null) {
                return p;
            }
            organisation = organisation.getOrganisation();
        }
        return null;
    }

    public List<Profile> search(String q, Organisation org, Session session) {
        log.info("search: q=" + q + " org=" + org.getName() );
        Criteria crit = session.createCriteria(Profile.class);
        String[] arr = q.split(" ");
        Conjunction con = Expression.conjunction();
        con.add(Expression.eq("adminOrg", org));
        for( String queryPart : arr ) {
            Disjunction dis = Expression.disjunction();
            String s = queryPart + "%";
            dis.add(Expression.ilike("firstName", s));
            dis.add(Expression.ilike("surName", s));
            dis.add(Expression.ilike("name", s));
            dis.add(Expression.ilike("email", s));
            con.add(dis);
        }
        crit.add(con);        

//        crit.add(Expression.ilike("name", q + "%"));

        crit.addOrder(Order.asc("surName"));
        crit.addOrder(Order.asc("firstName"));
        crit.addOrder(Order.asc("email"));

        List<Profile> list = DbUtils.toList(crit, Profile.class);
        log.info("user search: " + q + " -> " + list.size());
        return list;
    }
}
