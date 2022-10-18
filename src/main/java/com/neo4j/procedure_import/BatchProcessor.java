package com.neo4j.procedure_import;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

public class BatchProcessor {

   private LinkedBlockingQueue<List<String>> queue;
   private ExecutorService executorService;
   private GraphDatabaseService graphDatabaseService;

   public BatchProcessor(GraphDatabaseService graphDatabaseService, ExecutorService executorService) {
      this.graphDatabaseService = graphDatabaseService;
      this.queue = new LinkedBlockingQueue<>();
      this.executorService = executorService;
   }

   /**
    *
    * @return A future of the number of inserted nodes in the database
    */
   public Future<AtomicInteger> submit() {
      return executorService.submit(() -> {
         AtomicInteger insertedNodes = new AtomicInteger(0);
         List<List<String>> batch = new ArrayList<>();
         this.queue.drainTo(batch);
         batch.forEach(lines -> {
            try (Transaction tx = graphDatabaseService.beginTx()) {
               lines.stream()
                     .filter(line -> !line.isEmpty())
                     .filter(CSVColsValidator.INSTANCE)
                     .forEach(line -> {
                        String[] cols = line.split(",");
                        Result result = tx.execute(buildQuery(cols[1]),
                              Map.of(
                                    "fromNumber", cols[2],
                                    "toNumber", cols[3],
                                    "type", cols[1],
                                    "date", cols[0]));
                        insertedNodes.set(insertedNodes.get() + result.getQueryStatistics().getNodesCreated());
                     });
               tx.commit();
            }
         });
         return insertedNodes;
      });
   }

   /**
    *
    * @param type: The type of relationship to consider for creating between the nodes
    * @return A parameterized Cypher query built based on the provided CSV line
    */
   private static String buildQuery(String type) {
      return "MERGE (from:Number {number: $fromNumber}) " +
            "MERGE (to:Number {number: $toNumber}) " +
            (type.equals("SMS") ? "MERGE (from)-[:SMS {date: $date}]->(to)" : "MERGE (from)-[:CALL {date: $date}]->(to)");
   }

   /**
    *
    * @param lines: The lines read from the file which the size is equal to the batchSize
    * @return A future of the number of inserted nodes in the database
    * throws InterruptedException: Thrown if {@link BatchProcessor#submit()} submit fails
    */
   public Future<AtomicInteger> ingest(List<String> lines) throws InterruptedException {
      this.queue.put(lines);
      return submit();
   }
}
