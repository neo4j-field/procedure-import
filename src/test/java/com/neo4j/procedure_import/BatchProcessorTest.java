package com.neo4j.procedure_import;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;

public class BatchProcessorTest {

   private static Driver driver;
   private static Neo4j embeddedDatabaseServer;
   private static BatchProcessor batchProcessor;
   private static ExecutorService executorService;

   @BeforeAll
   public static void setUp() {
      embeddedDatabaseServer = Neo4jBuilders.newInProcessBuilder()
            .withDisabledServer()
            .withProcedure(ImportCsv.class)
            .build();
      driver = GraphDatabase.driver(embeddedDatabaseServer.boltURI());

      executorService = Executors.newFixedThreadPool(2);
      batchProcessor = new BatchProcessor(embeddedDatabaseServer.defaultDatabaseService(), executorService);
   }

   @AfterAll
   public static void tearDown() {
      driver.close();
      embeddedDatabaseServer.close();
   }

   @AfterEach
   public void cleanupDatabase() {
      try (Session session = driver.session()) {
         session.run("MATCH (n) DETACH DELETE n");
      }
   }

   @Test
   public void should_find_data_filled_in_database() throws InterruptedException, ExecutionException {

      Future<AtomicInteger> firstBatch = batchProcessor.ingest(Arrays.asList(
            "2121-02-25 11:02:36,CALL,+348880024555,+751924669862",
            "2020-05-19 12:05:51,SMS,+56247397924,+826584444673"
      ));
      Future<AtomicInteger> secondBatch = batchProcessor.ingest(Arrays.asList(
            "2121-07-26 09:07:14,CALL,+73937509218,+592876432164",
            "2020-03-02 03:03:30,CALL,+348880024555,+751924669862"
      ));

      executorService.awaitTermination(1, TimeUnit.SECONDS);
      Assertions.assertThat(firstBatch.get().get()).isEqualTo(6);

      try(Session session = driver.session()) {
         Result result = session.run("MATCH (n1:Number)-[:CALL]->(n2:Number) RETURN *");
         List<Record> records = result.list();
         Assertions.assertThat(records).hasSize(3);
         Assertions.assertThat(records.get(0).get("n1").get("number").asString()).isEqualTo("+348880024555");
         Assertions.assertThat(records.get(0).get("n2").get("number").asString()).isEqualTo("+751924669862");

         Assertions.assertThat(records.get(1).get("n1").get("number").asString()).isEqualTo("+73937509218");
         Assertions.assertThat(records.get(1).get("n2").get("number").asString()).isEqualTo("+592876432164");

         Assertions.assertThat(records.get(2).get("n1").get("number").asString()).isEqualTo("+348880024555");
         Assertions.assertThat(records.get(2).get("n2").get("number").asString()).isEqualTo("+751924669862");

         result = session.run("MATCH (n1:Number)-[:SMS]->(n2:Number) RETURN *");
         records = result.list();
         Assertions.assertThat(records).hasSize(1);
         Assertions.assertThat(records.get(0).get("n1").get("number").asString()).isEqualTo("+56247397924");
         Assertions.assertThat(records.get(0).get("n2").get("number").asString()).isEqualTo("+826584444673");

      }
   }
}