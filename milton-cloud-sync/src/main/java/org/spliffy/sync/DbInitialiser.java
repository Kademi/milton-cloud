package org.spliffy.sync;

import com.ettrema.db.UseConnection;
import com.ettrema.db.dialects.Dialect;
import com.ettrema.db.dialects.HqlDialect;
import java.io.File;
import javax.sql.DataSource;
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.impl.GenericObjectPool;

/**
 * Just hard codes initialisation of an embedded H2 instance
 *
 *
 * @author brad
 */
public class DbInitialiser {

    private final DataSource ds;
    private final UseConnection useConnection;
    private final Dialect dialect;

    public DbInitialiser(File dbFile) {
        String url = "jdbc:h2:" + dbFile.getAbsolutePath();

        GenericObjectPool connectionPool = new GenericObjectPool( null );
        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory( url, "sa", "sa" );
        PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory( connectionFactory, connectionPool, null, null, false, true );
        ds = new PoolingDataSource( connectionPool );

        this.useConnection = new UseConnection( ds );

        dialect = new HqlDialect();
    }

    public UseConnection getUseConnection() {
        return useConnection;
    }

    public Dialect getDialect() {
        return dialect;
    }

    
}
