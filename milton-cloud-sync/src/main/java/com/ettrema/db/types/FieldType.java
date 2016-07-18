package com.ettrema.db.types;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface FieldType<T> {

    T get(String name, ResultSet rs) throws SQLException;

    void set(PreparedStatement stmt, int index, T value) throws SQLException;

    /**
     * Attempt to parse whatever object is given to the actual value. Should handle
     * strings and similar types. Eg Integer should handle long, date should
     * handle timestamp, etc
     * 
     * @param v
     * @return 
     */
    T parse(Object v);
}
