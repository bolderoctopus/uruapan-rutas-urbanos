# Contributing

How work flows through this repository. It's a small project, so the rules are light, but they are followed on every change.

## Branching strategy: trunk-based

- `master` is the only long-lived branch. It must always build and pass checks, and it reflects what can be released.
- All work happens on **short-lived branches** cut from the latest `master` and merged back through a pull request, ideally within a few days.
- Nobody pushes to `master` directly, admins included. Branch protection requires a PR, passing `lint`, `test` and `pr-title` checks, a branch up to date with `master`, resolved review conversations, linear history, and no force pushes.
- There is no `development` or `release` branch. Releases are tags on `master` (see [Releases](#releases)).

```bash
git switch master && git pull
git switch -c fix/settings-button-npe
```

## Branch names

Format: `<type>/<short-kebab-case-description>`

| Type | Use for | Example |
|---|---|---|
| `feat/` | New user-facing functionality | `feat/favorite-routes` |
| `fix/` | Bug fixes | `fix/places-not-initialized-on-restore` |
| `refactor/` | Code changes with no behavior change | `refactor/move-route-search-to-viewmodel` |
| `test/` | Adding or improving tests only | `test/route-dao` |
| `docs/` | Documentation only | `docs/contributing-guide` |
| `ci/` | GitHub Actions and other CI configuration | `ci/add-pr-workflow` |
| `chore/` | Build, tooling, dependencies, version bumps | `chore/bump-version-1.5.5` |

Rules:
- Lowercase, words separated by `-`, no spaces or underscores.
- Describe the change, not the person or date (`fix/route-list-crash`, not `omar-fixes-oct`).
- One topic per branch. If you find something unrelated, note it and use another branch.

## Commit messages and PR titles

Use [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(optional scope): <summary in imperative mood>
```

- Types match the branch types: `feat`, `fix`, `refactor`, `test`, `docs`, `ci`, `chore`, plus `style` for formatting-only changes.
- Examples: `fix: prevent NPE in settings button when location is denied`, `chore(release): 1.5.5`.
- Summary in English, imperative ("add", not "added"), starting lowercase, no trailing period, about 72 characters max.
- Add a body when the *why* isn't obvious from the summary.

The **PR title** follows the same format, because it becomes the commit message on `master`. The `pr-title` check ([`.github/workflows/pr-title.yml`](.github/workflows/pr-title.yml)) fails if it doesn't. Editing the title re-runs the check.

## Pull requests

1. Push the branch and open a PR against `master`. The PR template asks for what changed, why, and how it was tested. User-facing changes get a line under "Sin publicar" in [`CHANGELOG.md`](CHANGELOG.md).
2. Run the checks locally first (see below). CI ([`.github/workflows/ci.yml`](.github/workflows/ci.yml)) runs them again on every PR and every push to `master`:
   - `lint`: ktlint, detekt and Android Lint
   - `test`: unit tests and a debug build

   If a job fails, its reports can be downloaded from the run's **Artifacts** section. All three checks (`lint`, `test`, `pr-title`) are required before merging. If GitHub says the branch is out of date, use **Update branch** and let CI run again.
3. **Review your own diff** in GitHub's "Files changed" tab before merging. It's the review step of a solo project.
4. Resolve any review comments, including automated ones. Merging is blocked until every conversation is resolved.
5. Merge:
   - **Squash and merge** (default): the whole PR becomes one commit, with the PR title as its message and the PR description as its body.
   - **Rebase and merge**: only when the individual commits should stay separate on `master`, for example a mechanical reformat that is listed in `.git-blame-ignore-revs`.
6. GitHub deletes the branch after merging. Locally, run `git switch master && git pull && git fetch --prune` and delete the local branch with `git branch -D <name>`. Squash-merged branches need `-D`.

## Dependency updates

Dependabot ([`.github/dependabot.yml`](.github/dependabot.yml)) opens PRs every Monday for Gradle dependencies and GitHub Actions. Related libraries are grouped, for example Kotlin with KSP and all AndroidX. Treat them like any other PR: wait for CI, read the release notes for major versions, and squash-merge. Dependabot does not track the ktlint version (`ktlint-cli` in `libs.versions.toml`), so check it by hand now and then.

## Issues

Use the issue templates: **Reporte de error / Bug report** or **Sugerencia / Feature request**. They are bilingual because most app users speak Spanish.

## Checks to run locally

From `RutasUruapan/`:

```bash
./gradlew ktlintCheck detekt lintDebug testDebugUnitTest
```

- `./gradlew ktlintFormat` auto-fixes most formatting issues. Style rules live in the root `.editorconfig`.
- Existing findings are recorded in `config/detekt/baseline.xml` and `app/lint-baseline.xml`. Fix new findings instead of adding them to a baseline.
- `./gradlew koverHtmlReportDebug` builds a test coverage report at `app/build/reports/kover/htmlDebug/index.html` (`koverLogDebug` prints the total). CI shows the total on each run's summary page and uploads the HTML report as the `coverage-report` artifact. No minimum is enforced yet.

## Releases

1. Open a `chore/bump-version-X.Y.Z` branch and bump `versionCode` (+1) and `versionName` in `RutasUruapan/app/build.gradle`. In [`CHANGELOG.md`](CHANGELOG.md), move the "Sin publicar" entries under a new `## [X.Y.Z] - YYYY-MM-DD` heading.
2. Title the PR `chore(release): X.Y.Z`, then merge it.
3. Tag the merge commit on `master` with the version name and push the tag:
   ```bash
   git switch master && git pull
   git tag -a 1.5.5 -m "1.5.5"
   git push origin 1.5.5
   ```
4. Build the signed bundle and upload it to Google Play by hand.

Versioning follows [SemVer](https://semver.org/): `MAJOR.MINOR.PATCH`. Bump PATCH for fixes, MINOR for new features, and MAJOR for breaking or very large changes.

## Git blame and reformat commits

Commits that only reformat code are listed in `.git-blame-ignore-revs`, so `git blame` points at the real author of a line. GitHub uses that file automatically. To use it locally, run once:

```bash
git config blame.ignoreRevsFile .git-blame-ignore-revs
```
