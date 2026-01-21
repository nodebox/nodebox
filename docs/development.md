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

## Notes

- Jython emits native-access warnings on newer JDKs; this is expected for now.
- `build/` and `dist/` are generated outputs; avoid manual edits.
