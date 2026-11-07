package io.student.rangiffler.fixture;

import io.student.rangiffler.data.db.Friendship;
import io.student.rangiffler.model.TestUser;

import java.util.List;

public enum Relation {

  FRIEND {
    @Override
    Friendship friendship(String user, String partner) {
      return Friendship.friends(partner, user);
    }

    @Override
    TestUser attach(TestUser user, List<TestUser> partners) {
      return user.withFriends(partners);
    }
  },

  INCOME_INVITATION {
    @Override
    Friendship friendship(String user, String partner) {
      return Friendship.invitation(partner, user);
    }

    @Override
    TestUser attach(TestUser user, List<TestUser> partners) {
      return user.withIncomeInvitations(partners);
    }
  },

  OUTCOME_INVITATION {
    @Override
    Friendship friendship(String user, String partner) {
      return Friendship.invitation(user, partner);
    }

    @Override
    TestUser attach(TestUser user, List<TestUser> partners) {
      return user.withOutcomeInvitations(partners);
    }
  };

  abstract Friendship friendship(String user, String partner);

  abstract TestUser attach(TestUser user, List<TestUser> partners);
}
