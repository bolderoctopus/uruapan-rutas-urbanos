# Repo foundations for Rutas Uruapan

## Context
This is a personal Android app (Kotlin, single `:app` module, Gradle version catalog, Hilt, Room, Maps/Places) on Google Play with few users. The goal is to practice professional repo hygiene: tests, CI on GitHub Actions, lint, and written conventions. Decisions so far: **CI only** (releases stay manual), **trunk-based** branching, **ktlint + detekt + Android Lint**, and **Conventional Commits enforced on PR titles**.

### What the repo has today
_Snapshot taken on 2026-10-25, before Phase 0. Later phases change some of these points._

- Gradle project lives in `RutasUruapan/` (not the repo root). AGP 8.11, Kotlin 2.2, Gradle 9.1, JDK 21.
- 3 unit tests (`AddressUtilsTest`, `UtilsTest`, `RouteViewModelTest`) using JUnit4, MockK, Turbine, Robolectric. No instrumented tests. No coverage.
- **CI blocker:** `app/build.gradle` calls `apiKeysProperties.load(new FileInputStream(apiKeysFile))`, so the build fails when `apikeys.properties` is missing, which it always is on a CI runner.
- Branch protection on `master` already requires PRs, linear history, and no force pushes, with 0 required approvals. **No required status checks yet.**
- Repo allows merge commits, and `delete_branch_on_merge` is off. Leftover branches: `development`, `bug-fix`, and several merged `fix/`/`chore/` branches.
- No `.github/`, `.editorconfig`, CONTRIBUTING, PR template, or Dependabot. `.DS_Store` is untracked at root (root `.gitignore` lacks it).
- Unused catalog entries: Compose `ui*`/`material3`, `core-ktx`, `lifecycle-runtime-ktx`. `android.enableJetifier=true` is probably unnecessary.
- `MainActivity.kt` is 813 lines, which explains why there are few tests. Testability work comes after the tooling is in place.

## Phases (each phase is one PR, so every step practices the new workflow)

### Phase 0 — Make the build CI-friendly  (`chore/ci-friendly-build`) ✅ Done in #11
- `app/build.gradle`: load `apikeys.properties` only if it exists, else fall back to the `MAPS_API_KEY` env var, else `""`. Unit tests don't need a real key.
- Root `.gitignore`: add `.DS_Store`. Optionally remove the unused catalog entries and the Jetifier flag.
- Verify: `mv apikeys.properties /tmp && ./gradlew testDebugUnitTest assembleDebug` succeeds, then restore the file.

