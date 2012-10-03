package io.milton.cloud.server.web;

import io.milton.vfs.db.Repository;


/**
 * Marker interface for resources which are contained within, or are, a repository
 *
 * @author brad
 */
public interface CommonRepositoryResource extends CommonResource {
    Repository getRepository();
}
