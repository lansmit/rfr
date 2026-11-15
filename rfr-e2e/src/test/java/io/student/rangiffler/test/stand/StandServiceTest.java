package io.student.rangiffler.test.stand;

import com.sun.net.httpserver.HttpServer;
import io.student.rangiffler.stand.StandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("StandService: адреса сервисов стенда")
class StandServiceTest {

  @Test
  @DisplayName("URL без порта — порт MySQL по умолчанию 3306")
  void urlWithoutPortUsesDefaultPort() {
    InetSocketAddress address = StandService.mysqlAddress("jdbc:mysql://localhost/rangiffler-auth?serverTimezone=UTC");

    assertEquals("localhost", address.getHostString(), "Wrong MySQL host");
    assertEquals(3306, address.getPort(), "URL without a port did not fall back to 3306");
  }

  @Test
  @DisplayName("URL с портом — берётся указанный порт")
  void urlWithPortUsesThatPort() {
    InetSocketAddress address = StandService.mysqlAddress("jdbc:mysql://127.0.0.1:3307/rangiffler-auth");

    assertEquals("127.0.0.1", address.getHostString(), "Wrong MySQL host");
    assertEquals(3307, address.getPort(), "Wrong MySQL port");
  }

  @Test
  @DisplayName("Хост с подчёркиванием (имя docker-сервиса) читается")
  void hostWithUnderscoreIsRead() {
    InetSocketAddress address = StandService.mysqlAddress("jdbc:mysql://rfr_mysql:3306/rangiffler-auth");

    assertEquals("rfr_mysql", address.getHostString(), "Host with an underscore was not read");
  }

  @Test
  @DisplayName("Сервис слушает только ::1 — HTTP-проверка по localhost до него достаёт")
  void httpCheckReachesServiceListeningOnlyOnIpv6Loopback() throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("::1"), 0), 0);
    server.createContext("/", exchange -> {
      exchange.sendResponseHeaders(200, -1);
      exchange.close();
    });
    server.start();
    try {
      URI uri = URI.create("http://localhost:" + server.getAddress().getPort() + "/");

      assertTrue(StandService.httpGet("rfr-gql-client", "npm run dev", uri).check().responds(Duration.ofSeconds(2)),
          "The check did not reach a server listening only on ::1");
    } finally {
      server.stop(0);
    }
  }

  @Test
  @DisplayName("Неразбираемый URL — ошибка с именем ключа")
  void unreadableUrlNamesTheKey() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> StandService.mysqlAddress("mysql://localhost/rangiffler-auth"));

    assertTrue(exception.getMessage().contains("rfr.auth.jdbc.url"),
        "The error does not name the key: " + exception.getMessage());
  }
}
