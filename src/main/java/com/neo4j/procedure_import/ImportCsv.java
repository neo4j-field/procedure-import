package com.neo4j.procedure_import;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

public class ImportCsv {

   @Context
   public GraphDatabaseAPI database;
   @Context
   public Log log;

   /**
    *
    * @param filePath: The path to load, make sure to comment dbms.directories.import=import in neo4j.conf to be able to
    *                load the file from outside the import directory
    * @param batchSize: The number of lines to pass to load at once in every thread
    * @param threads: The number of parallel threads to use for loading.
    * throws InterruptedException: Thrown if {@link BatchProcessor#submit()} submit fails
    */
   @Procedure(name = "com.neo4j.import.file", mode = Mode.WRITE)
   public void importFile(@Name("file") String filePath,
                          @Name("batchSize") Long batchSize,
                          @Name("threads") Long threads) {

      try {
         createConstraint();

         BatchProcessor processor = new BatchProcessor(database, getExecutorService(threads));

         AtomicInteger counter = new AtomicInteger(0);

         Stream<String> lines = Files.lines(Paths.get(filePath))
               .skip(0)
               .sorted((o1, o2) -> {
                  String[] cols1 = o1.split(",");
                  String[] cols2 = o2.split(",");
                  return cols1[2].compareTo(cols2[2]) != 0 ? cols1[2].compareTo(cols2[2]) : cols1[3].compareTo(cols2[3]);
               });

         batches(lines, batchSize)
               .forEach(batch -> {
                  try {
                     processor.ingest(batch);
                  } catch (InterruptedException e) {
                     throw new RuntimeException(e);
                  }
               });
      } catch (IOException e) {
         log.error("Error while loading file: %s", e.getMessage(), e);
         throw new RuntimeException(e);
      }
   }

   /**
    * Method to create constraints before the start of data loading
    */
   private void createConstraint() {
      try (Transaction transaction = database.beginTx()) {
         transaction.execute("CREATE CONSTRAINT unique_phone_number IF NOT EXISTS FOR (n:Number) REQUIRE n.number IS UNIQUE");
         transaction.commit();
      } catch (RuntimeException e) {
         log.error("Error while create constraint", e.getMessage());
      }
   }

   /**
    *
    * @param threads: The number of parallel threads to be reused in the Thread pool.
    * @return An ExecutorService with the best thread configuration depending on the desired threads vs the available processors
    */
   private static ExecutorService getExecutorService(Long threads) {
      int availableProcessors = Runtime.getRuntime().availableProcessors();
      return Executors.newFixedThreadPool((threads <= availableProcessors) ? threads.intValue() : availableProcessors);
   }

   private <T> Stream<List<T>> batches(Stream<T> stream, long batchSize) {
      return batchSize == 0
            ? Stream.of(stream.collect(Collectors.toList()))
            : StreamSupport.stream(new BatchSpliterator<>(stream.spliterator(), batchSize), stream.isParallel());
   }
}
