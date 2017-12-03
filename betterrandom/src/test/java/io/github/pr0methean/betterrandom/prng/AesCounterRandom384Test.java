package io.github.pr0methean.betterrandom.prng;

import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;

public class AesCounterRandom384Test extends AesCounterRandom256Test {

  private static final boolean UNLIMITED_STRENGTH_CRYPTO;

  static {
    try {
      UNLIMITED_STRENGTH_CRYPTO = Cipher.getMaxAllowedKeyLength("AES") >= 256;
    } catch (final NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  @Override protected int getNewSeedLength(final BaseRandom basePrng) {
    return UNLIMITED_STRENGTH_CRYPTO ? 48 : 32;
  }

  @Override public BaseRandom createRng() {
    return UNLIMITED_STRENGTH_CRYPTO ? new AesCounterRandom(48) : super.createRng();
  }
}
