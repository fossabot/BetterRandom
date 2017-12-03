package io.github.pr0methean.betterrandom.seed;

import io.github.pr0methean.betterrandom.ByteArrayReseedableRandom;
import io.github.pr0methean.betterrandom.EntropyCountingRandom;
import io.github.pr0methean.betterrandom.util.LogPreFormatter;
import io.github.pr0methean.betterrandom.util.LooperThread;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.logging.Level;

/**
 * Thread that loops over {@link Random} instances and reseeds them. No {@link
 * EntropyCountingRandom} will be reseeded when it's already had more input than output.
 * @author Chris Hennick
 */
@SuppressWarnings("ClassExplicitlyExtendsThread")
public final class RandomSeederThread extends LooperThread {

  private static final ExecutorService WAKER_UPPER = Executors.newSingleThreadExecutor();
  private static final LogPreFormatter LOG = new LogPreFormatter(RandomSeederThread.class);
  @SuppressWarnings("StaticCollection") private static final Map<SeedGenerator, RandomSeederThread>
      INSTANCES =
      Collections.synchronizedMap(new WeakHashMap<SeedGenerator, RandomSeederThread>(1));
  private static final long POLL_INTERVAL = 60;
  private static final AtomicBoolean loggingEnabled = new AtomicBoolean(true);
  private final SeedGenerator seedGenerator;
  private final byte[] longSeedArray = new byte[8];
  private Set<Random> prngs;
  private ByteBuffer longSeedBuffer;
  private Condition waitWhileEmpty;
  private Condition waitForEntropyDrain;
  private Set<Random> prngsThisIteration;
  private WeakHashMap<ByteArrayReseedableRandom, byte[]> seedArrays;

  /**
   * Private constructor because only one instance per seed source.
   */
  private RandomSeederThread(final SeedGenerator seedGenerator) {
    this.seedGenerator = seedGenerator;
    initTransientFields();
  }

  /**
   * Controls whether or not to log new instance creation and errors.
   * @param enabled whether to enable logging
   */
  public static void setLoggingEnabled(final boolean enabled) {
    loggingEnabled.set(enabled);
  }

  /**
   * Obtain the instance for the given {@link SeedGenerator}, creating and starting it if it doesn't
   * exist.
   * @param seedGenerator the {@link SeedGenerator} to use to seed PRNGs registered with this
   *     RandomSeederThread.
   * @return a RandomSeederThread that is running and is backed by {@code seedGenerator}.
   */
  private static RandomSeederThread getInstance(final SeedGenerator seedGenerator) {
    RandomSeederThread instance;
    synchronized (INSTANCES) {
      instance = INSTANCES.get(seedGenerator);
      if (instance == null) {
        if (loggingEnabled.get()) {
          LOG.info("Creating a RandomSeederThread for %s", seedGenerator);
        }
        instance = new RandomSeederThread(seedGenerator);
        instance.setName("RandomSeederThread for " + seedGenerator);
        instance.setDaemon(true);
        instance.setPriority(Thread.MIN_PRIORITY);
        instance.start();
      }
      INSTANCES.put(seedGenerator, instance);
    }
    return instance;
  }

  /**
   * Returns whether a RandomSeederThread using the given {@link SeedGenerator} is running or not.
   * @param seedGenerator a {@link SeedGenerator} to find an instance for.
   * @return true if a RandomSeederThread using the given {@link SeedGenerator} is running; false
   *     otherwise.
   */
  public static boolean hasInstance(final SeedGenerator seedGenerator) {
    synchronized (INSTANCES) {
      return INSTANCES.containsKey(seedGenerator);
    }
  }

