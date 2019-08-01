package org.molgenis;

public interface Column {
  Column addColumn(String name, Type type) throws MolgenisException;

  Table getTable();

  String getName();

  Type getDataType();

  Boolean isNullable();

  Boolean isReadonly();

  Boolean isUnique();

  String getDefaultValue();

  String getRefTable();

  String getRefColumn();

  String getDescription();

  Column setNullable(boolean nillable) throws MolgenisException;

  void setReadonly(boolean readonly);

  void setDescription(String description);

  void setDefaultValue(String defaultValue);
}
