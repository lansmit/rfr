package io.student.rangiffler.network;

import com.microsoft.playwright.Request;
import tools.jackson.databind.JsonNode;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

record GqlRequest(String operationName, JsonNode variables) {

  private static final ObjectMapper JSON = new ObjectMapper();

  static Optional<GqlRequest> from(Request request) {
    String body = request.postData();
    if (!request.url().endsWith("/graphql") || body == null) {
      return Optional.empty();
    }
    JsonNode json;
    try {
      json = JSON.readTree(body);
    } catch (JacksonException notJson) {
      return Optional.empty();
    }
    JsonNode operationName = json.get("operationName");
    return Optional.of(new GqlRequest(
        operationName == null || operationName.isNull() ? null : operationName.stringValue(),
        json.path("variables")
    ));
  }
}
