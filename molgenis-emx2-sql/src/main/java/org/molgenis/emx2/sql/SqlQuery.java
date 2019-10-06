package org.molgenis.emx2.sql;

import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.util.postgres.PostgresDSL;
import org.molgenis.emx2.Query;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.beans.QueryBean;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.Where;
import org.molgenis.emx2.utils.MolgenisException;

import java.util.*;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.ColumnType.MREF;
import static org.molgenis.emx2.sql.Constants.MG_SEARCH_INDEX_COLUMN_NAME;

public class SqlQuery extends QueryBean implements Query {

  private TableMetadata from;
  private DSLContext jooq;

  public SqlQuery(TableMetadata from, DSLContext jooq) {
    this.from = from;
    this.jooq = jooq;
  }

  @Override
  public List<Row> retrieve() throws MolgenisException {

    try {
      // todo major pitfall: names as paths might be too long, then we need aliases of some sort to
      // postprocess

      // 0. if select is empty, then select all columns of 'from'
      List<String> selectList = getSelectList();
      if (selectList.isEmpty()) {
        for (Column c : from.getColumns()) {
          selectList.add(c.getColumnName());
        }
      }

      // 1. create the select clause with tableAliases derived from the paths
      Set<String> tableAliases = new TreeSet<>();
      List<Field> fields = new ArrayList<>();
      for (String select : selectList) {
        String[] path = getPath(select);
        Column column = getColumn(from, path);
        // table alias = from.getTableName + path
        String tableAlias = from.getTableName();
        if (path.length > 1) {
          for (int i = 0; i < path.length - 1; i++) {
            tableAlias += "/" + path[i];
          }
        }
        if (MREF.equals(column.getColumnType())) {
          // select array(mref_col from mreftable...)
          fields.add(createMrefSubselect(column, tableAlias).as(select));
        } else {
          //
          fields.add(
              field(name(tableAlias, column.getColumnName()), SqlTypeUtils.jooqTypeOf(column))
                  .as(select));
        }
        tableAliases.add(tableAlias);
      }
      SelectSelectStep selectStep;
      if (!fields.isEmpty()) selectStep = jooq.select(fields);
      else selectStep = jooq.select();

      // 2. create from clause with joins to the ref linked tables
      SelectJoinStep fromStep =
          selectStep.from(table(name(from.getSchema().getName(), from.getTableName())));
      for (String tableAlias : tableAliases) {
        String[] path = getPath(tableAlias);
        if (path.length > 1) {
          Column fkey = getColumn(from, Arrays.copyOfRange(path, 1, path.length));
          String leftAlias = String.join("/", Arrays.copyOfRange(path, 0, path.length - 1));
          switch (fkey.getColumnType()) {
            case REF:
              fromStep =
                  fromStep
                      .leftJoin(
                          table(name(from.getSchema().getName(), fkey.getRefTableName()))
                              .as(name(tableAlias)))
                      .on(
                          field(name(leftAlias, fkey.getColumnName()))
                              .eq(field(name(tableAlias, fkey.getRefColumnName()))));
              break;
            case REF_ARRAY:
              fromStep =
                  fromStep
                      .leftJoin(
                          table(name(from.getSchema().getName(), fkey.getRefTableName()))
                              .as(name(tableAlias)))
                      .on(
                          "{0} = ANY ({1})",
                          field(name(tableAlias, fkey.getRefColumnName())),
                          field(name(leftAlias, fkey.getColumnName())));
              break;
            case MREF:
              String joinTable = fkey.getMrefJoinTableName();

              // to link table
              fromStep =
                  fromStep
                      .leftJoin(
                          table(name(from.getSchema().getName(), joinTable)).as(name(joinTable)))
                      .on(
                          field(name(joinTable, fkey.getRefColumnName()))
                              .eq(field(name(leftAlias, fkey.getColumnName()))));
              // to other end of the mref
              fromStep =
                  fromStep
                      .leftJoin(
                          table(name(from.getSchema().getName(), fkey.getRefTableName()))
                              .as(name(tableAlias)))
                      .on(
                          field(name(joinTable, fkey.getColumnName()))
                              .eq(field(name(tableAlias, fkey.getRefColumnName()))));
            default:
              break;
          }
        }
      }

      // todo, the 'from' must also include tableAliases for the 'where' clauses

      // 3. create the where clause
      Condition filterCondition = createFilterConditions(from.getTableName());
      Condition searchCondition = createSearchConditions(tableAliases);
      if (filterCondition != null) {
        if (searchCondition != null) {
          fromStep.where(filterCondition).and(searchCondition);
        } else {
          fromStep.where(filterCondition);
        }
      } else if (searchCondition != null) {
        fromStep.where(searchCondition);
      }

      System.out.println(fromStep.getSQL());

      // retrieve
      List<Row> result = new ArrayList<>();

      Result<Record> fetch = fromStep.fetch();
      for (Record r : fetch) {
        result.add(new SqlRow(r));
      }
      // createColumn the from & joins

      return result;
    } catch (MolgenisException e) {
      throw e;
    } catch (Exception e2) {
      if (e2.getCause() != null)
        throw new MolgenisException("Query failed:" + e2.getCause().getMessage(), e2);
      else throw new MolgenisException(e2);
    }
  }

