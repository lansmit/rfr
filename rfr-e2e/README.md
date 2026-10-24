# rfr-e2e

UI-тесты Rangiffler: JUnit 6 + Playwright Java + Allure 3.

## Перед запуском

Тесты ходят в настоящий стенд: логинятся через UI и создают пользователей напрямую в БД. Поэтому до запуска должны работать все сервисы (порядок важен):

```bash
bash localenv.sh                          # MySQL в Docker, :3306 (root/secret)
./gradlew :rfr-auth:bootRun               # :9001, стартует первым — rfr-api берёт у него ключи
./gradlew :rfr-api:bootRun                # :8081
cd rfr-gql-client && npm i && npm run dev # :3001
```

При первом запуске Playwright сам скачивает Chromium, поэтому первый прогон заметно дольше.

## Запуск

Все команды — из корня репозитория.

```bash
./gradlew :rfr-e2e:test                                                    # все тесты
./gradlew :rfr-e2e:test --tests 'io.student.rangiffler.test.profile.ProfileTest'   # один класс
./gradlew :rfr-e2e:test --tests '*ProfileTest.avatarShouldBeSaved'         # один тест
./gradlew :rfr-e2e:test -Drfr.headless=false                               # с видимым браузером
```

## Отчёт Allure

```bash
./gradlew :rfr-e2e:allureServe    # собрать отчёт и открыть в браузере
./gradlew :rfr-e2e:allureReport   # только собрать
```

В отчёте каждый метод Page Object — отдельный шаг с параметрами. Для упавшего теста прикладывается скриншот страницы.

## Настройки

По умолчанию тесты настроены на локальный стенд. Любое значение можно переопределить системным свойством `-Drfr.<ключ>=...` или переменной окружения `RFR_<КЛЮЧ>` (свойство важнее).

| Свойство | Переменная окружения | По умолчанию |
|---|---|---|
| `rfr.front.url` | `RFR_FRONT_URL` | `http://localhost:3001` |
| `rfr.auth.url` | `RFR_AUTH_URL` | `http://localhost:9001` |
| `rfr.headless` | `RFR_HEADLESS` | `true` |
| `rfr.auth.jdbc.url` | `RFR_AUTH_JDBC_URL` | `jdbc:mysql://localhost:3306/rangiffler-auth?serverTimezone=UTC` |
| `rfr.api.jdbc.url` | `RFR_API_JDBC_URL` | `jdbc:mysql://localhost:3306/rangiffler-api?serverTimezone=UTC` |
| `rfr.db.username` | `RFR_DB_USERNAME` | `root` |
| `rfr.db.password` | `RFR_DB_PASSWORD` | `secret` |

## Структура

```
src/test/
├── java/io/student/rangiffler/
│   ├── config/TestConfig          # адреса стенда, БД, headless
│   ├── jupiter/
│   │   ├── annotation/            # @WebTest, @User
│   │   └── extension/             # BrowserExtension, UserExtension
│   ├── fixture/UserFixture        # создание и удаление тестового пользователя
│   ├── data/                      # JDBC-репозитории rangiffler-auth / rangiffler-api
│   ├── model/                     # TestUser
│   ├── utils/                     # случайные данные, файлы из classpath
│   ├── page/                      # Page Object: Landing, Login, Register, MyTravels, Profile
│   │   └── component/             # общие куски UI: Header
│   └── test/
│       ├── auth/                  # регистрация, логин, негативный логин
│       ├── profile/               # профиль
│       └── fake/                  # проверка, что сборка и Allure работают
└── resources/
    ├── sql/                       # запросы для создания и удаления пользователей
    └── img/                       # тестовые картинки (аватары)
```

### Как устроен тест

```java
@WebTest
class ProfileTest {

  @Test
  void locationShouldBeChanged(Page page, @User TestUser user) {
    login(page, user)
        .selectLocation("Canada")
        .saveProfile()
        .checkProfileSaved()
        .reload()
        .checkLocation("Canada");
  }
}
```

- `@WebTest` на классе: перед каждым тестом открывается новый браузер (Chromium, локаль `en-US`), в тест передаётся `Page`. При падении в отчёт Allure прикладывается скриншот.
- `@User` на параметре: перед тестом в `rangiffler-auth` создаётся пользователь со случайными именем и паролем (`@User(password = "...")` задаёт пароль явно). После теста он удаляется из обеих БД вместе со всеми своими данными. Профиль в `rangiffler-api` появляется при первом входе.
- Тест — цепочка действий и проверок через Page Object. Правила написания Page Object: [`.claude/skills/rfr-page-objects/SKILL.md`](../.claude/skills/rfr-page-objects/SKILL.md).

## Покрытие

| Класс | Сценарии |
|---|---|
| `auth/RegistrationTest` | регистрация нового пользователя и вход под ним |
| `auth/LoginTest` | вход пользователя со случайным и с заданным паролем |
| `auth/LoginNegativeTest` | неверный пароль; несуществующий пользователь |
| `profile/ProfileTest` | пустой профиль нового пользователя; сохранение имени, фамилии, аватара, страны по отдельности; чтение всех полей после перезагрузки; Reset возвращает сохранённые значения; Reset отменяет несохранённые изменения; ошибки при слишком длинных имени и фамилии |
