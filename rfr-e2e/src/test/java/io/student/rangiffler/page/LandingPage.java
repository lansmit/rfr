package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.Config;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class LandingPage extends BasePage<LandingPage> {

  private final Locator loginButton;
  private final Locator registerButton;

  public LandingPage(Page page) {
    super(page);
    this.loginButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login").setExact(true));
    this.registerButton = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Register").setExact(true));
  }

  @Step("Open landing page")
  public static LandingPage open(Page page) {
    page.navigate(Config.frontUrl());
    return new LandingPage(page).checkThatPageLoaded();
  }

  @Override
  @Step("Check that landing page is loaded")
  public LandingPage checkThatPageLoaded() {
    assertThat(loginButton).isVisible();
    assertThat(registerButton).isVisible();
    return this;
  }

  @Step("Go to registration")
  public RegisterPage clickRegister() {
    registerButton.click();
    return new RegisterPage(page).checkThatPageLoaded();
  }

  @Step("Go to login")
  public LoginPage clickLogin() {
    loginButton.click();
    return new LoginPage(page).checkThatPageLoaded();
  }
}
