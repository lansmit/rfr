package io.student.rangiffler.network;

import com.microsoft.playwright.Response;
import java.util.function.Predicate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertFalse;

final class GraphQl {

  private static final ObjectMapper JSON = new ObjectMapper();

  private GraphQl() {
  }

  static Predicate<Response> operation(String operationName) {
    return response -> {
      String body = response.request().postData();
      return response.url().endsWith("/graphql") && body != null && body.contains("\"operationName\":\"" + operationName + "\"");
    };
  }

  // GraphQL reports failures with HTTP 200, so only the body tells an error from an empty result
  static JsonNode data(Response response) {
    JsonNode body = JSON.readTree(response.text());
    assertFalse(body.has("errors"), "GraphQL returned errors: " + body.get("errors"));
    return body.get("data");
  }
}
