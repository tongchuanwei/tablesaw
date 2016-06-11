package com.github.lwhite1.tablesaw.examples;

import au.com.bytecode.opencsv.CSVWriter;
import com.github.lwhite1.tablesaw.Table;
import com.github.lwhite1.tablesaw.api.ColumnType;
import com.github.lwhite1.tablesaw.api.QueryHelper;
import com.github.lwhite1.tablesaw.columns.IntColumn;
import com.github.lwhite1.tablesaw.io.CsvReader;
import com.github.lwhite1.tablesaw.columns.CategoryColumn;
import com.github.lwhite1.tablesaw.columns.FloatColumn;
import com.github.lwhite1.tablesaw.columns.LocalDateColumn;
import com.github.lwhite1.tablesaw.columns.packeddata.PackedLocalDate;
import com.github.lwhite1.tablesaw.store.StorageManager;
import com.google.common.base.Stopwatch;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Tests manipulation of large (but not big) data sets
 */
public class ObservationDataTest {

  private static final String CSV_FILE = "/Users/larrywhite/IdeaProjects/testdata/obs.csv";
  private static final String DB = "/Users/larrywhite/IdeaProjects/testdata/obs.db";

  // pools to get random test data from
  private static List<String> concepts = new ArrayList<>(100_000);
  private static IntArrayList patientIds = new IntArrayList(1_000_000);
  private static int size = 60 * 365;
  private static IntArrayList dates = new IntArrayList(size);


  public static void main(String[] args) throws Exception {

    int numberOfRecordsInTable = 500_000_000;
    Stopwatch stopwatch = Stopwatch.createStarted();

    Table t;

   // t = defineSchema();

   // generateTestData(t, numberOfRecordsInTable, stopwatch);

   // t = loadFromCsv(stopwatch);

    t = loadFromColumnStore(stopwatch);

   // writeToColumnStore(t, stopwatch);

    String randomConcept = t.categoryColumn("concept").get(RandomUtils.nextInt(0, t.rowCount()));

    stopwatch.reset().start();
    Table result = t.selectWhere(
        QueryHelper.column("concept").isEqualTo(randomConcept));
    System.out.println("concept found in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
    System.out.println(result.print());

    System.exit(0);
  }

  private static Table loadFromColumnStore(Stopwatch stopwatch) throws IOException {
    stopwatch.reset().start();
    Table t = StorageManager.readTable(DB + File.separator + "2bc6dca1-af95-4840-b07e-0c3f7b146bac");
    System.out.println("Loaded from column store in " + stopwatch.elapsed(TimeUnit.SECONDS) + " seconds");
    return t;
  }

  private static Table defineSchema() {
    Table t;
    t = new Table("Observations");
    CategoryColumn conceptId = CategoryColumn.create("concept");
    LocalDateColumn date = LocalDateColumn.create("date");
    FloatColumn value = FloatColumn.create("value");
    IntColumn patientId = IntColumn.create("patient");

    t.addColumn(conceptId);
    t.addColumn(date);
    t.addColumn(value);
    t.addColumn(patientId);
    return t;
  }

  private static void generateTestData(Table t, int numberOfRecordsInTable, Stopwatch stopwatch) throws IOException {
    stopwatch.reset().start();
    System.out.println("Generating test data");
    generateData(numberOfRecordsInTable, t);
    System.out.println("Time to generate " + numberOfRecordsInTable + " records: " + stopwatch.elapsed(TimeUnit.SECONDS) + " seconds");
  }

  private static void writeToColumnStore(Table t, Stopwatch stopwatch) throws Exception {
    stopwatch = stopwatch.reset().start();
    StorageManager.saveTable(DB, t);
    System.out.println("Time to write out in columnStore format " + stopwatch.elapsed(TimeUnit.SECONDS) + " seconds");
  }

  private static Table loadFromCsv(Stopwatch stopwatch) throws IOException {
    stopwatch.reset().start();
    Table t;// ConceptId, Date, Value, PatientNo
    ColumnType[] columnTypes = {ColumnType.CATEGORY, ColumnType.LOCAL_DATE, ColumnType.FLOAT, ColumnType.INTEGER};
    t = CsvReader.read(columnTypes, CSV_FILE);
    System.out.println("Time to read to CSV File " + stopwatch.elapsed(TimeUnit.SECONDS) + " seconds");
    return t;
  }

  private static void generateData(int observationCount, Table table) throws IOException {
    // create pools of random values

    while (concepts.size() <= 100_000) {
      concepts.add(RandomStringUtils.randomAscii(30));
    }

    while (patientIds.size() <= 1_000_000) {
      patientIds.add(RandomUtils.nextInt(0, 2_000_000_000));
    }

    while (dates.size() <= size){
      dates.add(PackedLocalDate.pack(randomDate()));
    }

    LocalDateColumn dateColumn = table.localDateColumn("date");
    CategoryColumn conceptColumn = table.categoryColumn("concept");
    FloatColumn valueColumn = table.floatColumn("value");
    IntColumn patientColumn = table.intColumn("patient");

    CSVWriter writer = new CSVWriter(new FileWriter(CSV_FILE));
    String[] line = new String[4];
    String[] header = {"concept", "date", "value", "patient"};

    writer.writeNext(header);
    // sample from the pools to write the data
    for (int i = 0; i < observationCount; i++) {
      line[0] = concepts.get(RandomUtils.nextInt(0, concepts.size()));
      line[1] = PackedLocalDate.toDateString(dates.getInt(RandomUtils.nextInt(0, dates.size())));
      line[2] = Float.toString(RandomUtils.nextFloat(0f, 100_000f));
      line[3] = Integer.toString(patientIds.getInt(RandomUtils.nextInt(0, patientIds.size())));
      writer.writeNext(line);
/*
      dateColumn.add(dates.getInt(RandomUtils.nextInt(0, dates.size())));
      conceptColumn.add(concepts.get(RandomUtils.nextInt(0, concepts.size())));
      valueColumn.add(RandomUtils.nextFloat(0f, 100_000f));
      patientColumn.add(patientIds.getInt(RandomUtils.nextInt(0, patientIds.size())));
*/
    }
    writer.flush();
    writer.close();
    concepts = null;
    patientIds = null;
    dates = null;
  }

  private static LocalDate randomDate() {
    Random random = new Random();
    int minDay = (int) LocalDate.of(1920, 1, 1).toEpochDay();
    int maxDay = (int) LocalDate.of(2016, 1, 1).toEpochDay();
    long randomDay = minDay + random.nextInt(maxDay - minDay);
    return LocalDate.ofEpochDay(randomDay);
  }
}