### Phase 1 — Lint & formatting  (`chore/add-linters`) ✅ Done
- `.editorconfig` at the repo root (Kotlin official style, 4-space indent, ktlint rules such as `ktlint_code_style = android_studio`).
- ktlint via the `org.jlleitschuh.gradle.ktlint` plugin (adds `ktlintCheck` / `ktlintFormat`). Run `ktlintFormat` once in its own commit so the reformat is isolated.
- detekt via the `io.gitlab.arturbosch.detekt` plugin with `config/detekt/detekt.yml` (`buildUponDefaultConfig = true`) and `detektBaseline` to grandfather existing issues (MainActivity's complexity).
- Android Lint: `lint { abortOnError true; warningsAsErrors false; baseline file("lint-baseline.xml") }`, then generate the baseline.
- All plugin versions go in `gradle/libs.versions.toml`.
- Verify: `./gradlew ktlintCheck detekt lintDebug` passes, and a deliberate violation fails it.
- **As built:** ktlint plugin 14.2.0 (ktlint 1.8.0), detekt 1.23.8 (JVM target pinned to 21), `max_line_length = 120`. detekt rules that overlap ktlint (`NewLineAtEndOfFile`, `WildcardImport`, `MaxLineLength`) and `MagicNumber` are off. Baselines: `config/detekt/baseline.xml` (28 findings), `app/lint-baseline.xml` (45 issues). The merged reformat commit is listed in `.git-blame-ignore-revs` (added after the merge, because merging changes SHAs).
- **Follow-ups found by the linters** (left in baselines on purpose):
  - `UseAppTint` in `current_location_list_item.xml`: switch to `app:tint` and check the icon tint on a device.
  - detekt: `RouteModel` overrides `equals` without `hashCode`, unused `Point.pointId` / `SearchFragment.routeViewModel`, empty secondary constructor in `Point`.

### Phase 2 — CI workflow  (`ci/add-pr-workflow`) ✅ Done
- `.github/workflows/ci.yml`, triggered on `pull_request` and `push` to `master`:
  - `actions/checkout`, `actions/setup-java` (temurin 21), `gradle/actions/setup-gradle` (caching), `working-directory: RutasUruapan`.
  - Jobs: `lint` (ktlintCheck + detekt + lintDebug) and `test` (testDebugUnitTest + assembleDebug), in parallel.
  - Upload test and lint reports as artifacts on failure. `concurrency` cancels superseded runs. `permissions: contents: read`.
- `.github/workflows/pr-title.yml`: `amannn/action-semantic-pull-request` checks Conventional Commits types.
- Optional: `gradle/actions/wrapper-validation`.
- Verify: open the PR itself and watch both workflows go green. Push a bad commit to see one fail.
- **As built:** checkout@v7, setup-java@v6 (temurin 21), gradle/actions/setup-gradle@v6 with `cache-provider: basic` (the default "enhanced" cache is a commercial service) and its built-in wrapper validation, upload-artifact@v7, amannn/action-semantic-pull-request@v6. The PR-title check uses `pull_request`, not `pull_request_target`, because the repo has no fork contributors and `pull_request_target` would only run once the workflow is on `master`. The title summary must start lowercase. Actions are pinned to major tags; Dependabot (Phase 3) keeps them current.

### Phase 3 — Repo rules & conventions  (`chore/repo-conventions`, plus GitHub settings) ✅ Done
- ✅ `CONTRIBUTING.md` (done early, in `docs/contributing-guide`): trunk-based flow, branch naming, Conventional Commits, PR and merge rules, local checks, releases (tags match `versionName` with no `v` prefix, as the existing tags do), and `.git-blame-ignore-revs`. When CI and the PR-title check land, update its "Pull requests" section to say they are required.
- ✅ `.github/pull_request_template.md` (what/why, how tested, checklist).
- ✅ `.github/ISSUE_TEMPLATE/bug_report.yml` and `feature_request.yml`, bilingual Spanish/English because the app's users speak Spanish.
- ✅ `.github/dependabot.yml`: weekly `gradle` (directory `/RutasUruapan`; groups kotlin+ksp, androidx, play services, testing, lint tools) plus `github-actions`. A `chore(deps)` / `ci(deps)` prefix keeps PR titles valid for the `pr-title` check.
- ✅ `CHANGELOG.md` (Spanish, Keep a Changelog), moved out of the README, with 1.5.2–1.5.4 filled in from git history. CI badge added to the README.
- ✅ GitHub settings (applied 2026-10-26 with `gh api`, after confirmation):
  - Merge commits off. Squash (title = PR title, body = PR description) and rebase on. Branches are deleted automatically after merging.
  - `master` protection: required checks `lint`, `test`, `pr-title` (strict: branch must be up to date), required conversation resolution, PR required with 0 approvals (solo maintainer), linear history, admins included, no force pushes or deletions.
- ✅ Stale branches deleted: the remote and local branches of merged PRs #5–#15, including `development`. Local `bug-fix` ("fix by gemini", never pushed, no PR) was kept for review.

### Phase 4 — Testing foundations  (`test/...`, iterative)
- Add JaCoCo (or Kover, which is simpler for Kotlin) with a `koverHtmlReport`, uploaded as a CI artifact. Report only, no threshold yet.
- Add Room DAO tests with Robolectric plus an in-memory DB (`RouteDAO` queries are core logic and good candidates).
- Extend ViewModel tests. Note business logic still sitting in `MainActivity`/`SearchFragment` as refactor candidates, so it can be moved and tested later.
- Later, optional: one instrumented smoke test using an emulator in CI (`reactivecircus/android-emulator-runner`).

### Phase 5 — Optional extras for later
- Pre-commit git hook running `ktlintCheck` (via a Gradle task that installs `.githooks`).
- `CODEOWNERS`, `SECURITY.md`, CodeQL workflow (free for public repos).
- Release automation (signed AAB on tag, then Play upload) once you want CD.
- Update `CLAUDE.md` with the new commands and conventions.

## How I'll guide you
For each phase, I'll create the branch, make the changes with an explanation of each file, run the checks locally, then open the PR so you can see CI in action. You review and merge. GitHub settings changes are made only after you confirm each one.

## Verification (end state)
- A fresh clone without `apikeys.properties` runs `./gradlew ktlintCheck detekt lintDebug testDebugUnitTest assembleDebug` successfully.
- A PR with a bad title, a formatting violation, or a failing test is blocked from merging.
- Merged branches are deleted automatically, and master history is linear with Conventional Commit messages.
