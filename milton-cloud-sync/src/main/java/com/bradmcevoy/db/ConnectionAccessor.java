package com.bradmcevoy.db;

import java.sql.Connection;

/**
 *
 * @author brad
 * 
 *  Simple interface for getting a connection. Class that use this will not assume
 *  the connection is open except during the execution of useConnection
 * 
 *  This allows providers to release the connection to the connection pool
 *  between calls.
 * 
 *  Note that implementations must consider transactionality when deciding whether
 *  or not to release a connection between calls
 */
public interface ConnectionAccessor<T> {
    T useConnection(Connection con);
}
