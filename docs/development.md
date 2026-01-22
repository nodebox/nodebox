# Development Guide

This guide covers how to build, run, and test NodeBox locally, plus the minimum environment setup.

## Prerequisites

- JDK 25 (only supported version)
- Apache Ant
- Maven (dependency resolution)

## Common Commands

```shell
# Run the app
ant run

# Run unit tests
ant test

# Run E2E UI tests (requires a graphical session)
NODEBOX_E2E=1 ant test-e2e

# Build a distributable app (macOS)
ant dist-mac
```

## E2E Artifacts

E2E failures produce screenshots and stack traces in `build/e2e-artifacts` by default. Override the output directory with:

```shell
NODEBOX_E2E_ARTIFACTS=/path/to/dir NODEBOX_E2E=1 ant test-e2e
```

## Adding an E2E Test (Manual)

1) Add a new `@Test` method in `src/test/java/nodebox/e2e/NodeBoxE2ETest.java`.
2) Use `focusCurrentDocument()` or `openExampleAndWait(exampleFile())` to ensure the UI is ready.
3) For UI mutations, wrap calls in `SwingUtilities.invokeAndWait(...)`.
4) Use `waitFor(...)` to assert state changes (avoid fixed sleeps).
5) Keep tests deterministic: prefer built-in examples and stable node names.

Example skeleton:

```java
@Test
public void myNewE2ETest() throws Exception {
    final NodeBoxDocument doc = focusCurrentDocument();
    assertNotNull(doc);
    SwingUtilities.invokeAndWait(() -> {
        // mutate UI or model
    });
    waitFor("Expected change", DEFAULT_TIMEOUT_MS, () -> {
        // return true when the state is correct
        return true;
    });
}
```

## Notes

- Jython emits native-access warnings on newer JDKs; this is expected for now.
- `build/` and `dist/` are generated outputs; avoid manual edits.
