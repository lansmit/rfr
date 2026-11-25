package io.student.rangiffler.test.people;

import com.microsoft.playwright.Page;
import io.student.rangiffler.domain.FriendStatus;
import io.student.rangiffler.jupiter.annotation.Session;
import io.student.rangiffler.jupiter.annotation.UiTest;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.model.TestUser;
import io.student.rangiffler.network.GqlErrorType;
import io.student.rangiffler.network.GqlStubs;
import io.student.rangiffler.page.component.PeopleTable;
import io.student.rangiffler.session.UserSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.student.rangiffler.utils.RandomDataUtils.randomErrorMessage;

@UiTest
@DisplayName("People: GraphQL stubs of two contexts are isolated")
class GqlStubsIsolationTest {

  @Test
  @DisplayName("Session stub of GetFriends does not reach the main context")
  void sessionStubShouldNotReachMainContext(Page page,
                                            @User(friends = 1) TestUser alice,
                                            @Session(useStubs = true) @User(friends = 1) UserSession bob) {
    String message = randomErrorMessage();
    bob.stubs().orElseThrow().error("GetFriends", GqlErrorType.BAD_REQUEST, message);

    bob.myTravelsPage()
        .clickPeople()
        .table()
        .checkLoadError(message);
    checkRealFriend(PeopleLogin.openAs(page, alice).table(), alice);
  }

  @Test
  @DisplayName("Main context stub of GetFriends does not reach the session")
  void mainStubShouldNotReachSession(Page page,
                                     GqlStubs stubs,
                                     @User(friends = 1) TestUser alice,
                                     @Session(useStubs = true) @User(friends = 1) UserSession bob) {
    String message = randomErrorMessage();
    stubs.error("GetFriends", GqlErrorType.BAD_REQUEST, message);

    PeopleLogin.openAs(page, alice)
        .table()
        .checkLoadError(message);
    checkRealFriend(bob.myTravelsPage().clickPeople().table(), bob.user());
  }

  private static void checkRealFriend(PeopleTable table, TestUser user) {
    String friend = user.friends().getFirst().username();
    table.checkUsernames(List.of(friend))
        .row(friend)
        .checkStatus(FriendStatus.FRIEND);
  }
}
