package io.student.rangiffler.stand;

import io.student.rangiffler.config.TestConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record StandService(String name, String startCommand, Check check) {

  private static final Pattern MYSQL_JDBC_URL = Pattern.compile("jdbc:mysql://([^:/?#]+)(?::(\\d+))?([/?#].*)?");
  private static final int MYSQL_DEFAULT_PORT = 3306;

  @FunctionalInterface
  public interface Check {
    boolean responds(Duration timeout);
  }

  public static List<StandService> fromConfig() {
    InetSocketAddress mysql = mysqlAddress(TestConfig.authJdbcUrl());
    return List.of(
        tcp("MySQL", "docker compose up -d --wait", mysql.getHostString(), mysql.getPort()),
        httpGet("rfr-auth", "./gradlew runAll", URI.create(TestConfig.authUrl() + "/.well-known/openid-configuration")),
        graphql("rfr-api", "./gradlew runAll", URI.create(TestConfig.apiUrl() + "/graphql")),
        httpGet("rfr-gql-client", "cd rfr-gql-client && npm run dev", URI.create(TestConfig.frontUrl())));
  }

  public static InetSocketAddress mysqlAddress(String jdbcUrl) {
    Matcher matcher = MYSQL_JDBC_URL.matcher(jdbcUrl);
    if (!matcher.matches()) {
      throw new IllegalArgumentException(
          "Cannot read the MySQL host and port from rfr.auth.jdbc.url '" + jdbcUrl + "', expected jdbc:mysql://host[:port]/schema");
    }
    int port = matcher.group(2) == null ? MYSQL_DEFAULT_PORT : Integer.parseInt(matcher.group(2));
    return InetSocketAddress.createUnresolved(matcher.group(1), port);
  }

  public static StandService tcp(String name, String startCommand, String host, int port) {
    return new StandService(name, startCommand, timeout -> {
      try (Socket socket = new Socket()) {
        socket.connect(new InetSocketAddress(host, port), Math.toIntExact(timeout.toMillis()));
        return true;
      } catch (IOException exception) {
        return false;
      }
    });
  }

  public static StandService httpGet(String name, String startCommand, URI uri) {
    return new StandService(name, startCommand,
        timeout -> status(HttpRequest.newBuilder(uri).GET(), timeout) / 100 == 2);
  }

  public static StandService graphql(String name, String startCommand, URI uri) {
    return new StandService(name, startCommand, timeout -> status(HttpRequest.newBuilder(uri)
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString("{\"query\":\"{__typename}\"}")), timeout) > 0);
  }

  private static int status(HttpRequest.Builder request, Duration timeout) {
    try (HttpClient client = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(timeout)
        .build()) {
      return client.send(request.timeout(timeout).build(), HttpResponse.BodyHandlers.discarding()).statusCode();
    } catch (IOException exception) {
      return -1;
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      return -1;
    }
  }
}
