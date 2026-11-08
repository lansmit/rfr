package io.student.rangiffler.jupiter.extension;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstanceFactoryContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.jupiter.api.extension.TestInstancePreConstructCallback;
import org.junit.jupiter.api.extension.TestInstancePreDestroyCallback;

import java.lang.reflect.Constructor;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class DemoExtension implements
    TestInstancePreConstructCallback,
    TestInstancePostProcessor,
    BeforeAllCallback,
    BeforeEachCallback,
    BeforeTestExecutionCallback,
    AfterTestExecutionCallback,
    AfterEachCallback,
    AfterAllCallback,
    TestInstancePreDestroyCallback,
    ParameterResolver {

  public static final String RESOLVED_VALUE = "resolved by DemoExtension";

  private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(DemoExtension.class);
  private static final AtomicInteger INSTANCES = new AtomicInteger();
  private static final int TOP_LEVEL_DEPTH = 2;
  private static final int NESTED_TEST_DEPTH = 3;

  private final int id = INSTANCES.incrementAndGet();
  private boolean used;

  @Override
  public ExtensionContextScope getTestInstantiationExtensionContextScope(ExtensionContext rootContext) {
    return ExtensionContextScope.TEST_METHOD;
  }

  @Override
  public void preConstructTestInstance(TestInstanceFactoryContext factoryContext, ExtensionContext context) {
    log("preConstructTestInstance", context);
  }

  @Override
  public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
    log("postProcessTestInstance", context);
  }

  @Override
  public void beforeAll(ExtensionContext context) {
    log("beforeAll", context);
  }

  @Override
  public void beforeEach(ExtensionContext context) {
    log("beforeEach", context);
  }

  @Override
  public void beforeTestExecution(ExtensionContext context) {
    log("beforeTestExecution", context);
  }

  @Override
  public void afterTestExecution(ExtensionContext context) {
    log("afterTestExecution", context);
  }

  @Override
  public void afterEach(ExtensionContext context) {
    log("afterEach", context);
  }

  @Override
  public void afterAll(ExtensionContext context) {
    log("afterAll", context);
  }

  @Override
  public void preDestroyTestInstance(ExtensionContext context) {
    log("preDestroyTestInstance", context);
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return parameterContext.getParameter().getType() == String.class;
  }

  @Override
  public String resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    String executable = parameterContext.getDeclaringExecutable() instanceof Constructor
        ? "constructor"
        : parameterContext.getDeclaringExecutable().getName();
    log("resolveParameter(" + executable + " #" + parameterContext.getIndex() + ")", extensionContext);
    return RESOLVED_VALUE;
  }

  public static void logTestBody() {
    System.out.printf("%s%s%n", indent(NESTED_TEST_DEPTH + 1), ">>> test body");
  }

  private void log(String callback, ExtensionContext context) {
    printUnseenAncestors(context);
    String instance = "DemoExtension#" + id + (used ? "" : " (новый экземпляр)");
    used = true;
    String line = indent(depth(context) + 1) + callback;
    System.out.printf("%-44s %s%n", line, instance);
  }

  @SuppressWarnings("unchecked")
  private static void printUnseenAncestors(ExtensionContext context) {
    Set<String> seen = context.getRoot().getStore(NAMESPACE)
        .computeIfAbsent("seen", key -> new HashSet<String>(), Set.class);
    Deque<ExtensionContext> unseen = new ArrayDeque<>();
    for (Optional<ExtensionContext> current = Optional.of(context);
         current.isPresent() && depth(current.get()) >= TOP_LEVEL_DEPTH;
         current = current.get().getParent()) {
      if (seen.add(current.get().getUniqueId())) {
        unseen.push(current.get());
      }
    }
    unseen.forEach(ancestor -> System.out.printf("%s%s%n", indent(depth(ancestor)), ancestor.getDisplayName()));
  }

  private static int depth(ExtensionContext context) {
    int depth = 0;
    for (Optional<ExtensionContext> parent = context.getParent(); parent.isPresent(); parent = parent.get().getParent()) {
      depth++;
    }
    return depth;
  }

  private static String indent(int depth) {
    return "  ".repeat(depth - TOP_LEVEL_DEPTH);
  }
}
