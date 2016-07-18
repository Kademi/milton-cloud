
package com.bradmcevoy.db;

import java.sql.Connection;

/**
 * Simply uses the given connection. No closing or releasing occurs
 * 
 * Not really in the spirit of connectionprovider, but certainly is
 *  simple
 * 
 * @author brad
 */
public class SimpleConnectionProvider implements ConnectionProvider {

    final Connection con;

    public SimpleConnectionProvider(Connection con) {
        this.con = con;
    }        
    
    public <T> T useConnection(ConnectionAccessor<T> acc) {
        return acc.useConnection(con);
    }

}
