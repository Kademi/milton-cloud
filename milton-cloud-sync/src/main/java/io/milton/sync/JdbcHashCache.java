package io.milton.sync;

import com.bradmcevoy.utils.With;
import com.ettrema.db.Table;
import com.ettrema.db.TableCreatorService;
import com.ettrema.db.TableDefinitionSource;
import com.ettrema.db.UseConnection;
import com.ettrema.db.dialects.Dialect;
import com.ettrema.db.types.FieldTypes;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import org.hashsplit4j.api.HashCache;

/**
 *
 * @author brad
 */
public class JdbcHashCache implements HashCache {

    public static final CrcCacheTable HASH_CACHE = new CrcCacheTable();
    private final UseConnection useConnection;
    private final String group; // just so we can use the same table for different things

    private long hits;
    private long misses;
    private long inserts;
    
    /**
     * 
     * @param useConnection
     * @param dialect
     * @param group - so we can cache different collections in one table
     */
    public JdbcHashCache(UseConnection useConnection, Dialect dialect, String group) {
        this.useConnection = useConnection;
        this.group = group;
        TableDefinitionSource defs = new TableDefinitionSource() {

            @Override
            public List<? extends Table> getTableDefinitions() {
                return Arrays.asList(HASH_CACHE);
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
    public boolean hasHash(final String hash) {
        final String sql = HASH_CACHE.getSelect() + " WHERE " + HASH_CACHE.crc.getName() + " = ? AND " + HASH_CACHE.group.getName() + " = ?";         
        Boolean result = useConnection.use(new With<Connection, Boolean>() {

            @Override
            public Boolean use(Connection con) throws Exception {
                try (PreparedStatement stmt = con.prepareStatement(sql)) {
                    HASH_CACHE.crc.set(stmt, 1, hash);
                    HASH_CACHE.group.set(stmt, 2, group);
                    ResultSet rs = stmt.executeQuery();
                    try {
                        while (rs.next()) {
                            return Boolean.TRUE;
                        }
                        return Boolean.FALSE;
                    } finally {
                        UseConnection.close(rs);
                        UseConnection.close(stmt);
                    }
                }
            }
        });
        if( result ) {
            hits++;
        } else {
            misses++;
        }
        return result;
    }

    public long getHits() {
        return hits;
    }

    public long getMisses() {
        return misses;
    }

    public long getInserts() {
        return inserts;
    }
    
    

    @Override
    public void setHash(final String hash) {
        inserts++;
        final String insertSql = HASH_CACHE.getInsert();

        useConnection.use(new With<Connection, Object>() {

            @Override
            public Object use(Connection con) throws Exception {

                try (PreparedStatement stmt = con.prepareStatement(insertSql)) {
                    HASH_CACHE.crc.set(stmt, 1, hash);
                    HASH_CACHE.group.set(stmt, 2, group);
                    stmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
                    stmt.execute();
                    UseConnection.close(stmt);
                    con.commit();

                    return null;
                }
            }
        });
    }

    public static class CrcCacheTable extends com.ettrema.db.Table {

        public final Table.Field<String> crc = add("crc", FieldTypes.CHARACTER_VARYING, false); // use "crc" instead of "hash" because hash is a reserved word
        public final Table.Field<String> group = add("crc_group", FieldTypes.CHARACTER_VARYING, false);
        public final Table.Field<java.sql.Timestamp> date = add("date_verified", FieldTypes.TIMESTAMP, false);

        public CrcCacheTable() {
            super("crc_cache");
        }
    }
}
