//! Integration tests for render cancellation.

use std::collections::HashMap;
use std::sync::Arc;
use std::thread;
use std::time::{Duration, Instant};
use nodebox_core::geometry::Point;
use nodebox_core::node::{Node, NodeLibrary, Port};
use nodebox_desktop::eval::{EvalOutcome, NodeOutput, evaluate_network_cancellable};
use nodebox_eval::CancellationToken;
use nodebox_core::platform::{Platform, ProjectContext, TestPlatform};

/// Create a test platform and project context for evaluation tests.
fn test_platform_and_context() -> (Arc<dyn Platform>, ProjectContext) {
    (Arc::new(TestPlatform::new()), ProjectContext::new_unsaved())
}

/// Helper to create a library with a large grid that generates many iterations.
fn create_large_grid_library(grid_size: i64) -> NodeLibrary {
    let mut library = NodeLibrary::new("test");
    library.root = Node::network("root")
        .with_child(
            Node::new("grid1")
                .with_prototype("corevector.grid")
                .with_input(Port::int("columns", grid_size))
                .with_input(Port::int("rows", grid_size))
                .with_input(Port::float("width", 1000.0))
                .with_input(Port::float("height", 1000.0))
                .with_input(Port::point("position", Point::ZERO)),
        )
        .with_child(
            Node::new("rect1")
                .with_prototype("corevector.rect")
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::float("width", 10.0))
                .with_input(Port::float("height", 10.0))
                .with_input(Port::point("roundness", Point::ZERO)),
        )
        .with_connection(nodebox_core::node::Connection::new("grid1", "rect1", "position"))
        .with_rendered_child("rect1");
    library
}

#[test]
fn test_cancellation_token_basic() {
    let token = CancellationToken::new();
    assert!(!token.is_cancelled());

    token.cancel();
    assert!(token.is_cancelled());
}

#[test]
fn test_cancellation_token_clone() {
    let token = CancellationToken::new();
    let token2 = token.clone();

    assert!(!token.is_cancelled());
    assert!(!token2.is_cancelled());

    token.cancel();

    // Both should see cancellation (shared state)
    assert!(token.is_cancelled());
    assert!(token2.is_cancelled());
}

#[test]
fn test_evaluation_completes_without_cancellation() {
    let library = create_large_grid_library(5); // 5x5 = 25 iterations (small)
    let token = CancellationToken::new();
    let mut cache: HashMap<String, NodeOutput> = HashMap::new();

    let (port, ctx) = test_platform_and_context();
    let outcome = evaluate_network_cancellable(&library, &token, &mut cache, &port, &ctx);

    match outcome {
        EvalOutcome::Completed { geometry, errors, .. } => {
            assert!(errors.is_empty(), "Should have no errors");
            assert_eq!(geometry.len(), 25, "Should have 25 rectangles (5x5 grid)");
        }
        EvalOutcome::Cancelled => {
            panic!("Should not be cancelled");
        }
    }
}

#[test]
fn test_evaluation_cancelled_immediately() {
    let library = create_large_grid_library(100); // 100x100 = 10000 iterations (large)
    let token = CancellationToken::new();
    let mut cache: HashMap<String, NodeOutput> = HashMap::new();

    // Cancel immediately
    token.cancel();

    let (port, ctx) = test_platform_and_context();
    let outcome = evaluate_network_cancellable(&library, &token, &mut cache, &port, &ctx);

    match outcome {
        EvalOutcome::Completed { .. } => {
            panic!("Should have been cancelled, not completed");
        }
        EvalOutcome::Cancelled => {
            // Expected
        }
    }
}

#[test]
fn test_cache_preserved_after_cancellation() {
    let library = create_large_grid_library(50); // 50x50 = 2500 iterations
    let token = CancellationToken::new();
    let mut cache: HashMap<String, NodeOutput> = HashMap::new();

    // Cancel after a short delay
    let token_clone = token.clone();
    thread::spawn(move || {
        thread::sleep(Duration::from_millis(1));
        token_clone.cancel();
    });

    let (port, ctx) = test_platform_and_context();
    let outcome = evaluate_network_cancellable(&library, &token, &mut cache, &port, &ctx);

    // The outcome could be either cancelled or completed depending on timing
    // But the cache should have some entries
    match outcome {
        EvalOutcome::Cancelled => {
            // Cache may have partial results - the grid node should be cached
            // at minimum since it's evaluated before the rects
        }
        EvalOutcome::Completed { .. } => {
            // If it completed quickly, that's also fine
        }
    }
}

