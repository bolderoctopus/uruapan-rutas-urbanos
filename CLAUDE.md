# CLAUDE.md

Custom instructions for Claude Code in this repository.

## Project overview

Rutas Uruapan — Android app for public transit routes in Uruapan. The Android project lives in `RutasUruapan/`.

## Instructions
At the beginning of each session always greet me with "Hi there Sir, 🐤"

## Current work

Repo foundations (testing, CI, linting, conventions) are tracked in [docs/repo-foundations-plan.md](docs/repo-foundations-plan.md). Read it before working on any of those areas, and mark a phase as done in the same PR that completes it.

## Build & test

Run from `RutasUruapan/`:

- `./gradlew assembleDebug`: build the debug APK
- `./gradlew testDebugUnitTest`: run unit tests (JUnit4, MockK, Turbine, Robolectric)
- `./gradlew ktlintCheck detekt lintDebug`: lint checks. `./gradlew ktlintFormat` auto-fixes formatting. Style comes from the root `.editorconfig`.
- New lint findings must be fixed, not added to the baselines (`config/detekt/baseline.xml`, `app/lint-baseline.xml`), unless there is a stated reason.
- The Maps/Places key comes from `RutasUruapan/apikeys.properties` (`mapsAndPlaces=KEY`, gitignored), as described in the README.
