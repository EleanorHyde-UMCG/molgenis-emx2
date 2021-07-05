package org.molgenis.emx2.sql;

import static graphql.Assert.assertTrue;
import static org.junit.Assert.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.FilterBean.*;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.molgenis.emx2.*;

public class TestCompositeForeignKeys {
  private Database database;

  @Before
  public void setUp() {
    database = TestDatabaseFactory.getTestDatabase();

    // create target table

  }

  @Test
  public void testCompositeRef() {
    Schema schema =
        database.dropCreateSchema(TestCompositeForeignKeys.class.getSimpleName() + "Ref");

    schema.create(
        table(
            "Person",
            column("firstName").setPkey(),
            column("lastName").setPkey(),
            column("uncle", REF).setRefTable("Person")));

    Table p = schema.getTable("Person");

    p.insert(new Row().setString("firstName", "Donald").setString("lastName", "Duck"));

    try {
      p.insert(
          new Row()
              .setString("firstName", "Kwik")
              .setString("lastName", "Duck")
              .setString("uncle.firstName", "Donald")
              .setString("uncle.lastName", "MISSING"));
      fail("should have failed on missing foreign key");
    } catch (Exception e) {
      System.out.println("errored correctly: " + e);
    }

    p.insert(new Row().setString("firstName", "Kwik").setString("lastName", "Duck"));
    assertNull(p.retrieveRows().get(1).getString("uncle.firstName"));

    int count =
        p.update(
            new Row()
                .setString("firstName", "Kwik")
                .setString("lastName", "Duck")
                .setString("uncle.firstName", "Donald")
                .setString("uncle.lastName", "Duck"));

    assertEquals(1, count);
    assertNotNull(p.retrieveRows().get(1).getString("uncle.firstName"));

    p.insert(
        new Row()
            .setString("firstName", "Kwek")
            .setString("lastName", "Duck")
            .setString("uncle.firstName", "Donald")
            .setString("uncle.lastName", "Duck"));

    p.insert(
        new Row()
            .setString("firstName", "Kwak")
            .setString("lastName", "Duck")
            .setString("uncle.firstName", "Donald")
            .setString("uncle.lastName", "Duck"));

    try {
      p.delete(new Row().setString("firstName", "Donald").setString("lastName", "Duck"));
      fail("should have failed on foreign key (Donald is used in foreign key)");
    } catch (Exception e) {
      System.out.println("errored correctly: " + e);
    }

    schema.create(table("Student").setInherit("Person"));
    Table s = schema.getTable("Student");
    s.insert(
        new Row()
            .setString("firstName", "Mickey")
            .setString("lastName", "Mouse")
            .setString("uncle.firstName", "Kwik")
            .setString("uncle.lastName", "Duck"));

    String result =
        schema
            .query(
                "Student",
                s("firstName"),
                s("lastName"),
                s("uncle", s("firstName"), s("lastName")),
                s("uncle", s("firstName"), s("lastName")))
            .retrieveJSON();
    System.out.println(result);

    result =
        schema
            .query("Student")
            .select(s("firstName"), s("lastName"))
            .where(
                or(
                    and(f("firstName", EQUALS, "Donald"), f("lastName", EQUALS, "Duck")),
                    and(f("firstName", EQUALS, "Mickey"), f("lastName", EQUALS, "Mouse"))))
            .retrieveJSON();

    System.out.println(result);
    assertTrue(result.contains("Mouse"));
    assertFalse(result.contains("Duck"));

    result =
        schema
            .query("Person")
            .select(s("firstName"), s("lastName"))
            .where(
                or(
                    and(f("firstName", EQUALS, "Donald"), f("lastName", EQUALS, "Duck")),
                    and(f("firstName", EQUALS, "Mickey"), f("lastName", EQUALS, "Mouse"))))
            .retrieveJSON();

    System.out.println(result);
    assertTrue(result.contains("Mouse"));
    assertTrue(result.contains("Duck"));

    // composite key filter
    result =
        schema
            .query("Person")
            .select(s("firstName"), s("lastName"))
            // composite filter, should result in 'donald duck' OR 'mickey mouse'
            .where(
                f(
                    "uncle",
                    or(
                        and(f("firstName", EQUALS, "Donald"), f("lastName", EQUALS, "Duck")),
                        and(f("firstName", EQUALS, "Mickey"), f("lastName", EQUALS, "Mouse")))))
            .retrieveJSON();

    System.out.println(result);
    assertTrue(result.contains("Kwik"));
    assertFalse(result.contains("Mouse"));

    // refback
    schema
        .getTable("Person")
        .getMetadata()
        .add(column("nephew").setType(REFBACK).setRefTable("Person").setRefBack("uncle"));

    s.insert(
        new Row()
            .setString("firstName", "Katrien")
            .setString("lastName", "Duck")
            .setString("nephew.firstName", "Kwik")
            .setString("nephew.lastName", "Duck")); // I know, not true

    assertTrue(
        List.of(
                s.query()
                    .select(
                        s("firstName"),
                        s("lastName"),
                        s("nephew", s("firstName"), s("lastName")),
                        s("uncle", s("firstName"), s("lastName")))
                    .where(f("firstName", EQUALS, "Katrien"))
                    .retrieveRows()
                    .get(0)
                    .getStringArray("nephew-firstName"))
            .contains("Kwik"));
    assertTrue(
        List.of(
                p.query()
                    .select(
                        s("firstName"),
                        s("lastName"),
                        s("nephew", s("firstName"), s("lastName")),
                        s("uncle", s("firstName"), s("lastName")))
                    .where(f("firstName", EQUALS, "Kwik"))
                    .retrieveRows()
                    .get(0)
                    .getStringArray("uncle-firstName"))
            .contains("Katrien"));
  }

