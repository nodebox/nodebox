# JUnit 5 Migration Plan (NodeBox)

## Scope & Goals
- Move tests from JUnit 4 to JUnit 5 (Jupiter).
- Keep Ant-based build/test pipeline functional throughout.
- Avoid breaking existing tests during transition.

## Current State
- Tests use a mix of `org.junit.Test` and `junit.framework.TestCase`.
- Ant `junit` task discovers `**/*Test.class` and runs with the current classpath.
- JUnit is pinned to 4.13.2.

## Strategy Overview
### Phase 1 — Dual-Run (Bridge)
- Add JUnit 5 dependencies **alongside** JUnit 4:
  - `org.junit.jupiter:junit-jupiter-api`
  - `org.junit.jupiter:junit-jupiter-engine`
  - `org.junit.vintage:junit-vintage-engine` (to keep JUnit 4 tests running)
- Update Ant test target to run JUnit Platform:
  - Prefer Ant’s `junitlauncher` (Ant 1.10+).
  - Keep old `junit` task as fallback until the suite is fully migrated.

### Phase 2 — Convert Tests
- Convert `junit.framework.TestCase` subclasses to plain classes with `@Test`.
- Replace static imports from `junit.framework.TestCase.*` with `org.junit.jupiter.api.Assertions.*`.
- Update any `@Test(expected=...)` to `assertThrows`.
- Keep tests green after each batch conversion.

### Phase 3 — Remove JUnit 4
- Remove `junit` dependency and `junit-vintage-engine`.
- Drop the legacy Ant `junit` task; keep only `junitlauncher`.
- Update documentation and CI to reflect the new test runner.

## Ant Build Changes (Proposed)
- Add a `test-jupiter` target that uses `junitlauncher` with:
  - `classpath` set to test + runtime + JUnit 5 engine jars.
  - Test selector for `**/*Test.class`.
- Keep existing `test` target for now; once migration is complete, replace it.

## Risks & Mitigations
- JUnit 4/5 conflicts: use Vintage engine during transition.
- Ant task support: ensure Ant 1.10+ in CI (already on 1.10.15 locally).
- Long conversion effort: migrate by package or module to reduce risk.

## Migration Checklist
- [ ] Add JUnit 5 dependencies and Vintage engine.
- [ ] Add `test-jupiter` Ant target using `junitlauncher`.
- [ ] Convert `junit.framework.TestCase` subclasses.
- [ ] Replace `@Test(expected=...)` with `assertThrows`.
- [ ] Drop JUnit 4 + Vintage engine after all tests migrate.
- [ ] Update docs/CI for the new test task.
