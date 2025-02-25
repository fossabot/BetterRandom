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
package io.github.pr0methean.betterrandom.seed;

import java.io.Serializable;

/**
 * Strategy interface for seeding random number generators. Implementations that can have multiple
 * equivalent instances should implement {@link Object#equals(Object)} and {@link Object#hashCode()}
 * to support {@link RandomSeederThread}.
 * @author Daniel Dyer
 * @author Chris Hennick
 */
@FunctionalInterface
public interface SeedGenerator extends Serializable {

  byte[] EMPTY_SEED = {};

  /**
   * Generates a seed value for a random number generator in an existing array.
   * @param output The array that is to be populated with the seed.
   * @throws SeedException If a seed cannot be generated for any reason.
   */
  void generateSeed(byte[] output) throws SeedException;

  /**
   * Generates and returns a seed value for a random number generator as a new array.
   * @param length The length of the seed to generate (in bytes).
   * @return A byte array containing the seed data.
   * @throws SeedException If a seed cannot be generated for any reason.
   */
  default byte[] generateSeed(final int length) throws SeedException {
    if (length <= 0) {
      return EMPTY_SEED;
    }
    final byte[] output = new byte[length];
    generateSeed(output);
    return output;
  }

  /**
   * Returns true if we cannot determine quickly (i.e. without I/O calls) that this SeedGenerator
   * would throw a {@link SeedException} if {@link #generateSeed(int)} or {@link
   * #generateSeed(byte[])} were being called right now.
   * @return true if this SeedGenerator will get as far as an I/O call or other slow operation in
   *     attempting to generate a seed immediately.
   */
  default boolean isWorthTrying() {
    return true;
  }
}
