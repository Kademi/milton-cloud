package com.ettrema.db.types;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 *
 * @author brad
 */
public class Float8Type implements FieldType<Float>, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public String toString() {
        return "double precision";
    }

    @Override
    public Float get(String name, ResultSet rs) throws SQLException {
        return rs.getFloat(name);
    }

    @Override
    public void set(PreparedStatement stmt, int index, Float value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.REAL);
        } else {
            stmt.setFloat(index, value);
        }        
        
    }

    @Override
    public Float parse(Object v) {
        if (v instanceof Float) {
            return (Float) v;
        } else if (v instanceof Double) {
            Double d = (Double) v;
            return d.floatValue();
        } else if (v instanceof String) {
            String s = (String) v;
            return Float.parseFloat(s);
        } else {
            throw new RuntimeException("Cant convert type: " + v.getClass());
        }
    }
}
