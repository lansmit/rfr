#!/bin/bash
# PostToolUse hook: after the agent edits a Java or frontend file, compile/check that module.
# Exit 2 sends stderr back to the agent so it fixes the error in the same turn; exit 0 stays silent.

root="${CLAUDE_PROJECT_DIR:-$(pwd)}"
file=$(jq -r '.tool_input.file_path // empty')
rel="${file#"$root"/}"

fail() {
  echo "$1" >&2
  echo "$2" | tail -n 60 >&2
  exit 2
}

case "$rel" in
  rfr-api/*.java | rfr-auth/*.java | rfr-e2e/*.java)
    module="${rel%%/*}"
    task=compileJava
    [[ "$rel" == */src/test/* ]] && task=compileTestJava
    if ! out=$(cd "$root" && ./gradlew -q --console=plain ":$module:$task" 2>&1); then
      # Gradle repeats the compiler errors in its FAILURE block; keep the block only when there's nothing before it
      errors=$(sed '/^FAILURE: Build failed/,$d' <<< "$out")
      fail ":$module:$task failed after editing $rel:" "${errors:-$out}"
    fi
    ;;

  rfr-gql-client/src/*.ts | rfr-gql-client/src/*.tsx)
    front="$root/rfr-gql-client"
    [[ -x "$front/node_modules/.bin/tsc" ]] || exit 0
    out=$(cd "$front" && node_modules/.bin/tsc --noEmit 2>&1) \
      || fail "tsc failed after editing $rel:" "$out"
    out=$(cd "$front" && node_modules/.bin/eslint --max-warnings 0 "${rel#rfr-gql-client/}" 2>&1) \
      || fail "eslint failed on $rel:" "$out"
    ;;
esac

exit 0
