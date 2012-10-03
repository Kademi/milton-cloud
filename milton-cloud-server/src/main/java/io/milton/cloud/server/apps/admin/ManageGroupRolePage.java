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
package io.milton.cloud.server.apps.admin;

import io.milton.cloud.server.web.AbstractResource;
import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.DeletableResource;
import io.milton.vfs.db.GroupRole;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.utils.SessionManager;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Resource which represents a group role attached to a group
 *
 * @author brad
 */
public class ManageGroupRolePage extends AbstractResource implements DeletableResource {

    private static final Logger log = LoggerFactory.getLogger(ManageGroupRolePage.class);
    private final ManageGroupFolder parent;
    private final GroupRole groupRole;
    private final String name;
    
    public ManageGroupRolePage(ManageGroupFolder parent, GroupRole groupRole) {
        this.groupRole = groupRole;
        this.parent = parent;
        String s = groupRole.getRoleName();
        if( groupRole.getRepository() != null ) {
            s += "_r_" + groupRole.getRepository().getName();
        } else if( groupRole.getWithinOrg() != null ) {
            s += "_o_" + groupRole.getWithinOrg().getName();
        }
        this.name = s;
    }
    
    public String getTitle() {
        String t = groupRole.getRoleName();
        if( groupRole.getRepository() != null ) {
            t += ", on " + groupRole.getRepository();
        } else if( groupRole.getWithinOrg() != null ) {
            t += ", on " + groupRole.getWithinOrg().getOrgId();
        } else {
            t += ", on their own organisation";
        }
        return t;
    }

    @Override
    public CommonCollectionResource getParent() {
        return parent;
    }

    @Override
    public Organisation getOrganisation() {
        return parent.getOrganisation();
    }

    @Override
    public Priviledge getRequiredPostPriviledge(Request request) {
        return Priviledge.WRITE_ACL;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
        Session session = SessionManager.session();
        Transaction tx = session.beginTransaction();
        groupRole.getGrantee().getGroupRoles().remove(groupRole);
        groupRole.delete(session);
        tx.commit();
    }

}
