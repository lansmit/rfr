package io.student.rangiffler.network;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Route;
import io.qameta.allure.Step;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public final class GqlStubs implements AutoCloseable {

  private static final String GRAPHQL_URL = "**/graphql";
  private static final ObjectMapper JSON = new ObjectMapper();

  private final BrowserContext context;
  private final List<GqlOperation> operations = new ArrayList<>();
  private final Consumer<Route> handler = this::handle;
  private boolean routed;

  public GqlStubs(BrowserContext context) {
    this.context = context;
  }

  public GqlStubs stub(GqlOperation operation) {
    if (!routed) {
      context.route(GRAPHQL_URL, handler);
      routed = true;
    }
    operations.add(operation);
    return this;
  }

  @Step("Hold GraphQL operation {operationName}")
  public HeldOperation hold(String operationName) {
    HeldOperation operation = new HeldOperation(operationName);
    stub(operation);
    return operation;
  }

  @Step("Respond to GraphQL operation {operationName} with {responseResource}")
  public StubbedOperation respond(String operationName, String responseResource) {
    StubbedOperation operation = StubbedOperation.withFile(operationName, responseResource);
    stub(operation);
    return operation;
  }

  @Step("Respond to GraphQL operation {operationName} with error {type}: {message}")
  public StubbedOperation error(String operationName, GqlErrorType type, String message) {
    StubbedOperation operation = StubbedOperation.withBody(operationName, errorBody(type, message));
    stub(operation);
    return operation;
  }

  @Override
  public void close() {
    operations.clear();
    if (routed) {
      context.unroute(GRAPHQL_URL, handler);
    }
    routed = false;
  }

  private void handle(Route route) {
    Optional<GqlOperation> operation = GqlRequest.from(route.request())
        .flatMap(request -> stubFor(request.operationName()));
    if (operation.isPresent()) {
      operation.get().handle(route);
    } else {
      route.fallback();
    }
  }

  private static String errorBody(GqlErrorType type, String message) {
    Map<String, Object> error = Map.of(
        "message", message,
        "extensions", Map.of("classification", type.name())
    );
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("errors", List.of(error));
    body.put("data", null);
    return JSON.writeValueAsString(body);
  }

  private Optional<GqlOperation> stubFor(String operationName) {
    for (int index = operations.size() - 1; index >= 0; index--) {
      GqlOperation operation = operations.get(index);
      if (operation.operationName().equals(operationName)) {
        return Optional.of(operation);
      }
    }
    return Optional.empty();
  }
}
