# Проверка хуков *(занятие 2)*

Агент (Claude Code) по просьбе внёс заведомо некорректные правки и запускал запрещённые команды. Ниже — дословный вывод хуков, который он получил, 2026-10-25. Все правки после проверки откачены.

## Хук после правки: `check-changed-file.sh`

PostToolUse на `Edit|MultiEdit|Write` ([`.claude/hooks/check-changed-file.sh`](../.claude/hooks/check-changed-file.sh)). Ошибка возвращается агенту как blocking error, и он исправляет её в том же ходе.

### Java: ошибка компиляции

Правка в `rfr-api/src/main/java/io/student/rangiffler/RangifflerApiApplication.java`:

```java
int port = "8081";
```

Вывод хука:

```
PostToolUse:Edit hook blocking error from command: ""$CLAUDE_PROJECT_DIR"/.claude/hooks/check-changed-file.sh":
:rfr-api:compileJava failed after editing rfr-api/src/main/java/io/student/rangiffler/RangifflerApiApplication.java:
/Users/macbook/rfr/rfr-api/src/main/java/io/student/rangiffler/RangifflerApiApplication.java:10: error: incompatible types: String cannot be converted to int
    int port = "8081";
               ^
1 error
```

### TSX: ошибка типов (`tsc`)

Правка в `rfr-gql-client/src/App.tsx` — prop, которого у компонента нет:

```tsx
<AppContent title={42}/>
```

Вывод хука:

```
PostToolUse:Edit hook blocking error from command: ""$CLAUDE_PROJECT_DIR"/.claude/hooks/check-changed-file.sh":
tsc failed after editing rfr-gql-client/src/App.tsx:
src/App.tsx(13,29): error TS2322: Type '{ title: number; }' is not assignable to type 'IntrinsicAttributes'.
  Property 'title' does not exist on type 'IntrinsicAttributes'.
```

### TSX: нарушение правила ESLint

Правка в `rfr-gql-client/src/App.tsx`, которая проходит `tsc`, но нарушает `no-debugger`. Отдельная правка нужна потому, что при ошибке `tsc` хук до ESLint не доходит:

```tsx
const App = () => {
    debugger;
```

Вывод хука:

```
PostToolUse:Edit hook blocking error from command: ""$CLAUDE_PROJECT_DIR"/.claude/hooks/check-changed-file.sh":
eslint failed on rfr-gql-client/src/App.tsx:

/Users/macbook/rfr/rfr-gql-client/src/App.tsx
  9:5  error  Unexpected 'debugger' statement  no-debugger

✖ 1 problem (1 error, 0 warnings)
```

## Хук перед командой: `block-push-and-delete.py`

PreToolUse на `Bash` ([`.claude/hooks/block-push-and-delete.py`](../.claude/hooks/block-push-and-delete.py)). Зачем он нужен и как устроен — в корневом [README](../README.md), раздел «AI-агент».

До хука правила `deny` (`Bash(git push *)`, `Bash(rm *)`) пропускали эти команды — агент их выполнил:

| Команда | Без хука |
|---|---|
| `git -c push.default=current push --dry-run` | выполнилась: `* [new branch] hw/02-env-agent -> hw/02-env-agent` |
| `git --git-dir=.git push --dry-run` | выполнилась, остановил только git: нет upstream |
| `git 'push' --dry-run` | выполнилась, остановил только git: нет upstream |
| `git -C . push --dry-run` | выполнилась, остановил только git: нет upstream |
| `git rm --dry-run -- ./test.txt` | выполнилась; без `--dry-run` удалила бы файл (хук `--dry-run` пропускает, `git rm` без него — нет) |
| `unlink ./__permission_test_missing_7f3a9__.tmp` | выполнилась (файла не было) |

С хуком — вывод при реальных вызовах агента:

```
$ unlink ./__nonexistent_check__.tmp
PreToolUse:Bash hook error: Deleting files is denied in this project: rename to <name>.bak instead (mv file file.bak) and tell the user.
```

```
$ GIT_CONFIG_COUNT=1 … git -c push.default=current push --dry-run …
PreToolUse:Bash hook error: git push is denied for the agent: the user pushes.
```

Второй вызов — проверка самого агента сразу после подключения хука: в команде был `git push`, и хук её не пропустил.

Перед подключением хук прогнан на 29 командах, передавая ему тот же JSON, что и Claude Code: все 18 обходов заблокированы, 11 обычных команд (`git stash push -m wip`, `git commit -m 'fix push button'`, `git rm --cached x`, `git clean -n`, `find … -print`, `mv a a.bak` и др.) проходят.
