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

import com.google.common.collect.ImmutableList;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import org.testng.annotations.Test;

/**
 * Unit test for the JDK RNG.
 * @author Daniel Dyer
 */
public class RandomWrapperRandomTest extends BaseRandomTest {

  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return RandomWrapper.class;
  }

  @Override public Map<Class<?>, Object> constructorParams() {
    Map<Class<?>, Object> params = super.constructorParams();
    params.put(Random.class, new Random());
    return params;
  }

  /**
   * Assertion-free with respect to the long/double methods because, contrary to its contract to be
   * thread-safe, {@link Random#nextLong()} is not transactional. Rather, it uses two calls to
   * {@link Random#next(int)} that can interleave with calls from other threads.
   */
  @Override public void testThreadSafety() {
    testThreadSafety(ImmutableList.of(NEXT_INT),
        Collections.<NamedFunction<Random,Double>>emptyList());
    testThreadSafetyVsCrashesOnly(FUNCTIONS_FOR_THREAD_SAFETY_TEST);
  }

  /**
   * Test to ensure that two distinct RNGs with the same seed return the same sequence of numbers.
   */
  @Override @Test(timeOut = 15000) public void testRepeatability() throws SeedException {
    // Create an RNG using the default seeding strategy.
    final RandomWrapper rng = new RandomWrapper();
    // Create second RNG using same seed.
    final RandomWrapper duplicateRNG = new RandomWrapper(rng.getSeed());
    assert RandomTestUtils.testEquivalence(rng, duplicateRNG, 1000)
        : "Generated sequences do not match.";
  }

  @Override protected BaseRandom createRng() throws SeedException {
    return new RandomWrapper();
  }

  @Override protected BaseRandom createRng(final byte[] seed) throws SeedException {
    return new RandomWrapper(seed);
  }
}
