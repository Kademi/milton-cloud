package com.ettrema.db;

import com.ettrema.db.dialects.Dialect;
import com.ettrema.common.Service;
import com.ettrema.context.Context;
import com.ettrema.context.Executable2;
import com.ettrema.context.RootContext;
import com.ettrema.vfs.PostgresUtils;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 *
 * @author brad
 */
public class TableCreatorService implements Service {

	private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TableCreatorService.class);
	private final RootContext rootContext;
	private final Dialect dialect;
	protected List<TableDefinitionSource> definitionSources;

	public TableCreatorService(RootContext rootContext, List<TableDefinitionSource> definitionSources, Dialect dialect) {
		this.rootContext = rootContext;
		this.definitionSources = definitionSources;
		this.dialect = dialect;
	}

	@Override
	public void start() {
		rootContext.execute(new Executable2() {

			@Override
			public void execute(Context context) {
				log.warn("doing check and create of tables. Tables which do not exist will be created");
				try {
					Connection con = PostgresUtils.con();
					processTableDefinitions(con);
				} catch (Exception e) {
					log.error("Exception checking table definitions, will continue anyway...", e);
				}
			}
		});
	}

	public void processTableDefinitions(Connection con) {
		for (TableDefinitionSource source : definitionSources) {
			for (Table t : source.getTableDefinitions()) {
				checkAndCreate(t, con, source);
			}
		}

	}

	private void checkAndCreate(Table table, Connection con, TableDefinitionSource source) {
		if (!dialect.tableExists(table.tableName, con)) {
			log.warn("create table: " + table.tableName);
			dialect.createTable(table, con);
			source.onCreate(table, con);
			commit(con);
		}
	}

	private void commit(Connection con) {
		try {
			con.commit();
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public void stop() {
	}
}
