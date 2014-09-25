package com.ettrema.db.types;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BinaryType implements FieldType<byte[]>, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public String toString() {
        return "binary";
    }

    @Override
    public byte[] get(String name, ResultSet rs) throws SQLException {
        return rs.getBytes(name);
    }

    @Override
    public void set(PreparedStatement stmt, int index, byte[] value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, java.sql.Types.VARBINARY);
        } else {
            stmt.setBytes(index, value);
        }
    }

    @Override
    public byte[] parse(Object v) {
        if (v instanceof byte[]) {
            return (byte[]) v;
        } else {
            throw new RuntimeException("Cant parse type of: " + v.getClass());
        }
    }
}
