package io.student.rangiffler.data.db;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class Jdbc {

  private static final Map<Databases, JdbcClient> CLIENTS = new ConcurrentHashMap<>();
  private static final Map<String, String> SQL_CACHE = new ConcurrentHashMap<>();

  private Jdbc() {
  }

  public static JdbcClient client(Databases db) {
    return CLIENTS.computeIfAbsent(db, d -> JdbcClient.create(d.dataSource()));
  }

  public static <T> T inTransaction(Databases db, Supplier<T> action) {
    return new TransactionTemplate(db.transactionManager()).execute(status -> action.get());
  }

  public static void inTransaction(Databases db, Runnable action) {
    new TransactionTemplate(db.transactionManager()).executeWithoutResult(status -> action.run());
  }

  public static String sql(String resourcePath) {
    return SQL_CACHE.computeIfAbsent(resourcePath, Jdbc::load);
  }

  private static String load(String resourcePath) {
    try (InputStream is = Jdbc.class.getClassLoader().getResourceAsStream(resourcePath)) {
      if (is == null) {
        throw new IllegalArgumentException("SQL resource not found: " + resourcePath);
      }
      // the file keeps its ';' for IDE tooling, but a JDBC statement must not end with it
      return new String(is.readAllBytes(), StandardCharsets.UTF_8).strip().replaceFirst(";$", "");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
