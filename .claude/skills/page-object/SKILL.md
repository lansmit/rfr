---
name: page-object
description: Создание и изменение Page Object / компонентов страниц для UI-тестов rfr-e2e (Playwright Java + Allure). Использовать, когда нужно написать новый PO, добавить в PO методы или локаторы, вынести общий кусок UI в компонент, или когда UI-тест требует страницы, которой ещё нет.
---

# Page Object в rfr-e2e

Пакеты: страницы — `io.student.rangiffler.page`, переиспользуемые куски UI (хедер, сайдбар, диалоги, формы, встречающиеся на нескольких страницах) — `io.student.rangiffler.page.component`. Адреса стенда берутся только из `io.student.rangiffler.config.TestConfig`.

## Правила

Действуют всегда, если пользователь явно не сказал иначе.

1. **Класс `final`.** Никаких базовых классов страниц и `extends`.
2. **Композиция вместо наследования.** Общая функциональность — отдельный компонент в `page.component` (тоже `final`), страница держит его в `private final` поле и отдаёт геттером (`header()`). Компонент находит свой корневой `Locator` в конструкторе и ищет элементы внутри него.
3. **Конструктор принимает только `Page`** — и у страниц, и у компонентов. Внутри — только присвоение `page` и инициализация `private final` полей (локаторы, компоненты). Никакой навигации, ожиданий, проверок и других вызовов, взаимодействующих со страницей.
4. **URL — `public static final String URL`** для каждой страницы, у которой есть свой адрес: `TestConfig.frontUrl() + "/my-travels"`, `TestConfig.authUrl() + "/register"`. Используется в `open()` и в проверке URL, если адрес сам по себе предмет теста (например, редирект после логина).
5. **Методы проверки содержат только проверки.** Никаких `click`, `fill`, `navigate` и прочих действий внутри `check*`. Если для проверки нужно действие — это два метода, тест вызывает их по очереди.
6. **Методы действий не содержат проверок.** Playwright сам ждёт элемент перед действием, явные ожидания и `assertThat` в действиях не нужны.
7. **Одно бизнес-действие — один метод, и имя называет это действие.** Заполнение формы — один метод со всеми полями формы в параметрах, а не `setUsername` + `setPassword` + `setPasswordSubmit`. Из имени должно быть понятно, какая это форма: `fillRegistrationForm(...)`, `fillLoginForm(...)`, `fillProfileForm(...)`, а не обезличенные `fillForm()` / `submit()` / `clickButton()`. Отправка формы — отдельный метод (`submitRegistrationForm()`), чтобы негативные тесты могли заполнить форму с ошибкой и проверить результат. Если в тестах форма всегда заполняется и отправляется вместе, допустим дополнительный метод-сценарий с бизнес-именем (`login(...)`, `register(...)`), собранный из fill- и submit-методов.
8. **`@Step` на каждом публичном методе** (кроме конструктора и геттеров компонентов), с параметрами в тексте шага: `@Step("Fill registration form: username={username}, password={password}")`. Логируются все параметры с тестовыми данными (строки, числа, модели), включая пароли. Не логируются `Page` и `Locator` — в отчёте от них только `toString()` вида `PageImpl@1a2b3c`.
9. **Проверки — только через Playwright assertions** (`com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat`): `isVisible()`, `hasText()`, `hasValue()`, `hasURL()`, `hasCount()`, `isDisabled()` и т.д. Они ретраят до таймаута. Нельзя: `locator.isVisible()` / `textContent()` / `inputValue()` + JUnit `assertEquals`/`assertTrue`.
10. **Никаких `checkThatPageLoaded()` / `checkPageLoaded()`** и других проверок «страница загрузилась». Playwright использует web-first assertions и auto-waiting: каждое действие ждёт, пока элемент станет видимым и доступным, каждая проверка ретраит до таймаута. Отдельная проверка загрузки ничего не добавляет — следующее действие или проверка и так дождутся нужного элемента или упадут с понятной ошибкой про конкретный элемент. Проверять нужно то, что важно для теста (результат действия, данные на странице), а не «что страница открылась».
11. **Fluent API.** Метод, оставляющий пользователя на той же странице, возвращает `this`; метод, уводящий на другую страницу, возвращает её новый PO.

