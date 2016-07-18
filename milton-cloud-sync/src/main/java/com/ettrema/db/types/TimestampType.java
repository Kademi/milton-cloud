package com.ettrema.db.types;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

/**
 *
 * @author brad
 */
public class TimestampType implements FieldType<java.sql.Timestamp>, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public String toString() {
        return "timestamp";
    }

    @Override
    public java.sql.Timestamp get(String name, ResultSet rs) throws SQLException {
        return rs.getTimestamp(name);
    }

    @Override
    public void set(PreparedStatement stmt, int index, Timestamp value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.TIMESTAMP);
        } else {
            stmt.setTimestamp(index, value);
        }               
    }

    @Override
    public java.sql.Timestamp parse(Object v) {
        if (v instanceof java.sql.Timestamp) {
            return (java.sql.Timestamp) v;
        } else if (v instanceof String) {
            String s = (String) v;
            return java.sql.Timestamp.valueOf(s);
        } else {
            throw new RuntimeException("Cant convert type: " + v.getClass());
        }
    }
}
