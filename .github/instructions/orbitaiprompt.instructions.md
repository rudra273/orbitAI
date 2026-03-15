---
description: Core rules for working on the orbitAI Android project. Always load these rules.
applyTo: "**"
---

# OrbitAI Project Rules

## General Behavior

- **Do exactly what is asked.** Do not overthink. Do not add unrequested features, refactors, comments, or abstractions.
- **Keep it simple.** If a goal can be achieved simply, do not introduce unnecessary complexity.
- **Use standard practices.** Follow idiomatic Android/Kotlin conventions — no creative workarounds.
- **Always use the latest version of the code in the workspace.** Never reference or restore old/stale code.

## When Blocked — Stop and Ask

- **Version conflicts:** If a dependency version does not match or causes a conflict, stop immediately and report the exact error. Do not attempt to resolve it yourself. Ask the user for the correct version.
- **Missing URLs:** If a URL is needed anywhere in the app (API endpoint, base URL, image URL, etc.) and it is not already in the codebase, stop and ask the user to provide it. Do not guess or fabricate URLs.
- **Critical decisions:** Do not make critical architectural or design decisions independently. Present the problem clearly and ask the user to decide.

## No Workarounds

- Never use a workaround to bypass a problem. If something is broken or unclear, surface the exact error or question to the user.
- Never use `--force`, `--no-verify`, suppressions, or hacks to get past a failing step.

## Avoid Infinite Loops

- If a task is not progressing after one or two attempts, stop. Report what is failing and ask the user how to proceed. Do not retry the same failing approach repeatedly.