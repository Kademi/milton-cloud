/*
 * Copyright (C) 2012 McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.apps.email;

import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.db.EmailTrigger;
import io.milton.cloud.server.db.GroupRecipient;
import io.milton.cloud.server.mail.EmailTriggerType;
import io.milton.cloud.server.mail.Option;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.cloud.server.web.*;
import io.milton.cloud.server.web.templating.DataBinder;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.cloud.server.web.templating.MenuItem;
import io.milton.resource.AccessControlledResource.Priviledge;
import io.milton.http.Auth;
import io.milton.http.FileItem;
import io.milton.http.Range;
import io.milton.http.Response.Status;
import io.milton.principal.Principal;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.http.values.ValueAndType;
import io.milton.http.webdav.PropFindResponse.NameAndError;
import io.milton.http.webdav.PropertySourcePatchSetter;
import io.milton.property.BeanPropertyResource;
import io.milton.resource.GetableResource;
import io.milton.resource.PostableResource;
import io.milton.vfs.db.utils.SessionManager;
import javax.xml.namespace.QName;
import org.hibernate.Session;
import org.hibernate.Transaction;

import static io.milton.context.RequestContext._;
import io.milton.vfs.db.Group;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 *
 * @author brad
 */
@BeanPropertyResource(value = "milton")
public class ManageAutoEmailPage extends AbstractResource implements GetableResource, PostableResource, PropertySourcePatchSetter.CommitableResource {

    private static final Logger log = LoggerFactory.getLogger(GroupEmailPage.class);
    private final CommonCollectionResource parent;
    private final EmailTrigger job;
    private JsonResult jsonResult;
    
    private List<EmailTriggerType> triggerTypes;

    public ManageAutoEmailPage(EmailTrigger job, CommonCollectionResource parent) {
        this.job = job;
        this.parent = parent;
    }

    @Override
    public String processForm(Map<String, String> parameters, Map<String, FileItem> files) throws BadRequestException, NotAuthorizedException, ConflictException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();

        if (parameters.containsKey("group")) {
            String groupName = parameters.get("group");
            String sIsRecip = parameters.get("isRecip");
            if ("true".equals(sIsRecip)) {
                addGroup(groupName);
            } else {
                removeGroup(groupName);
            }
        }

        try {
            _(DataBinder.class).populate(job, parameters);
            job.checkNulls();
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
        session.save(job);
        tx.commit();

        jsonResult = new JsonResult(true);
        return null;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
        if (jsonResult != null) {
            jsonResult.write(out);
        } else {
            MenuItem.setActiveIds("menuTalk", "menuEmails", "menuAutoEmail");
            _(HtmlTemplater.class).writePage("admin", "email/manageAutoEmail", this, params, out);
        }
    }

    public EmailTrigger getTrigger() {
        return job;
    }

    public String getTitle() {
        return job.getTitle();
    }

    public void setTitle(String s) {
        job.setTitle(s);
    }

    public String getNotes() {
        return job.getNotes();
    }

    public void setNotes(String s) {
        job.setNotes(s);
    }

    public String getFromAddress() {
        return job.getFromAddress();
    }

    public void setFromAddress(String s) {
        job.setFromAddress(s);
    }

    public String getSubject() {
        return job.getSubject();
    }

    public void setSubject(String s) {
        job.setSubject(s);
    }

    public String getHtml() {
        return job.getHtml();
    }

    @Override
    public boolean isDir() {
        return false;
    }

    @Override
    public CommonCollectionResource getParent() {
        return parent;
    }

    @Override
    public String getName() {
        return job.getName();
    }

    @Override
    public Date getModifiedDate() {
        return null;
    }

    @Override
    public Date getCreateDate() {
        return null;
    }

    @Override
    public Map<Principal, List<Priviledge>> getAccessControlList() {
        return null;
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return null;
    }

    @Override
    public String getContentType(String accepts) {
        return "text/html";
    }

    @Override
    public Long getContentLength() {
        return null;
    }

    @Override
    public Organisation getOrganisation() {
        return parent.getOrganisation();
    }

    @Override
    public boolean is(String type) {
        if (type.equals("manageRewards")) {
            return true;
        }
        return super.is(type);
    }

