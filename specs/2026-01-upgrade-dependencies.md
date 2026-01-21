# Dependency Upgrade Plan (January 2026)

## Baseline Snapshot (current repo state)
- Build system: Ant (`build.xml`) with Maven used only for dependency resolution.
- Java target: `maven.compiler.source/target = 11` (`pom.xml`).
- Packaging: `ant dist-mac`/`ant dist-win` uses `jpackage` (see `build.xml`).
- Local build helper jars: `lib/maven-ant-tasks-2.1.3.jar`, `lib/appbundler-1.0ea.jar`, `lib/nsisant-1.2.jar`.
- Tests: Ant JUnit task runs all `**/*Test.class` from `src/test/java` and emits `reports/TEST-*.xml`.
- Local toolchain snapshot (captured 2026-01-21):
  - Ant: 1.10.15 (Homebrew).
  - JDK: OpenJDK 25.0.1 at `/opt/homebrew/opt/openjdk` (used by the Ant wrapper).
  - Maven: not installed in PATH.
  - `java`/`javac` in PATH resolve to the macOS stub (no runtime); rely on Ant’s JAVA_HOME default unless PATH is updated.

## Dependency Inventory (from `pom.xml`)
Runtime:
- `org.clojure:clojure:1.8.0`
- `com.google.guava:guava:29.0-jre`
- `org.apache.httpcomponents:httpclient:4.5.13`
- `com.itextpdf:itextpdf:5.5.0`
- `net.java.dev.jna:jna:4.1.0`
- `com.jayway.jsonpath:json-path:0.8.1`
- `org.python:jython-standalone:2.7.2`
- `net.sf.opencsv:opencsv:2.3`
- `org.clojars.processing-core:org.processing.core:1.5.1`
- `ddf.minim:ddf.minim:2.2.0`
- `de.sojamo:oscp5:0.9.8`

Test:
- `junit:junit:4.13.1`

Repositories:
- Maven Central, Clojars.

## Upgrade Staging Proposal
Each stage should end with `ant run` and `ant test`, plus a quick UI smoke check (launch, open example, render/export if relevant).

### Stage 0 — Guardrails & Repro
- Record current Java/Ant/Maven versions used by the team.
- Add a short “build matrix” note to docs if not already present (e.g., supported JDKs).
- Ensure CI covers a baseline JDK (e.g., 11) before upgrades.

### Stage 1 — Build Tooling (no runtime libs)
- Review/upgrade Ant tasks and helper jars if newer equivalents are available.
- Confirm `jpackage` path assumptions in `build.xml` still align with modern JDK installs.

### Stage 2 — Low-risk Runtime Libraries
- Group A: `guava`, `httpclient`, `json-path`, `opencsv`, `jna`.
- Upgrade one at a time; run tests + launch app after each.
- Watch for API removals (e.g., Guava, HttpClient) and Java module warnings.

### Stage 3 — Content & Media Stack
- Group B: `itextpdf`, `processing.core`, `ddf.minim`, `oscp5`.
- Validate export and media-related flows (PDF/SVG/CSV, audio/OSC if used).

### Stage 4 — Language Runtimes
- Group C: `clojure`, `jython-standalone`.
- Clojure upgrade risk: breaking changes across major versions.
- Jython status is tricky (2.7 line is legacy); consider pinning or isolating.

### Stage 5 — Testing Framework
- Move from JUnit 4 to a newer 4.x (if not already) or plan a JUnit 5 migration.
- If migrating, update Ant test task + test source structure incrementally.

## Proposed Dependency Batches (for PR sizing)
- PR 1: Build tooling + documentation updates (no library upgrades).
- PR 2: Group A low-risk runtime libs (1–2 deps per PR).
- PR 3: Group B media libs.
- PR 4: Group C language runtimes.
- PR 5: Test framework modernization.

## Risks & Watchouts
- Some libraries are very old; jumping to latest may require code changes.
- Jython 2.7 ecosystem is frozen; verify compatibility or plan replacements.
- Processing/Minim/OSCP5 versions may be tied to older APIs.
- `itextpdf` licensing/version changes may require a compliance review.

## Next Steps
- Baseline JDK selected: 25 (matches local toolchain and CI).
- Decide the target ordering for Group A libraries based on usage frequency.
- Create a tracking checklist for manual UI smoke tests.
