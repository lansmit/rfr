package io.student.rangiffler.network;

import com.microsoft.playwright.Route;

public interface GqlOperation {

  String operationName();

  void handle(Route route);
}
