package io.student.rangiffler.jupiter.extension;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import io.student.rangiffler.config.Config;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

import java.io.ByteArrayInputStream;

public class BrowserExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver,
    TestExecutionExceptionHandler {

  private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(BrowserExtension.class);

  @Override
  public void beforeEach(ExtensionContext context) {
    Playwright playwright = Playwright.create();
    Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(Config.headless()));
    BrowserContext browserContext = browser.newContext();
    ExtensionContext.Store store = context.getStore(NAMESPACE);
    store.put(Playwright.class, playwright);
    store.put(Page.class, browserContext.newPage());
  }

  @Override
  public void afterEach(ExtensionContext context) {
    // JUnit closes AutoCloseable values left in the store itself; Page would then be closed after its Playwright
    ExtensionContext.Store store = context.getStore(NAMESPACE);
    store.remove(Page.class);
    Playwright playwright = store.remove(Playwright.class, Playwright.class);
    if (playwright != null) {
      playwright.close();
    }
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return parameterContext.getParameter().getType().isAssignableFrom(Page.class);
  }

  @Override
  public Page resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return extensionContext.getStore(NAMESPACE).get(Page.class, Page.class);
  }

  @Override
  public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
    Page page = context.getStore(NAMESPACE).get(Page.class, Page.class);
    if (page != null && !page.isClosed()) {
      Allure.attachment("Screenshot on failure", "image/png",
          new ByteArrayInputStream(page.screenshot(new Page.ScreenshotOptions().setFullPage(true))), AttachmentOptions.withFileExtension("png"));
    }
    throw throwable;
  }
}
