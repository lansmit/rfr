package io.student.rangiffler.data.db;

import static io.student.rangiffler.data.db.Databases.API;
import static io.student.rangiffler.data.db.Jdbc.client;
import static io.student.rangiffler.data.db.Jdbc.sql;

public record Friendship(String requester, String addressee, Status status) {

  public enum Status {
    PENDING, ACCEPTED
  }

  public static Friendship friends(String requester, String addressee) {
    return new Friendship(requester, addressee, Status.ACCEPTED);
  }

  public static Friendship invitation(String inviter, String invitee) {
    return new Friendship(inviter, invitee, Status.PENDING);
  }

  public void create() {
    int inserted = client(API).sql(sql("sql/api/create_friendship.sql"))
        .param("requester", requester)
        .param("addressee", addressee)
        .param("status", status.name())
        .update();
    if (inserted != 1) {
      throw new IllegalStateException("No API profile for " + requester + " or " + addressee);
    }
  }

  public void delete() {
    client(API).sql(sql("sql/api/delete_friendship.sql"))
        .param("requester", requester)
        .param("addressee", addressee)
        .update();
  }

  public boolean exists() {
    return client(API).sql(sql("sql/api/exists_friendship.sql"))
        .param("requester", requester)
        .param("addressee", addressee)
        .param("status", status.name())
        .query(Boolean.class)
        .single();
  }
}
