package io.student.rangiffler.data.db;

import java.util.List;

import static io.student.rangiffler.data.db.Databases.API;
import static io.student.rangiffler.data.db.Jdbc.client;
import static io.student.rangiffler.data.db.Jdbc.inTransaction;
import static io.student.rangiffler.data.db.Jdbc.sql;

public record Profile(String username) {

  private static final String DEFAULT_COUNTRY_CODE = "ru";

  // children first: every table here references `user` (directly or via photo/like) without on delete cascade
  private static final List<String> DELETE_CHILDREN_SQL = List.of(
      "sql/api/delete_user_photo_likes.sql",
      "sql/api/delete_user_given_likes.sql",
      "sql/api/delete_user_likes.sql",
      "sql/api/delete_user_photos.sql",
      "sql/api/delete_user_statistic.sql"
  );

  public void create() {
    int inserted = client(API).sql(sql("sql/api/create_profile.sql"))
        .param("username", username)
        .param("firstname", null)
        .param("lastName", null)
        .param("countryCode", DEFAULT_COUNTRY_CODE)
        .update();
    if (inserted != 1) {
      throw new IllegalStateException("No country with code " + DEFAULT_COUNTRY_CODE);
    }
  }

  // users in rangiffler-api have their own ids, the only link to rangiffler-auth is the username
  public void delete() {
    inTransaction(API, () -> {
      for (String path : DELETE_CHILDREN_SQL) {
        client(API).sql(sql(path))
            .param("username", username)
            .update();
      }
      new Friendships(username).delete();
      client(API).sql(sql("sql/api/delete_user.sql"))
          .param("username", username)
          .update();
    });
  }

  public boolean exists() {
    return client(API).sql(sql("sql/api/exists_profile.sql"))
        .param("username", username)
        .query(Boolean.class)
        .single();
  }
}
