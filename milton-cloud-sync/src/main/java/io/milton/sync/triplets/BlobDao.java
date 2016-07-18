package io.milton.sync.triplets;

import com.ettrema.db.Table;
import com.ettrema.db.types.FieldTypes;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author brad
 */
public class BlobDao {
    
    public static final BlobTable BLOB = new BlobTable();
    
    
    public List<BlobVector> listBlobsByHash(Connection c, String hash) throws SQLException {
        final String q = BLOB.getSelect() + " WHERE " + BLOB.crc.getName() + " = ?";
        List<BlobVector> blobVectors = new ArrayList<>();
        try (PreparedStatement stmt = c.prepareStatement(q)) {
            BLOB.crc.set(stmt, 1, hash);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String crc = BLOB.crc.get(rs);
                    String path = BLOB.path.get(rs);
                    Timestamp date = BLOB.date.get(rs);
                    long offset = BLOB.offset.get(rs);
                    int length = BLOB.length.get(rs);
                    BlobVector r = new BlobVector(path, crc, offset, length, date);
                    blobVectors.add(r);
                }
            }
        }
        return blobVectors;
    }

    public void insertBlob(String hash, byte[] bytes,String path, long offset, Connection con) throws RuntimeException {
        String sql = BLOB.getInsert();
        try {
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                BLOB.path.set(stmt, 1, path);
                BLOB.crc.set(stmt, 2, hash);
                BLOB.offset.set(stmt, 3, offset);
                BLOB.length.set(stmt, 4, bytes.length);
                BLOB.date.set(stmt, 5, new Timestamp(System.currentTimeMillis()));
                stmt.execute();
            }
            
        } catch (SQLException ex) {
            throw new RuntimeException(sql, ex);
        }
    }

    public void deleteBlob(String path, String crc, long offset, Connection c) throws SQLException {
        String sql = "DELETE FROM " + BLOB.tableName + " WHERE " + BLOB.path.getName() + " = ?" + " AND " + BLOB.crc.getName() + " = ? AND " + BLOB.offset.getName() + " = ?";
        try (PreparedStatement stmt = c.prepareStatement(sql)) {
            BLOB.path.set(stmt, 1, path);
            BLOB.crc.set(stmt, 2, crc);
            BLOB.offset.set(stmt, 3, offset);
            stmt.execute();
        }        
    }

    
    public static class BlobTable extends com.ettrema.db.Table {

        public final Table.Field<String> path = add("path", FieldTypes.CHARACTER_VARYING, false);
        public final Table.Field<String> crc = add("crc", FieldTypes.CHARACTER_VARYING, false);
        public final Table.Field<Long> offset = add("offset", FieldTypes.LONG, false);
        public final Table.Field<Integer> length = add("length", FieldTypes.INTEGER, false);
        public final Table.Field<java.sql.Timestamp> date = add("date_verified", FieldTypes.TIMESTAMP, false);

        public BlobTable() {
            super("file_blobs");
        }
    }


    /**
     * Specifies where to find a blob, ie what file its in, at the given offset
     * and of the given length
     *
     */
    public class BlobVector {

        final String path;
        final String crc;
        final long offset;
        final int length;
        final Timestamp date;

        public BlobVector(String path, String crc, long offset, int length, Timestamp date) {
            this.path = path;
            this.crc = crc;
            this.offset = offset;
            this.length = length;
            this.date = date;
        }

        @Override
        public String toString() {
            return "BlobVector: " + path + "/" + offset + "/" + length;
        }
    }
}
