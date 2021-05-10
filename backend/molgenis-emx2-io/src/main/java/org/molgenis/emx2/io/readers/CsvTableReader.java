package org.molgenis.emx2.io.readers;

import java.io.*;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.simpleflatmapper.csv.CsvParser;

public class CsvTableReader {

  private CsvTableReader() {
    // to prevent new CsvRowReader()
  }

  public static Iterable<Row> read(File f) throws IOException {
    return read(new FileReader(f));
  }

  public static Iterable<Row> read(Reader in) {
    try {
      BufferedReader bufferedReader = new BufferedReader(in);
      bufferedReader.mark(2000000);
      String firstLine = bufferedReader.readLine();
      char separator = ',';
      if (firstLine.contains("\t")) {
        separator = '\t';
      }
      if (firstLine.contains(";")) {
        separator = ';';
      }

      // push back in
      bufferedReader.reset();

      // don't use buffered, it is slower
      Iterator<LinkedHashMap> iterator =
          CsvParser.dsl()
              .separator(separator)
              .trimSpaces()
              .mapTo(LinkedHashMap.class)
              .iterator(bufferedReader);

      return () ->
          new Iterator<>() {
            final Iterator<LinkedHashMap> it = iterator;
            final AtomicInteger line = new AtomicInteger(1);

            public boolean hasNext() {
              try {
                return it.hasNext();
              } catch (Exception e) {
                throw new MolgenisException(
                    "Import failed: "
                        + e.getClass().getName()
                        + ": "
                        + e.getMessage()
                        + ". Error at line "
                        + line.get()
                        + ".",
                    e);
              }
            }

            public Row next() {
              return new Row(it.next());
            }

            @Override
            public void remove() {
              throw new UnsupportedOperationException();
            }
          };
    } catch (IOException ioe) {
      throw new MolgenisException("Import failed", ioe);
    }
  }
}
