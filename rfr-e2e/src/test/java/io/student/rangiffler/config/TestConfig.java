package io.student.rangiffler.config;

import java.util.Locale;

public final class TestConfig {

  private static final String DEFAULT_FRONT_URL = "http://localhost:3001";
  private static final String DEFAULT_AUTH_URL = "http://localhost:9001";
  private static final boolean DEFAULT_HEADLESS = true;
  private static final String DEFAULT_AUTH_JDBC_URL = "jdbc:mysql://localhost:3306/rangiffler-auth?serverTimezone=UTC";
  private static final String DEFAULT_API_JDBC_URL = "jdbc:mysql://localhost:3306/rangiffler-api?serverTimezone=UTC";
  private static final String DEFAULT_DB_USERNAME = "root";
  private static final String DEFAULT_DB_PASSWORD = "secret";

  private TestConfig() {
  }

  public static String frontUrl() {
    return get("front.url", DEFAULT_FRONT_URL);
  }

  public static String authUrl() {
    return get("auth.url", DEFAULT_AUTH_URL);
  }

  public static boolean headless() {
    return Boolean.parseBoolean(get("headless", String.valueOf(DEFAULT_HEADLESS)));
  }

  public static String authJdbcUrl() {
    return get("auth.jdbc.url", DEFAULT_AUTH_JDBC_URL);
  }

  public static String apiJdbcUrl() {
    return get("api.jdbc.url", DEFAULT_API_JDBC_URL);
  }

  public static String dbUsername() {
    return get("db.username", DEFAULT_DB_USERNAME);
  }

  public static String dbPassword() {
    return get("db.password", DEFAULT_DB_PASSWORD);
  }

  private static String get(String key, String defaultValue) {
    String value = System.getProperty("rfr." + key);
    if (isBlank(value)) {
      value = System.getenv("RFR_" + key.replace('.', '_').toUpperCase(Locale.ROOT));
    }
    return isBlank(value) ? defaultValue : value;
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
