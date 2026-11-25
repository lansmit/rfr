# План: GraphQL subscription на изменения дружбы (живое обновление People)

Источник требований: `.claude/hw06-task4-requirements.md` (дальше — «Требования», ссылки вида §3, критерии — К1…К13).
План ничего не реализует. Пункты, помеченные **[решение]**, — выбор между вариантами, его делает пользователь до реализации; пункты **[сверить]** — места, где имя класса или поведение библиотеки надо проверить по версии из проекта (Spring Boot 4.1.1 / Spring for GraphQL 2.x, Apollo Client 4, Playwright 1.63).

## 0. Что есть сейчас (то, на что опирается план)

- Схема: один файл `rfr-api/src/main/resources/graphql/query.graphqls`, типа `Subscription` нет. DGS Codegen генерирует типы из всей папки `graphql/`.
- Дружба меняется в `UserServiceImpl.addFriend / acceptInvitation / declineInvitation / removeFriend` (транзакционные, `flush` внутри); мутация — `UserMutationController.friendship`.
- Безопасность: `RangifflerApiConfiguration` — `POST /graphql` `permitAll`, всё остальное `authenticated()`; авторизация — `@PreAuthorize` на контроллерах; JWT-конвертер `authorities` без префикса.
- Транспорт фронта: `apolloClient.ts` — только `HttpLink`; `ErrorLink` на HTTP 401 делает `clearSession()` и редирект на `/`.
- People: `PeoplePage` рендерит только активную вкладку (`AllTable`, `FriendsTable`, `InvitationsTable`, `OutcomeInvitationsTable`); каждая — свой хук (`GetPeople`, `GetFriends`, `GetInvitations`, `GetOutcomeInvitations`) с `fetchPolicy: "cache-and-network"`, `page`/`search` в `useState` таблицы. Сортировка и поиск (username, firstname, surname, подстрока) — на сервере (`UserRepository`).
- После мутации `useUpdateFriendshipStatus` перезапрашивает все 4 запроса People (`awaitRefetchQueries: true`), snackbar показывается после этого.
- `access-token-time-to-live: 30m` (`rfr-auth/application.yaml`), refresh-токена у фронта нет. rfr-api проверяет JWT локально, отзыв токена до `exp` не действует (корневой CLAUDE.md).

## 1. Ключевое решение: как событие меняет экран

Требования §3 и §6 говорят: экран после события = экран после перезагрузки (порядок, поиск, страница, «не больше одного раза», итоговое состояние после серии). Варианты:

- **A. Событие — сигнал «перезапроси» (рекомендуется).** Подписка приносит минимум (`userId`, новый `friendStatus` с точки зрения получателя), клиент перезапрашивает открытый список. Порядок, поиск, страница, отсутствие дублей и итоговое состояние получаются от сервера автоматически — это тот же запрос, что при перезагрузке. Цена — один запрос списка на событие.
- **B. Событие правит кэш Apollo (`cache.modify`).** Клиенту пришлось бы повторить на клиенте серверную сортировку, поиск (по трём полям, подстрока, без учёта регистра) и границы страниц (`hasNextPage` при вставке/удалении), а при пропущенных событиях всё равно перезапрашивать. Ошибкоопасно, не даёт гарантий §6.

План дальше написан под вариант A. Тип события — **не `User`**, а отдельный тип: если вернуть `User`, Apollo нормализует его в `User:{id}` и сразу перерисует кнопки в строке (например, Add в строке вкладки Friends до того, как перезапрос уберёт строку) — промежуточное состояние, которого нет после перезагрузки (§3).

## 2. Шаги бэкенда (rfr-api)

### B1. Транспорт: GraphQL over WebSocket

- Добавить `org.springframework.boot:spring-boot-starter-websocket` в `rfr-api/build.gradle`.
- `application.yaml`: `spring.graphql.websocket.path`. **[решение]** путь:
  - `/subscriptions` — отдельный путь, правило безопасности узкое и явное, в DevTools/Playwright WebSocket легко отличить по URL;
  - `/graphql` — как в примерах Spring Boot, один URL на всё; но тогда `GET /graphql` надо открыть в filter chain.
