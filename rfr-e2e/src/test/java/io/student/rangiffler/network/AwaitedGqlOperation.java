package io.student.rangiffler.network;

import com.microsoft.playwright.Page;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;

public final class AwaitedGqlOperation {

  private final Page page;
  private final String operationName;
  private final Runnable action;

  public AwaitedGqlOperation(Page page, String operationName, Runnable action) {
    this.page = page;
    this.operationName = operationName;
    this.action = action;
  }

  @Step("Wait for GraphQL operation {this.operationName}")
  public void await() {
    page.waitForResponse(GraphQl.operation(operationName),
        new Page.WaitForResponseOptions().setTimeout(TestConfig.navigationTimeout().toMillis()),
        action);
  }
}
