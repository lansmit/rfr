package io.student.rangiffler.page;

import com.microsoft.playwright.Page;

public abstract class BasePage<T extends BasePage<T>> {

  protected final Page page;

  protected BasePage(Page page) {
    this.page = page;
  }

  public abstract T checkThatPageLoaded();
}