- Протокол — `graphql-transport-ws` (единственный, который поддерживает Spring for GraphQL). SSE не берём: Apollo Client из коробки его не умеет.
- Keep-alive (ping) — свойство `spring.graphql.websocket.keep-alive`, если оно есть в Boot 4 **[сверить]**; нужно, чтобы обрыв обнаруживался, а не висел полуоткрытый сокет (§5 «обрыв связи»).
- Проверка origin при handshake: фронт на `:3001`, API на `:8081` — кросс-доменное соединение. **[сверить]**, что handshake с `Origin: http://localhost:3001` проходит, а с чужого — нет (CORS `CorsCustomizer` на WebSocket handshake сам не распространяется).

**Как проверить:** `./gradlew :rfr-api:bootRun`; `websocat -H 'Sec-WebSocket-Protocol: graphql-transport-ws' -H 'Origin: http://localhost:3001' ws://localhost:8081/<путь>` — соединение открывается (HTTP 101). Без `Origin` фронта / с чужим — отказ.

### B2. Авторизация подписки

- Браузер не может поставить заголовок `Authorization` на WebSocket, поэтому токен идёт в payload `connection_init`: `{"Authorization": "Bearer <access_token>"}`.
- Handshake (`GET <путь>`) открыть в `RangifflerApiConfiguration.securityFilterChain` (`permitAll`), аутентификацию делать на уровне GraphQL-сессии: `AuthenticationWebSocketInterceptor` + `BearerTokenAuthenticationExtractor` из Spring for GraphQL **[сверить]** имена и пакет для WebMVC в 2.x, `AuthenticationManager` = `JwtAuthenticationProvider(jwtDecoder)` с тем же `jwtAuthenticationConverter()`, что в HTTP. Без токена / с невалидным / с `id_token` (нет аудитории `rangiffler-api`) — сервер закрывает соединение (`4401 Unauthorized`), подписки не создаются.
- На контроллере подписки — `@PreAuthorize("hasAuthority('read')")` (rfr-api/CLAUDE.md: аннотация — единственная проверка). **[сверить]**, что method security видит `SecurityContext` WebSocket-сессии и для метода, возвращающего `Flux`.
- Подписка **без аргументов**: username берётся из `sub` токена соединения. Так нельзя подписаться на чужую дружбу (§5).
- **Истечение токена (§5).** JWT проверяется один раз, на `connection_init`; без доработки соединение жило бы дольше токена. Сохранить `exp` в атрибутах сессии и завершать поток подписки в момент `exp` (например, `takeUntilOther(Mono.delay(exp - now))` с ошибкой) или закрывать сессию кодом `4401`. Поведение клиента при этом — см. «Открытые вопросы», В3.

**Как проверить:** скриптом/`websocat`: (1) `connection_init` без токена → close `4401`; (2) с `id_token` → `4401`; (3) с access-токеном → `connection_ack`; (4) с токеном без `read` (если такой можно получить) → ошибка подписки `FORBIDDEN`; (5) токен с коротким `exp` (временно уменьшить `access-token-time-to-live` в локальном запуске) → соединение/подписка закрываются в момент `exp`.

### B3. Схема

- Новый файл `rfr-api/src/main/resources/graphql/subscription.graphqls` (не трогаем форму `query.graphqls`, rfr-api/CLAUDE.md):

  ```graphql
  type Subscription {
      friendshipChanged: FriendshipChange!
  }

  type FriendshipChange {
      user: ID!
      friendStatus: FriendStatus!
  }
  ```

  `user` — id второй стороны, `friendStatus` — её статус относительно **подписчика** после события (как `User.friendStatus` в `users`). Действие (`ADD`/`DELETE`…) не передаём: отмена заявки и удаление из друзей — одна мутация `DELETE`, а клиенту для перезапроса оно не нужно.
