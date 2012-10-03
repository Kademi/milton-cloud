package io.milton.cloud.server.web;

import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.Resource;

import java.util.Date;

import static io.milton.context.RequestContext._;
import io.milton.http.Request;

/**
 *
 * @author brad
 */
public abstract class AbstractCollectionResource extends AbstractResource implements CommonCollectionResource {

    private Date modDate;
    private Date createdDate;

    public AbstractCollectionResource() {
    }

    public AbstractCollectionResource(Date createDate, Date modDate) {
        this.createdDate = createDate;
        this.modDate = modDate;
    }

    @Override
    public boolean isDir() {
        return true;
    }

    @Override
    public Date getModifiedDate() {
        return modDate;
    }

    @Override
    public Date getCreateDate() {
        return createdDate;
    }

    /**
     * Simple implementation which just traverses the getChildren collection
     * looking for a matching name. Override if you need better peformance, eg
     * for large lists of children
     *
     * @param childName
     * @return
     * @throws NotAuthorizedException
     * @throws BadRequestException
     */
    @Override 
    public Resource child(String childName) throws NotAuthorizedException, BadRequestException {
        Resource r = _(ApplicationManager.class).getPage(this, childName);
        if (r != null) {
            return r;
        }
        return NodeChildUtils.childOf(getChildren(), childName);
    }

    @Override
    public boolean is(String type) {
        if( type.equals("folder") || type.equals("collection") || "directory".equals(type) ) {
            return true;
        }
        return super.is(type);
    }
    
    @Override
    public boolean isPublic() {
        return false;
    }

    @Override
    public Priviledge getRequiredPostPriviledge(Request request) {
        return Priviledge.READ_CONTENT;
    }      


    
}