  private Condition createSearchConditions(Set<String> tableAliases) {
    if (getSearchList().size() == 0) return null;
    String search = String.join("|", getSearchList());
    Condition searchCondition = null;
    for (String tableAlias : tableAliases) {
      Condition condition =
          condition(
              name(tableAlias, MG_SEARCH_INDEX_COLUMN_NAME) + " @@ to_tsquery('" + search + "' )");
      if (searchCondition == null) {
        searchCondition = condition;
      } else {
        searchCondition.or(condition);
      }
    }
    return searchCondition;
  }

  /** subselect for mref in select and/or filter clauses */
  private Field<Object[]> createMrefSubselect(Column column, String tableAlias) {
    return PostgresDSL.array(
        DSL.select(field(name(column.getMrefJoinTableName(), column.getRefColumnName())))
            .from(name(from.getSchema().getName(), column.getMrefJoinTableName()))
            .where(
                field(name(column.getMrefJoinTableName(), column.getReverseRefColumn()))
                    .eq(field(name(tableAlias, column.getReverseRefColumn())))));
  }

  private Condition createFilterConditions(String tableName) throws MolgenisException {
    Condition conditions = null;
    for (Where w : this.getWhereLists()) {
      Condition newCondition = null;
      newCondition = createFilterCondition(w, tableName);
      if (newCondition != null) {
        if (conditions == null) conditions = newCondition;
        else {
          conditions = conditions.and(newCondition);
        }
      }
    }

    return conditions;
  }

  private Condition createFilterCondition(Where w, String tableName) throws MolgenisException {
    // in case of field operator
    String[] path = getPath(w.getPath());
    StringBuilder tableAlias = new StringBuilder(tableName);

    if (path.length > 1) {
      tableAlias.append("/" + String.join("/", Arrays.copyOfRange(path, 0, path.length - 1)));
    }
    Name selector = name(tableAlias.toString(), path[path.length - 1]);
    switch (w.getOperator()) {
      case EQUALS:
        // type check
        Object[] values = w.getValues();
        for (int i = 0; i < values.length; i++)
          values[i] = SqlTypeUtils.getTypedValue(values[i], getColumn(from, path));
        return field(selector).in(values);
      case ANY:
        Column column = getColumn(from, path);
        if (MREF.equals(column.getColumnType())) {
          return condition(
              "{0} && {1}",
              SqlTypeUtils.getTypedValue(w.getValues(), column),
              createMrefSubselect(column, tableAlias.toString()));
        } else {
          return condition(
              "{0} && {1}", SqlTypeUtils.getTypedValue(w.getValues(), column), field(selector));
        }
      default:
        throw new MolgenisException(
            "invalid_query",
            "Creation of filter condition failed",
            "Where clause '" + w.toString() + "' is not supported");
    }
  }

  private String[] getPath(String s) {
    // todo check for escaping with //
    return s.split("/");
  }

  /** recursive getColumn */
  private Column getColumn(TableMetadata t, String[] path) throws MolgenisException {
    Column c = t.getColumn(path[0]);
    if (c == null)
      throw new MolgenisException(
          "undefined_column",
          "Column not found",
          "Column '" + path[0] + "' cannot be found in table " + t.getTableName());
    if (path.length == 1) {
      return c;
    } else {
      return getColumn(
          t.getSchema().getTableMetadata(c.getRefTableName()),
          Arrays.copyOfRange(path, 1, path.length));
    }
  }
}