- Codegen сгенерирует `io.student.rangiffler.model.types.FriendshipChange`; руками не писать.

**Как проверить:** `./gradlew :rfr-api:build` — класс `FriendshipChange` в `build/generated/sources/dgs-codegen`; после старта `/graphiql` → Docs показывает `Subscription.friendshipChanged`; запрос `query.graphqls` не изменился (`git diff` пустой по нему).

### B4. Публикация событий после коммита

- Доменное событие (record, например `data/event/FriendshipChangedEvent`): actor id/username, вторая сторона id/username, итоговое состояние пары (`PENDING` с направлением / `ACCEPTED` / нет строки).
- `UserServiceImpl`: в `addFriend`, `acceptInvitation`, `declineInvitation`, `removeFriend` после успешного `flush` — `ApplicationEventPublisher.publishEvent(...)`. При `FriendshipActionException` события нет.
- Отдельный бин (`FriendshipEventBroadcaster`) с `@TransactionalEventListener(phase = AFTER_COMMIT)` кладёт событие в `Sinks.Many` (multicast, без буфера для опоздавших подписчиков). **Только после коммита**: иначе клиент второй стороны может перезапросить список раньше коммита и увидеть старое состояние (нарушит §6 «итоговое состояние»).
- `emitNext` вызывается из разных HTTP-потоков одновременно — эмиссию сериализовать (`EmitFailureHandler` с повтором на `FAIL_NON_SERIALIZED` или `synchronized`), иначе событие молча теряется.
- Ограничение: in-memory sink работает в одном экземпляре rfr-api. При выносе дружбы в отдельный сервис (README, gRPC) понадобится брокер — вне рамок этой задачи (см. В10).

**Как проверить:** лог/`websocat`-подписка двух пользователей: на каждое из 4 действий оба получают по одному `next`; при ошибочном действии (например, `ACCEPT` отправителем → `BAD_REQUEST`) событий нет; событие приходит после ответа на `SELECT` из БД с новым состоянием (`docker exec rfr-mysql mysql ... select * from friendship`).

### B5. Контроллер подписки

- `controller/FriendshipSubscriptionController`: `@Controller`, `@PreAuthorize("hasAuthority('read')")`, `@SubscriptionMapping Flux<FriendshipChange> friendshipChanged(@AuthenticationPrincipal Jwt principal)`.
- Поток: `sink.asFlux()` → фильтр «событие касается `sub`» (actor или вторая сторона) → маппинг в `FriendshipChange` с `friendStatus` с точки зрения подписчика:

  | Действие | Подписчику-актору (`user` = B) | Подписчику B (`user` = A) |
  |---|---|---|
  | ADD | `INVITATION_SENT` | `INVITATION_RECEIVED` |
  | DELETE (отмена заявки) | `NOT_FRIEND` | `NOT_FRIEND` |
  | ACCEPT | `FRIEND` | `FRIEND` |
  | REJECT | `NOT_FRIEND` | `NOT_FRIEND` |
  | DELETE (из друзей) | `NOT_FRIEND` | `NOT_FRIEND` |

  Расчёт держать рядом с `calculateFriendStatus` (одна логика статуса).
- Окна самого актора получают событие так же, как B (§2, §4): фильтр по username, а не «кроме актора».
- Завершение по `exp` — из B2.

**Как проверить:** две подписки (A и B) + третья (C, посторонний): на действия A↔B C не получает ничего (§5); у A и B — статусы по таблице выше. Две подписки A (два соединения) — обе получают событие.

### B6. Ошибки подписки

- Ошибки внутри `Flux` не идут через `DataFetcherExceptionResolver`: добавить `SubscriptionExceptionResolverAdapter` с тем же отображением, что `GraphQlExceptionResolver` (`ResourceNotFoundException` → `NOT_FOUND`, `FriendshipActionException`/`IllegalArgumentException` → `BAD_REQUEST`, остальное → `INTERNAL_ERROR` без деталей) — корневой CLAUDE.md, «Errors».

