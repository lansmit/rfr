package io.student.rangiffler.network;

import com.microsoft.playwright.Response;
import java.util.function.Predicate;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class AppResponses {

  private AppResponses() {
  }

  public static Predicate<Response> profileSaved() {
    return GraphQl.operation("UpdateUser");
  }

  public static Predicate<Response> feedLoaded() {
    return GraphQl.operation("GetFeed");
  }

  public static Predicate<Response> friendsLoaded() {
    return GraphQl.operation("GetFriends");
  }

  public static Predicate<Response> allPeopleLoaded() {
    return GraphQl.operation("GetPeople");
  }

  public static Predicate<Response> outcomeInvitationsLoaded() {
    return GraphQl.operation("GetOutcomeInvitations");
  }

  public static Predicate<Response> incomeInvitationsLoaded() {
    return GraphQl.operation("GetInvitations");
  }

  public static void checkPeopleListEmpty(Response peopleList) {
    JsonNode data = GraphQl.data(peopleList);
    JsonNode edges = data == null ? null : data.findValue("edges");
    assertNotNull(edges, "no people list in the response: " + data);
    assertEquals(0, edges.size(), "people list is not empty: " + edges);
  }
}
