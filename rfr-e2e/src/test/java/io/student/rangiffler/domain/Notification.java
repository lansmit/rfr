package io.student.rangiffler.domain;

public enum Notification {
  PROFILE_UPDATED("Your profile is successfully updated"),
  INVITATION_SENT("Invitation sent"),
  INVITATION_ACCEPTED("Invitation accepted"),
  INVITATION_DECLINED("Invitation declined"),
  INVITATION_CANCELLED("Invitation cancelled"),
  FRIEND_DELETED("Friend deleted"),
  PHOTO_DELETED("Post deleted");

  private final String text;

  Notification(String text) {
    this.text = text;
  }

  public String text() {
    return text;
  }

  @Override
  public String toString() {
    return text;
  }
}
