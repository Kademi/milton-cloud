package com.ettrema.db;

import com.bradmcevoy.utils.With;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;

/**
 *
 * @author brad
 */
public class UseConnection {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( UseConnection.class );


    private final DataSource ds;

    public UseConnection( DataSource ds ) {
        this.ds = ds;
    }

    public <O> O use( With<Connection, O> with ) {
        Connection con = null;
        try {
            con = ds.getConnection();
            O o = with.use( con );
            commit( con );
            return o;
        } catch( Exception ex ) {
            rollback(con);
            throw new RuntimeException( ex );
        } finally {
            close( con );
        }
    }

    private void commit(Connection con) throws SQLException {
        con.commit();
    }

    private void rollback( Connection con ) {
        if( con != null ) {
            try {
                con.rollback();
            } catch( SQLException ex ) {
                log.error("exception rolling back", ex);
            }
        }
    }

    public static void close( Connection con ) {
        if( con != null ) {
            try {
                con.close();
            } catch( SQLException ex ) {
            }
        }
    }

    public static void close( ResultSet rs ) {
        if( rs != null ) {
            try {
                rs.close();
            } catch( SQLException ex ) {
            }
        }
    }

    public static void close( PreparedStatement stmt ) {
        if( stmt != null ) {
            try {
                stmt.close();
            } catch( SQLException ex ) {
            }
        }

    }

}
