package com.neo4j.procedure_import;

import java.util.Objects;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;

class ImportCsvTest {

   private static Driver driver;
   private static Neo4j embeddedDatabaseServer;

   @BeforeAll
   public static void setUp() {
      embeddedDatabaseServer = Neo4jBuilders.newInProcessBuilder()
            .withDisabledServer()
            .withProcedure(ImportCsv.class)
            .build();
      driver = GraphDatabase.driver(embeddedDatabaseServer.boltURI());
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
   void should_have_data_created_in_database_when_loading_file() throws InterruptedException {
      try (Session session = driver.session()) {

         String file = Objects.requireNonNull(ImportCsvTest.class.getClassLoader().getResource("phones.csv")).getFile();

         session.run("CALL com.neo4j.import.file($file, $batchSize, $threads)",
               Values.parameters("file", file, "batchSize", 100, "threads", 1));
      }

      Thread.sleep(2000);

      try (Session session = driver.session()) {
         Result result = session.run("MATCH (n) RETURN COUNT(n) as count");
         Assertions.assertThat(result.single().get("count").asLong()).isEqualTo(32);
      }
   }
}