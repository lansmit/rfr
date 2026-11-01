package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;

public final class LandingPage {

  public static final String URL = TestConfig.frontUrl();

  private final Page page;
  private final Locator loginButton;
  private final Locator registerButton;

  public LandingPage(Page page) {
    this.page = page;
    this.loginButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login").setExact(true));
    this.registerButton = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Register").setExact(true));
  }

  @Step("Open landing page")
  public static LandingPage open(Page page) {
    page.navigate(URL);
    return new LandingPage(page);
  }

  @Step("Go to registration")
  public RegisterPage clickRegister() {
    registerButton.click();
    return new RegisterPage(page);
  }

  @Step("Go to login")
  public LoginPage clickLogin() {
    loginButton.click();
    return new LoginPage(page);
  }
}
