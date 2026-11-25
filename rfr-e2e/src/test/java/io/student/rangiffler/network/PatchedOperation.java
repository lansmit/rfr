package io.student.rangiffler.network;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Route;

import java.util.List;

public final class PatchedOperation implements GqlOperation {

  private static final Configuration PATH_LIST = Configuration.builder()
      .options(Option.AS_PATH_LIST, Option.SUPPRESS_EXCEPTIONS)
      .build();

  private final String operationName;
  private final String pathText;
  private final JsonPath path;
  private final Object value;

  PatchedOperation(String operationName, String path, Object value) {
    this.operationName = operationName;
    this.pathText = path;
    this.path = JsonPath.compile(path);
    this.value = value;
  }

  @Override
  public String operationName() {
    return operationName;
  }

  @Override
  public void handle(Route route) {
    try {
      APIResponse response = route.fetch();
      DocumentContext document = JsonPath.parse(response.text());
      List<String> matches = JsonPath.using(PATH_LIST).parse((Object) document.json()).read(path);
      if (matches == null || matches.isEmpty()) {
        throw new IllegalStateException("JSON Path matched nothing in the response");
      }
      String patched = document.set(path, value).jsonString();
      route.fulfill(new Route.FulfillOptions().setResponse(response).setBody(patched));
      response.dispose();
    } catch (RuntimeException failure) {
      IllegalStateException patchFailure = new IllegalStateException(
          "Patch of GraphQL operation " + operationName + " by JSON Path " + pathText + " failed: " + failure.getMessage(),
          failure);
      try {
        route.abort();
      } catch (RuntimeException abortFailure) {
        patchFailure.addSuppressed(abortFailure);
      }
      throw patchFailure;
    }
  }
}
