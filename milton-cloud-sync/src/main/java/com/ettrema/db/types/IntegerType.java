package com.ettrema.db.types;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class IntegerType implements FieldType<Integer>, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public String toString() {
        return "integer";
    }

    @Override
    public Integer get(String name, ResultSet rs) throws SQLException {
        return rs.getInt(name);
    }

    @Override
    public void set(PreparedStatement stmt, int index, Integer value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.INTEGER);
        } else {
            stmt.setInt(index, value);
        }             
        
    }

    @Override
    public Integer parse(Object v) {
        if (v instanceof Integer) {
            return (Integer) v;
        } else if (v instanceof Long) {
            Long d = (Long) v;
            return d.intValue();
        } else if (v instanceof String) {
            String s = (String) v;
            return Integer.parseInt(s);
        } else {
            throw new RuntimeException("Cant convert type: " + v.getClass());
        }
    }
}
