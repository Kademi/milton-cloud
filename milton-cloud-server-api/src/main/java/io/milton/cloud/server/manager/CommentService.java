/*
 * Copyright 2012 McEvoy Software Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.manager;

import io.milton.cloud.common.CurrentDateService;
import io.milton.cloud.server.db.Comment;
import io.milton.cloud.server.db.ForumPost;
import io.milton.cloud.server.db.ForumReply;
import io.milton.cloud.server.web.AbstractContentResource;
import io.milton.cloud.server.web.CommentBean;
import io.milton.cloud.server.web.ProfileBean;
import io.milton.common.Path;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Website;
import io.milton.vfs.db.utils.SessionManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.Session;

import org.owasp.validator.html.*;

/**
 *
 * @author brad
 */
public class CommentService {

    private final CurrentDateService currentDateService;
    private String policyFile = "classpath:/antisamy-slashdot-1.4.4.xml";

    public CommentService(CurrentDateService currentDateService) {
        this.currentDateService = currentDateService;
    }

    public ForumReply newComment(ForumPost post, String newComment, ForumPost forumPost, Profile currentUser, Session session) {
        newComment = cleanComment(newComment);
        return forumPost.addComment(newComment, currentUser, currentDateService.getNow(), session);
    }

    public List<CommentBean> comments(AbstractContentResource r) {
        String contentId = getContentId(r);
        List<Comment> comments = Comment.findByContentId(contentId, SessionManager.session());
        List<CommentBean> beans = new ArrayList<>();
        for (Comment c : comments) {
            CommentBean b = toBean(c);
            beans.add(b);
        }
        return beans;
    }

    public Comment newComment(AbstractContentResource r, String userComment, Website website, Profile poster, Session session) {
        String contentId = getContentId(r);
        if (contentId == null) {
            throw new RuntimeException("No contentid for: " + r.getPath());
        }
        userComment = cleanComment(userComment);


        String title = r.getTitle();

        Comment c = new Comment();
        c.setContentId(contentId);
        c.setContentTitle(title);
        c.setContentHref(r.getHref());
        c.setPostDate(currentDateService.getNow());
        c.setNotes(userComment);
        c.setPoster(poster);
        c.setWebsite(website);
        session.save(c);
        return c;
    }

    private String cleanComment(String dirtyComment) {
        try {
            Policy policy;
            if (policyFile.startsWith("classpath:")) {
                String s = policyFile.replace("classpath:", "");
                try (InputStream in = getClass().getResourceAsStream(s)) {
                    if( in == null ) {
                        throw new RuntimeException("Couldnt find resource: " + s + " from " + getClass().getClassLoader());
                    }
                    policy = Policy.getInstance(in);
                } catch (IOException e) {
                    throw new RuntimeException(s, e);
                }
            } else {
                policy = Policy.getInstance(policyFile);
            }

            AntiSamy as = new AntiSamy();
            CleanResults cr = as.scan(dirtyComment, policy);
            return cr.getCleanHTML();
        } catch (PolicyException | ScanException e) {
            throw new RuntimeException(policyFile, e);
        }
    }

    public String getContentId(AbstractContentResource r) {
        Path p = r.getPath();
        Organisation org = r.getOrganisation();
        return org.getOrgId() + ":" + p;
    }

    private CommentBean toBean(Comment c) {
        CommentBean b = new CommentBean();
        b.setComment(c.getNotes());
        b.setDate(c.getPostDate().getTime());
        ProfileBean pb = ProfileBean.toBean(c.getPoster());
        b.setUser(pb);
        return b;
    }

    public String getPolicyFile() {
        return policyFile;
    }

    public void setPolicyFile(String policyFile) {
        this.policyFile = policyFile;
    }
}
