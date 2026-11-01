package io.student.rangiffler.data;

import java.util.List;

import static io.student.rangiffler.data.db.Databases.API;
import static io.student.rangiffler.data.db.Jdbc.client;
import static io.student.rangiffler.data.db.Jdbc.inTransaction;
import static io.student.rangiffler.data.db.Jdbc.sql;

public final class ApiUserRepository {

  // children first: every table here references `user` (directly or via photo/like) without on delete cascade
  private static final List<String> DELETE_USER_SQL = List.of(
      "sql/api/delete_user_photo_likes.sql",
      "sql/api/delete_user_given_likes.sql",
      "sql/api/delete_user_likes.sql",
      "sql/api/delete_user_photos.sql",
      "sql/api/delete_user_statistic.sql",
      "sql/api/delete_user_friendships.sql",
      "sql/api/delete_user.sql"
  );

  // users in rangiffler-api have their own ids, the only link to rangiffler-auth is the username
  public void deleteByUsername(String username) {
    inTransaction(API, () -> {
      for (String path : DELETE_USER_SQL) {
        client(API).sql(sql(path))
            .param("username", username)
            .update();
      }
    });
  }
}
