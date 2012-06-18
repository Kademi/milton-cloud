package io.milton.cloud.server.web;

import io.milton.principal.DiscretePrincipal;
import io.milton.http.caldav.CalDavPrincipal;
import io.milton.http.carddav.CardDavPrincipal;
import io.milton.ldap.LdapPrincipal;



/**
 * Defines a type of principal which is also a Resource, so it can
 * be returned from the resource factory
 *
 * @author brad
 */
public interface PrincipalResource  extends DiscretePrincipal, CalDavPrincipal, CardDavPrincipal, CommonCollectionResource, LdapPrincipal {
    
}
