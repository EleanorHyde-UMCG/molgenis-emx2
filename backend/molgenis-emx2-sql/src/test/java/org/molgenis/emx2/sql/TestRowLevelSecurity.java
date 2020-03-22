package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.DefaultRoles;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

public class TestRowLevelSecurity {
  public static final String TEST_RLS_HAS_NO_PERMISSION = "test_rls_has_no_permission";
  public static final String TESTRLS_HAS_RLS_VIEW = "testrls_has_rls_view";
  public static final String TEST_RLS = "TestRLS";
  private static Database database;

  @BeforeClass
  public static void setUp() throws SQLException {
    database = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testRls() {
    try {
      // create schema
      Schema s = database.createSchema(TEST_RLS);

      // create two users
      database.addUser(TEST_RLS_HAS_NO_PERMISSION);
      assertEquals(true, database.hasUser(TEST_RLS_HAS_NO_PERMISSION));

      database.addUser(TESTRLS_HAS_RLS_VIEW);

      // grant both owner on TestRLS schema so can add row level security
      s.addMember("testrls1", DefaultRoles.OWNER.toString());
      s.addMember("testrls2", DefaultRoles.OWNER.toString());

      s.addMember(
          TESTRLS_HAS_RLS_VIEW,
          DefaultRoles.VIEWER.toString()); // can view table but only rows with right RLS

      // let one user create the table
      database.setActiveUser("testrls1");
      database.tx(
          db -> {
            db.getSchema(TEST_RLS).create(table(TEST_RLS).addColumn(column("col1")));
          });

      // let the other user add RLS
      database.setActiveUser("testrls2");
      database.tx(
          db -> {
            db.getSchema(TEST_RLS).getTable(TEST_RLS).getMetadata().enableRowLevelSecurity();
          });

      // let the first add a row (checks if admin permissions are setup correctly)
      database.setActiveUser("testrls1");
      database.tx(
          db -> {
            db.getSchema(TEST_RLS)
                .getTable(TEST_RLS)
                .insert(
                    new Row()
                        .setString("col1", "Hello World")
                        .set(Constants.MG_EDIT_ROLE, TESTRLS_HAS_RLS_VIEW),
                    new Row()
                        .setString("col1", "Hello World2")
                        .set(Constants.MG_EDIT_ROLE, TEST_RLS_HAS_NO_PERMISSION));
          });

      // let the second admin see it
      database.setActiveUser("testrls2");
      database.tx(
          db -> {
            assertEquals(2, db.getSchema(TEST_RLS).getTable(TEST_RLS).getRows().size());
          });

      // have RLS user query and see one row
      database.setActiveUser(TESTRLS_HAS_RLS_VIEW);
      database.tx(
          db -> {
            assertEquals(1, db.getSchema(TEST_RLS).getTable(TEST_RLS).getRows().size());
          });

      database.clearActiveUser();
      database.removeUser(TESTRLS_HAS_RLS_VIEW);
      assertEquals(false, database.hasUser(TESTRLS_HAS_RLS_VIEW));
    } finally {
      database.clearActiveUser();
    }
  }
}