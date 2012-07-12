package io.milton.cloud.server.web;

import io.milton.vfs.db.BaseEntity;

/**
 *
 * @author brad
 */
public interface BaseEntityResource extends CommonCollectionResource {
    /**
     * Get the entity this resource represents
     * 
     * @return 
     */
    BaseEntity getBaseEntity();
}