## Локаторы

- `private final Locator` поля, инициализируются в конструкторе.
- Приоритет: `getByRole` с `setName(...).setExact(true)` → `getByLabel` → `getByText` → `id` / `name` → CSS по стабильному классу. XPath и селекторы по MUI-классам (`.css-xxxx`, `.Mui...`) не использовать.
- Исключение: локатор элемента, текст которого проверяется (`hasText`), не строится по этому тексту — иначе проверка ничего не проверяет. Для таких элементов — `id` / стабильный CSS-класс / роль без имени.
- Для `getByLabel` / `getByRole` с именем всегда `setExact(true)`: без него «Password» найдёт и кнопку с `aria-label="Show password"`.
- Перед написанием сверить локаторы с реальной разметкой: шаблоны Thymeleaf в `rfr-auth/src/main/resources/templates/`, компоненты фронта в `rfr-gql-client/src/` (тексты кнопок, `aria-label`, `id`). Если стенд запущен — можно посмотреть живую страницу через Playwright MCP.
- Регулярки в `hasURL(Pattern)` выполняются в браузере как JS RegExp: не использовать `Pattern.quote` (`\Q...\E` в JS не работает).

## Шаблон страницы

```java
package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.page.component.Header;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class ProfilePage {

  public static final String URL = TestConfig.frontUrl() + "/profile";

  private final Page page;
  private final Header header;
  private final Locator firstnameInput;
  private final Locator surnameInput;
  private final Locator usernameInput;
  private final Locator saveButton;

  public ProfilePage(Page page) {
    this.page = page;
    this.header = new Header(page);
    this.firstnameInput = page.getByLabel("First Name");
    this.surnameInput = page.getByLabel("Surname");
    this.usernameInput = page.locator("#username");
    this.saveButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save").setExact(true));
  }

  public Header header() {
    return header;
  }

  @Step("Open profile page")
  public static ProfilePage open(Page page) {
    page.navigate(URL);
    return new ProfilePage(page);
  }

  @Step("Fill profile form: firstname={firstname}, surname={surname}")
  public ProfilePage fillProfileForm(String firstname, String surname) {
    firstnameInput.fill(firstname);
    surnameInput.fill(surname);
    return this;
  }

  @Step("Save profile")
  public ProfilePage saveProfile() {
    saveButton.click();
    return this;
  }

  @Step("Check that profile username is {username}")
  public ProfilePage checkUsername(String username) {
    assertThat(usernameInput).hasValue(username);
    return this;
  }
}
```

## Шаблон компонента

```java
package io.student.rangiffler.page.component;

public final class Header {

  private final Page page;
  private final Locator profileButton;

  public Header(Page page) {
    this.page = page;
    Locator self = page.getByRole(AriaRole.BANNER);
    this.profileButton = self.getByLabel("Profile");
  }

  @Step("Open profile from header")
  public ProfilePage openProfile() {
    profileButton.click();
    return new ProfilePage(page);
  }

  @Step("Check that header of an authenticated user is shown")
  public Header checkUserIsLoggedIn() {
    assertThat(profileButton).isVisible();
    return this;
  }
}
```

## Использование в тесте

Тест — цепочка действий и проверок результата, без проверок загрузки страниц (правило 10):

```java
LandingPage.open(page)
    .clickRegister()
    .fillRegistrationForm(username, password, password)
    .submitRegistrationForm()
    .checkRegistrationSuccess();
```

## Комментарии

Без javadoc и комментариев, пересказывающих код («Страница профиля», «Нажимает кнопку»). Комментарий только когда он объясняет неочевидное «почему» (например, почему на страницу можно попасть только через определённый переход).
