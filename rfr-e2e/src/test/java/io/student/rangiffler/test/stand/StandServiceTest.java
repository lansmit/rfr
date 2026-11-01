package io.student.rangiffler.test.stand;

import io.student.rangiffler.stand.StandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("StandService: адрес MySQL из JDBC URL")
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
  @DisplayName("Неразбираемый URL — ошибка с именем ключа")
  void unreadableUrlNamesTheKey() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> StandService.mysqlAddress("mysql://localhost/rangiffler-auth"));

    assertTrue(exception.getMessage().contains("rfr.auth.jdbc.url"),
        "The error does not name the key: " + exception.getMessage());
  }
}
