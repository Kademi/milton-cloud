package com.ettrema.vfs;

import com.ettrema.context.RequestContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;


/**
 *
 * @author brad
 */
public class PostgresUtils {


    public static Connection con() {
        return requestContext().get( Connection.class );
    }

    public static RequestContext requestContext() {
        return RequestContext.getCurrent();
    }

    public static void close( InputStream in ) {
        if( in == null ) return;
        try {
            in.close();
        } catch( IOException ex ) {
            throw new RuntimeException( ex );
        }
    }

    public static void close( OutputStream out ) {
        if( out == null ) return;

        try {
            out.close();
        } catch( IOException ex ) {
            throw new RuntimeException( ex );
        }
    }

    public static void close( ResultSet rs ) {
        if( rs == null ) {
            return;
        }
        try {
            rs.close();
        } catch( SQLException ex ) {
            throw new RuntimeException( ex );
        }
    }


    static boolean isEmpty( List roots ) {
        return roots == null || roots.size()==0;
    }

    public static void close( PreparedStatement stmt ) {
        if( stmt == null ) {
            return ;
        } else {
            try {
                stmt.close();
            } catch( SQLException ex ) {
                
            }
        }
    }
}