**Как проверить:** временно бросить исключение в маппинге — клиент получает `error` с `extensions.classification` = `INTERNAL_ERROR` без текста исключения.

### B7. API-тесты подписки (rfr-e2e, без браузера)

Не UI-тесты (`@Tag("ui")` не ставить): клиент `graphql-transport-ws` на `java.net.http.WebSocket` в `network/` (рядом с `GraphQl`), пользователи через существующие `@User`. Покрывают B2–B5: доступ (без токена, `id_token`, чужие события), статусы по таблице B5, событие только после успешной мутации. Это быстрее и стабильнее, чем ловить те же вещи через UI.

## 3. Шаги фронтенда (rfr-gql-client)

### F1. Зависимость

- `graphql-ws` (опциональная peer-зависимость `@apollo/client`, в lock-файле только как peer). Установить ровно её; затем проверить `git diff package-lock.json` на лишние строки `"peer": true` (rfr-gql-client/CLAUDE.md) и что `npm ci` проходит.

**Как проверить:** `npm ci && npm run build && npm run lint` зелёные.

### F2. Apollo: split-link и WebSocket-клиент

- `src/api/apolloClient.ts`: `createClient` из `graphql-ws` + `GraphQLWsLink` (`@apollo/client/link/subscriptions`), `split` по типу операции: подписки → WS, остальное → текущая цепочка `headerLink` → `HttpLink`. `errorLink` оставить общим.
- URL: из `VITE_API_URL` заменой `http`→`ws` + путь из B1, или новая переменная `VITE_API_WS_URL` **[решение]** (новая переменная — явнее, но её надо добавить в `.env` и в стенд/CI).
- `connectionParams` — **функция**, читающая `accessTokenFromLocalStorage()` при каждом (пере)подключении: после нового входа берётся новый токен.
- `lazy: true` (по умолчанию): соединение открывается при первой подписке и закрывается после последней — WebSocket есть только пока открыта People (§3 «другая страница — ничего не показывается»).
- Повторы: `retryAttempts` / `shouldRetry` — переподключаться при обрыве; **не** переподключаться при `4401`/`4403` (токен истёк/невалиден). Сколько ждать и сколько раз пробовать — В9.
- Экспортировать `wsClient` (для слушателя `connected` в F4 и закрытия при выходе в F6).

**Как проверить:** `npm run dev`, People → DevTools → Network → WS: одно соединение на путь из B1, кадры `connection_init` (с `Authorization`), `connection_ack`, `subscribe` с `operationName: "FriendshipChanged"`; уход на My travels — соединение закрывается (после `lazyCloseTimeout`); `GetPeople` и прочие запросы по-прежнему идут по HTTP.

### F3. Хук подписки

- `src/hooks/useFriendshipChanged.ts` (одна операция — один хук, rfr-gql-client/CLAUDE.md), операция `subscription FriendshipChanged { friendshipChanged { user friendStatus } }`, `useSubscription` с `onData` → `refreshPeople()` из F4.
- Вызывается в `PeoplePage` (живёт, пока открыта People, независимо от вкладки), не в таблицах: при смене вкладки подписка не пересоздаётся.
- Ошибки подписки на экран не выводить (§3 «ничего нового на экране»): только `console.error` в DEV, как в `errorLink`.

**Как проверить:** два браузера, A и B на People; A жмёт Add у B → в WS-кадрах B приходит `next` с `user` = id A, `friendStatus: "INVITATION_RECEIVED"`; у A во втором окне — `INVITATION_SENT`.

### F4. Перезапрос открытого списка (вариант A из раздела 1)

