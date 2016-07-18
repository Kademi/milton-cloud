package com.ettrema.db.types;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author brad
 */
public class LongType implements FieldType<Long>, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public String toString() {
        return "long";
    }

    @Override
    public Long get(String name, ResultSet rs) throws SQLException {        
        Object oLong = rs.getObject(name);
        if( oLong == null ) {
            return null;
        } else {
            return (Long)oLong;
        }
    }

    @Override
    public void set(PreparedStatement stmt, int index, Long value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, java.sql.Types.BIGINT);
        } else {
            stmt.setLong(index, value);
        }
    }

    @Override
    public Long parse(Object v) {
        if (v instanceof Long) {
            return (Long) v;
        } else if (v instanceof Integer) {
            Integer d = (Integer) v;
            return d.longValue();
        } else if (v instanceof String) {
            String s = (String) v;
            return Long.parseLong(s);
        } else {
            throw new RuntimeException("Cant convert type: " + v.getClass());
        }
    }
}
