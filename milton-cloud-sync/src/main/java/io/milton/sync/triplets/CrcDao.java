package io.milton.sync.triplets;

import com.ettrema.db.Table;
import com.ettrema.db.TableDefinitionSource;
import com.ettrema.db.types.FieldTypes;
import java.sql.*;
import java.util.*;

/**
 *
 * @author brad
 */
public class CrcDao {
    
    public static final CrcTable CRC = new CrcTable();
    
    
    public static  Map<String, CrcRecord> toMap(List<CrcRecord> records) {
        Map<String, CrcRecord> map = new HashMap<>();
        for (CrcRecord r : records) {
            map.put(r.name, r);
        }
        return map;
    }
    

    public void deleteCrc(Connection c, String path, String name) throws SQLException {
        String sql = "DELETE FROM " + CRC.tableName + " WHERE " + CRC.path.getName() + " = ?" + " AND " + CRC.name.getName() + " = ?";
        try (PreparedStatement stmt = c.prepareStatement(sql)) {
            CRC.path.set(stmt, 1, path);
            CRC.name.set(stmt, 2, name);
            stmt.execute();
        }
    }

    public void insertCrc(Connection c, String path, String name, String crc, long modDate) throws SQLException {
        String sql = CRC.getInsert();
        try (PreparedStatement stmt = c.prepareStatement(sql)) {
            CRC.crc.set(stmt, 1, crc);
            CRC.path.set(stmt, 2, path);
            CRC.name.set(stmt, 3, name);
            CRC.date.set(stmt, 4, new Timestamp(modDate));
            stmt.execute();
        }
    }

    public long getCrcRecordCount(Connection c) throws SQLException {
        final String q = CRC.getSelect();
        long n=0;
        try (PreparedStatement stmt = c.prepareStatement(q)) {
            try (ResultSet rs = stmt.executeQuery()) {                
                while (rs.next()) {
                    n++;
                }
            }
        }
        return n;
    }
    
    public List<CrcRecord> listCrcRecords(Connection c, String path) throws SQLException {        
        final String q = CRC.getSelect() + " WHERE " + CRC.path.getName() + " = ?";
        List<CrcRecord> oldRecords = new ArrayList<>();
        try (PreparedStatement stmt = c.prepareStatement(q)) {
            CRC.path.set(stmt, 1, path);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String crc = CRC.crc.get(rs);
                    String name = CRC.name.get(rs);
                    Timestamp date = CRC.date.get(rs);
                    CrcRecord r = new CrcRecord(crc, name, date);
                    oldRecords.add(r);
                }
            }
        }
        return oldRecords;
    }


    
    
    public class CrcRecord {

        String crc;
        String name;
        Timestamp date;

        CrcRecord(String crc, String name, Timestamp date) {
            this.crc = crc;
            this.name = name;
            this.date = date;
        }
    }
    

    public static class CrcTable extends com.ettrema.db.Table {

        public final Table.Field<String> crc = add("crc", FieldTypes.CHARACTER_VARYING, false); // use "crc" instead of "hash" because hash is a reserved word
        public final Table.Field<String> path = add("path", FieldTypes.CHARACTER_VARYING, false);
        public final Table.Field<String> name = add("name", FieldTypes.CHARACTER_VARYING, false);
        public final Table.Field<java.sql.Timestamp> date = add("date_verified", FieldTypes.TIMESTAMP, false);

        public CrcTable() {
            super("file_crcs");
        }
    }    
}
