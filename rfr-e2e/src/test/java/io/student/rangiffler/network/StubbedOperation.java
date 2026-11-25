package io.student.rangiffler.network;

import com.microsoft.playwright.Route;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

public final class StubbedOperation implements GqlOperation {

  private final String operationName;
  private final Route.FulfillOptions response;

  private StubbedOperation(String operationName, Route.FulfillOptions response) {
    this.operationName = operationName;
    this.response = response;
  }

  static StubbedOperation withFile(String operationName, String responseResource) {
    return new StubbedOperation(operationName, json().setPath(resource(operationName, responseResource)));
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

  private static Path resource(String operationName, String responseResource) {
    URL url = StubbedOperation.class.getClassLoader().getResource(responseResource);
    if (url == null) {
      throw new IllegalArgumentException(
          "Response " + responseResource + " for GraphQL operation " + operationName + " is not on the test classpath");
    }
    try {
      return Path.of(url.toURI());
    } catch (URISyntaxException exception) {
      throw new IllegalArgumentException(exception);
    }
  }

  private static Route.FulfillOptions json() {
    return new Route.FulfillOptions()
        .setStatus(200)
        .setContentType("application/json");
  }
}
