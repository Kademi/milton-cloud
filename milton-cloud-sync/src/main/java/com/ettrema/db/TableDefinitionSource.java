package com.ettrema.db;

import java.sql.Connection;
import java.util.List;

/**
 *
 * @author brad
 */
public interface TableDefinitionSource {
    List<? extends Table> getTableDefinitions();

    void onCreate(Table t, Connection con);
}
