
package io.milton.vfs.db;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitBasicColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.ImplicitPrimaryKeyJoinColumnNameSource;

/**
 *
 * @author brad
 */


public class ImprovedImplicitNamingStrategy extends  ImplicitNamingStrategyJpaCompliantImpl{


    @Override
    public Identifier determineBasicColumnName(ImplicitBasicColumnNameSource source) {
        Identifier id = super.determineBasicColumnName(source); 
        //System.out.println("determineBasicColumnName -> " + id.getText() );
        return id;
    }

    @Override
    public Identifier determinePrimaryKeyJoinColumnName(ImplicitPrimaryKeyJoinColumnNameSource source) {
        Identifier id = super.determinePrimaryKeyJoinColumnName(source); 
        //System.out.println("determinePrimaryKeyJoinColumnName -> " + id.getText() );
        return id;
    }
    
    

    
    
}
