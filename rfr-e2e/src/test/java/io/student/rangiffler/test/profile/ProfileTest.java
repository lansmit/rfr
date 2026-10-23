package io.student.rangiffler.test.profile;

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
  private static final String FIRSTNAME = "Ivan";
  private static final String SURNAME = "Petrov";
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

  // each field is saved alone to show it doesn't depend on the others being filled
  @Test
  @DisplayName("First name alone is saved")
  void firstnameShouldBeSaved(Page page, @User TestUser user) {
    login(page, user)
        .fillProfileForm(FIRSTNAME, "")
        .saveProfile()
        .checkProfileSaved()
        .reload()
        .checkProfileForm(FIRSTNAME, "");
  }

  @Test
  @DisplayName("Surname alone is saved")
  void surnameShouldBeSaved(Page page, @User TestUser user) {
    login(page, user)
        .fillProfileForm("", SURNAME)
        .saveProfile()
        .checkProfileSaved()
        .reload()
        .checkProfileForm("", SURNAME);
  }

  @Test
  @DisplayName("Uploaded avatar is shown before saving and kept after saving")
  void avatarShouldBeSaved(Page page, @User TestUser user) {
    login(page, user)
        .uploadAvatar(AVATAR)
        .checkAvatar(AVATAR)
        .saveProfile()
        .checkProfileSaved()
        .reload()
        .checkAvatar(AVATAR);
  }

  @Test
  @DisplayName("Location changed from the default to Canada is saved")
  void locationShouldBeChanged(Page page, @User TestUser user) {
    login(page, user)
        .checkLocation(DEFAULT_LOCATION)
        .selectLocation(LOCATION)
        .checkLocation(LOCATION)
        .saveProfile()
        .checkProfileSaved()
        .reload()
        .checkLocation(LOCATION);
  }

  @Test
  @DisplayName("All fields saved together are read back after reload")
  void allFieldsShouldBeReadAfterReload(Page page, @User TestUser user) {
    login(page, user)
        .fillProfileForm(FIRSTNAME, SURNAME)
        .selectLocation(LOCATION)
        .uploadAvatar(AVATAR)
        .saveProfile()
        .checkProfileSaved()
        .reload()
        .checkUsername(user.username())
        .checkProfileForm(FIRSTNAME, SURNAME)
        .checkLocation(LOCATION)
        .checkAvatar(AVATAR);
  }

  // Reset rebuilds the form from the session user, so it must see the values of the last save, not of page load
  @Test
  @DisplayName("Reset returns all fields to the last saved values")
  void resetShouldReturnSavedValues(Page page, @User TestUser user) {
    login(page, user)
        .fillProfileForm(FIRSTNAME, SURNAME)
        .selectLocation(LOCATION)
        .uploadAvatar(AVATAR)
        .saveProfile()
        .checkProfileSaved()
        .fillProfileForm("Pyotr", "Ivanov")
        .selectLocation("France")
        .uploadAvatar(classpathFile("img/avatar-new.png"))
        .resetProfileForm()
        .checkProfileForm(FIRSTNAME, SURNAME)
        .checkLocation(LOCATION)
        .checkAvatar(AVATAR);
  }

  @Test
  @DisplayName("Reset on a new profile discards unsaved changes")
  void resetShouldDiscardUnsavedChanges(Page page, @User TestUser user) {
    login(page, user)
        .fillProfileForm(FIRSTNAME, SURNAME)
        .selectLocation(LOCATION)
        .uploadAvatar(AVATAR)
        .resetProfileForm()
        .checkProfileForm("", "")
        .checkLocation(DEFAULT_LOCATION)
        .checkNoAvatar();
  }

  @Test
  @DisplayName("First name longer than 50 characters is rejected")
  void tooLongFirstnameShouldNotBeSaved(Page page, @User TestUser user) {
    login(page, user)
        .fillProfileForm("a".repeat(51), SURNAME)
        .saveInvalidProfile()
        .checkFirstnameError("First name length has to be not longer that 50 symbols")
        .reload()
        .checkProfileForm("", "");
  }

  @Test
  @DisplayName("Surname longer than 100 characters is rejected")
  void tooLongSurnameShouldNotBeSaved(Page page, @User TestUser user) {
    login(page, user)
        .fillProfileForm(FIRSTNAME, "a".repeat(101))
        .saveInvalidProfile()
        .checkSurnameError("Surname length has to be not longer that 100 symbols")
        .reload()
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
