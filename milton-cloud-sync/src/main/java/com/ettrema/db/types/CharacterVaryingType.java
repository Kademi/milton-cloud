package com.ettrema.db.types;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class CharacterVaryingType implements FieldType<String>, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public String toString() {
        return "character varying";
    }

    @Override
    public String get(String name, ResultSet rs) throws SQLException {
        return rs.getString(name);
    }

    @Override
    public void set(PreparedStatement stmt, int index, String value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.VARCHAR);
        } else {
            stmt.setString(index, value);
        }
    }

    @Override
    public String parse(Object v) {
        if (v instanceof String) {
            return (String) v;
        } else {
            return v.toString();
        }
    }
}
