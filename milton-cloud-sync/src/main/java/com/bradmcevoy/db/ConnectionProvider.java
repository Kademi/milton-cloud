package com.bradmcevoy.db;

/**
 *
 * @author brad
 */
public interface ConnectionProvider {
    <T> T useConnection(ConnectionAccessor<T> acc);
}
