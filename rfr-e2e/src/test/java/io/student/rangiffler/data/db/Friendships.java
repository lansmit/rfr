package io.student.rangiffler.data.db;

import static io.student.rangiffler.data.db.Databases.API;
import static io.student.rangiffler.data.db.Jdbc.client;
import static io.student.rangiffler.data.db.Jdbc.sql;

public record Friendships(String username) {

  public void delete() {
    client(API).sql(sql("sql/api/delete_user_friendships.sql"))
        .param("username", username)
        .update();
  }

  public boolean exists() {
    return client(API).sql(sql("sql/api/exists_user_friendships.sql"))
        .param("username", username)
        .query(Boolean.class)
        .single();
  }
}
