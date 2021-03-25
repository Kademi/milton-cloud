package com.ettrema.db.dialects;

import com.ettrema.db.Table;
import com.ettrema.db.types.BinaryType;
import com.ettrema.db.types.FieldType;
import java.sql.Connection;
import java.sql.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class PostgresDialect implements Dialect {

    private static final Logger log = LoggerFactory.getLogger(PostgresDialect.class);


    private String catalog;

    private String schemaPattern;

    @Override
    public boolean tableExists( String name, Connection con ) {
        try {
            ResultSet tables = con.getMetaData().getTables( catalog, schemaPattern, name, null );
            boolean b = tables.next();
            log.warn( "does table exist: " + name + " = " + b);
            return b;
        } catch(Exception e) {
            throw new RuntimeException( "Exception looking for tables: catalog:" + catalog + " schema:" + schemaPattern, e);
        }
    }

    @Override
    public void createTable( Table table, Connection con ) {
        table.createTable( con, this );
    }

    public String getCatalog() {
        return catalog;
    }

    public void setCatalog( String catalog ) {
        this.catalog = catalog;
    }

    public String getSchemaPattern() {
        return schemaPattern;
    }

    public void setSchemaPattern( String schemaPattern ) {
        this.schemaPattern = schemaPattern;
    }

    @Override
    public String getTypeName( FieldType type ) {
        if( type instanceof BinaryType) {
            return "bytea";
        } else {
            return type.toString().toLowerCase();
        }
    }
}
