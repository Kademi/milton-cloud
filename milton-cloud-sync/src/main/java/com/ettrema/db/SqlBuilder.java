package com.ettrema.db;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO
 *
 * @author brad
 */
public class SqlBuilder {
    public final SelectClause select = new SelectClause();
    public final FromClause from = new FromClause();
    public final WhereClause where = new WhereClause();



    public class SelectClause {
        private final List<SelectField> fields = new ArrayList<SelectField>();

        public SelectField add(String name, FromTable table) {
            SelectField f= new SelectField( name, table );
            fields.add(f);
            return f;
        }
    }

    public class SelectField {
        private final String fieldAlias;
        private final FromTable table;

        public SelectField( String fieldAlias, FromTable table ) {
            this.fieldAlias = fieldAlias;
            this.table = table;
        }

        public String getFieldAlias() {
            return fieldAlias;
        }

        public FromTable getTable() {
            return table;
        }
    }

    public class FromClause {
        private final List<FromTable> tables = new ArrayList<FromTable>();

        public FromTable add(String name, String alias) {
            FromTable t = new FromTable( name, alias);
            tables.add( t );
            return t;

        }

        public List<FromTable> getTables() {
            return tables;
        }
    }

    public class FromTable {
        private final String tableName;
        private final String tableAlias;

        public FromTable( String tableName, String tableAlias ) {
            this.tableName = tableName;
            this.tableAlias = tableAlias;
        }

        /**
         * alias or table name if no alias
         * @return
         */
        public String getFrom() {
            if( tableAlias == null ) {
                return tableName;
            } else {
                return tableAlias;
            }
        }
    }

    public class WhereClause {
        private Expression exp;


    }

    public interface Expression {
        
    }

    public class AndExpression implements Expression {
        private Expression lhs;
        private Expression rhs;
    }

    public class EqualsExpression implements Expression {
        private Value lhs;
        private Value rhs;
    }

    public interface Value {

    }

    public class Field implements Value {
        private final String fieldName;
        private final String tableAlias;

        public Field( String fieldName, String tableAlias ) {
            this.fieldName = fieldName;
            this.tableAlias = tableAlias;
        }

        public Field(String fieldName) {
            this.fieldName = fieldName;
            this.tableAlias = null;
        }
    }

    public class Parameter implements Value {
        private final Object value;

        public Parameter( Object value ) {
            this.value = value;
        }
    }

}
