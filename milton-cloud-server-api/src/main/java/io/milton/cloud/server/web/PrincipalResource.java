package io.milton.cloud.server.web;

import io.milton.principal.DiscretePrincipal;



/**
 * Defines a type of principal which is also a Resource, so it can
 * be returned from the resource factory
 *
 * @author brad
 */
public interface PrincipalResource  extends DiscretePrincipal, CommonCollectionResource { //, LdapPrincipal {
    
}
