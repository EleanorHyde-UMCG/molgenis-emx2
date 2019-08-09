package org.molgenis.emx2.io.format;

import org.molgenis.Type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public enum EmxDefinitionTerm {
  // for columns and tables
  UNIQUE(true),
  // for column types
  STRING(false),
  INT(false),
  BOOL(false),
  DECIMAL(false),
  TEXT(false),
  DATE(false),
  DATETIME(false),
  REF(true),
  REF_ARRAY(true),
  MREF(true),
  UUID(false),
  NILLABLE(false),
  READONLY(false),
  DEFAULT(true);

  private boolean hasParameter;
  private String parameterValue;

  EmxDefinitionTerm(Boolean hasParameter) {
    this.hasParameter = hasParameter;
  }

  public boolean hasParameter() {
    return hasParameter;
  }

  public String getParameterValue() {
    return parameterValue;
  }

  public List<String> getParameterList() {
    List<String> result = new ArrayList();
    if (getParameterValue() != null) {
      for (String el : getParameterValue().split(",")) {
        result.add(el.trim());
      }
    }
    return result;
  }

  public EmxDefinitionTerm setParameterValue(String parameterValue) {
    this.parameterValue = parameterValue;
    return this;
  }

  @Override
  public String toString() {
    if (hasParameter && getParameterValue() != null) {
      return this.name().toLowerCase() + "(" + parameterValue + ")";
    } else {
      return this.name().toLowerCase();
    }
  }

  public static EmxDefinitionTerm valueOf(Type type) {
    return valueOf(type.toString());
  }

  public EmxDefinitionTerm setParameterList(Collection<String> columnNames) {
    this.parameterValue =
        columnNames.stream().map(Object::toString).collect(Collectors.joining(","));
    return this;
  }
}
