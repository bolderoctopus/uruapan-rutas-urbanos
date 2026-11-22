# CLAUDE.md

Custom instructions for Claude Code in this repository.

## Project overview

Rutas Uruapan — Android app for public transit routes in Uruapan. The Android project lives in `RutasUruapan/`.

## Instructions
At the beginning of each session always greet me with "Hi there Sir, 🐤"

## Current work

Repo foundations (testing, CI, linting, conventions) are tracked in [docs/repo-foundations-plan.md](docs/repo-foundations-plan.md). Read it before working on any of those areas, and mark a phase as done in the same PR that completes it.

## Conventions

Branching, branch names, commit and PR title format, merge strategy and releases are in [CONTRIBUTING.md](CONTRIBUTING.md). Follow them for every branch, commit and PR.

## Build & test

Run from `RutasUruapan/`:

- `./gradlew assembleDebug`: build the debug APK
- `./gradlew testDebugUnitTest`: run unit tests (JUnit4, MockK, Turbine, Robolectric)
- `./gradlew koverHtmlReportDebug` / `koverLogDebug`: test coverage (Kover). Generated code is excluded in `app/build.gradle`.
- `./gradlew ktlintCheck detekt lintDebug`: lint checks. `./gradlew ktlintFormat` auto-fixes formatting. Style comes from the root `.editorconfig`.
- A pre-commit hook runs `ktlintCheck` on commits that touch Kotlin. If it fails, run `./gradlew ktlintFormat` and commit again. Don't bypass it with `--no-verify`.
- New lint findings must be fixed, not added to the baselines (`config/detekt/baseline.xml`, `app/lint-baseline.xml`), unless there is a stated reason.
- The Maps/Places key comes from `RutasUruapan/apikeys.properties` (`mapsAndPlaces=KEY`, gitignored), as described in the README.

## Definition of done

Every change, by a person or an agent, is done when:

- A unit test covers it, and for a bug fix that test failed before the fix. When a change can't be unit tested (layouts, map drawing), the PR says so and lists what to check on a device.
- `./gradlew ktlintCheck detekt lintDebug testDebugUnitTest assembleDebug` passes, with no new baseline entries.
- Changes users will notice have a Spanish line under "Sin publicar" in `CHANGELOG.md`.
- The PR covers one ticket or topic, with no unrelated changes and no dependency, build or CI changes the task doesn't need.
- The PR is open with CI green. Agents never merge it.

## Backlog tickets

The backlog is the **Tasks** database in Notion (Proyectos de software → Rutas Uruapan). `/ticket RU-123` works on one ticket, from reading it to an open PR: the skill in `.claude/skills/ticket/SKILL.md` handles Notion and the user, and hands the coding to the `ticket-implementer` subagent in `.claude/agents/`. Statuses: Not started → Ready → In progress → In review → Done, plus Needs info when the ticket can't be done as written.