  @Test
  public void testCompositeRefArray() {
    Schema schema =
        database.dropCreateSchema(TestCompositeForeignKeys.class.getSimpleName() + "RefArray");

    schema.create(
        table(
            "Person",
            column("firstName").setPkey(),
            column("lastName").setPkey(),
            column("cousins", REF_ARRAY).setRefTable("Person")));

    Table p = schema.getTable("Person");

    p.insert(new Row().setString("firstName", "Kwik").setString("lastName", "Duck"));

    p.insert(
        new Row()
            .setString("firstName", "Donald")
            .setString("lastName", "Duck")
            .setString("cousins.firstName", "Kwik")
            .setString("cousins.lastName", "Duck"));

    try {
      p.delete(new Row().setString("firstName", "Kwik").setString("lastName", "Duck"));
      fail("should have failed on foreign key error");
    } catch (Exception e) {
      System.out.println("errored correctly: " + e);
    }

    schema.create(table("Student").setInherit("Person"));
    Table s = schema.getTable("Student");
    s.insert(
        new Row()
            .setString("firstName", "Mickey")
            .setString("lastName", "Mouse")
            .setString("cousins.firstName", "Kwik")
            .setString("cousins.lastName", "Duck"));

    String result =
        schema
            .query("Student")
            .select(s("firstName"), s("lastName"), s("cousins", s("firstName"), s("lastName")))
            .retrieveJSON();

    System.out.println(result);

    // refback
    schema
        .getTable("Person")
        .getMetadata()
        .add(column("uncles").setType(REFBACK).setRefTable("Person").setRefBack("cousins"));

    s.insert(
        new Row()
            .setString("firstName", "Kwok") // doesn't exist
            .setString("lastName", "Duck")
            .setString("uncles.firstName", "Donald")
            .setString("uncles.lastName", "Duck"));

    assertTrue(
        List.of(
                s.query()
                    .select(
                        s("firstName"),
                        s("lastName"),
                        s("uncles", s("firstName"), s("lastName")),
                        s("cousins", s("firstName"), s("lastName")))
                    .where(f("firstName", EQUALS, "Kwok"))
                    .retrieveRows()
                    .get(0)
                    .getStringArray("uncles-firstName"))
            .contains("Donald"));
    assertTrue(
        List.of(
                p.query()
                    .select(
                        s("firstName"),
                        s("lastName"),
                        s("cousins", s("firstName"), s("lastName")),
                        s("uncles", s("firstName"), s("lastName")))
                    .where(f("firstName", EQUALS, "Donald"))
                    .retrieveRows()
                    .get(1) //
                    .getStringArray("cousins-firstName")) // TODO should be array?
            .contains("Kwok"));
  }

  //  @Test
  //  public void testCompositeRefArrayWithInheritance() {
  //
  //    String schemaName = TestCompositeForeignKeys.class.getSimpleName() +
  // "RefArrayWithInheritance";
  //    Schema schema = database.dropCreateSchema(schemaName);
  //
  //    schema.create(table("AllVariables"));
  //  }

  @Test
  public void testCompositeKeyThatIsReference() {
    Schema schema =
        database.dropCreateSchema(TestCompositeForeignKeys.class.getSimpleName() + "RefAsKey");

    schema.create(table("Target", column("firstName").setPkey(), column("lastName").setPkey()));
    schema.create(
        table(
            "Source",
            column("person").setType(REF).setPkey().setRefTable("Target").setRequired(true),
            column("name").setPkey(),
            column("someValue")));

    Table target = schema.getTable("Target");
    target.insert(row("firstName", "Donald", "lastName", "Duck"));

    Table source = schema.getTable("Source");

    try {
      source.insert(row("person.firstName", "Donald", "name", "Kwik"));
      fail("Should fail on required");
    } catch (Exception e) {
      System.out.println("errored correctly: " + e);
    }

    source.insert(
        row(
            "person.firstName",
            "Donald",
            "person.lastName",
            "Duck",
            "name",
            "Kwik",
            "someValue",
            "foo"));
    source.update(
        row(
            "person.firstName",
            "Donald",
            "person.lastName",
            "Duck",
            "name",
            "Kwik",
            "someValue",
            "bar"));
    assertEquals("bar", source.retrieveRows().get(0).getString("someValue"));
  }
}
