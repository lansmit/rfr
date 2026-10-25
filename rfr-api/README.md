# RFR-API - GraphQL Backend

## Описание

`rfr-api` - бэкенд проекта Rangiffler с GraphQL API, работающий как OAuth 2.0 Resource Server.
Пользователи, страны и дружба реализованы на реальной БД; фотографии, лайки, лента и статистика пока
отдаются mock-слоем из JSON-файлов и **ничего не сохраняют**.

## Технологический стек

- **Java 25**, Gradle 9.7.1 (wrapper в корне проекта)
- **Spring Boot 4.1.1** (Jackson 3)
- **Spring for GraphQL** + **DGS Codegen** - Java-модель генерируется из схемы
- **Spring Security 7** - OAuth 2.0 Resource Server (JWT), `@PreAuthorize` на контроллерах
- **Spring Data JPA / Hibernate** - работа с БД
- **Flyway** - миграции
- **MySQL 8.4**, схема `rangiffler-api`

## Структура модуля

```
rfr-api/
├── build.gradle                                   # Spring Boot + DGS codegen (generateJava)
└── src/main/
    ├── java/io/student/rangiffler/
    │   ├── RangifflerApiApplication.java          # Точка входа
    │   ├── config/
    │   │   ├── RangifflerApiConfiguration.java    # Security: resource server, audience, authorities, CORS
    │   │   └── GraphQlExceptionResolver.java      # Исключения -> GraphQL errors (BAD_REQUEST / NOT_FOUND)
    │   ├── controller/
    │   │   ├── CountryQueryController.java        # Query.countries
    │   │   ├── UserQueryController.java           # Query.user, Query.users, User.friends/...Invitations
    │   │   ├── UserMutationController.java        # Mutation.user, Mutation.friendship
    │   │   ├── FeedMockQueryController.java       # Query.feed, Feed.photos, Feed.stat, Photo.likes (mock)
    │   │   └── PhotoMockMutationController.java   # Mutation.photo, Mutation.deletePhoto (mock)
    │   ├── data/
    │   │   ├── entity/                            # CountryEntity, UserEntity, FriendshipEntity, FriendshipStatus
    │   │   ├── projection/UserWithStatus.java     # Пользователь + статус дружбы относительно текущего
    │   │   └── repository/                        # CountryRepository, UserRepository, FriendshipRepository
    │   ├── exception/                             # ResourceNotFoundException, FriendshipActionException
    │   ├── service/
    │   │   ├── api/                               # CountryService, UserService
    │   │   ├── impl/                              # CountryServiceImpl, UserServiceImpl
    │   │   ├── mock/FeedMockData.java             # Чтение mock-ленты из JSON
    │   │   └── cors/CorsCustomizer.java           # CORS для фронтенда
    │   └── util/                                  # BytesAsString, StringAsBytes, GqlQueryPaginationAndSort
    └── resources/
        ├── application.yml
        ├── graphql/query.graphqls                 # GraphQL схема (источник для codegen)
        ├── db/migration/rangiffler-api/V1__schema_init.sql
        └── mock/
            ├── query_feed.json                    # Лента "Only my": 4 фото
            └── query_feed_with_friends.json       # Лента "With friends": те же 4 + 3 фото друзей
```

Сгенерированные классы (`io.student.rangiffler.model.*`, например `User`, `Photo`, `UserInput`) появляются в
`build/generated/sources/dgs-codegen` после `./gradlew :rfr-api:generateJava` (выполняется автоматически при сборке).
`UserConnection` и `PhotoConnection` маппятся на `org.springframework.data.domain.Page<...>`, скаляр `Date` - на `LocalDate`.

## Что реализовано, а что mock

| Возможность | Реализация |
|---|---|
| Профиль пользователя (`Query.user`, `Mutation.user`) | БД. Профиль создается при первом `Query.user` со страной `ru` |
| Справочник стран (`Query.countries`) | БД, заполняется миграцией (238 стран) |
| Люди, поиск, пагинация (`Query.users`) | БД, сортировка по username |
| Дружба (`Mutation.friendship`, `friends`, `incomeInvitations`, `outcomeInvitations`) | БД, одна строка на пару, проверка переходов |
| Лента и статистика (`Query.feed`) | Mock: JSON-файлы, одинаковые для всех пользователей; фильтр по стране по захардкоженному соответствию |
| Создание / редактирование / лайк фото (`Mutation.photo`) | Mock: возвращает корректный объект, но ничего не сохраняет |
| Удаление фото (`Mutation.deletePhoto`) | Mock: всегда `true`, ничего не удаляет |

