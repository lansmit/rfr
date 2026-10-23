package io.student.rangiffler.test.web;

import com.microsoft.playwright.Page;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.jupiter.annotation.WebTest;
import io.student.rangiffler.model.TestUser;
import io.student.rangiffler.page.LandingPage;
import io.student.rangiffler.page.ProfilePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static io.student.rangiffler.utils.ResourceUtils.classpathFile;

@WebTest
@DisplayName("Profile")
class ProfileTest {

  // the API creates a profile on first login with the "ru" country and no names or avatar
  private static final String DEFAULT_LOCATION = "Russian Federation";
  private static final String LOCATION = "Canada";
  private static final Path AVATAR = classpathFile("img/avatar.png");

  @Test
  @DisplayName("New user has an empty profile with the default location")
  void newUserShouldHaveEmptyProfile(Page page, @User TestUser user) {
    login(page, user)
        .checkUsername(user.username())
        .checkProfileForm("", "")
        .checkLocation(DEFAULT_LOCATION)
        .checkNoAvatar();
  }

  @Test
  @DisplayName("First name and surname are saved")
  void firstnameAndSurnameShouldBeSaved(Page page, @User TestUser user) {
    login(page, user)
        .fillProfileForm("Ivan", "Petrov")
        .saveProfile()
        .checkProfileSaved();

    ProfilePage.open(page)
        .checkProfileForm("Ivan", "Petrov");
  }

  @Test
  @DisplayName("Location is saved")
  void locationShouldBeSaved(Page page, @User TestUser user) {
    login(page, user)
        .selectLocation(LOCATION)
        .checkLocation(LOCATION)
        .saveProfile()
        .checkProfileSaved();

    ProfilePage.open(page)
        .checkLocation(LOCATION);
  }

  @Test
  @DisplayName("Uploaded avatar is shown before saving and kept after saving")
  void avatarShouldBeSaved(Page page, @User TestUser user) {
    login(page, user)
        .uploadAvatar(AVATAR)
        .checkAvatar(AVATAR)
        .saveProfile()
        .checkProfileSaved();

    ProfilePage.open(page)
        .checkAvatar(AVATAR);
  }

  @Test
  @DisplayName("Reset discards unsaved changes of all fields")
  void resetShouldDiscardUnsavedChanges(Page page, @User TestUser user) {
    login(page, user)
        .fillProfileForm("Ivan", "Petrov")
        .selectLocation(LOCATION)
        .uploadAvatar(AVATAR)
        .resetProfileForm()
        .checkProfileForm("", "")
        .checkLocation(DEFAULT_LOCATION)
        .checkNoAvatar();
  }

  // Reset rebuilds the form from the session user, so it must see the values of the last save
  @Test
  @DisplayName("Reset after saving returns the saved values, not the ones from page load")
  void resetShouldReturnLastSavedValues(Page page, @User TestUser user) {
    login(page, user)
        .fillProfileForm("Ivan", "Petrov")
        .selectLocation(LOCATION)
        .saveProfile()
        .checkProfileSaved()
        .fillProfileForm("Pyotr", "Ivanov")
        .selectLocation(DEFAULT_LOCATION)
        .resetProfileForm()
        .checkProfileForm("Ivan", "Petrov")
        .checkLocation(LOCATION);
  }

  @Test
  @DisplayName("First name longer than 50 characters is rejected")
  void tooLongFirstnameShouldNotBeSaved(Page page, @User TestUser user) {
    login(page, user)
        .fillProfileForm("a".repeat(51), "Petrov")
        .saveInvalidProfile()
        .checkFirstnameError("First name length has to be not longer that 50 symbols");

    ProfilePage.open(page)
        .checkProfileForm("", "");
  }

  @Test
  @DisplayName("Surname longer than 100 characters is rejected")
  void tooLongSurnameShouldNotBeSaved(Page page, @User TestUser user) {
    login(page, user)
        .fillProfileForm("Ivan", "a".repeat(101))
        .saveInvalidProfile()
        .checkSurnameError("Surname length has to be not longer that 100 symbols");

    ProfilePage.open(page)
        .checkProfileForm("", "");
  }

  private static ProfilePage login(Page page, TestUser user) {
    return LandingPage.open(page)
        .clickLogin()
        .login(user.username(), user.password())
        .header()
        .openProfile();
  }
}
