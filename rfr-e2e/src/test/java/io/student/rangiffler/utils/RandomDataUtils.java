package io.student.rangiffler.utils;

import java.util.concurrent.ThreadLocalRandom;

public final class RandomDataUtils {

  private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";

  private RandomDataUtils() {
  }

  /** rfr-auth allows usernames of 3..50 characters. */
  public static String randomUsername() {
    return "rfr_" + randomString(10);
  }

  /** rfr-auth allows passwords of 3..12 characters. */
  public static String randomPassword() {
    return randomString(10);
  }

  private static String randomString(int length) {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
    }
    return sb.toString();
  }
}
