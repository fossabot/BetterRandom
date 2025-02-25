package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.prng.AesCounterRandom;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;
import org.openjdk.jmh.annotations.Param;

import static io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator.SECURE_RANDOM_SEED_GENERATOR;

public class AesCounterRandomBenchmark extends AbstractRandomBenchmarkWithReseeding {

  @Param({"16", "32"}) public int seedSize;

  @Override protected Random createPrng() throws SeedException {
    return new AesCounterRandom(SECURE_RANDOM_SEED_GENERATOR.generateSeed(seedSize));
  }
}
