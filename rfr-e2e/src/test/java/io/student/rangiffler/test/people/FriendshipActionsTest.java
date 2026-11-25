package io.student.rangiffler.test.people;

import com.microsoft.playwright.Page;
import io.student.rangiffler.domain.Notification;
import io.student.rangiffler.jupiter.annotation.Session;
import io.student.rangiffler.jupiter.annotation.UiTest;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.model.TestUser;
import io.student.rangiffler.page.PeoplePage;
import io.student.rangiffler.page.PeopleTab;
import io.student.rangiffler.session.UserSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

// The actor does one action and waits for its notification; the observer opens People in the other context only then,
// since People shows friendship changes only after a fresh load.
@UiTest
@DisplayName("People: friendship actions on two sessions")
class FriendshipActionsTest {

  @Test
  @DisplayName("Sent invitation shows up in the addressee's income invitations")
  void invitationShouldReachAddressee(Page page, @User TestUser alice, @Session @User UserSession bob) {
    String bobName = bob.user().username();
    PeoplePage people = PeopleLogin.openAs(page, alice)
        .openTab(PeopleTab.ALL_PEOPLE)
        .searchPeople(bobName);
    people.table().row(bobName).clickAdd();
    people.checkNotification(Notification.INVITATION_SENT);

    bob.myTravelsPage()
        .clickPeople()
        .openTab(PeopleTab.INCOME_INVITATIONS)
        .table()
        .checkUsernames(List.of(alice.username()));
  }

  @Test
  @DisplayName("Accepted invitation makes the sender and the addressee friends")
  void acceptedInvitationShouldMakeFriends(Page page, @Session @User(outcomeInvitations = 1) UserSession alice) {
    TestUser invitee = alice.user().outcomeInvitations().getFirst();
    PeoplePage people = PeopleLogin.openAs(page, invitee).openTab(PeopleTab.INCOME_INVITATIONS);
    people.table().row(alice.user().username()).clickAccept();
    people.checkNotification(Notification.INVITATION_ACCEPTED);

    alice.myTravelsPage()
        .clickPeople()
        .openTab(PeopleTab.FRIENDS)
        .table()
        .checkUsernames(List.of(invitee.username()));
  }

  @Test
  @DisplayName("Declined invitation disappears from the sender's outcome invitations")
  void declinedInvitationShouldLeaveSender(Page page, @Session @User(outcomeInvitations = 1) UserSession alice) {
    PeoplePage people = PeopleLogin.openAs(page, alice.user().outcomeInvitations().getFirst())
        .openTab(PeopleTab.INCOME_INVITATIONS);
    people.table().row(alice.user().username()).clickDecline();
    people.checkNotification(Notification.INVITATION_DECLINED);

    alice.myTravelsPage()
        .clickPeople()
        .openTab(PeopleTab.OUTCOME_INVITATIONS)
        .checkNoPeople();
  }

  @Test
  @DisplayName("Cancelled invitation disappears from the addressee's income invitations")
  void cancelledInvitationShouldLeaveAddressee(Page page, @Session @User(outcomeInvitations = 1) UserSession alice) {
    TestUser invitee = alice.user().outcomeInvitations().getFirst();

    PeoplePage people = alice.myTravelsPage().clickPeople().openTab(PeopleTab.OUTCOME_INVITATIONS);

    people.table().row(invitee.username()).clickCancel();
    people.checkNotification(Notification.INVITATION_CANCELLED);

    PeopleLogin.openAs(page, invitee)
        .openTab(PeopleTab.INCOME_INVITATIONS)
        .checkNoPeople();
  }

  @Test
  @DisplayName("Friendship removed by the requester disappears for the addressee")
  void friendRemovedByRequesterShouldDisappearForAddressee(Page page, @Session @User(friends = 1) UserSession alice) {
    TestUser friend = alice.user().friends().getFirst();
    PeoplePage people = alice.myTravelsPage().clickPeople().openTab(PeopleTab.FRIENDS);
    people.table().row(friend.username()).clickRemove();
    people.checkNotification(Notification.FRIEND_DELETED);

    PeopleLogin.openAs(page, friend)
        .openTab(PeopleTab.FRIENDS)
        .checkNoPeople();
  }

  @Test
  @DisplayName("Friendship removed by the addressee disappears for the requester")
  void friendRemovedByAddresseeShouldDisappearForRequester(Page page, @Session @User(friends = 1) UserSession alice) {
    PeoplePage people = PeopleLogin.openAs(page, alice.user().friends().getFirst()).openTab(PeopleTab.FRIENDS);
    people.table().row(alice.user().username()).clickRemove();
    people.checkNotification(Notification.FRIEND_DELETED);

    alice.myTravelsPage()
        .clickPeople()
        .openTab(PeopleTab.FRIENDS)
        .checkNoPeople();
  }
}
