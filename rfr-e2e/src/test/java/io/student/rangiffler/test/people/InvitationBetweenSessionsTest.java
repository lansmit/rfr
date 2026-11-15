package io.student.rangiffler.test.people;

import io.student.rangiffler.jupiter.annotation.Session;
import io.student.rangiffler.jupiter.annotation.UiTest;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.page.PeoplePage;
import io.student.rangiffler.page.PeopleTab;
import io.student.rangiffler.session.UserSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

@UiTest
@DisplayName("People: invitation between two browsers")
class InvitationBetweenSessionsTest {

  @Test
  @DisplayName("Invitation sent in the inviter's browser is shown as an income invitation in the invitee's browser")
  void invitationShouldReachInviteeBrowser(@Session @User UserSession inviter, @Session @User UserSession invitee) {
    String inviterName = inviter.user().username();
    String inviteeName = invitee.user().username();

    PeoplePage.open(inviter.page())
        .openTab(PeopleTab.ALL_PEOPLE)
        .searchPeople(inviteeName)
        .table()
        .row(inviteeName)
        .sendInvitation();

    PeoplePage.open(invitee.page())
        .openTab(PeopleTab.INCOME_INVITATIONS)
        .table()
        .checkUsernames(List.of(inviterName))
        .row(inviterName)
        .checkIncomeInvitation();
  }
}
