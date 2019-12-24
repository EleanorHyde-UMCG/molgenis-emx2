package org.molgenis.emx2;

import org.molgenis.emx2.utils.TableSort;

import java.util.*;

public class SchemaMetadata {

  private String name;
  protected Map<String, TableMetadata> tables = new LinkedHashMap<>();

  public SchemaMetadata() {}

  public SchemaMetadata(String name) {
    if (name == null || name.isEmpty())
      throw new MolgenisException("Create schema failed", "Schema name was null or empty");
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Collection<String> getTableNames() {
    return Collections.unmodifiableCollection(tables.keySet());
  }

  public TableMetadata create(TableMetadata table) {
    if (tables.get(table.getTableName()) != null)
      throw new MolgenisException(
          "Create table failed",
          "Table with name '"
              + table.getTableName()
              + "'already exists in schema '"
              + getName()
              + "'");
    this.tables.put(table.getTableName(), table);
    return table;
  }

  public TableMetadata getTableMetadata(String name) {
    return tables.get(name);
  }

  public void drop(String tableId) {
    tables.remove(tableId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (TableMetadata t : tables.values()) {
      sb.append(t);
    }
    return sb.toString();
  }

  public Iterable<? extends TableMetadata> getTables() {
    List<TableMetadata> tables = new ArrayList<>(this.tables.values());
    TableSort.sortTableByDependency(tables);
    return tables;
  }
}
