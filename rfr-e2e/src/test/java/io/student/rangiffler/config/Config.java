package io.student.rangiffler.config;

public final class Config {

  private Config() {
  }

  public static String frontUrl() {
    return System.getProperty("frontUrl", "http://localhost:3001");
  }

  public static String authUrl() {
    return System.getProperty("authUrl", "http://localhost:9001");
  }

  public static boolean headless() {
    return Boolean.parseBoolean(System.getProperty("headless", "true"));
  }
}
