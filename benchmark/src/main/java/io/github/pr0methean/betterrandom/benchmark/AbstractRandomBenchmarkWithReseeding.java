/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.seed.RandomDotOrgSeedGenerator;
import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
import io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator;
import java.util.UUID;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Timeout;
import org.openjdk.jmh.annotations.Warmup;

@SuppressWarnings("MethodMayBeStatic")
@State(Scope.Benchmark)
public abstract class AbstractRandomBenchmarkWithReseeding extends AbstractRandomBenchmark {

  private static final RandomSeederThread RANDOM_SEEDER = new RandomSeederThread(
      SecureRandomSeedGenerator.SECURE_RANDOM_SEED_GENERATOR);

  @SuppressWarnings("CallToSystemGetenv") @Setup public void setApiKey() {
    final String apiKeyString = System.getenv("RANDOM_DOT_ORG_KEY");
    RandomDotOrgSeedGenerator
        .setApiKey((apiKeyString == null) ? null : UUID.fromString(apiKeyString));
  }

  @Override
  @Setup(Level.Trial)
  public void setUp() throws Exception {
    super.setUp();
    RANDOM_SEEDER.add(prng);
  }

  @TearDown(Level.Trial)
  public void tearDown() {
    RANDOM_SEEDER.remove(prng);
  }

  @Timeout(time = 60) // seconds per iteration
  @Measurement(iterations = 5, time = 4)
  @Warmup(iterations = 5, time = 4)
  @Benchmark public byte testBytesSequentialReseeding() {
    return innerTestBytesSequential();
  }

  /*
  @Benchmark
  @Group("contended")
  public byte testBytesContendedReseeding()
      throws SeedException, InterruptedException {
    randomSeeder.add(prng);
    final byte b = innerTestBytesContended();
    randomSeeder.remove(prng);
    return b;
  }
  */
}
