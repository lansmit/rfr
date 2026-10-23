package io.student.rangiffler.utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public final class ResourceUtils {

  private ResourceUtils() {
  }

  public static Path classpathFile(String name) {
    URL resource = ResourceUtils.class.getClassLoader().getResource(name);
    if (resource == null) {
      throw new IllegalArgumentException("Classpath resource not found: " + name);
    }
    try {
      return Path.of(resource.toURI());
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }

  // same string the browser's FileReader.readAsDataURL produces for a .png file
  public static String pngDataUrl(Path png) {
    try {
      return "data:image/png;base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(png));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