- `refreshPeople()` (например, в том же хуке): перезапросить **активные** запросы People — `client.refetchQueries({include: ["GetPeople", "GetFriends", "GetInvitations", "GetOutcomeInvitations"]})` (тот же список, что `PEOPLE_QUERIES` в `useUpdateFriendshipStatus` — вынести в общее место, чтобы не дублировать). Перезапрос идёт с текущими `page`/`searchQuery` таблицы, поэтому вкладка, поиск и страница сохраняются, а порядок, фильтр поиска и границы страниц — серверные (§3).
- Сериализация и склейка: не больше одного перезапроса в полёте; события, пришедшие во время него, ставят один «хвостовой» перезапрос после него. Так последний ответ всегда отправлен после последнего полученного события → экран = итоговое состояние (§6, К10), и устаревший ответ не перекрывает свежий. Небольшая задержка склейки (100–300 мс) допустима — бюджет 2 с.
- Переподключение: `wsClient.on("connected", (_socket, _payload, wasRetry) => wasRetry && refreshPeople())` — события, пропущенные во время обрыва, восстанавливаются перезапросом; список заменяется целиком, дублей нет (§5, §6, К13). Слушатель снимать при размонтировании People.
- Неактивные вкладки: их таблицы размонтированы, при открытии вкладки хук сам делает сетевой запрос (`cache-and-network`), но **сначала показывает старые данные из кэша** — см. В6. Если ответ «старый кадр недопустим» — в `refreshPeople()` дополнительно `cache.evict` полей неактивных списков (`ROOT_QUERY.users`, `User:{me}.friends/incomeInvitations/outcomeInvitations`) + `cache.gc()`, не трогая активный.

**Как проверить:** B на Income invitations, страница 1, A шлёт заявку → в Network у B один `GetInvitations` с теми же `page`/`searchQuery`, строка A на месте по алфавиту; серия «Add → Cancel» у A — у B не больше двух `GetInvitations`, итог — пустая вкладка; DevTools → Network → Offline на 10 с, A шлёт заявку, Online → после переподключения один `GetInvitations`, строка A один раз.

### F5. Тихий перезапрос (без мигания)

- В Apollo Client 4 `notifyOnNetworkStatusChange` по умолчанию `true`: при `refetch` `loading` становится `true`. `PeopleTable` показывает спиннер при `loading && !data.length` — на пустой вкладке фоновый перезапрос заменит «There are no users yet» спиннером на время запроса. Это новое на экране (§3).
- В хуках People не считать загрузкой `networkStatus === NetworkStatus.refetch`, вызванный событием (или отдавать из хука отдельный флаг «первичная загрузка»). Не сломать существующий спиннер при смене страницы/поиска (`setVariables`) и `onSearchSubmit` → `refetch()`.

**Как проверить:** B на пустой Income invitations, A шлёт и отменяет заявку — спиннер не появляется (видео/trace Playwright); переключение страниц и поиск показывают спиннер, как раньше.

### F6. Окно актора и выход

- Окно, где нажата кнопка: строка меняется от ответа мутации и её `refetchQueries`, как сейчас. Собственное событие этого окна тоже вызовет `refreshPeople()` — ответ совпадёт с уже показанным, видимых изменений нет. Нужно ли пропускать собственные события окна — В4.
- Выход (`MenuAppBar.onLogoutClick`): перед `window.location.replace` вызвать `wsClient.terminate()` (или `dispose()`), чтобы окно гарантированно не получило событие между выходом и уходом со страницы. Другие окна/контексты не затрагиваются: у каждого своё соединение со своим токеном (§5, как в задаче 2).
- Закрытие с `4401`/`4403` (истёк токен) — по ответу на В3: либо как HTTP 401 в `errorLink` (`clearSession()` + редирект на `/`), либо молча прекратить подписку до следующего запроса.

**Как проверить:** два окна A; кнопка во втором — в первом строка обновилась, во втором без мигания. Выход в окне 1 браузера B → в WS окна 1 нет активного соединения; во втором браузере B события приходят.

## 4. Как проверять e2e (rfr-e2e) — общая часть

