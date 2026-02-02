# Async Node Implementation Guide

This document explains how to implement async-aware nodes in NodeBox's Rust codebase, including proper cancellation support for long-running operations.

## Overview

NodeBox uses cooperative cancellation to allow users to stop long-running renders. When implementing nodes that perform I/O operations (file reading, network requests) or expensive computations, you should check for cancellation at appropriate boundaries.

## Cancellation Architecture

### How It Works

1. **CancellationToken**: A thread-safe token shared between the main thread and render worker
2. **Cooperative checks**: Nodes check `is_cancelled()` at iteration boundaries
3. **Early return**: When cancelled, evaluation stops and returns cached partial results
4. **RAII cleanup**: Resources are automatically cleaned up via Rust's Drop trait

### Cancellation Check Locations

The evaluation engine automatically checks for cancellation at:
- **Before each node**: Before starting to evaluate any node
- **During list-matching iterations**: Between iterations in list-matching loops

For standard nodes, this provides < 500ms response time for typical workloads.

## Implementing Async-Aware Nodes

### Basic Pattern

For nodes that perform expensive operations:

```rust
fn execute_expensive_node(
    node: &Node,
    inputs: &HashMap<String, NodeOutput>,
    cancel_token: &CancellationToken,
) -> EvalResult {
    // Check cancellation before starting
    if cancel_token.is_cancelled() {
        return Err(EvalError::Cancelled);
    }

    // Do work in chunks, checking periodically
    let mut results = Vec::new();
    for item in items {
        // Check at iteration boundaries
        if cancel_token.is_cancelled() {
            return Err(EvalError::Cancelled);
        }

        let result = process_item(item);
        results.push(result);
    }

    Ok(NodeOutput::from(results))
}
```

### I/O Operations with smol

For nodes that perform async I/O (file reading, network requests), use the `smol` runtime:

```rust
use smol::fs;
use smol::future::block_on;

fn execute_file_read_node(
    node: &Node,
    inputs: &HashMap<String, NodeOutput>,
    cancel_token: &CancellationToken,
) -> EvalResult {
    let path = get_string(inputs, "path", "");

    // Use smol for async file operations
    let content = block_on(async {
        // Check cancellation before I/O
        if cancel_token.is_cancelled() {
            return Err(std::io::Error::new(
                std::io::ErrorKind::Interrupted,
                "Cancelled",
            ));
        }

        fs::read_to_string(&path).await
    });

    match content {
        Ok(text) => Ok(NodeOutput::String(text)),
        Err(e) if e.kind() == std::io::ErrorKind::Interrupted => {
            Err(EvalError::Cancelled)
        }
        Err(e) => Err(EvalError::ProcessingError(format!(
            "{}: {}",
            node.name, e
        ))),
    }
}
```

### Network Requests

```rust
use smol::net::TcpStream;
use smol::io::AsyncReadExt;

async fn fetch_url(url: &str, cancel_token: &CancellationToken) -> Result<String, EvalError> {
    // Periodically check cancellation during long operations
    if cancel_token.is_cancelled() {
        return Err(EvalError::Cancelled);
    }

    // Use async-net or smol's networking
    let stream = TcpStream::connect(url).await
        .map_err(|e| EvalError::ProcessingError(e.to_string()))?;

    // Read with cancellation checks
    let mut buffer = Vec::new();
    // ... read in chunks, checking cancel_token between reads

    Ok(String::from_utf8_lossy(&buffer).to_string())
}
```

## Best Practices

### Check Frequency

- **Too few checks**: User waits too long for cancellation (> 500ms)
- **Too many checks**: Performance overhead from atomic reads
- **Rule of thumb**: Check every ~100ms of work, or at natural iteration boundaries

### Resource Cleanup

Use Rust's RAII pattern for automatic cleanup:

```rust
struct TempFile {
    path: PathBuf,
}

impl Drop for TempFile {
    fn drop(&mut self) {
        // Automatically cleaned up even on cancellation
        let _ = std::fs::remove_file(&self.path);
    }
}
```

### Cache Preservation

When implementing caching, ensure partial results are preserved:

```rust
// Good: Cache results as you go
for (i, item) in items.iter().enumerate() {
    if cancel_token.is_cancelled() {
        // partial_cache already contains results [0..i)
        return Err(EvalError::Cancelled);
    }

    let result = process(item);
    partial_cache.insert(i, result.clone());
    results.push(result);
}

// Bad: Only cache at the end
let results: Vec<_> = items.iter().map(process).collect();
cache.insert_all(results);  // Lost if cancelled
```

## Testing Async Nodes

### Basic Cancellation Test

```rust
#[test]
fn test_node_respects_cancellation() {
    let token = CancellationToken::new();
    let mut cache = HashMap::new();

    // Pre-cancel
    token.cancel();

    let result = evaluate_network_cancellable(&library, &token, &mut cache);

    assert!(matches!(result, EvalOutcome::Cancelled));
}
```

### Response Time Test

```rust
#[test]
fn test_cancellation_response_time() {
    let token = CancellationToken::new();

    let token_clone = token.clone();
    let handle = thread::spawn(move || {
        let mut cache = HashMap::new();
        evaluate_network_cancellable(&library, &token_clone, &mut cache)
    });

    thread::sleep(Duration::from_millis(100));
    let cancel_time = Instant::now();
    token.cancel();

    let _result = handle.join().unwrap();

    assert!(
        cancel_time.elapsed() < Duration::from_millis(500),
        "Cancellation should respond within 500ms"
    );
}
```

## UI Integration

The stop button in the address bar becomes prominent after 3 seconds of rendering:

- **Idle**: SLATE_700 (subtle, near background)
- **Rendering < 3s**: SLATE_700 (subtle)
- **Rendering >= 3s**: SLATE_300 (prominent)

Keyboard shortcut: `Cmd+.` (macOS) or `Ctrl+.` (Windows/Linux)

## Related Files

- `crates/nodebox-gui/src/render_worker.rs` - CancellationToken, worker loop
- `crates/nodebox-gui/src/eval.rs` - evaluate_network_cancellable()
- `crates/nodebox-gui/src/address_bar.rs` - Stop button UI
- `crates/nodebox-gui/tests/cancellation_tests.rs` - Integration tests
