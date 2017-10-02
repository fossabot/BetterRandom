package io.github.pr0methean.betterrandom.util;

import java.util.Spliterator.OfInt;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An unordered, concurrent {@link OfInt} that invokes an {@link IntSupplier} to get its values and
 * has a preset size.
 */
public class IntSupplierSpliterator implements OfInt {

  private final AtomicLong remaining;
  private final IntSupplier supplier;

  public IntSupplierSpliterator(final long size, final IntSupplier supplier) {
    this(new AtomicLong(size), supplier);
  }

  /** Used to share the AtomicLong between partitions. */
  private IntSupplierSpliterator(final AtomicLong remaining, final IntSupplier supplier) {
    this.remaining = remaining;
    this.supplier = supplier;
  }

  @SuppressWarnings("override.return.invalid") // actually is nullable in the interface
  @Override
  public @Nullable OfInt trySplit() {
    return (remaining.get() <= 0) ? null : new IntSupplierSpliterator(remaining, supplier);
  }

  @Override
  public long estimateSize() {
    return remaining.get();
  }

  @Override
  public int characteristics() {
    return SIZED | CONCURRENT | IMMUTABLE | NONNULL;
  }

  @Override
  public boolean tryAdvance(final IntConsumer action) {
    if (remaining.decrementAndGet() >= 0) {
      action.accept(supplier.getAsInt());
      return true;
    } else {
      return false;
    }
  }
}
