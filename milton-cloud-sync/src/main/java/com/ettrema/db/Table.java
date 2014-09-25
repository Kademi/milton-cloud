package com.ettrema.db;

import com.ettrema.db.dialects.Dialect;
import com.ettrema.db.types.FieldType;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 *
 * @author brad
 */
public class Table implements Serializable {

    private static final long serialVersionUID = 1L;
    public final String tableName;
    private Field pk;
    private final List<Field> fields = new ArrayList<Field>();
    private final List<Index> indexes = new ArrayList<Index>();

    public Table(String name) {
        this.tableName = name;
    }

    public List<Field> getFields() {
        return fields;
    }

    public List<Index> getIndexes() {
        return indexes;
    }

    public Field getPk() {
        return pk;
    }

    public String getTableName() {
        return tableName;
    }

    public Index addIndex(String name, Field... fields) {
        Index i = new Index(name);
        i.addAll(Arrays.asList(fields));
        indexes.add(i);
        return i;
    }

    public Index addIndex(String name, List<Field> fields) {
        Index i = new Index(name);
        i.addAll(fields);
        indexes.add(i);
        return i;
    }

    public String getDelete() {
        return "DELETE FROM " + tableName + " WHERE " + pk.getName() + " = ?";
    }

    public String getDeleteBy(Field f) {
        return "DELETE FROM " + tableName + " WHERE " + f.getName() + " = ?";
    }

    /**
     * Returns a select statement naming each column name and with a simple from
     * clause
     *
     * @return
     */
    public String getSelect() {
        return "SELECT " + getColumnNames() + " FROM " + tableName;
    }

    public String getColumnNames() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Field f : fields) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append(f.name);
        }
        return sb.toString();
    }

    public String getQuestionMarks() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Field f : fields) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append('?');
        }
        return sb.toString();
    }

    public String getInsert() {
        return "INSERT INTO " + tableName + "(" + getColumnNames() + ") VALUES(" + getQuestionMarks() + ")";
    }

    public PreparedStatement prepareInsertStatement(Connection con) {
        String insertSql = getInsert();
        PreparedStatement stmt;
        try {
            stmt = con.prepareStatement(insertSql);
        } catch (SQLException ex) {
            throw new RuntimeException(insertSql, ex);

        }
        return stmt;
    }

    public void insert(PreparedStatement stmt, Map<String, Object> mapOfValues) {
        int index = 0;
        for (Field f : fields) {
            index++;
            Object v = mapOfValues.get(f.getName());
            if (v != null) {
                try {
                    f.setObject(stmt, index, v);
                } catch (Throwable ex) {
                    throw new RuntimeException("Table: " + tableName + " Field: " + f.getName() + " Value: " + v, ex);
                }
            }
        }
        try {
            stmt.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Exception inserting into: " + tableName, ex);
        }
    }

    public String getCreateTable(Dialect dialect) {
        StringBuffer sb = new StringBuffer("CREATE TABLE ").append(tableName).append("(\n");
        boolean first = true;
        for (Field f : fields) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append('\n');
            sb.append(f.name).append(' ').append(dialect.getTypeName(f.getType()));
            if (!f.nullable) {
                sb.append(" NOT NULL");
            }

        }
        if (pk != null) {
            sb.append(',');
            sb.append('\n');
            sb.append("CONSTRAINT ").append(tableName).append("_pk PRIMARY KEY (").append(pk.name).append(")");
        }

        for (Index i : getIndexes()) {
            if( i.getForeignKey() != null ) {
                String fkName = i.getForeignKeyName();
                sb.append(',');                
                sb.append("CONSTRAINT " + fkName +  " FOREIGN KEY (").append(i.getFieldNamesCsv()).append(") ");
                Field fk = i.getForeignKey();
                sb.append("REFERENCES ").append(fk.getTable().tableName).append("(").append(fk.getName()).append(")");
            }
        }
