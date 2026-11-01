package io.student.rangiffler.test.demo;

import io.student.rangiffler.jupiter.extension.DemoExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("demo")
@DisplayName("Места установки @ExtendWith")
class ExtendWithPlacesTest {

  private static final String NOT_RESOLVED = "DemoExtension did not resolve the parameter";

  @ExtendWith(DemoExtension.class)
  interface Extended {
  }

  @ExtendWith(DemoExtension.class)
  abstract static class ExtendedBase {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.TYPE, ElementType.METHOD})
  @ExtendWith(DemoExtension.class)
  @interface WithDemoExtension {
  }

  @Nested
  @ExtendWith(DemoExtension.class)
  @DisplayName("На классе")
  class OnClass {

    @Test
    @DisplayName("Первый тест")
    void firstTest(String value) {
      DemoExtension.logTestBody();
      assertEquals(DemoExtension.RESOLVED_VALUE, value, NOT_RESOLVED);
    }

    @Test
    @DisplayName("Второй тест")
    void secondTest(String value) {
      DemoExtension.logTestBody();
      assertEquals(DemoExtension.RESOLVED_VALUE, value, NOT_RESOLVED);
    }
  }

  @Nested
  @DisplayName("На интерфейсе, который реализует класс")
  class OnInterface implements Extended {

    @Test
    @DisplayName("Первый тест")
    void firstTest(String value) {
      DemoExtension.logTestBody();
      assertEquals(DemoExtension.RESOLVED_VALUE, value, NOT_RESOLVED);
    }

    @Test
    @DisplayName("Второй тест")
    void secondTest(String value) {
      DemoExtension.logTestBody();
      assertEquals(DemoExtension.RESOLVED_VALUE, value, NOT_RESOLVED);
    }
  }

  @Nested
  @DisplayName("На суперклассе")
  class OnSuperclass extends ExtendedBase {

    @Test
    @DisplayName("Первый тест")
    void firstTest(String value) {
      DemoExtension.logTestBody();
      assertEquals(DemoExtension.RESOLVED_VALUE, value, NOT_RESOLVED);
    }

    @Test
    @DisplayName("Второй тест")
    void secondTest(String value) {
      DemoExtension.logTestBody();
      assertEquals(DemoExtension.RESOLVED_VALUE, value, NOT_RESOLVED);
    }
  }

  @Nested
  @WithDemoExtension
  @DisplayName("В составной аннотации")
  class OnMetaAnnotation {

    @Test
    @DisplayName("Первый тест")
    void firstTest(String value) {
      DemoExtension.logTestBody();
      assertEquals(DemoExtension.RESOLVED_VALUE, value, NOT_RESOLVED);
    }

    @Test
    @DisplayName("Второй тест")
    void secondTest(String value) {
      DemoExtension.logTestBody();
      assertEquals(DemoExtension.RESOLVED_VALUE, value, NOT_RESOLVED);
    }
  }

  @Nested
  @DisplayName("На статическом поле")
  class OnStaticField {

    @ExtendWith(DemoExtension.class)
    static Object extension;

    @Test
    @DisplayName("Первый тест")
    void firstTest(String value) {
      DemoExtension.logTestBody();
      assertEquals(DemoExtension.RESOLVED_VALUE, value, NOT_RESOLVED);
    }

    @Test
    @DisplayName("Второй тест")
    void secondTest(String value) {
      DemoExtension.logTestBody();
      assertEquals(DemoExtension.RESOLVED_VALUE, value, NOT_RESOLVED);
    }
  }

  @Nested
  @DisplayName("На поле экземпляра")
  class OnInstanceField {

    @ExtendWith(DemoExtension.class)
    Object extension;

    @Test
    @DisplayName("Первый тест")
    void firstTest(String value) {
      DemoExtension.logTestBody();
      assertEquals(DemoExtension.RESOLVED_VALUE, value, NOT_RESOLVED);
    }

    @Test
    @DisplayName("Второй тест")
    void secondTest(String value) {
      DemoExtension.logTestBody();
      assertEquals(DemoExtension.RESOLVED_VALUE, value, NOT_RESOLVED);
    }
  }

  @Nested
  @DisplayName("На параметре конструктора")
  class OnConstructorParameter {

    private final String value;

    OnConstructorParameter(@ExtendWith(DemoExtension.class) String value) {
      this.value = value;
    }

    @Test
    @DisplayName("Первый тест")
    void firstTest() {
      DemoExtension.logTestBody();
      assertEquals(DemoExtension.RESOLVED_VALUE, value, NOT_RESOLVED);
    }

    @Test
    @DisplayName("Второй тест")
    void secondTest() {
      DemoExtension.logTestBody();
      assertEquals(DemoExtension.RESOLVED_VALUE, value, NOT_RESOLVED);
    }
  }

  @Nested
  @DisplayName("На параметре @BeforeAll")
  class OnBeforeAllParameter {

    private static String value;

    @BeforeAll
    static void setUp(@ExtendWith(DemoExtension.class) String resolved) {
      value = resolved;
    }

    @Test
    @DisplayName("Первый тест")
    void firstTest() {
      DemoExtension.logTestBody();
      assertEquals(DemoExtension.RESOLVED_VALUE, value, NOT_RESOLVED);
    }

    @Test
    @DisplayName("Второй тест")
    void secondTest() {
      DemoExtension.logTestBody();
      assertEquals(DemoExtension.RESOLVED_VALUE, value, NOT_RESOLVED);
    }
  }

  @Nested
  @DisplayName("На параметре @BeforeEach")
  class OnBeforeEachParameter {

    private String value;

    @BeforeEach
    void setUp(@ExtendWith(DemoExtension.class) String resolved) {
      value = resolved;
    }

    @Test
    @DisplayName("Первый тест")
    void firstTest() {
      DemoExtension.logTestBody();
      assertEquals(DemoExtension.RESOLVED_VALUE, value, NOT_RESOLVED);
    }

    @Test
    @DisplayName("Второй тест")
    void secondTest() {
      DemoExtension.logTestBody();
      assertEquals(DemoExtension.RESOLVED_VALUE, value, NOT_RESOLVED);
    }
  }

  @Nested
  @DisplayName("На тестовом методе")
  class OnMethod {

    @Test
    @ExtendWith(DemoExtension.class)
    @DisplayName("Первый тест")
    void firstTest(String value) {
      DemoExtension.logTestBody();
      assertEquals(DemoExtension.RESOLVED_VALUE, value, NOT_RESOLVED);
    }

    @Test
    @ExtendWith(DemoExtension.class)
    @DisplayName("Второй тест")
    void secondTest(String value) {
      DemoExtension.logTestBody();
      assertEquals(DemoExtension.RESOLVED_VALUE, value, NOT_RESOLVED);
    }
  }

  @Nested
  @DisplayName("На параметре тестового метода")
  class OnMethodParameter {

    @Test
    @DisplayName("Первый тест")
    void firstTest(@ExtendWith(DemoExtension.class) String value) {
      DemoExtension.logTestBody();
      assertEquals(DemoExtension.RESOLVED_VALUE, value, NOT_RESOLVED);
    }

    @Test
    @DisplayName("Второй тест")
    void secondTest(@ExtendWith(DemoExtension.class) String value) {
      DemoExtension.logTestBody();
      assertEquals(DemoExtension.RESOLVED_VALUE, value, NOT_RESOLVED);
    }
  }
}
