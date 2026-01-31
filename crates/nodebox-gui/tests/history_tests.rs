//! Tests for undo/redo history functionality.

mod common;

use nodebox_gui::{History, Node, NodeLibrary, Port};

/// Create a simple test library with an ellipse.
fn create_test_library(x: f64) -> NodeLibrary {
    let mut library = NodeLibrary::new("test");
    library.root = Node::network("root")
        .with_child(
            Node::new("ellipse1")
                .with_prototype("corevector.ellipse")
                .with_input(Port::float("x", x))
                .with_input(Port::float("y", 0.0))
                .with_input(Port::float("width", 100.0))
                .with_input(Port::float("height", 100.0)),
        )
        .with_rendered_child("ellipse1");
    library
}

#[test]
fn test_history_new_is_empty() {
    let history = History::new();
    assert!(!history.can_undo());
    assert!(!history.can_redo());
    assert_eq!(history.undo_count(), 0);
    assert_eq!(history.redo_count(), 0);
}

#[test]
fn test_history_save_enables_undo() {
    let mut history = History::new();
    let library = create_test_library(0.0);

    history.save_state(&library);

    assert!(history.can_undo());
    assert!(!history.can_redo());
    assert_eq!(history.undo_count(), 1);
}

#[test]
fn test_history_undo_restores_previous_state() {
    let mut history = History::new();

    // Save initial state
    let library_v1 = create_test_library(0.0);
    history.save_state(&library_v1);

    // Current state has different x value
    let library_v2 = create_test_library(100.0);

    // Undo should restore v1
    let restored = history.undo(&library_v2).unwrap();

    // Check that the x value was restored
    let node = restored.root.child("ellipse1").unwrap();
    let x = node.input("x").unwrap().value.as_float().unwrap();
    assert!((x - 0.0).abs() < 0.001);
}

#[test]
fn test_history_undo_enables_redo() {
    let mut history = History::new();

    let library_v1 = create_test_library(0.0);
    history.save_state(&library_v1);

    let library_v2 = create_test_library(100.0);
    history.undo(&library_v2);

    assert!(history.can_redo());
    assert_eq!(history.redo_count(), 1);
}

#[test]
fn test_history_redo_restores_undone_state() {
    let mut history = History::new();

    let library_v1 = create_test_library(0.0);
    history.save_state(&library_v1);

    let library_v2 = create_test_library(100.0);

    // Undo returns v1
    let after_undo = history.undo(&library_v2).unwrap();

    // Redo should return v2
    let after_redo = history.redo(&after_undo).unwrap();

    let node = after_redo.root.child("ellipse1").unwrap();
    let x = node.input("x").unwrap().value.as_float().unwrap();
    assert!((x - 100.0).abs() < 0.001);
}

#[test]
fn test_history_new_changes_clear_redo_stack() {
    let mut history = History::new();

    let library_v1 = create_test_library(0.0);
    history.save_state(&library_v1);

    let library_v2 = create_test_library(100.0);

    // Undo to enable redo
    history.undo(&library_v2);
    assert!(history.can_redo());

    // Save new state (simulating new change)
    let library_v3 = create_test_library(50.0);
    history.save_state(&library_v3);

    // Redo should now be unavailable
    assert!(!history.can_redo());
    assert_eq!(history.redo_count(), 0);
}

#[test]
fn test_history_multiple_undos() {
    let mut history = History::new();

    // Create and save multiple states
    let library_v1 = create_test_library(0.0);
    history.save_state(&library_v1);

    let library_v2 = create_test_library(50.0);
    history.save_state(&library_v2);

    let library_v3 = create_test_library(100.0);

    assert_eq!(history.undo_count(), 2);

    // Undo twice
    let after_first_undo = history.undo(&library_v3).unwrap();
    let node = after_first_undo.root.child("ellipse1").unwrap();
    let x = node.input("x").unwrap().value.as_float().unwrap();
    assert!((x - 50.0).abs() < 0.001);

    let after_second_undo = history.undo(&after_first_undo).unwrap();
    let node = after_second_undo.root.child("ellipse1").unwrap();
    let x = node.input("x").unwrap().value.as_float().unwrap();
    assert!((x - 0.0).abs() < 0.001);

    // No more undos available
    assert!(!history.can_undo());
}

#[test]
fn test_history_multiple_redos() {
    let mut history = History::new();

    let library_v1 = create_test_library(0.0);
    history.save_state(&library_v1);

    let library_v2 = create_test_library(50.0);
    history.save_state(&library_v2);

    let library_v3 = create_test_library(100.0);

    // Undo twice
    let after_first_undo = history.undo(&library_v3).unwrap();
    let after_second_undo = history.undo(&after_first_undo).unwrap();

    assert_eq!(history.redo_count(), 2);

    // Redo twice
    let after_first_redo = history.redo(&after_second_undo).unwrap();
    let node = after_first_redo.root.child("ellipse1").unwrap();
    let x = node.input("x").unwrap().value.as_float().unwrap();
    assert!((x - 50.0).abs() < 0.001);

    let after_second_redo = history.redo(&after_first_redo).unwrap();
    let node = after_second_redo.root.child("ellipse1").unwrap();
    let x = node.input("x").unwrap().value.as_float().unwrap();
    assert!((x - 100.0).abs() < 0.001);

    // No more redos available
    assert!(!history.can_redo());
}

#[test]
fn test_history_clear() {
    let mut history = History::new();

    let library_v1 = create_test_library(0.0);
    history.save_state(&library_v1);
    history.save_state(&library_v1);
    history.save_state(&library_v1);

    assert_eq!(history.undo_count(), 3);

    history.clear();

    assert!(!history.can_undo());
    assert!(!history.can_redo());
    assert_eq!(history.undo_count(), 0);
    assert_eq!(history.redo_count(), 0);
}

#[test]
fn test_history_mark_saved_and_unsaved_changes() {
    let mut history = History::new();

    let library_v1 = create_test_library(0.0);
    history.mark_saved(&library_v1);

    // Same library should not have unsaved changes
    assert!(!history.has_unsaved_changes(&library_v1));

    // Different library should have unsaved changes
    let library_v2 = create_test_library(100.0);
    assert!(history.has_unsaved_changes(&library_v2));
}

#[test]
fn test_history_undo_on_empty_returns_none() {
    let mut history = History::new();
    let library = create_test_library(0.0);

    let result = history.undo(&library);
    assert!(result.is_none());
}

#[test]
fn test_history_redo_on_empty_returns_none() {
    let mut history = History::new();
    let library = create_test_library(0.0);

    let result = history.redo(&library);
    assert!(result.is_none());
}