//CONSTRAINT fk_PerOrders FOREIGN KEY (P_Id)
//REFERENCES Persons(P_Id)        

        sb.append(")");
        return sb.toString();
    }

    public void reCreateTable(Connection con, Dialect dialect) {
        dropTable(con);
        createTable(con, dialect);
    }

    public void dropTable(Connection con) {
        String drop = "DROP TABLE " + tableName;
        execute(con, drop);
    }

    public void createTable(Connection con, Dialect dialect) {
        String create = getCreateTable(dialect);
        execute(con, create);
        for (Index index : indexes) {
            String createIndexSql = getCreateIndexSql(index);
            execute(con, createIndexSql);
        }
    }

    protected void execute(Connection con, String sql) {
        try {
            Statement stmt = con.createStatement();
            stmt.execute(sql);
            stmt.close();
        } catch (SQLException ex) {
            throw new RuntimeException("sql:" + sql, ex);
        }
    }

//CREATE TABLE relations
//(
//  id bigserial NOT NULL,
//  from_uuid character varying NOT NULL,
//  to_uuid character varying NOT NULL,
//  "name" character varying NOT NULL,
//  CONSTRAINT relations_pk PRIMARY KEY (id),
//  CONSTRAINT relations_from_fk FOREIGN KEY (from_uuid)
//      REFERENCES "names" (uuid) MATCH SIMPLE
//      ON UPDATE NO ACTION ON DELETE CASCADE,
//  CONSTRAINT relations_to_fk FOREIGN KEY (to_uuid)
//      REFERENCES "names" (uuid) MATCH SIMPLE
//      ON UPDATE NO ACTION ON DELETE CASCADE
//)
//WITH (OIDS=FALSE);
//ALTER TABLE relations OWNER TO postgres;
//
//-- Index: from_uuid_idx
//
//-- DROP INDEX from_uuid_idx;
//
//CREATE INDEX from_uuid_idx
//  ON relations
//  USING btree
//  (from_uuid);
//
//-- Index: to_uuid_idx
//
//-- DROP INDEX to_uuid_idx;
//
//CREATE INDEX to_uuid_idx
//  ON relations
//  USING btree
//  (to_uuid);
    public Field add(String name, FieldType type, boolean nullable) {
        Field f = new Field(name, type, nullable);
        fields.add(f);
        return f;
    }

    public String getCreateIndexSql(Index i) {
        String sql = "CREATE INDEX " + tableName + "_" + i.getName() + "_idx";
        sql += " ON " + tableName;
        sql += "(";
        boolean first = true;
        for (Field f : i) {
            if (!first) {
                sql += ", ";
            }
            first = false;
            sql += f.getName();
        }
        sql += ")";
        return sql;
    }

    public String getCreateIndexScript() {
        String s = "";
        for (Index i : this.indexes) {
            s += getCreateIndexSql(i) + ";";
        }
        return s;
    }

    public Table setPrimaryKey(Field f) {
        this.pk = f;
        return this;
    }

    public Field getField(String fieldName) {
        for (Field f : fields) {
            if (f.getName().equals(fieldName)) {
                return f;
            }
        }
        return null;
    }

    public class Field<T> implements Serializable {

        private static final long serialVersionUID = 1L;
        String name;
        FieldType<T> type;
        boolean nullable;

        public Field(String name, FieldType<T> type, boolean nullable) {
            this.name = name;
            this.type = type;
            this.nullable = nullable;
        }

        public String getName() {
            return name;
        }

        public FieldType getType() {
            return type;
        }

        public boolean isNullable() {
            return nullable;
        }

        public Table getTable() {
            return Table.this;
        }

        public T get(ResultSet rs) throws SQLException {
            return type.get(name, rs);
        }

        public void setObject(PreparedStatement stmt, int index, Object value) throws SQLException {
            T tValue;
            if (value != null) {
                tValue = type.parse(value);
            } else {
                tValue = null;
            }
            set(stmt, index, tValue);
        }

        public void set(PreparedStatement stmt, int index, T value) throws SQLException {
            try {
                type.set(stmt, index, value);
            } catch (Throwable e) {
                throw new RuntimeException("Exception setting parameter: " + index + " of type: " + type.getClass() + " to value: " + value, e);
            }
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public class Index extends ArrayList<Field> implements Serializable {

        private static final long serialVersionUID = 1L;
        private final String name;
        private Field foreignKey;

        public Index(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setForeignKey(Field foreignKey) {
            this.foreignKey = foreignKey;
        }

        public Field getForeignKey() {
            return foreignKey;
        }

        private String getFieldNamesCsv() {
            StringBuilder sb = new StringBuilder();
            for (Field f : this) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(f.getName());
            }
            return sb.toString();
        }

        private String getForeignKeyName() {
            return "fk_" + name;
        }
    }
}
