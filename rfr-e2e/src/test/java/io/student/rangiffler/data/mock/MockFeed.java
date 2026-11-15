package io.student.rangiffler.data.mock;

import io.student.rangiffler.config.TestConfig;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// the feed in rfr-api is served from these JSON files, so the tests read expectations from them, not from the UI
public record MockFeed(List<Photo> photos, int countriesVisited) {

  private static final ObjectMapper JSON = new ObjectMapper();

  public record Photo(String description, String country, boolean isOwner) {
  }

  public static MockFeed onlyMine() {
    return read("query_feed.json");
  }

  public static MockFeed withFriends() {
    return read("query_feed_with_friends.json");
  }

  public List<String> descriptions() {
    return photos.stream().map(Photo::description).toList();
  }

  public List<Photo> ownPhotos() {
    return photos.stream().filter(Photo::isOwner).toList();
  }

  public List<Photo> friendsPhotos() {
    return photos.stream().filter(photo -> !photo.isOwner()).toList();
  }

  public List<String> descriptionsIn(String country) {
    return photos.stream().filter(photo -> photo.country().equals(country)).map(Photo::description).toList();
  }

  private static MockFeed read(String fileName) {
    JsonNode feed = JSON.readTree(Path.of(TestConfig.feedMocks(), fileName).toFile());
    List<Photo> photos = new ArrayList<>();
    for (JsonNode edge : feed.path("photos").path("edges")) {
      JsonNode node = edge.path("node");
      photos.add(new Photo(
          node.path("description").asString(),
          node.path("country").path("name").asString(),
          node.path("isOwner").asBoolean()));
    }
    return new MockFeed(List.copyOf(photos), feed.path("stat").size());
  }
}
