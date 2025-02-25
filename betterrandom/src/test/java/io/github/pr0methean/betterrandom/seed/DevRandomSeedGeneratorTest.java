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

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.File;
import org.testng.annotations.Test;

/**
 * Unit test for the seed generator that reads data from /dev/random (on platforms that provide
 * it).
 * @author Daniel Dyer
 */
@SuppressWarnings("HardcodedFileSeparator")
public class DevRandomSeedGeneratorTest extends AbstractSeedGeneratorTest {

  public DevRandomSeedGeneratorTest() {
    super(DevRandomSeedGenerator.DEV_RANDOM_SEED_GENERATOR);
  }

  @Test(timeOut = 60_000) public void testGenerator() {
    if (new File("/dev/random").exists()) {
      SeedTestUtils.testGenerator(seedGenerator, true);
      assertTrue(seedGenerator.isWorthTrying());
    } else {
      try {
        DevRandomSeedGenerator.DEV_RANDOM_SEED_GENERATOR.generateSeed(new byte[1]);
        fail("Should have thrown a SeedException");
      } catch (SeedException expected) {}
    }
  }
}