  /**
   * Shut down all instances with which no {@link Random} instances are registered.
   */
  public static void stopAllEmpty() {
    final ArrayList<RandomSeederThread> toStop = new ArrayList<>();
    do {
      toStop.clear();
      synchronized (INSTANCES) {
        // This method is complicated because stopIfEmpty can't be called from inside this loop due
        // to SynchronizedMap limitations.
        for (final RandomSeederThread instance : INSTANCES.values()) {
          if (instance.isEmpty()) {
            toStop.add(instance);
          }
        }
      }
      for (final RandomSeederThread instance : toStop) {
        instance.stopIfEmpty();
      }
    } while (!toStop.isEmpty());
  }

  /**
   * Asynchronously triggers reseeding of the given {@link EntropyCountingRandom} if it is
   * associated with a live RandomSeederThread corresponding to the given {@link SeedGenerator}.
   * @param seedGenerator the {@link SeedGenerator} that should reseed {@code random}
   * @param random a {@link Random} to be reseeded
   * @return Whether or not the reseed was successfully scheduled.
   */
  public static boolean asyncReseed(final SeedGenerator seedGenerator, final Random random) {
    synchronized (INSTANCES) {
      return getInstance(seedGenerator).asyncReseed(random);
    }
  }

  public static boolean isEmpty(final SeedGenerator seedGenerator) {
    synchronized (INSTANCES) {
      return (!hasInstance(seedGenerator)) || getInstance(seedGenerator).isEmpty();
    }
  }

  /**
   * Add one or more {@link Random} instances to the thread for the given {@link SeedGenerator}.
   * @param seedGenerator The {@link SeedGenerator} that will reseed the {@code randoms}
   * @param randoms One or more {@link Random} instances to be reseeded
   */
  public static void add(final SeedGenerator seedGenerator, final Random... randoms) {
    synchronized (INSTANCES) {
      getInstance(seedGenerator).add(randoms);
    }
  }

  /**
   * Remove one or more {@link Random} instances from the thread for the given {@link SeedGenerator}
   * if such a thread exists and contains them.
   * @param seedGenerator The {@link SeedGenerator} that will reseed the {@code randoms}
   * @param randoms One or more {@link Random} instances to be reseeded
   */
  public static void remove(final SeedGenerator seedGenerator, final Random... randoms) {
    synchronized (INSTANCES) {
      final RandomSeederThread thread = INSTANCES.get(seedGenerator);
      if (thread != null) {
        thread.remove(randoms);
      }
    }
  }

  public static void stopIfEmpty(final SeedGenerator seedGenerator) {
    synchronized (INSTANCES) {
      if (hasInstance(seedGenerator)) {
        getInstance(seedGenerator).stopIfEmpty();
      }
    }
  }

