#!/usr/bin/env python3
# PreToolUse hook for Bash: denies push and file deletion however the command is spelled.
# Prefix rules in permissions.deny miss `git -C . push`, `git 'push'`, `/usr/bin/rm`, `unlink`, `find -delete`:
# they compare text, so this parses the command the way the shell would and looks at what actually runs.

import json
import os
import re
import shlex
import sys

SEPARATORS = {";", "&", "&&", "|", "||", "\n", "(", ")"}
WRAPPERS = {"sudo", "env", "command", "exec", "nohup", "time", "nice", "builtin", "xargs"}
# wrapper options that take the next token as their value: without them `sudo -u root rm` looks like the command `root`
WRAPPER_OPTS_WITH_ARG = {
    "sudo": {"-u", "-g", "-h", "-p", "-C", "-D", "-r", "-t", "-U", "-T",
             "--user", "--group", "--host", "--prompt", "--close-from", "--chdir", "--chroot",
             "--role", "--type", "--other-user", "--command-timeout"},
    "env": {"-u", "-C", "-P", "--unset", "--chdir"},
    "nice": {"-n", "--adjustment"},
    "time": {"-o", "-f", "--output", "--format"},
    "exec": {"-a"},
    "xargs": {"-I", "-J", "-L", "-n", "-P", "-s", "-E", "-d", "-a", "-R", "-S",
              "--max-args", "--max-procs", "--max-chars", "--delimiter", "--arg-file", "--process-slot-var"},
}
# env -S splits its value into words and runs them, so the value is a command line of its own
SPLIT_STRING_OPTS = {"-S", "--split-string"}
SHELLS = {"sh", "bash", "zsh", "dash"}
DELETERS = {"rm", "unlink", "rmdir", "shred"}
# git global options that take the next token as their value
GIT_OPTS_WITH_ARG = {"-C", "-c", "--git-dir", "--work-tree", "--namespace", "--config-env", "--super-prefix"}

NO_DELETE = "Deleting files is denied in this project: rename to <name>.bak instead (mv file file.bak) and tell the user."
NO_PUSH = "git push is denied for the agent: the user pushes."


def tokenize(command):
    lex = shlex.shlex(command, posix=True, punctuation_chars="();<>|&\n")
    lex.whitespace = " \t\r"
    lex.whitespace_split = True
    lex.commenters = ""
    return list(lex)


def segments(tokens):
    current = []
    for tok in tokens:
        if tok in SEPARATORS:
            if current:
                yield current
            current = []
        else:
            current.append(tok)
    if current:
        yield current


def split_string_value(argv, i):
    """Value of env's -S at argv[i] and how many tokens it took, or None if argv[i] isn't -S."""
    tok = argv[i]
    if tok in SPLIT_STRING_OPTS:
        return (argv[i + 1], 2) if i + 1 < len(argv) else ("", 1)
    if tok.startswith("--split-string="):
        return tok.partition("=")[2], 1
    if tok.startswith("-S"):
        return tok[2:], 1
    return None


def strip_prefix(argv):
    i = 0
    while i < len(argv):
        tok = argv[i]
        if re.match(r"^[A-Za-z_][A-Za-z0-9_]*=", tok):
            i += 1
        elif os.path.basename(tok) in WRAPPERS:
            wrapper = os.path.basename(tok)
            opts_with_arg = WRAPPER_OPTS_WITH_ARG.get(wrapper, set())
            i += 1
            while i < len(argv) and (argv[i].startswith("-") or re.match(r"^[A-Za-z_][A-Za-z0-9_]*=", argv[i])):
                if wrapper == "env":
                    split = split_string_value(argv, i)
                    if split:
                        value, consumed = split
                        return strip_prefix(tokenize(value) + argv[i + consumed:])
                i += 2 if argv[i] in opts_with_arg else 1
        else:
            break
    return argv[i:]


def check_git(args):
    i = 0
    while i < len(args) and args[i].startswith("-"):
        i += 2 if args[i] in GIT_OPTS_WITH_ARG else 1
    if i >= len(args):
        return None
    sub, rest = args[i], args[i + 1:]
    if sub == "push":
        return NO_PUSH
    # dry runs and untracking (--cached) leave files on disk
    dry = "--dry-run" in rest or any(re.match(r"^-[a-zA-Z]*n", a) for a in rest if not a.startswith("--"))
    if sub == "rm" and not (dry or "--cached" in rest):
        return NO_DELETE
    if sub == "clean" and not dry:
        return NO_DELETE
    return None


def check_find(args):
    if "-delete" in args:
        return NO_DELETE
    for i, a in enumerate(args[:-1]):
        if a in {"-exec", "-execdir", "-ok", "-okdir"} and os.path.basename(args[i + 1]) in DELETERS:
            return NO_DELETE
    return None


def check(command, depth=0):
    if depth > 3:
        return None
    try:
        tokens = tokenize(command)
    except ValueError:
        # unbalanced quotes (e.g. an unquoted heredoc with an apostrophe): fall back to a coarse text check
        if re.search(r"\bgit\b[^;&|\n]*\bpush\b", command):
            return NO_PUSH
        if re.search(r"(^|[;&|\s/])(rm|unlink|shred)\s", command) or re.search(r"\s-delete\b", command):
            return NO_DELETE
        return None
    for seg in segments(tokens):
        argv = strip_prefix(seg)
        if not argv:
            continue
        cmd, args = os.path.basename(argv[0]), argv[1:]
        if cmd in DELETERS:
            return NO_DELETE
        if cmd == "git":
            reason = check_git(args)
        elif cmd == "find":
            reason = check_find(args)
        elif cmd in SHELLS and "-c" in args and args.index("-c") + 1 < len(args):
            reason = check(args[args.index("-c") + 1], depth + 1)
        elif cmd == "eval":
            reason = check(" ".join(args), depth + 1)
        else:
            reason = None
        if reason:
            return reason
    return None


def main():
    data = json.load(sys.stdin)
    reason = check(data.get("tool_input", {}).get("command", ""))
    if reason:
        print(json.dumps({
            "hookSpecificOutput": {
                "hookEventName": "PreToolUse",
                "permissionDecision": "deny",
                "permissionDecisionReason": reason,
            }
        }))


if __name__ == "__main__":
    main()
