package io.milton.sync;

import com.bradmcevoy.utils.With;
import io.milton.common.Path;
import com.ettrema.db.Table;
import com.ettrema.db.TableCreatorService;
import com.ettrema.db.TableDefinitionSource;
import com.ettrema.db.UseConnection;
import com.ettrema.db.dialects.Dialect;
import com.ettrema.db.types.FieldTypes;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

/**
 * Uses a local database to record the sync status for local files
 * and directories.
 * 
 * A record is recorded whenever a local resource is found to be in sync with
 * the remote resource.
 * 
 * An instance is for a single local root directory and remote server and path.
 * Records when stored are stored with that information, so syncing to different
 * remote servers, or different local directories, is possible with different instances
 * using the same table.
 *
 * @author brad
 */
public class JdbcSyncStatusStore implements SyncStatusStore {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(JdbcSyncStatusStore.class);
    
    public static final LastBackedUpTable SYNC_TABLE = new LastBackedUpTable();
    private final UseConnection useConnection;
    private final String baseRemoteAddress;
    private final File root;

    /**
     *
     * @param useConnection
     * @param dialect
     * @param group - so we can cache different collections in one table
     */
    public JdbcSyncStatusStore(UseConnection useConnection, Dialect dialect,String baseRemoteAddress, File root) {
        this.useConnection = useConnection;
        this.baseRemoteAddress = baseRemoteAddress;
        this.root = root;
        TableDefinitionSource defs = new TableDefinitionSource() {

            @Override
            public List<? extends Table> getTableDefinitions() {
                return Arrays.asList(SYNC_TABLE);
            }

            @Override
            public void onCreate(Table t, Connection con) {
            }
        };
        final TableCreatorService creatorService = new TableCreatorService(null, Arrays.asList(defs), dialect);

        useConnection.use(new With<Connection, Object>() {

            @Override
            public Object use(Connection con) throws Exception {
                creatorService.processTableDefinitions(con);
                return null;
            }
        });
    }

    @Override 
    public String findBackedUpHash(Path path) {
        final File f = toFile(path);
        final String sql = SYNC_TABLE.getSelect() + " WHERE " + SYNC_TABLE.localPath.getName() + " = ? AND " + SYNC_TABLE.remoteRoot.getName() + " = ?";
        String hash = useConnection.use(new With<Connection, String>() {

            @Override
            public String use(Connection con) throws Exception {
                PreparedStatement stmt = con.prepareStatement(sql);
                stmt.setString(1, f.getAbsolutePath());
                stmt.setString(2, baseRemoteAddress);
                ResultSet rs = stmt.executeQuery();
                try {
                    if (rs.next()) {
                        String hash = SYNC_TABLE.crc.get(rs);
                        return hash;
                    } else {
                        return null;
                    }
                } finally {
                    UseConnection.close(rs);
                    UseConnection.close(stmt);
                }
            }
        });
        return hash;
    }

    @Override
    public void setBackedupHash(Path path, final String hash) {        
        final File f = toFile(path);
//        log.trace("setBackedupHash: " + path + " hash: " + hash);
        final String deleteSql = SYNC_TABLE.getDeleteBy(SYNC_TABLE.localPath);

        final String insertSql = SYNC_TABLE.getInsert();

        useConnection.use(new With<Connection, Object>() {

            @Override
            public Object use(Connection con) throws Exception {
                PreparedStatement stmt = con.prepareStatement(deleteSql);
                stmt.setString(1, f.getAbsolutePath());
                stmt.execute();
                UseConnection.close(stmt);

                stmt = con.prepareStatement(insertSql);
                SYNC_TABLE.localPath.set(stmt, 1, f.getAbsolutePath());
                SYNC_TABLE.remoteRoot.set(stmt, 2, baseRemoteAddress);
                SYNC_TABLE.crc.set(stmt, 3, hash);
                SYNC_TABLE.date.set(stmt, 4, new Timestamp(System.currentTimeMillis()));
                stmt.execute();
                UseConnection.close(stmt);
                con.commit();

                return null;
            }
        });
    }

    @Override
    public void clearBackedupHash(Path path) {
        final File f = toFile(path);
        final String deleteSql = SYNC_TABLE.getDeleteBy(SYNC_TABLE.localPath);

        useConnection.use(new With<Connection, Object>() {

            @Override
            public Object use(Connection con) throws Exception {
                try (PreparedStatement deleteStmt = con.prepareStatement(deleteSql)) {
                    deleteStmt.setString(1, f.getAbsolutePath());
                    deleteStmt.execute();
                }
                con.commit();

                return null;
            }
        });
    }
    



    public static class LastBackedUpTable extends com.ettrema.db.Table {
        /**
         * Full path to the local file
         */
        public final Table.Field<String> localPath = add("localPath", FieldTypes.CHARACTER_VARYING, false);
        
        /**
         * Base address for the remote server
         */
        public final Table.Field<String> remoteRoot = add("remoteRoot", FieldTypes.CHARACTER_VARYING, false);
        public final Table.Field<String> crc = add("crc", FieldTypes.CHARACTER_VARYING, false); // the last backed up crc of this local file
        public final Table.Field<java.sql.Timestamp> date = add("date_modified", FieldTypes.TIMESTAMP, false); // the date/time which the file was backed up, or that it was first discoverd assert having been backed up;

        public LastBackedUpTable() {
            super("sync_status");
        }
    }
    
    private File toFile(Path path) {
        File f = root;
        for (String fname : path.getParts()) {
            f = new File(f, fname);
        }
        return f;
    }    
}