    @Override
    public void doCommit(Map<QName, ValueAndType> knownProps, Map<Status, List<NameAndError>> errorProps) throws BadRequestException, NotAuthorizedException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        session.save(job);
        tx.commit();
    }

    public List<Group> getGroupRecipients() {
        List<Group> list = new ArrayList<>();
        if (job.getGroupRecipients() != null) {
            for (GroupRecipient gr : job.getGroupRecipients()) {
                list.add(gr.getRecipient());
            }
        }
        return list;
    }

    public List<Group> getAllGroups() {
        return job.getOrganisation().groups(SessionManager.session());
    }

    public boolean isSelected(Group g) {
        return getGroupRecipients().contains(g);
    }

    private void removeGroup(String groupName) {
        if (job.getGroupRecipients() == null) {
            return;
        }
        Iterator<GroupRecipient> it = job.getGroupRecipients().iterator();
        Session session = SessionManager.session();
        while (it.hasNext()) {
            GroupRecipient gr = it.next();
            Group g = gr.getRecipient();
            if (g.getName().equals(groupName)) {
                System.out.println("removed: " + groupName);
                it.remove();
                session.delete(gr);
            }
        }
    }

    private void addGroup(String groupName) {
        if (job.getGroupRecipients() == null) {
            job.setGroupRecipients(new ArrayList<GroupRecipient>());
        }
        for (GroupRecipient gr : job.getGroupRecipients()) {
            if (gr.getRecipient().getName().equals(groupName)) {
                System.out.println("already in");
                return;
            }
        }
        Session session = SessionManager.session();
        Group g = job.getOrganisation().group(groupName, session);
        if (g == null) {
            log.warn("group not found: " + groupName);
            return;
        }
        GroupRecipient gr = new GroupRecipient();
        gr.setJob(job);
        gr.setRecipient(g);
        session.save(gr);
        job.getGroupRecipients().add(gr);

    }
    
    public Map<String,Map<String,List<Option>>> getEmailTriggerTypes() {
        if( triggerTypes == null ) {
            triggerTypes = _(ApplicationManager.class).getEmailTriggerTypes();
        }
        Map<String,Map<String,List<Option>>> map = new LinkedHashMap<>();
        for( EmailTriggerType t : triggerTypes ) {
            System.out.println("Trigger type: " + t.getEventId());
            map.put(t.getEventId(), toOptions(t));
        }
        System.out.println("map size: " + map.size());
        return map;
    }
    
    public String triggerOptionLabel(String eventId, String optionCode ) {
        for(EmailTriggerType t : triggerTypes) {
            if( t.getEventId().equals(eventId)) {
                return t.label(optionCode);
            }
        }
        return null;
    }
    
    public Map<String,List<Option>> toOptions(EmailTriggerType type) {
        Map<String,List<Option>> map = new LinkedHashMap<>();
        List<Option> options1 = type.options1(getOrganisation());
        if(options1 != null ) {
            map.put("triggerCondition1", options1);
        }
        List<Option> options2 = type.options2(getOrganisation());
        if(options2 != null ) {
            map.put("triggerCondition2", options2);
        }
        List<Option> options3 = type.options3(getOrganisation());
        if(options3 != null ) {
            map.put("triggerCondition3", options3);
        }
        List<Option> options4 = type.options4(getOrganisation());
        if(options4 != null ) {
            map.put("triggerCondition4", options4);
        }
        List<Option> options5 = type.options5(getOrganisation());
        if(options5 != null ) {
            map.put("triggerCondition5", options5);
        }
        System.out.println("options map suze: " + map.size() + " for " + type.getEventId());
        return map;
    }
    
    public String triggerOptionValue(String optCode) {
        switch (optCode) {
            case "triggerCondition1":
                return getTrigger().getTriggerCondition1();
            case "triggerCondition2":
                return getTrigger().getTriggerCondition2();
            case "triggerCondition3":
                return getTrigger().getTriggerCondition3();
            case "triggerCondition4":
                return getTrigger().getTriggerCondition4();
            case "triggerCondition5":
                return getTrigger().getTriggerCondition5();
                
        }
        return null;
    }
}
