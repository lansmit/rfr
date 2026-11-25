package io.student.rangiffler.test.people;

import com.microsoft.playwright.Page;
import io.student.rangiffler.jupiter.annotation.UiTest;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.model.TestUser;
import io.student.rangiffler.network.GqlStubs;
import io.student.rangiffler.network.HeldOperation;
import io.student.rangiffler.page.PeoplePage;
import io.student.rangiffler.page.PeopleTab;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UiTest
@DisplayName("People: loading state")
class PeopleLoadingTest {

  @Test
  @DisplayName("All People table shows a loader while the people list is loading")
  void allPeopleTableShouldShowLoaderWhileLoading(Page page, @User TestUser user, GqlStubs stubs) {
    PeoplePage peoplePage = PeopleLogin.openAs(page, user);
    HeldOperation getPeople = stubs.hold("GetPeople");

    peoplePage.clickTab(PeopleTab.ALL_PEOPLE)
        .table()
        .checkLoading();
    getPeople.release();
    peoplePage.table()
        .checkLoaded();
  }
}
