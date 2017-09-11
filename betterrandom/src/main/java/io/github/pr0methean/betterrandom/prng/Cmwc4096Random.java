// ============================================================================
//   Copyright 2006-2012 Daniel W. Dyer
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
// ============================================================================
package io.github.pr0methean.betterrandom.prng;

import com.google.common.base.MoreObjects.ToStringHelper;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import java.util.Arrays;
import java.util.Random;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;

/**
 * <p>A Java version of George Marsaglia's <a href="http://school.anhb.uwa.edu.au/personalpages/kwessen/shared/Marsaglia03.html">Complementary
 * Multiply With Carry (CMWC) RNG</a>. This is a very fast PRNG with an extremely long period
 * (2^131104).  It should be used in preference to the {@link io.github.pr0methean.betterrandom.prng.MersenneTwisterRandom}
 * when a very long period is required.</p> <p>One potential drawback of this RNG is that it
 * requires significantly more seed data than the other RNGs provided by Uncommons Maths.  It
 * requires just over 16 kilobytes, which may be a problem if your are obtaining seed data from a
 * slow or limited entropy source. In contrast, the Mersenne Twister requires only 128 bits of seed
 * data.</p> <p><em>NOTE: Because instances of this class require 16-kilobyte seeds, it is not
 * possible to seed this RNG using the {@link #setSeed(long)} method inherited from {@link
 * java.util.Random}.  Calls to this method will have no effect. Instead the seed must be set by a
 * constructor.</em></p>
 *
 * @author Daniel Dyer
 * @version $Id: $Id
 * @since 1.2
 */
public class Cmwc4096Random extends BaseEntropyCountingRandom {

  private static final int SEED_SIZE_BYTES = 16384; // Needs 4,096 32-bit integers.

  private static final long A = 18782L;
  private static final long serialVersionUID = 1731465909906078875L;

  private int[] state;
  private int carry;
  private int index;

  /**
   * Creates a new RNG and seeds it using the default seeding strategy.
   *
   * @throws io.github.pr0methean.betterrandom.seed.SeedException if any.
   */
  public Cmwc4096Random() throws SeedException {
    this(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(SEED_SIZE_BYTES));
  }

  /**
   * Seed the RNG using the provided seed generation strategy.
   *
   * @param seedGenerator The seed generation strategy that will provide the seed value for this
   *     RNG.
   * @throws io.github.pr0methean.betterrandom.seed.SeedException If there is a problem
   *     generating a seed.
   */
  public Cmwc4096Random(final SeedGenerator seedGenerator) throws SeedException {
    this(seedGenerator.generateSeed(SEED_SIZE_BYTES));
  }

  /**
   * Creates an RNG and seeds it with the specified seed data.
   *
   * @param seed The seed data used to initialise the RNG.
   */
  public Cmwc4096Random(final byte[] seed) {
    super(seed);
    assert state != null : "@AssumeAssertion(nullness)";
  }

  /** {@inheritDoc} */
  @Override
  protected ToStringHelper addSubSubclassFields(final ToStringHelper original) {
    return original
        .add("state", Arrays.toString(state));
  }

  /**
   * <p>getSeed.</p>
   *
   * @return an array of byte.
   */
  public byte[] getSeed() {
    return seed.clone();
  }

  /** {@inheritDoc} */
  @Override
  public synchronized void setSeed(@UnknownInitialization(Random.class)Cmwc4096Random this,
      final long seed) {
    if (superConstructorFinished) {
      super.setSeed(seed);
    } // Otherwise ignore; it's Random.<init> calling us without a full-size seed
  }

  /** {@inheritDoc} */
  @EnsuresNonNull({"this.seed", "state"})
  @Override
  protected void setSeedInternal(@UnknownInitialization(Random.class)Cmwc4096Random this,
      final byte[] seed) {
    if (seed == null || seed.length != SEED_SIZE_BYTES) {
      throw new IllegalArgumentException("CMWC RNG requires 16kb of seed data.");
    }
    super.setSeedInternal(seed);
    state = BinaryUtils.convertBytesToInts(seed);
    carry = 362436; // TODO: This should be randomly generated.
    index = 4095;
  }

  /** {@inheritDoc} */
  @Override
  protected int next(final int bits) {
    lock.lock();
    try {
      index = (index + 1) & 4095;
      final long t = A * (state[index] & 0xFFFFFFFFL) + carry;
      carry = (int) (t >> 32);
      int x = ((int) t) + carry;
      if (x < carry) {
        x++;
        carry++;
      }
      state[index] = 0xFFFFFFFE - x;
      recordEntropySpent(bits);
      return state[index] >>> (32 - bits);
    } finally {
      lock.unlock();
    }
  }

  /** {@inheritDoc} */
  @Override
  public int getNewSeedLength(@UnknownInitialization Cmwc4096Random this) {
    return SEED_SIZE_BYTES;
  }
}