Таблицы `photo`, `like`, `photo_like`, `statistic` уже есть в миграции, но приложение их не использует:
реализация фото, лайков и статистики запланирована (см. [«Статус и планы»](../README.md#статус-и-планы) в корневом README).

## GraphQL API

Схема: `src/main/resources/graphql/query.graphqls`. Endpoint: `POST http://localhost:8081/graphql`.

### Queries

```graphql
type Query {
    countries: [Country!]!
    user: User!
    users(page:Int = 0, size:Int = 10, searchQuery:String): UserConnection
    feed(withFriends: Boolean!): Feed!
}
```

Текущий пользователь и его связи:

```graphql
query GetUser {
  user {
    id
    username
    firstname
    surname
    avatar
    location { code name flag }
    friends(page: 0, size: 10) {
      edges { node { id username friendStatus } }
      pageInfo { hasNextPage hasPreviousPage }
    }
  }
}
```

Лента с фильтром по стране:

```graphql
query GetFeed($withFriends: Boolean!, $country: String) {
  feed(withFriends: $withFriends) {
    photos(page: 0, size: 12, country: $country) {
      edges {
        node {
          id src description isOwner creationDate
          country { code name flag }
          likes { total likes { user username creationDate } }
        }
      }
      pageInfo { hasNextPage hasPreviousPage }
    }
    stat { count country { code } }
  }
}
```

`withFriends: false` читает `query_feed.json`, `true` - `query_feed_with_friends.json`. Список фото режется по `page/size`.
`Feed.stat` берется из JSON и не зависит от фильтра; в JSON у страны в `stat` есть только `code`.

### Mutations

```graphql
type Mutation {
    user(input: UserInput!): User!
    photo(input: PhotoInput!): Photo!
    deletePhoto(id: ID!): Boolean
    friendship(input: FriendshipInput!): User!
}
```

Обновление профиля (`null`/пропущенное поле - "не менять"):

```graphql
mutation UpdateUser($input: UserInput!) {
  user(input: $input) { id username firstname surname avatar location { code } }
}
```
```json
{ "input": { "firstname": "Ivan", "surname": "Petrov", "avatar": "data:image/png;base64,...", "location": { "code": "fr" } } }
```

Дружба:

```graphql
mutation FriendshipAction($input: FriendshipInput!) {
  friendship(input: $input) { id username friendStatus }
}
```
```json
{ "input": { "user": "<API UUID другого пользователя>", "action": "ADD" } }
```

| Действие | Кто может | Результат |
|---|---|---|
| `ADD` | Любой, если у пары еще нет записи | Строка `PENDING`, ответ `INVITATION_SENT` |
| `ACCEPT` | Только адресат заявки | `ACCEPTED`, ответ `FRIEND` |
| `REJECT` | Только адресат заявки | Строка удаляется, ответ `NOT_FRIEND` |
| `DELETE` | Любая сторона, любой статус | Строка удаляется: удаление друга или отмена исходящей заявки |

Фото (mock): создание требует `src` и `country`; при `id` существующего mock-фото и `like` - добавляется лайк:

```graphql
mutation CreatePhoto($input: PhotoInput!) {
  photo(input: $input) { id src description country { code name flag } likes { total } }
}
```
```json
{ "input": { "src": "data:image/jpeg;base64,...", "description": "Paris", "country": { "code": "fr" } } }
```

## Ошибки

HTTP-статус GraphQL-ответа - 200 и при ошибке; проверяйте `errors`. `GraphQlExceptionResolver` переводит исключения:

| Исключение | `extensions.classification` | Пример `message` |
|---|---|---|
| `ResourceNotFoundException` | `NOT_FOUND` | `Country not found by code: xx`, `User not found by id: ...` |
| `FriendshipActionException` | `BAD_REQUEST` | `Invitation already sent`, `No pending invitation from this user`, `Cannot perform friendship action on yourself` |
| `IllegalArgumentException` | `BAD_REQUEST` | `Photo country is required`, некорректный UUID, `size=0` |
| Нет токена / нет нужной authority | `UNAUTHORIZED` / `FORBIDDEN` | Spring for GraphQL |
| Прочие | `INTERNAL_ERROR` | Без деталей |

Фронтенд показывает текст ошибки в снэкбаре только для `BAD_REQUEST` и `NOT_FOUND` (`rfr-gql-client/src/api/graphqlError.ts`), поэтому понятный пользователю текст есть только в этих двух классах, а внутренние детали наружу не уходят. Негативные e2e-тесты проверяют `classification` и `message`.

## Security

- `POST /graphql` пропускается фильтрами, доступ проверяется на контроллерах:
  query-контроллеры - `@PreAuthorize("hasAuthority('read')")`, mutation-контроллеры - `hasAuthority('write')`.
- JWT проверяется по JWKS `rfr-auth` (`spring.security.oauth2.resourceserver.jwt.issuer-uri`): подпись, `iss`, `exp`
  и **audience** (`jwt.audiences: rangiffler-api`). Принимается только access token; `id_token` (aud = client_id) отклоняется с 401.
- Claim `authorities` access token превращается в `GrantedAuthority` без префикса (`JwtAuthenticationConverter`
  в `RangifflerApiConfiguration`).
- Username текущего пользователя - claim `sub` (`@AuthenticationPrincipal Jwt`).
- Невалидный или просроченный Bearer-токен - HTTP 401 от фильтра, до GraphQL.
- Access token живёт 30 минут (`access-token-time-to-live` в `rfr-auth/src/main/resources/application.yaml`). При logout фронтенд отзывает его через `POST /oauth2/revoke` (RFC 7009) и вызывает OIDC logout с `id_token` в `id_token_hint`. `rfr-api` проверяет JWT локально по подписи и об отзыве не знает, поэтому отозванный токен принимается до `exp`. Мгновенный отказ потребовал бы opaque-токенов и introspection.
- GraphiQL (`/graphiql`) включён (`spring.graphql.graphiql.enabled`), но `GET` на него требует аутентификации: в фильтрах открыт только `POST /graphql`. Чтобы пользоваться им локально, нужно открыть `GET /graphiql/**` через `permitAll()`, а в запросах передавать заголовок `Authorization: Bearer <access_token>` (токен — в `localStorage` фронтенда, ключ `access_token`).

## Технические решения

### Пагинация и Connection-типы

В `query.graphqls` используются типы `UserConnection` и `PhotoConnection`, которые в схеме не описаны, поэтому IDEA подсвечивает их красным:

<img src="../IDEA-error.png" width="600">

Это не ошибка: Spring for GraphQL сам строит типы `{Typename}Connection` (список `edges` + `pageInfo`) из `Slice`/`Page`, которые возвращает контроллер ([документация](https://docs.spring.io/spring-graphql/reference/request-execution.html#execution.pagination)). Java-класс нужен только для самого типа (`User`, `Photo`), его генерирует DGS Codegen. Соответствие для codegen прописано в `build.gradle`:

```groovy
generateJava {
    typeMapping = [
        'UserConnection' : 'org.springframework.data.domain.Page<io.student.rangiffler.model.types.User>',
        'PhotoConnection': 'org.springframework.data.domain.Page<io.student.rangiffler.model.types.Photo>',
        'Date'           : 'java.time.LocalDate'
    ]
}
```

Контроллер возвращает `Page<User>`:

```java
@QueryMapping
public Page<User> users(@AuthenticationPrincipal Jwt principal,
                        @Argument("page") @Nullable Integer page,
                        @Argument("size") @Nullable Integer size,
                        @Argument("searchQuery") @Nullable String searchQuery) {
  return userService.allUsers(
      principal.getClaim("sub"),
      pageRequest(page, size),
      searchQuery
  );
}

private static PageRequest pageRequest(@Nullable Integer page, @Nullable Integer size) {
  return PageRequest.of(
      Objects.requireNonNullElse(page, DEFAULT_PAGE),
      Objects.requireNonNullElse(size, DEFAULT_SIZE)
  );
}
```

- `page` и `size` — `@Nullable Integer`, а не `int`: у аргументов в схеме есть значения по умолчанию, но клиент может явно передать `null`, и примитив уронит запрос.
- Имена аргументов указаны явно (`@Argument("page")`). Без этого Spring for GraphQL берёт имя из байткода, а для этого нужен флаг компилятора `-parameters`. Gradle его включает, а, например, сборка VS Code (`bin/main`) — нет, и приложение падает на старте с `Name for argument of type [...] not specified`.
- Сервис превращает `Page<UserWithStatus>` из репозитория в `Page<User>` через `Page.map()`.

Пагинация работает только когда данные достаются **одним запросом** в репозиторий с параметром `Pageable`. Из коллекции сущности (`user.getFriends()`) страницу получить нельзя.

### Дружба: одна строка на пару пользователей

Дружба хранится в таблице `friendship` одной строкой на пару: `requester` — кто отправил заявку, `addressee` — кому, статус `PENDING`/`ACCEPTED`. При принятии заявки меняется только статус. Отклонение и удаление из друзей удаляют строку.

Уникальность неупорядоченной пары гарантирует БД, поэтому встречные заявки, отправленные одновременно, не создадут две строки:

```sql
user_low_id  binary(16) as (least(requester_id, addressee_id)) stored,
user_high_id binary(16) as (greatest(requester_id, addressee_id)) stored,
constraint uq_friendship_pair unique (user_low_id, user_high_id),
constraint ck_friendship_distinct check (requester_id <> addressee_id)
```

В JPA дружба — самостоятельный агрегат `FriendshipEntity` со своим `FriendshipRepository`, а не коллекции внутри `UserEntity`:
- связи `requester`/`addressee` — `@ManyToOne(fetch = LAZY)` без каскадов;
- `@Version` — optimistic lock на случай, если один пользователь принимает заявку, а другой в этот момент её отменяет;
- правила переходов живут в сущности: `FriendshipEntity.request(...)`, `accept(actor)`, `assertCanDecline(actor)`. Принять или отклонить заявку может только её получатель;
- связь двух пользователей в любом направлении находится одним запросом:

```java
@Query("select f from FriendshipEntity f " +
    "where (f.requester.id = :first and f.addressee.id = :second) " +
    "   or (f.requester.id = :second and f.addressee.id = :first)")
Optional<FriendshipEntity> findPair(@Param("first") UUID first, @Param("second") UUID second);
```

Конфликты параллельных изменений сервис превращает в `FriendshipActionException` (`BAD_REQUEST`), а не в `INTERNAL_ERROR`. Новая заявка сохраняется через `saveAndFlush` (ловится `DataIntegrityViolationException` от уникального индекса), после принятия или удаления вызывается явный `flush()` (ловится `OptimisticLockingFailureException`). Без явного flush SQL выполнился бы только на коммите транзакции, уже за пределами `try/catch` в методе сервиса.

### Списки пользователей со статусом дружбы одним запросом

Все списки людей (All People, Friends, входящие и исходящие заявки) достаются одним запросом с пагинацией и сразу со статусом дружбы относительно текущего пользователя. Результат — record `UserWithStatus`, который заполняется в JPQL через `select new`:

```java
public record UserWithStatus(
    UUID id,
    String username,
    String firstname,
    String lastName,
    byte[] avatar,
    String countryCode,
    String countryName,
    byte[] countryFlag,
    FriendshipStatus friendshipStatus,
    Boolean isRequester
) {
}
```

```java
String SELECT_USER_WITH_STATUS =
    "select new io.student.rangiffler.data.projection.UserWithStatus(" +
        "u.id, u.username, u.firstname, u.lastName, u.avatar, c.code, c.name, c.flag, " +
        "f.status, " +
        "case when f.requester.id = :me then true when f.addressee.id = :me then false else null end) " +
        "from UserEntity u join u.country c ";
String PAIR_WITH_ME =
    "FriendshipEntity f on (f.requester.id = :me and f.addressee = u) " +
        "or (f.addressee.id = :me and f.requester = u) ";
String ALL_USERS = SELECT_USER_WITH_STATUS + "left join " + PAIR_WITH_ME + "where u.id <> :me ";
String FRIENDS = SELECT_USER_WITH_STATUS + "join " + PAIR_WITH_ME +
    "where f.status = io.student.rangiffler.data.entity.FriendshipStatus.ACCEPTED ";

@Query(ALL_USERS + ORDER)
Page<UserWithStatus> findAllUsersWithFriendshipStatus(@Param("me") UUID me, Pageable pageable);

@Query(ALL_USERS + SEARCH + ORDER)
Page<UserWithStatus> findAllUsersWithFriendshipStatus(@Param("me") UUID me,
                                                      @Param("searchQuery") String searchQuery,
                                                      Pageable pageable);
// friends, incomeInvitations, outcomeInvitations — так же, полный код в UserRepository
```

- `:me` — UUID текущего пользователя. Сервис один раз находит его по `username` из JWT, дальше запросы сравнивают id, а не строки.
- Связь с текущим пользователем ищется одним `join` по паре в любом направлении, `isRequester` говорит, кто отправил заявку. Из `friendshipStatus` + `isRequester` сервис вычисляет `FriendStatus`: `ACCEPTED` → `FRIEND`, `PENDING` + `true` → `INVITATION_SENT`, `PENDING` + `false` → `INVITATION_RECEIVED`, нет связи → `NOT_FRIEND`.
- Страна приходит в той же строке (`join u.country c`), без отдельного запроса на каждого пользователя (N+1).
- Каждый запрос есть в двух вариантах, с поиском и без: `searchQuery` в JPQL обязателен, поэтому какой метод вызвать, решает сервис.

### Вложенные поля через @SchemaMapping

Поля типа, которые запрашиваются не всегда (`User.friends`, `incomeInvitations`, `outcomeInvitations`, `Feed.photos`, `Photo.likes`), заполняются отдельными методами с `@SchemaMapping`. Spring for GraphQL вызывает их, только если клиент запросил это поле:

```java
@QueryMapping
public User user(@AuthenticationPrincipal Jwt principal) {
  return userService.createNewUserIfNotPresent(principal.getClaim("sub")); // поле friends здесь null
}

@SchemaMapping(typeName = "User", field = "friends")
public Page<User> friends(User user,
                          @Argument("page") @Nullable Integer page,
                          @Argument("size") @Nullable Integer size,
                          @Argument("searchQuery") @Nullable String searchQuery) {
  return userService.friends(user.getUsername(), pageRequest(page, size), searchQuery);
}
```

Методы с `@SchemaMapping` в своём коде напрямую не вызываются.

### Особенности Spring Boot 4

- Jackson 3: пакеты `tools.jackson.databind.*` вместо `com.fasterxml.jackson.databind.*`.
- HQL в `@Query` проверяется только при старте приложения (нужна БД), `./gradlew build` ошибки в запросах не находит.

## База данных

Схема `rangiffler-api` (создается автоматически, `createDatabaseIfNotExist=true`), миграция
`db/migration/rangiffler-api/V1__schema_init.sql`, `ddl-auto: none`.

| Таблица | Назначение |
|---|---|
| `user` | API-профиль: username (связь с `rfr-auth` только по username), имя, фамилия, аватар (LONGBLOB, data URL), страна |
| `country` | Справочник стран, флаги - data URL в BLOB |
| `friendship` | Одна строка на пару: `requester_id`, `addressee_id`, `status` (`PENDING`/`ACCEPTED`), `created_at`, `responded_at`, `version`; уникальный индекс неупорядоченной пары |
| `photo`, `like`, `photo_like`, `statistic` | Заготовлены, приложением пока не используются |

UUID пользователя в `rangiffler-api` и `rangiffler-auth` независимы.

## Запуск

Требования: JDK 25 (Gradle toolchain), Docker для MySQL, запущенный `rfr-auth` (нужен для JWKS).

```bash
# из корня проекта: MySQL в Docker
bash localenv.sh

cd rfr-api
../gradlew bootRun
# или main class io.student.rangiffler.RangifflerApiApplication из IDE
```

Проверка (access token можно взять в localStorage фронтенда после логина, ключ `access_token`):

```bash
curl -X POST http://localhost:8081/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <access_token>" \
  -d '{"query":"{ user { id username location { code } } }"}'
```
