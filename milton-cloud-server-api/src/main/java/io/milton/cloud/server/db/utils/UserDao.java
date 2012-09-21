package io.milton.cloud.server.db.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import io.milton.vfs.db.PasswordCredential;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.utils.DbUtils;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;
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
        crit.add(Restrictions.eq("email", email));
        return (PasswordCredential) crit.uniqueResult();
    }

    public List<Profile> listProfiles(Organisation org, Session sess) {
        Criteria crit = sess.createCriteria(Profile.class);
        Criteria critMembership = crit.createCriteria("memberships");
        Criteria critSubordinate = critMembership.createCriteria("subordinates");
        critSubordinate.add(Restrictions.eq("withinOrg", org));
        crit.addOrder(Order.asc("surName"));
        crit.addOrder(Order.asc("firstName"));
        crit.addOrder(Order.asc("email"));
        List<Profile> list = crit.list();
        if (list == null || list.isEmpty()) {
            return Collections.EMPTY_LIST;
        } else {
            List<Profile> deduped = new ArrayList<>();
            for (Profile p : list) {
                if (!deduped.contains(p)) {
                    deduped.add(p);
                }
            }
            return deduped;
        }
    }

    public List<Profile> search(String q, Organisation org, Session session) {
        log.info("search: q=" + q + " org=" + org.getName());
        Criteria crit = session.createCriteria(Profile.class);

        Criteria critMembership = crit.createCriteria("memberships");
        Criteria critSubordinate = critMembership.createCriteria("subordinates");
        critSubordinate.add(Restrictions.eq("withinOrg", org));

        String[] arr = q.split(" ");
        Conjunction con = Restrictions.conjunction();
        for (String queryPart : arr) {
            Disjunction dis = Restrictions.disjunction();
            String s = queryPart + "%";
            dis.add(Restrictions.ilike("firstName", s));
            dis.add(Restrictions.ilike("surName", s));
            dis.add(Restrictions.ilike("name", s));
            dis.add(Restrictions.ilike("email", s));
            con.add(dis);
        }
        crit.add(con);


//        crit.add(Expression.ilike("name", q + "%"));

        crit.addOrder(Order.asc("surName"));
        crit.addOrder(Order.asc("firstName"));
        crit.addOrder(Order.asc("email"));

        List<Profile> list = DbUtils.toList(crit, Profile.class);
        List<Profile> deduped = new ArrayList<>();
        for (Profile p : list) {
            if (!deduped.contains(p)) {
                deduped.add(p);
            }
        }
        log.info("user search: " + q + " -> " + deduped.size());
        return deduped;
    }
}
