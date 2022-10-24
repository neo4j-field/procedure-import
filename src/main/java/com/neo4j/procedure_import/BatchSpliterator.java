package com.neo4j.procedure_import;

import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

public class BatchSpliterator<E> implements Spliterator<List<E>> {

   private final Spliterator<E> base;
   private final Long batchSize;

   public BatchSpliterator(Spliterator<E> base, Long batchSize) {
      this.base = base;
      this.batchSize = batchSize;
   }

   @Override
   public boolean tryAdvance(Consumer<? super List<E>> action) {
      final List<E> batch = new ArrayList<>(batchSize.intValue());
      for (int i=0; i < batchSize && base.tryAdvance(batch::add); i++);
      if (batch.isEmpty())
         return false;
      action.accept(batch);
      return true;
   }

   @Override
   public Spliterator<List<E>> trySplit() {
      if (base.estimateSize() <= batchSize)
         return null;
      final Spliterator<E> splitBase = this.base.trySplit();
      return splitBase == null ? null
            : new BatchSpliterator<>(splitBase, batchSize);
   }

   @Override
   public long estimateSize() {
      final double baseSize = base.estimateSize();
      return baseSize == 0 ? 0
            : (long) Math.ceil(baseSize / (double) batchSize);
   }

   @Override
   public int characteristics() {
      return base.characteristics();
   }

}