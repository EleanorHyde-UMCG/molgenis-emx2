package org.molgenis.sql;

import org.jooq.DSLContext;
import org.jooq.DataType;
import org.jooq.Field;
import org.molgenis.MolgenisException;
import org.molgenis.Type;
import org.molgenis.beans.ColumnBean;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

public class SqlColumn extends ColumnBean {
  private DSLContext jooq;

  public SqlColumn(SqlTable table, String columnName, Type columnType, Boolean isNullable) {
    super(table, columnName, columnType, isNullable);
    this.jooq = table.getJooq();
  }

  // for ref subclass
  protected SqlColumn(
      SqlTable table,
      String name,
      Type type,
      String refTable,
      String refColumn,
      Boolean isNullable) {
    super(table, name, type, refTable, refColumn, isNullable);
    this.jooq = table.getJooq();
  }

  /** constructor for REF */
  public SqlColumn createColumn() throws MolgenisException {
    DataType thisType = SqlTypeUtils.jooqTypeOf(this);
    Field thisColumn = field(name(getName()), thisType);
    jooq.alterTable(asJooqTable()).addColumn(thisColumn).execute();

    jooq.alterTable(asJooqTable())
        .alterColumn(thisColumn)
        .setNotNull()
        .execute(); // seperate to not interfere with type

    // save metadata
    MetadataUtils.saveColumn(this);
    return this;
  }

  @Override
  public SqlColumn setNullable(boolean nillable) throws MolgenisException {
    if (nillable) jooq.alterTable(asJooqTable()).alterColumn(getName()).dropNotNull().execute();
    else jooq.alterTable(asJooqTable()).alterColumn(getName()).setNotNull().execute();
    super.setNullable(isNullable());
    return this;
  }

  protected org.jooq.Table asJooqTable() {
    return table(name(getTable().getSchema().getName(), getTable().getName()));
  }

  public SqlColumn setIndexed(boolean indexed) {
    jooq.createIndexIfNotExists(name("INDEX_" + getName()))
        .on(asJooqTable(), field(name(getName())))
        .execute();
    return this;
  }

  DSLContext getJooq() {
    return jooq;
  }
}