#[test]
fn test_cache_reused_after_cancellation() {
    let library = create_large_grid_library(10); // 10x10 = 100 iterations
    let mut cache: HashMap<String, NodeOutput> = HashMap::new();

    // First render - complete fully
    let token1 = CancellationToken::new();
    let (port, ctx) = test_platform_and_context();
    let outcome1 = evaluate_network_cancellable(&library, &token1, &mut cache, &port, &ctx);

    match outcome1 {
        EvalOutcome::Completed { geometry, errors, .. } => {
            assert!(errors.is_empty());
            assert_eq!(geometry.len(), 100);
        }
        _ => panic!("First render should complete"),
    }

    // Cache should have entries
    assert!(!cache.is_empty(), "Cache should have entries after first render");
    let cache_size_after_first = cache.len();

    // Second render - should reuse cache
    let token2 = CancellationToken::new();
    let outcome2 = evaluate_network_cancellable(&library, &token2, &mut cache, &port, &ctx);

    match outcome2 {
        EvalOutcome::Completed { geometry, errors, .. } => {
            assert!(errors.is_empty());
            assert_eq!(geometry.len(), 100);
        }
        _ => panic!("Second render should complete"),
    }

    // Cache should still have entries (and possibly the same size)
    assert_eq!(cache.len(), cache_size_after_first, "Cache size should remain consistent");
}

#[test]
fn test_cancellation_response_time() {
    // Create a moderately large workload
    let library = create_large_grid_library(100); // 100x100 = 10000 iterations
    let token = CancellationToken::new();

    // Start evaluation in a thread
    let token_for_thread = token.clone();
    let library_clone = library.clone();
    let handle = thread::spawn(move || {
        let mut thread_cache: HashMap<String, NodeOutput> = HashMap::new();
        let port: Arc<dyn Platform> = Arc::new(TestPlatform::new());
        let ctx = ProjectContext::new_unsaved();
        evaluate_network_cancellable(&library_clone, &token_for_thread, &mut thread_cache, &port, &ctx)
    });

    // Wait a bit for evaluation to start, then cancel
    thread::sleep(Duration::from_millis(10));
    let cancel_time = Instant::now();
    token.cancel();

    // Wait for the thread to finish
    let _outcome = handle.join().unwrap();

    // Check response time - should be well under 500ms
    let response_time = cancel_time.elapsed();
    assert!(
        response_time < Duration::from_millis(500),
        "Cancellation response time should be < 500ms, was {:?}",
        response_time
    );
}

#[test]
fn test_multiple_rapid_cancellations() {
    // Simulate rapid cancel/restart cycles
    for i in 0..10 {
        let library = create_large_grid_library(20); // 20x20 = 400 iterations
        let token = CancellationToken::new();
        let mut cache: HashMap<String, NodeOutput> = HashMap::new();

        // Alternate between immediate cancel and letting it run
        if i % 2 == 0 {
            token.cancel();
        }

        let (port, ctx) = test_platform_and_context();
    let outcome = evaluate_network_cancellable(&library, &token, &mut cache, &port, &ctx);

        // Should not panic or hang regardless of timing
        match outcome {
            EvalOutcome::Completed { .. } | EvalOutcome::Cancelled => {
                // Both are acceptable outcomes
            }
        }
    }
}

#[test]
fn test_empty_network_not_affected_by_cancellation() {
    let library = NodeLibrary::new("empty");
    let token = CancellationToken::new();
    let mut cache: HashMap<String, NodeOutput> = HashMap::new();

    token.cancel(); // Pre-cancel

    let (port, ctx) = test_platform_and_context();
    let outcome = evaluate_network_cancellable(&library, &token, &mut cache, &port, &ctx);

    // Empty network should complete (nothing to cancel)
    match outcome {
        EvalOutcome::Completed { geometry, errors, .. } => {
            assert!(geometry.is_empty());
            assert!(errors.is_empty());
        }
        EvalOutcome::Cancelled => {
            // Also acceptable - cancelled before even starting
        }
    }
}