- Двух пользователей в разных контекстах дают существующие `Page page` + `@Session @User UserSession` (как в `FriendshipActionsTest`, `LogoutTest`). Тестовые данные — по skill `rfr-test-data`, Page Object'ы — по `rfr-page-objects`, ревью — `rfr-test-review`.
- **Порядок:** наблюдатель открывает People и нужную вкладку **до** действия (в отличие от текущего `FriendshipActionsTest`, где наблюдатель открывает People после).
- **Готовность подписки:** перед действием актора дождаться, что наблюдатель отправил `subscribe` `FriendshipChanged` (`page.waitForWebSocket(...)` + `waitForFrameSent(...)`, слушатель зарегистрировать до открытия People). Иначе гонка: действие раньше подписки — тест флейкует. Где держать это ожидание (PO/хелпер), решить по правилам `rfr-page-objects` (правило 12 — про подтверждение действий, а это предусловие).
- **«Сразу» = 2 с:** проверку наблюдателя делать с таймаутом 2000 мс, отсчитанным после `checkNotification(...)` актора (нужна перегрузка `PeopleTable.checkUsernames/checkEmpty` / `PeopleRow.check*` с таймаутом).
- **«Без перезагрузки»:** до действия поставить метку `page.evaluate("window.__rfrNoReload = true")`, после проверки убедиться, что она на месте (и/или счётчик `framenavigated` = 0).
- **Обрыв связи:** `page.routeWebSocket(...)` (есть в Playwright 1.63): проксировать к серверу через `connectToServer()`, на «обрыв» закрыть соединение и не пускать новые, на «восстановление» — пускать. `BrowserContext.setOffline(true)` проще, но **[сверить]**, рвёт ли он уже открытый WebSocket в Chromium.
- Негативные проверки «строки нет» проходят и без фичи — у каждой нужен позитивный контроль в том же тесте (см. В7).

## 5. Критерии приёмки → шаги → e2e

