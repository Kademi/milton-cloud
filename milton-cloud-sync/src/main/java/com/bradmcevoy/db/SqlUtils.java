
package com.bradmcevoy.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SqlUtils {

    public static void close(PreparedStatement stmt) {
        if (stmt == null) return;
        try {
            stmt.close();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
    public static void close(ResultSet rs) throws RuntimeException{
        if (rs == null) return;
        try {
            rs.close();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
}
