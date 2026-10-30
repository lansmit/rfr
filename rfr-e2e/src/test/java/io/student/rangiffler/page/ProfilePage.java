package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.page.component.Header;

import java.nio.file.Path;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.student.rangiffler.utils.ResourceUtils.pngDataUrl;

public final class ProfilePage {

  public static final String URL = TestConfig.frontUrl() + "/profile";

  private final Page page;
  private final Header header;
  private final Locator avatarInput;
  private final Locator avatarImage;
  private final Locator firstnameInput;
  private final Locator firstnameError;
  private final Locator surnameInput;
  private final Locator surnameError;
  private final Locator usernameInput;
  private final Locator locationSelect;
  private final Locator resetButton;
  private final Locator saveButton;
  private final Locator snackbar;

  public ProfilePage(Page page) {
    this.page = page;
    this.header = new Header(page);
    Locator main = page.getByRole(AriaRole.MAIN);
    // the photo dialog stays in the DOM outside main and has a file input with the same id
    this.avatarInput = main.locator("#image__input");
    // the avatar img has no alt; the only other img in main is the flag inside the Location select, which has one
    this.avatarImage = main.locator("img:not([alt])");
    this.firstnameInput = page.getByLabel("First Name", new Page.GetByLabelOptions().setExact(true));
    this.firstnameError = page.locator("#firstname-helper-text");
    this.surnameInput = page.getByLabel("Surname", new Page.GetByLabelOptions().setExact(true));
    this.surnameError = page.locator("#surname-helper-text");
    this.usernameInput = page.getByLabel("Username", new Page.GetByLabelOptions().setExact(true));
    this.locationSelect = page.getByRole(AriaRole.COMBOBOX, new Page.GetByRoleOptions().setName("Location").setExact(true));
    this.resetButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Reset").setExact(true));
    this.saveButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save").setExact(true));
    this.snackbar = page.getByRole(AriaRole.ALERT);
  }

  public Header header() {
    return header;
  }

  @Step("Open profile page")
  public static ProfilePage open(Page page) {
    page.navigate(URL);
    return new ProfilePage(page);
  }

  @Step("Reload profile page")
  public ProfilePage reload() {
    page.reload();
    return this;
  }

  @Step("Fill profile form: firstname={firstname}, surname={surname}")
  public ProfilePage fillProfileForm(String firstname, String surname) {
    firstnameInput.fill(firstname);
    surnameInput.fill(surname);
    return this;
  }

  @Step("Select location {country}")
  public ProfilePage selectLocation(String country) {
    locationSelect.click();
    // options have no accessible name of their own, only the flag img alt identifies a country exactly
    page.getByRole(AriaRole.OPTION)
        .filter(new Locator.FilterOptions().setHas(
            page.getByRole(AriaRole.IMG, new Page.GetByRoleOptions().setName(country).setExact(true))))
        .click();
    return this;
  }

  @Step("Upload avatar {avatar}")
  public ProfilePage uploadAvatar(Path avatar) {
    avatarInput.setInputFiles(avatar);
    return this;
  }

  @Step("Reset profile form")
  public ProfilePage resetProfileForm() {
    resetButton.click();
    return this;
  }

  // without waiting, a navigation right after Save may abort the mutation still in flight
  @Step("Save profile")
  public ProfilePage saveProfile() {
    page.waitForResponse(ProfilePage::isUpdateUserResponse, saveButton::click);
    return this;
  }

  // client-side validation blocks the request, so there is no response to wait for
  @Step("Try to save profile that fails validation")
  public ProfilePage saveInvalidProfile() {
    saveButton.click();
    return this;
  }

  // username is resolved by the API from the JWT, so a match proves the session belongs to this user
  @Step("Check that profile username is {username}")
  public ProfilePage checkUsername(String username) {
    assertThat(usernameInput).hasValue(username);
    return this;
  }

  @Step("Check profile form: firstname={firstname}, surname={surname}")
  public ProfilePage checkProfileForm(String firstname, String surname) {
    assertThat(firstnameInput).hasValue(firstname);
    assertThat(surnameInput).hasValue(surname);
    return this;
  }

  @Step("Check that location is {country}")
  public ProfilePage checkLocation(String country) {
    assertThat(locationSelect).hasText(country);
    return this;
  }

  // the frontend sends the avatar as a data URL and the API returns it unchanged
  @Step("Check that avatar is {avatar}")
  public ProfilePage checkAvatar(Path avatar) {
    assertThat(avatarImage).hasAttribute("src", pngDataUrl(avatar));
    return this;
  }

  @Step("Check that avatar is not set")
  public ProfilePage checkNoAvatar() {
    assertThat(avatarImage).hasCount(0);
    return this;
  }

  @Step("Check that profile is saved")
  public ProfilePage checkProfileSaved() {
    assertThat(snackbar).hasText("Your profile is successfully updated");
    return this;
  }

  @Step("Check that first name error is {message}")
  public ProfilePage checkFirstnameError(String message) {
    assertThat(firstnameError).hasText(message);
    return this;
  }

  @Step("Check that surname error is {message}")
  public ProfilePage checkSurnameError(String message) {
    assertThat(surnameError).hasText(message);
    return this;
  }

  private static boolean isUpdateUserResponse(Response response) {
    String body = response.request().postData();
    return response.url().endsWith("/graphql") && body != null && body.contains("\"operationName\":\"UpdateUser\"");
  }
}
