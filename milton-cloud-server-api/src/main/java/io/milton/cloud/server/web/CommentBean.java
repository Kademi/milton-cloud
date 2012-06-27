package io.milton.cloud.server.web;

import java.io.Serializable;
import java.util.Date;


/**
 * Represents a comment by a user on some resource.
 *
 * @author brad
 */
public class CommentBean implements Serializable{
    private static final long serialVersionUID = 1L;
    
    private String comment;
    private long date;
    private ProfileBean user;

    public ProfileBean getUser() {
        return user;
    }

    public void setUser(ProfileBean profile) {
        this.user = profile;
    }
    

    public long getDate() {
        return date;
    }
    
    public void setDate(long dt) {
        date = dt;
    }

    public String getComment() {
        return comment;
    }

    public void setComment( String comment ) {
        this.comment = comment;
    }

}