| # | Критерий | Шаги | e2e на двух контекстах |
|---|---|---|---|
| К1 | Заявка отправлена → у B на Income строка A с Accept/Decline | B1–B5, F1–F4 | `@User alice` в `page`, `@Session @User bob`. Bob: People → Income (пусто), ждём `subscribe`. Alice: All People → поиск Bob → Add → `INVITATION_SENT`. Bob за 2 с: `checkUsernames([alice])`, `row(alice).checkIncomeInvitation()`, метка «без перезагрузки» цела. |
| К2 | Заявка отменена → у B Income пуста | B4 (DELETE после коммита), F4 | `@Session @User(outcomeInvitations = 1) alice`, invitee в `page`: Income, видит Alice, ждём `subscribe`. Alice: Outcome → Cancel → `INVITATION_CANCELLED`. Invitee за 2 с: `checkEmpty()`. |
| К3 | Заявка принята → у A на Friends появляется B с Remove | B4, B5 (ACCEPT → `FRIEND` обоим), F4 | `@Session @User(outcomeInvitations = 1) alice`: Friends (пусто), ждём `subscribe`. Invitee в `page`: Income → Accept → `INVITATION_ACCEPTED`. Alice за 2 с: `checkUsernames([invitee])`, `row(invitee).checkFriend()`. |
| К4 | Заявка отклонена → у A Outcome пуста | B4, F4 | Как К3, Alice на Outcome (видит invitee); invitee → Decline → `INVITATION_DECLINED`; Alice за 2 с `checkEmpty()`. |
| К5 | Дружба удалена получателем → у A Friends пуста | B4, B5, F4 | **[данные]** нужен признак, кто в паре отправитель: «A отправил, B принял». `@User(friends = 1)` сейчас не говорит, кто requester (см. В8). A (отправитель) — `@Session`, на Friends видит B; B (получатель) в `page` → Remove → `FRIEND_DELETED`; A за 2 с `checkEmpty()`. |
| К6 | То же, удаляет отправитель, смотрит получатель | те же | Зеркально К5: удаляет requester, смотрит addressee. |
| К7 | All People: у A в строке B Accept/Decline вместо Add | B5, F4, F5 | A (`@Session`) на All People видит B с Add. **Проблема данных:** All People — все пользователи БД по 10 на страницу, B может не быть на странице 1 → A сначала ищет B (поиск сохраняется при обновлении, §3), см. В7. B в `page` → Add у A → `INVITATION_SENT`. A за 2 с: `row(B).checkIncomeInvitation()` (Accept, Decline, нет Add). |
| К8 | Окно актора: второй браузер A видит строку B на Outcome | B5 (событие и актору), F3, F4 | Нужны **два контекста одного пользователя**: `@User alice` + вход в `page` и `@Session` той же Alice — фикстуры сейчас так не умеют (В8). Контекст 2: Outcome (пусто), `subscribe`. Контекст 1: Add у B → `INVITATION_SENT`. Контекст 2 за 2 с: `checkUsernames([B])`, `row(B).checkOutcomeInvitation()` («Waiting...», Cancel). |
| К9 | Порядок `anna`, `mike`, `zoe` | F4 (серверный порядок) | Имена должны сортироваться как a < m < z и быть уникальными в общей БД: один случайный префикс + суффиксы `anna`/`mike`/`zoe` (В8). B (`@Session`, `incomeInvitations` от anna и zoe) на Income; mike в `page` → Add у B. B за 2 с: `checkUsernames([anna, mike, zoe])` — проверка порядка. |
| К10 | Быстрая серия: Add + Cancel → через 2 с у B пусто | B4 (после коммита), F4 (сериализация), F5 | B на Income (пусто), `subscribe`. A: Add → `INVITATION_SENT` → сразу Cancel → `INVITATION_CANCELLED`. B: подождать 2 с от второго уведомления, затем `checkEmpty()` (с коротким таймаутом — состояние должно быть уже итоговым). Позитивный контроль: в том же тесте зафиксировать, что строка A у B появлялась (`waitForCondition`/`expect` до Cancel) — иначе тест зелёный и без фичи. |
| К11 | Поиск `zoe`: строки `mike` нет | F4 (перезапрос с `searchQuery`) | B на All People ищет `<prefix>zoe`; mike в `page` → Add у B. B через 2 с: в результатах только zoe, mike нет. Позитивный контроль, иначе критерий проходит без фичи: после mike zoe тоже шлёт B заявку → в строке zoe за 2 с Accept/Decline (В7). |
| К12 | Выход в другом окне: второй браузер B получает событие | B2 (токен на соединение), F6 | Два контекста B (В8). Контекст 1: Logout → гостевая. Контекст 2: Income (пусто), `subscribe`. A (третий контекст или `page`, если B — две `@Session`) → Add → `INVITATION_SENT`. Контекст 2 за 2 с: строка A. Нужны **три** входа — проверить, что фикстуры это позволяют. |
| К13 | Обрыв без дублей: строка A ровно один раз | F2 (повторы), F4 (перезапрос на `connected` после retry) | B на Income, `subscribe`; `routeWebSocket` рвёт соединение и не пускает новое. A → Add → `INVITATION_SENT`. Восстановить соединение → B за 2 с после переподключения: `checkUsernames([A])` (ровно одна строка, `hasCount(1)`), повторно через ~1 с — всё ещё одна. |

Плюс (без отдельного критерия, но из §5): «чужие события не приходят» и «без токена — отказ» — API-тесты B7, а не UI.

## 6. Порядок работ

1. B3 → B4 → B5 → B6 (схема, события, контроллер) — проверка через `websocat` с токеном из `localStorage`.
2. B1–B2 (транспорт, авторизация, `exp`), API-тесты B7.
3. F1–F3 (клиент, подписка) — проверка в DevTools.
4. F4–F6 (перезапрос, тишина, выход).
5. e2e по таблице раздела 5; инфраструктура тестов (ожидание `subscribe`, таймаут 2 с, метка перезагрузки, `routeWebSocket`, фикстуры из В8) — до самих тестов.

## 7. Открытые вопросы

