package com.ettrema.db.dialects;

import com.ettrema.db.Table;
import com.ettrema.db.types.FieldType;
import java.sql.Connection;

/**
 *
 * @author brad
 */
public interface Dialect {
    boolean tableExists(String name, Connection con);

    void createTable( Table table, Connection con );

    String getTypeName(FieldType type);
}
