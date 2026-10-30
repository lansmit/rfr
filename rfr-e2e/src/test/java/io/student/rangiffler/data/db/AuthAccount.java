package io.student.rangiffler.data.db;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;

import static io.student.rangiffler.data.db.Databases.AUTH;
import static io.student.rangiffler.data.db.Jdbc.client;
import static io.student.rangiffler.data.db.Jdbc.inTransaction;
import static io.student.rangiffler.data.db.Jdbc.sql;

public record AuthAccount(UUID id, String username, String password) {

  private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();
  private static final List<String> AUTHORITIES = List.of("read", "write");

  public static AuthAccount of(String username, String password) {
    return new AuthAccount(UUID.randomUUID(), username, password);
  }

  public void create() {
    inTransaction(AUTH, () -> {
      client(AUTH).sql(sql("sql/auth/insert_user.sql"))
          .param("id", toBytes(id))
          .param("username", username)
          .param("password", encode(password))
          .update();
      for (String authority : AUTHORITIES) {
        client(AUTH).sql(sql("sql/auth/insert_authority.sql"))
            .param("id", toBytes(UUID.randomUUID()))
            .param("userId", toBytes(id))
            .param("authority", authority)
            .update();
      }
    });
  }

  public void delete() {
    inTransaction(AUTH, () -> {
      client(AUTH).sql(sql("sql/auth/delete_authorities.sql"))
          .param("userId", toBytes(id))
          .update();
      client(AUTH).sql(sql("sql/auth/delete_user.sql"))
          .param("id", toBytes(id))
          .update();
    });
  }

  // for accounts created by rfr-auth itself (UI registration): their id is unknown, the username is
  public void deleteByUsername() {
    inTransaction(AUTH, () -> {
      client(AUTH).sql(sql("sql/auth/delete_authorities_by_username.sql"))
          .param("username", username)
          .update();
      client(AUTH).sql(sql("sql/auth/delete_user_by_username.sql"))
          .param("username", username)
          .update();
    });
  }

  public boolean exists() {
    return client(AUTH).sql(sql("sql/auth/exists_user.sql"))
        .param("username", username)
        .query(Boolean.class)
        .single();
  }

  // rfr-auth uses DelegatingPasswordEncoder, which picks the algorithm by the {id} prefix of the stored hash
  private static String encode(String password) {
    return "{bcrypt}" + BCRYPT.encode(password);
  }

  // same byte order as Hibernate writes UUID into binary(16) in rfr-auth, so ids stay readable by both sides
  private static byte[] toBytes(UUID uuid) {
    return ByteBuffer.allocate(16)
        .putLong(uuid.getMostSignificantBits())
        .putLong(uuid.getLeastSignificantBits())
        .array();
  }
}
