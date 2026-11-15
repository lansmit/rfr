package io.student.rangiffler.page.component;

public enum PeopleAction {
  ADD("Add"),
  ACCEPT("Accept"),
  DECLINE("Decline"),
  REMOVE("Remove"),
  CANCEL("Cancel");

  private final String title;

  PeopleAction(String title) {
    this.title = title;
  }

  public String title() {
    return title;
  }
}
