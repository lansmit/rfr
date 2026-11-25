package io.student.rangiffler.network;

import com.microsoft.playwright.Route;
import io.qameta.allure.Step;

import java.util.ArrayList;
import java.util.List;

public final class HeldOperation implements GqlOperation {

  private final String operationName;
  private final List<Route> heldRoutes = new ArrayList<>();
  private boolean released;

  HeldOperation(String operationName) {
    this.operationName = operationName;
  }

  @Override
  public String operationName() {
    return operationName;
  }

  @Override
  public void handle(Route route) {
    if (released) {
      route.fallback();
    } else {
      heldRoutes.add(route);
    }
  }

  @Step("Release held GraphQL operation {this.operationName}")
  public void release() {
    released = true;
    for (Route route : heldRoutes) {
      route.resume();
    }
    heldRoutes.clear();
  }
}
