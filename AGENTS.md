# Repository Guidelines

## Project Structure & Module Organization
- `src/main/java` holds the core Java application (`nodebox.*` packages).
- `src/main/python` contains bundled Python node libraries and helpers.
- `src/main/resources` stores runtime assets and `version.properties`.
- `src/test/java` contains JUnit tests; `src/test/python` and `src/test/clojure` hold language fixtures.
- `libraries/` and `examples/` ship built-in node libraries and example projects.
- `res/`, `artwork/`, and `platform/` contain assets and platform-specific launchers.
- `build/` and `dist/` are generated outputs; avoid manual edits.
- `build.xml` (Ant) and `pom.xml` (Maven deps) define the build and test pipeline.

## Build, Test, and Development Commands
- `ant run` builds and launches NodeBox.
- `ant test` compiles and runs JUnit tests; XML reports land in `reports/`.
- `ant test-perf` runs the drag-responsiveness measurement harness (needs a display); writes `build/e2e-artifacts/drag-perf.txt`. Pick a scenario with `-Dperf.example=… -Dperf.node=… -Dperf.port=…`.
- `ant generate-test-reports` renders HTML reports from `reports/TEST-*.xml`.
- `ant dist-mac` / `ant dist-win` create packaged apps in `dist/`.
- `ant clean` removes build artifacts.

Prereqs: Java JDK and Apache Ant are required; Maven is used for dependency resolution (see `README.md`).

## Coding Style & Naming Conventions
- Java: 4-space indentation, braces on the same line, and standard Java naming (classes `UpperCamelCase`, methods `lowerCamelCase`, constants `UPPER_SNAKE_CASE`).
- Python: follow existing API naming (many public helpers are `lowerCamelCase`), keep function signatures consistent with current modules.
- Keep edits localized and match the surrounding file’s formatting and ordering.

## Testing Guidelines
- JUnit is the primary test framework; tests are discovered by `**/*Test.class` in `src/test/java`.
- Run `ant clean` after changing a `static final` constant such as `NodeLibrary.CURRENT_FORMAT_VERSION`: javac copies it into other classes, and Ant only recompiles changed sources.
- `ant test` does not halt on failures; read the results in `reports/TEST-*.xml`.
- The render cache must never change what renders. `ant test` and `ant test-e2e` run with `-Dnodebox.cache.verify=true`, which recomputes every cache hit and fails on a difference. `RenderCacheDifferentialTest` replays random user edits on every example plus a scenario document and compares cached with uncached renders; replay with `-Dnodebox.cache.seed=… -Dnodebox.cache.steps=…`. A change to what a node's output depends on needs a scenario edit there that exercises it.
- To check that a test catches a planted bug, delete the affected `build/prod` classes first: Ant skips recompiling a source edited within the same second as its class file.
- End-to-end tests run with `NODEBOX_E2E=1 ant test-e2e`. Drive the UI through Swing (`SwingUtilities.processKeyBindings`, document APIs), not `java.awt.Robot` input: macOS keeps a background app from activating, so OS-level keystrokes land in the frontmost app.
- Name new Java tests `SomethingTest.java` and keep them close to the package they cover.
- Run `ant test` before shipping changes that affect core behavior or UI flows.

## Commit & Pull Request Guidelines
- Base branch is `master`. Work on a feature branch and merge through a pull request; pushing to `master` builds nightly installers, and a `v*` tag builds a release (see `docs/RELEASING.md`).
- Recent history favors short, sentence-style commit messages (e.g., “Use Ctrl key on Windows.”). Keep messages concise and specific.
- PRs should describe the user-visible change, list test commands run, and include screenshots or recordings for UI updates.
- Link relevant issues or tickets when applicable.

## Notes for Contributors
- Versioning lives in `src/main/resources/version.properties`; update it when preparing a release build.
