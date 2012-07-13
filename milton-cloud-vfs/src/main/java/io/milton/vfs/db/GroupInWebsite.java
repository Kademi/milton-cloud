package io.milton.vfs.db;

import io.milton.vfs.db.Website;
import io.milton.vfs.db.utils.DbUtils;
import java.io.Serializable;
import java.util.List;
import javax.persistence.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.criterion.Expression;

/**
 * Creates a link between a program and a website, which makes that program
 * available on that website. Groups are given access to programs, so a user can
 * only access program content on a website if they are in a group which has
 * been given access to that program
 *
 * @author brad
 */
@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_group", "website"})}// item names must be unique within a directory
)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class GroupInWebsite implements Serializable {

    public static List<GroupInWebsite> findByWebsite(Website w, Session session) {
        Criteria crit = session.createCriteria(GroupInWebsite.class);
        crit.add(Expression.eq("website", w));
        return DbUtils.toList(crit, GroupInWebsite.class);
    }

    public static List<GroupInWebsite> findByGroup(Group g, Session session) {
        Criteria crit = session.createCriteria(GroupInWebsite.class);
        crit.add(Expression.eq("userGroup", g));
        return DbUtils.toList(crit, GroupInWebsite.class);
    }
    
    
    private long id;
    private Group userGroup;
    private Website website;
    private String registrationMode;

    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @ManyToOne(optional=false)
    public Group getUserGroup() {
        return userGroup;
    }

    public void setUserGroup(Group g) {
        this.userGroup = g;
    }

    @ManyToOne(optional = false)
    public Website getWebsite() {
        return website;
    }

    public void setWebsite(Website website) {
        this.website = website;
    }

    /**
     * Allowable registration option: - "o" = open, anyone can register and be
     * immediately active - "c" = closed, no self registration - "a" =
     * administrator enabled, anyone can register but their account only becomes
     * active after being enabled
     *
     * @return
     */
    @Column(nullable = false)
    public String getRegistrationMode() {
        return registrationMode;
    }

    public void setRegistrationMode(String registrationMode) {
        this.registrationMode = registrationMode;
    }
}
