package io.student.rangiffler.test.people;

import com.microsoft.playwright.Page;
import io.student.rangiffler.domain.FriendStatus;
import io.student.rangiffler.domain.Notification;
import io.student.rangiffler.jupiter.annotation.UiTest;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.model.TestUser;
import io.student.rangiffler.network.GqlErrorType;
import io.student.rangiffler.network.GqlStubs;
import io.student.rangiffler.page.PeoplePage;
import io.student.rangiffler.page.PeopleTab;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.student.rangiffler.utils.RandomDataUtils.randomErrorMessage;

@UiTest
@DisplayName("People: backend error on a friendship action")
class FriendshipErrorTest {

  @Test
  @DisplayName("BAD_REQUEST on Add shows exactly the server message and the user stays not a friend")
  void badRequestOnAddShouldShowServerMessage(Page page, GqlStubs stubs, @User TestUser alice, @User TestUser bob) {
    String message = randomErrorMessage();
    stubs.error("FriendshipAction", GqlErrorType.BAD_REQUEST, message);

    PeoplePage people = openAllPeopleWith(page, alice, bob);
    people.table().row(bob.username()).clickAdd();
    people.checkServerMessage(message)
        .table()
        .row(bob.username())
        .checkStatus(FriendStatus.NOT_FRIEND);
  }

  @Test
  @DisplayName("INTERNAL_ERROR on Add shows the frontend's generic text, not the server message, and the user stays not a friend")
  void internalErrorOnAddShouldShowGenericText(Page page, GqlStubs stubs, @User TestUser alice, @User TestUser bob) {
    stubs.error("FriendshipAction", GqlErrorType.INTERNAL_ERROR, randomErrorMessage());

    PeoplePage people = openAllPeopleWith(page, alice, bob);
    people.table().row(bob.username()).clickAdd();
    people.checkNotification(Notification.INVITATION_NOT_SENT)
        .table()
        .row(bob.username())
        .checkStatus(FriendStatus.NOT_FRIEND);
  }

  private static PeoplePage openAllPeopleWith(Page page, TestUser user, TestUser other) {
    return PeopleLogin.openAs(page, user)
        .openTab(PeopleTab.ALL_PEOPLE)
        .searchPeople(other.username());
  }
}
