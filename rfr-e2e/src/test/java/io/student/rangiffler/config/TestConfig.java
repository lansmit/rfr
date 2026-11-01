package io.student.rangiffler.config;

import java.time.Duration;
import java.util.Locale;

public final class TestConfig {

  private static final String DEFAULT_FRONT_URL = "http://localhost:3001";
  private static final String DEFAULT_AUTH_URL = "http://localhost:9001";
  private static final String DEFAULT_API_URL = "http://localhost:8081";
  private static final int DEFAULT_STAND_TIMEOUT_SECONDS = 180;
  private static final boolean DEFAULT_HEADLESS = true;
  private static final String DEFAULT_AUTH_JDBC_URL = "jdbc:mysql://localhost:3306/rangiffler-auth?serverTimezone=UTC";
  private static final String DEFAULT_API_JDBC_URL = "jdbc:mysql://localhost:3306/rangiffler-api?serverTimezone=UTC";
  private static final String DEFAULT_DB_USERNAME = "root";
  private static final String DEFAULT_DB_PASSWORD = "secret";
  private static final String DEFAULT_AUTH_MIGRATIONS = "../rfr-auth/src/main/resources/db/migration/rangiffler-auth";
  private static final String DEFAULT_API_MIGRATIONS = "../rfr-api/src/main/resources/db/migration/rangiffler-api";

  private TestConfig() {
  }

  public static String frontUrl() {
    return get("front.url", DEFAULT_FRONT_URL);
  }

  public static String authUrl() {
    return get("auth.url", DEFAULT_AUTH_URL);
  }

  public static String apiUrl() {
    return get("api.url", DEFAULT_API_URL);
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

  public static boolean standCheck() {
    return Boolean.parseBoolean(get("standCheck", "true"));
  }

  public static Duration standTimeout() {
    return Duration.ofSeconds(Long.parseLong(get("standTimeout", String.valueOf(DEFAULT_STAND_TIMEOUT_SECONDS))));
  }

  public static boolean migrationCheck() {
    return Boolean.parseBoolean(get("migrationCheck", "false"));
  }

  public static String authMigrations() {
    return get("authMigrations", DEFAULT_AUTH_MIGRATIONS);
  }

  public static String apiMigrations() {
    return get("apiMigrations", DEFAULT_API_MIGRATIONS);
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