  private void initTransientFields() {
    prngs =
        Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<Random, Boolean>(1)));
    longSeedBuffer = ByteBuffer.wrap(longSeedArray);
    waitWhileEmpty = lock.newCondition();
    waitForEntropyDrain = lock.newCondition();
    prngsThisIteration = new HashSet<>(1);
    seedArrays = new WeakHashMap<>(1);
  }

  /**
   * Asynchronously triggers reseeding of the given {@link EntropyCountingRandom} if it is
   * associated with a live RandomSeederThread.
   * @param random a {@link Random} object.
   * @return Whether or not the reseed was successfully scheduled.
   */
  private boolean asyncReseed(final Random random) {
    if (!(random instanceof EntropyCountingRandom)) {
      // Reseed of non-entropy-counting Random happens every iteration anyway
      return prngs.contains(random);
    }
    final boolean eligible;
    synchronized (prngs) {
      eligible = prngs.contains(random);
    }
    if (eligible) {
      WAKER_UPPER.submit(new Runnable() {
        @Override public void run() {
          lock.lock();
          try {
            waitForEntropyDrain.signalAll();
          } finally {
            lock.unlock();
          }
        }
      });
      return true;
    } else {
      return false;
    }
  }

  @SuppressWarnings({"InfiniteLoopStatement", "ObjectAllocationInLoop", "AwaitNotInLoop"}) @Override
  protected boolean iterate() throws InterruptedException {
    while (true) {
      synchronized (prngs) {
        prngsThisIteration.addAll(prngs);
      }
      if (prngsThisIteration.isEmpty()) {
        waitWhileEmpty.await();
      } else {
        break;
      }
    }
    final Iterator<Random> iterator = prngsThisIteration.iterator();
    boolean entropyConsumed = false;
    while (iterator.hasNext()) {
      final Random random = iterator.next();
      iterator.remove();
      if ((random instanceof EntropyCountingRandom) && (
          ((EntropyCountingRandom) random).getEntropyBits() > 0)) {
        continue;
      } else {
        entropyConsumed = true;
      }
      try {
        if ((random instanceof ByteArrayReseedableRandom) && !((ByteArrayReseedableRandom) random)
            .preferSeedWithLong()) {
          final ByteArrayReseedableRandom reseedable = (ByteArrayReseedableRandom) random;
          byte[] seedArray = seedArrays.get(reseedable);
          if (seedArray == null) {
            seedArray = new byte[reseedable.getNewSeedLength()];
            seedArrays.put(reseedable, seedArray);
          }
          seedGenerator.generateSeed(seedArray);
          reseedable.setSeed(seedArray);
        } else {
          seedGenerator.generateSeed(longSeedArray);
          random.setSeed(longSeedBuffer.getLong(0));
        }
      } catch (final Throwable t) {
        // Must unlock before interrupt; otherwise we somehow get a deadlock
        lock.unlock();
        if (loggingEnabled.get()) {
          LOG.error("%s", t);
          LOG.logStackTrace(Level.SEVERE, t.getStackTrace());
        }
        interrupt();
        // Must lock again before returning, so we can notify conditions
        lock.lock();
        return false;
      }
    }
    if (!entropyConsumed) {
      waitForEntropyDrain.await(POLL_INTERVAL, TimeUnit.SECONDS);
    }
    return true;
  }

  @Override public void interrupt() {
    synchronized (INSTANCES) {
      // Ensure dying instance is unregistered
      if (INSTANCES.get(seedGenerator) == this) {
        INSTANCES.remove(seedGenerator);
      }
    }
    super.interrupt();
  }

  /**
   * Returns true if no {@link Random} instances are registered with this RandomSeederThread.
   * @return true if no {@link Random} instances are registered with this RandomSeederThread.
   */
  private boolean isEmpty() {
    synchronized (prngs) {
      return prngs.isEmpty();
    }
  }

  /**
   * Add one or more {@link Random} instances. The caller must not hold locks on any of these
   * instances that are also acquired during {@link Random#setSeed(long)} or {@link
   * ByteArrayReseedableRandom#setSeed(byte[])}, as one of those methods may be called immediately
   * and this would cause a circular deadlock.
   * @param randoms One or more {@link Random} instances to be reseeded.
   */
  private void add(final Random... randoms) {
    lock.lock();
    if ((getState() == State.TERMINATED) || isInterrupted()) {
      throw new IllegalStateException("Already shut down");
    }
    Collections.addAll(prngs, randoms);
    try {
      waitForEntropyDrain.signalAll();
      waitWhileEmpty.signalAll();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Remove one or more {@link Random} instances. If this is called while {@link #getState()} ==
   * {@link State#RUNNABLE}, they may still be reseeded once more.
   * @param randoms the {@link Random} instances to remove.
   */
  private void remove(final Random... randoms) {
    prngs.removeAll(Arrays.asList(randoms));
  }

  /**
   * Shut down this thread if no {@link Random} instances are registered with it.
   */
  private void stopIfEmpty() {
    lock.lock();
    try {
      if (isEmpty()) {
        if (loggingEnabled.get()) {
          LOG.info("Stopping empty RandomSeederThread for %s", seedGenerator);
        }
        interrupt();
      }
    } finally {
      lock.unlock();
    }
  }
}