- **В1. Изменение схемы.** rfr-api/CLAUDE.md: «не менять форму `query.graphqls`, фронт собран под неё». План добавляет тип `Subscription` в отдельный файл, `query.graphqls` не трогает, но схема API всё же расширяется. Разрешено ли это (и в отдельном ли файле)?
- **В2. «Окно» = вкладка браузера или контекст.** Вкладки одного браузера делят `localStorage`: выход в одной вкладке стирает токен у всех вкладок этого браузера. §5 «выход в одном окне не прекращает получение событий в других» выполним только для разных браузеров/контекстов (как в задаче 2 и в критериях, где «A и B в разных браузерах»). В другой вкладке того же браузера уже открытое соединение продолжит получать события до переподключения, а следующий запрос пойдёт без токена. Считаем ли «окна» в §4–§5 разными браузерами/контекстами?
- **В3. Истечение токена.** «События перестают приходить так же, как перестают работать запросы»: сейчас запрос с истёкшим токеном → 401 → `clearSession()` и редирект на `/`. Должно ли закрытие подписки по `exp` (сервер, `4401`) тоже разлогинивать и уводить на `/` сразу, или только тихо прекращать события до следующего запроса пользователя? Отдельно: согласны ли, что сервер сам закрывает подписку в момент `exp` (без этого соединение переживает токен)?
- **В4. «Повторного изменения от события нет» (§4).** При варианте A окно актора тоже перезапросит список на своё событие; данные совпадут, видимых изменений нет (при F5). Достаточно ли «нет видимого изменения», или окно должно вообще игнорировать собственные события (тогда нужен идентификатор окна, передаваемый с мутацией и возвращаемый в событии, — это меняет и мутацию)?
- **В5. Прокрутка (§3).** При вставке/удалении строки выше видимой области `scrollY` сохранится, но видимые строки сдвинутся; при укорочении страницы браузер сам уменьшит `scrollY`. Что требуется: неизменный `scrollY` или «якорь» на видимых строках? Как это проверять в e2e?
- **В6. «При переходе на вкладку список загружается актуальным».** Сейчас (`cache-and-network`) при открытии вкладки сначала на мгновение показываются данные из кэша, потом свежие. Это допустимо, или старый кадр недопустим (тогда `cache.evict` неактивных списков в F4 или смена `fetchPolicy`, что меняет поведение всех вкладок)? То же касается окна, которое было на другой странице приложения: подписки там нет, кэш People может быть старым.
- **В7. Проверяемость критериев.** К11 («строки mike нет») и К10 («пусто») истинны и без фичи — нужен позитивный контроль в тесте; согласовать, что добавляем его (zoe шлёт заявку в К11, «строка появлялась» в К10). К7: All People — все пользователи общей БД по 10 на страницу; без поиска B может не попасть на первую страницу. Можно ли в «Дано» К7 добавить поиск B? К11: «подходит под поиск» — серверная семантика (подстрока username/firstname/surname без учёта регистра)?
- **В8. Тестовые данные.** (а) К8, К12 — один пользователь в двух-трёх контекстах: сейчас `@Session @User` создаёт нового пользователя на каждый параметр. (б) К5/К6 — нужен признак, кто в дружбе был отправителем. (в) К9/К11 — имена с заданным порядком (`anna` < `mike` < `zoe`) и уникальностью в общей БД. Нужны новые возможности `@User`/фикстур (skill `rfr-test-data`) — делаем в этой задаче?
- **В9. Обрыв связи (§5, К13).** Какой обрыв имеется в виду: только WebSocket или вся сеть (HTTP тоже)? Сколько длится и сколько клиенту пытаться переподключиться (бесконечно с backoff или N попыток, а потом?) Действует ли бюджет 2 с от момента восстановления связи?
- **В10. Масштаб.** In-memory `Sinks.Many` работает только с одним экземпляром rfr-api. При распиле на микросервисы (README) события между сервисами потребуют брокера/стрима по gRPC. Достаточно ли сейчас одного экземпляра?
- **В11. Поведение при ошибке подписки.** §3 запрещает новое на экране. Если подписка упала с `INTERNAL_ERROR` (не обрыв сети) — молча переподписываться, молча прекратить или показать ошибку (это было бы новым элементом)? План предполагает «молча, только лог в DEV».
