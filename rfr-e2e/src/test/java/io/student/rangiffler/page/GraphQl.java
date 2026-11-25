package io.student.rangiffler.page;

import com.microsoft.playwright.Response;

public final class GraphQl {

  private GraphQl() {
  }

  public static boolean isOperationResponse(Response response, String operationName) {
    String body = response.request().postData();
    return response.url().endsWith("/graphql") && body != null && body.contains("\"operationName\":\"" + operationName + "\"");
  }
}
