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
- Name new Java tests `SomethingTest.java` and keep them close to the package they cover.
- Run `ant test` before shipping changes that affect core behavior or UI flows.

## Commit & Pull Request Guidelines
- Recent history favors short, sentence-style commit messages (e.g., “Use Ctrl key on Windows.”). Keep messages concise and specific.
- PRs should describe the user-visible change, list test commands run, and include screenshots or recordings for UI updates.
- Link relevant issues or tickets when applicable.

## Notes for Contributors
- Versioning lives in `src/main/resources/version.properties`; update it when preparing a release build.
