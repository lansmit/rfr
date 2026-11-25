package io.student.rangiffler.network;

import com.microsoft.playwright.Route;
import io.student.rangiffler.utils.ResourceUtils;

public final class StubbedOperation implements GqlOperation {

  private final String operationName;
  private final Route.FulfillOptions response;

  private StubbedOperation(String operationName, Route.FulfillOptions response) {
    this.operationName = operationName;
    this.response = response;
  }

  static StubbedOperation withFile(String operationName, String responseResource) {
    return new StubbedOperation(operationName, json().setPath(ResourceUtils.classpathFile(responseResource)));
  }

  static StubbedOperation withBody(String operationName, String responseBody) {
    return new StubbedOperation(operationName, json().setBody(responseBody));
  }

  @Override
  public String operationName() {
    return operationName;
  }

  @Override
  public void handle(Route route) {
    route.fulfill(response);
  }

  private static Route.FulfillOptions json() {
    return new Route.FulfillOptions()
        .setStatus(200)
        .setContentType("application/json");
  }
}
