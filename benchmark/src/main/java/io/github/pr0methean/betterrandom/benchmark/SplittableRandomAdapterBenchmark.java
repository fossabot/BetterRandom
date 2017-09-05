package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.prng.adapter.SplittableRandomAdapter;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

public class SplittableRandomAdapterBenchmark extends AbstractRandomBenchmark {

  @Override
  protected Random createPrng(@UnknownInitialization SplittableRandomAdapterBenchmark
      this) throws SeedException {
    return new SplittableRandomAdapter(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR);
  }
}
