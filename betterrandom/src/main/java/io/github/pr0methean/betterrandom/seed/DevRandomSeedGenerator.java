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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RNG seed strategy that gets data from {@code /dev/random} on systems that provide it (e.g.
 * Solaris/Linux).  If {@code /dev/random} does not exist or is not accessible, a {@link
 * SeedException} is thrown. If it didn't exist during a previous call to this method or to {@link
 * #generateSeed(int)}, then for performance reasons, we assume for the rest of the JVM's lifespan
 * that it still doesn't exist.
 * @author Daniel Dyer
 */
public enum DevRandomSeedGenerator implements SeedGenerator {

  /** Singleton instance. */
  DEV_RANDOM_SEED_GENERATOR;

  private static final Logger LOG = LoggerFactory.getLogger(DevRandomSeedGenerator.class);
  @SuppressWarnings("HardcodedFileSeparator") private static final String DEV_RANDOM_STRING =
      "/dev/random";
  private static final File DEV_RANDOM = new File(DEV_RANDOM_STRING);
  private static final AtomicBoolean DEV_RANDOM_DOES_NOT_EXIST = new AtomicBoolean(false);
  private static volatile InputStream inputStream;

  /**
   * @throws SeedException if {@literal /dev/random} does not exist or is not accessible.
   */
  @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod") @Override public void generateSeed(
      final byte[] randomSeed) throws SeedException {
    if (!isWorthTrying()) {
      throw new SeedException(DEV_RANDOM_STRING + " did not exist when previously checked for");
    }

    try {
      if (inputStream == null) {
        synchronized (DevRandomSeedGenerator.class) {
          if (inputStream == null) {
            inputStream = new FileInputStream(DEV_RANDOM);
          }
        }
      }
      final int length = randomSeed.length;
      int count = 0;
      while (count < length) {
        final int bytesRead = inputStream.read(randomSeed, count, length - count);
        if (bytesRead == -1) {
          throw new SeedException("EOF encountered reading random data.");
        }
        count += bytesRead;
      }
    } catch (final IOException ex) {
      if (!DEV_RANDOM.exists()) {
        LOG.error("{} does not exist", DEV_RANDOM_STRING);
        DEV_RANDOM_DOES_NOT_EXIST.lazySet(true);
      }
      throw new SeedException("Failed reading from " + DEV_RANDOM_STRING, ex);
    } catch (final SecurityException ex) {
      // Might be thrown if resource access is restricted (such as in
      // an applet sandbox).
      throw new SeedException("SecurityManager prevented access to " + DEV_RANDOM_STRING, ex);
    }
  }

  @Override public boolean isWorthTrying() {
    return !(DEV_RANDOM_DOES_NOT_EXIST.get());
  }

  /** Returns "/dev/random". */
  @Override public String toString() {
    return DEV_RANDOM_STRING;
  }
}
