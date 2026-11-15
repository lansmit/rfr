package io.student.rangiffler.fixture;

import io.qameta.allure.Allure;
import io.student.rangiffler.model.TestUser;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class StaticUserFixture implements UserFixture {

  private static final Duration TIMEOUT = Duration.ofSeconds(30);
  // pool users are not created by tests, so their id in our DB is unknown
  private static final BlockingQueue<TestUser> QUEUE = new LinkedBlockingQueue<>(List.of(
      new TestUser(null, "static_user_1", "static_password_1"),
      new TestUser(null, "static_user_2", "static_password_2"),
      new TestUser(null, "static_user_3", "static_password_3")
  ));

  private TestUser user;

  @Override
  public TestUser create() {
    if (user != null) {
      throw new IllegalStateException("Static user " + user.username() + " is already taken");
    }
    TestUser taken = poll();
    if (taken == null) {
      throw new IllegalStateException("No free static user within " + TIMEOUT.toSeconds() + " s: all pool users are taken");
    }
    user = taken;
    Allure.step("Take static user from queue: username=" + taken.username());
    return taken;
  }

  @Override
  public void close() {
    if (user == null) {
      return;
    }
    TestUser taken = user;
    user = null;
    QUEUE.offer(taken);
    Allure.step("Return static user to queue: username=" + taken.username());
  }

  private static TestUser poll() {
    try {
      return QUEUE.poll(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting for a static user", e);
    }
  }
}
