package com.ettrema.context;

import java.sql.Connection;
import java.sql.SQLException;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CtxJdbcConnectionFactory implements Factory<Connection>  {
    public static Class[] classes = {Connection.class};
    
    private static final Logger log = LoggerFactory.getLogger( CtxJdbcConnectionFactory.class );
    
    private final String url;

    private final String user;

    private final String password;

    private final String driverClassName;

    private BasicDataSource dataSource;

    private boolean poolPreparedStatements = false;

    private int initialSize = 1;

    private int minIdle = GenericObjectPool.DEFAULT_MIN_IDLE;

    private int maxIdle = GenericObjectPool.DEFAULT_MAX_IDLE;

    private int maxActive = GenericObjectPool.DEFAULT_MAX_ACTIVE;

    public CtxJdbcConnectionFactory( String url, String user, String password, String driverClassName) {
        this.url = url;
        this.user = user;
        this.password = password;
        this.driverClassName = driverClassName;
    }

    public CtxJdbcConnectionFactory(BasicDataSource dataSource) {
        this.dataSource = dataSource;
        this.url = dataSource.getUrl();
        this.user = dataSource.getUsername();
        this.password = null;
        this.driverClassName = dataSource.getDriverClassName();
    }

    public CtxJdbcConnectionFactory() {
        this.url = null;
        this.user = null;
        this.password = null;
        this.driverClassName = null;
    }


    public void setDataSource(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    
    @Override
    public Class[] keyClasses() {
        return classes;
    }
    
    @Override
    public String[] keyIds() {
        return null;
    }
    
    @Override
    public Registration<Connection> insert(RootContext context, Context requestContext) {
        try {
            //Connection con = new MyConnection();
            Connection con = dataSource.getConnection();
            Registration<Connection> reg = requestContext.put(con, this);
            return reg;
        } catch (SQLException ex) {
            throw new RuntimeException("Exception opening connection: " + url + " user:" + user, ex);
        }
    }
    
    @Override
    public void onRemove(Connection item) {
        try {
            item.close();
        } catch (SQLException ex) {
            log.warn("Exception closing connection");
        }
    }
    
    @Override
    public void destroy() {
        if( dataSource != null ) {
            try {
                dataSource.close();
            } catch( SQLException ex ) {
                log.warn( "exception closing connection pool", ex);
            }
        }
    }

	@Override
	protected void finalize() throws Throwable {
		destroy();
	}
	
	
    
    @Override
    public void init(RootContext ctx) {
//        this.url = ctx.get("db.url");
//        this.user = (String)ctx.get("db.userid");
//        password = (String)ctx.get("db.password");
//        driverClassName = (String)ctx.get("db.driver");

        if( dataSource == null ) {
            BasicDataSource ds = new BasicDataSource();
            ds.setDriverClassName(driverClassName);
            ds.setUsername(user);
            ds.setPassword(password);
            ds.setUrl(url);
            ds.setDefaultAutoCommit(false);

            ds.setPoolPreparedStatements( poolPreparedStatements );
            ds.setInitialSize( initialSize );
            ds.setMinIdle( minIdle );
            ds.setMaxIdle( maxIdle );
            ds.setMaxActive( maxActive );

            log.debug( "created datasource: " + ds.getClass().getCanonicalName());
            log.debug( "driver: " + driverClassName + " user:" + user + " url:" + url + " poolPrepStatements:" + poolPreparedStatements);
            log.debug( "initialSize:" + initialSize + " minIdle:" + minIdle + " maxIdle:" + maxIdle + " maxAction:" + maxActive);
            dataSource = ds;
        }
    }

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public BasicDataSource getDataSource() {
        return dataSource;
    }

    public boolean isPoolPreparedStatements() {
        return poolPreparedStatements;
    }

    public void setPoolPreparedStatements( boolean poolPreparedStatements ) {
        this.poolPreparedStatements = poolPreparedStatements;
    }

    public int getInitialSize() {
        return initialSize;
    }

    public void setInitialSize( int initialSize ) {
        this.initialSize = initialSize;
    }

    public int getMinIdle() {
        return minIdle;
    }

    public void setMinIdle( int minIdle ) {
        this.minIdle = minIdle;
    }

    public int getMaxIdle() {
        return maxIdle;
    }

    public void setMaxIdle( int maxIdle ) {
        this.maxIdle = maxIdle;
    }

    public int getMaxActive() {
        return maxActive;
    }

    public void setMaxActive( int maxActive ) {
        this.maxActive = maxActive;
    }
}
