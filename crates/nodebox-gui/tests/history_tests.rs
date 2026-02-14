//! Tests for undo/redo history functionality.

mod common;

use std::sync::Arc;
use nodebox_gui::{History, SelectionSnapshot, Node, NodeLibrary, Port};

/// Create a simple test library with an ellipse.
fn create_test_library(x: f64) -> Arc<NodeLibrary> {
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
    Arc::new(library)
}

fn default_sel() -> SelectionSnapshot {
    SelectionSnapshot::default()
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

    history.save_state(&library, &default_sel());

    assert!(history.can_undo());
    assert!(!history.can_redo());
    assert_eq!(history.undo_count(), 1);
}

#[test]
fn test_history_undo_restores_previous_state() {
    let mut history = History::new();

    // Save initial state
    let library_v1 = create_test_library(0.0);
    history.save_state(&library_v1, &default_sel());

    // Current state has different x value
    let library_v2 = create_test_library(100.0);

    // Undo should restore v1
    let (restored, _) = history.undo(&library_v2, &default_sel()).unwrap();

    // Check that the x value was restored
    let node = restored.root.child("ellipse1").unwrap();
    let x = node.input("x").unwrap().value.as_float().unwrap();
    assert!((x - 0.0).abs() < 0.001);
}

#[test]
fn test_history_undo_enables_redo() {
    let mut history = History::new();

    let library_v1 = create_test_library(0.0);
    history.save_state(&library_v1, &default_sel());

    let library_v2 = create_test_library(100.0);
    history.undo(&library_v2, &default_sel());

    assert!(history.can_redo());
    assert_eq!(history.redo_count(), 1);
}

#[test]
fn test_history_redo_restores_undone_state() {
    let mut history = History::new();

    let library_v1 = create_test_library(0.0);
    history.save_state(&library_v1, &default_sel());

    let library_v2 = create_test_library(100.0);

    // Undo returns v1
    let (after_undo, _) = history.undo(&library_v2, &default_sel()).unwrap();

    // Redo should return v2
    let (after_redo, _) = history.redo(&after_undo, &default_sel()).unwrap();

    let node = after_redo.root.child("ellipse1").unwrap();
    let x = node.input("x").unwrap().value.as_float().unwrap();
    assert!((x - 100.0).abs() < 0.001);
}

#[test]
fn test_history_new_changes_clear_redo_stack() {
    let mut history = History::new();

    let library_v1 = create_test_library(0.0);
    history.save_state(&library_v1, &default_sel());

    let library_v2 = create_test_library(100.0);

    // Undo to enable redo
    history.undo(&library_v2, &default_sel());
    assert!(history.can_redo());

    // Save new state (simulating new change)
    let library_v3 = create_test_library(50.0);
    history.save_state(&library_v3, &default_sel());

    // Redo should now be unavailable
    assert!(!history.can_redo());
    assert_eq!(history.redo_count(), 0);
}

#[test]
fn test_history_multiple_undos() {
    let mut history = History::new();

    // Create and save multiple states
    let library_v1 = create_test_library(0.0);
    history.save_state(&library_v1, &default_sel());

    let library_v2 = create_test_library(50.0);
    history.save_state(&library_v2, &default_sel());

    let library_v3 = create_test_library(100.0);

    assert_eq!(history.undo_count(), 2);

    // Undo twice
    let (after_first_undo, _) = history.undo(&library_v3, &default_sel()).unwrap();
    let node = after_first_undo.root.child("ellipse1").unwrap();
    let x = node.input("x").unwrap().value.as_float().unwrap();
    assert!((x - 50.0).abs() < 0.001);

    let (after_second_undo, _) = history.undo(&after_first_undo, &default_sel()).unwrap();
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
    history.save_state(&library_v1, &default_sel());

    let library_v2 = create_test_library(50.0);
    history.save_state(&library_v2, &default_sel());

    let library_v3 = create_test_library(100.0);

    // Undo twice
    let (after_first_undo, _) = history.undo(&library_v3, &default_sel()).unwrap();
    let (after_second_undo, _) = history.undo(&after_first_undo, &default_sel()).unwrap();

    assert_eq!(history.redo_count(), 2);

    // Redo twice
    let (after_first_redo, _) = history.redo(&after_second_undo, &default_sel()).unwrap();
    let node = after_first_redo.root.child("ellipse1").unwrap();
    let x = node.input("x").unwrap().value.as_float().unwrap();
    assert!((x - 50.0).abs() < 0.001);

    let (after_second_redo, _) = history.redo(&after_first_redo, &default_sel()).unwrap();
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
    history.save_state(&library_v1, &default_sel());
    history.save_state(&library_v1, &default_sel());
    history.save_state(&library_v1, &default_sel());

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

    let result = history.undo(&library, &default_sel());
    assert!(result.is_none());
}

#[test]
fn test_history_redo_on_empty_returns_none() {
    let mut history = History::new();
    let library = create_test_library(0.0);

    let result = history.redo(&library, &default_sel());
    assert!(result.is_none());
}

// --- Undo group tests ---

#[test]
fn test_save_state_suppressed_during_group() {
    let mut history = History::new();
    let library_v1 = create_test_library(0.0);

    // Begin a group
    history.begin_undo_group(&library_v1, &default_sel());

    // Multiple save_state calls should be suppressed
    for i in 1..=5 {
        let lib = create_test_library(i as f64 * 10.0);
        history.save_state(&lib, &default_sel());
    }

    // End the group with the final state
    let library_final = create_test_library(50.0);
    history.end_undo_group(&library_final);

    // Only one undo entry (the group itself)
    assert_eq!(history.undo_count(), 1);
}

#[test]
fn test_group_undo_restores_pre_group_state() {
    let mut history = History::new();
    let library_v1 = create_test_library(0.0);

    // Begin group with state A (x=0)
    history.begin_undo_group(&library_v1, &default_sel());

    // Simulate intermediate changes during drag
    let library_v2 = create_test_library(25.0);
    history.save_state(&library_v2, &default_sel());
    let library_v3 = create_test_library(50.0);
    history.save_state(&library_v3, &default_sel());

    // End group with final state (x=50)
    history.end_undo_group(&library_v3);

    assert_eq!(history.undo_count(), 1);

    // Undo should restore the pre-group state (x=0)
    let (restored, _) = history.undo(&library_v3, &default_sel()).unwrap();
    let node = restored.root.child("ellipse1").unwrap();
    let x = node.input("x").unwrap().value.as_float().unwrap();
    assert!((x - 0.0).abs() < 0.001, "Expected x=0.0 (pre-group), got x={}", x);
}

#[test]
fn test_end_group_without_begin_is_noop() {
    let mut history = History::new();
    let library = create_test_library(0.0);

    // end_undo_group without begin should not crash or add entries
    history.end_undo_group(&library);

    assert_eq!(history.undo_count(), 0);
    assert!(!history.can_undo());
}

#[test]
fn test_no_op_group_creates_no_entry() {
    let mut history = History::new();
    let library = create_test_library(0.0);

    // Begin and end group with the same state (no actual changes)
    history.begin_undo_group(&library, &default_sel());
    history.end_undo_group(&library);

    // No undo entry should be created since nothing changed
    assert_eq!(history.undo_count(), 0);
}

#[test]
fn test_group_clears_redo_stack() {
    let mut history = History::new();

    // Set up: create some undo history, then undo to populate redo stack
    let library_v1 = create_test_library(0.0);
    history.save_state(&library_v1, &default_sel());
    let library_v2 = create_test_library(50.0);
    history.undo(&library_v2, &default_sel());
    assert!(history.can_redo());

    // Now perform a group operation
    let library_v3 = create_test_library(100.0);
    history.begin_undo_group(&library_v3, &default_sel());
    let library_v4 = create_test_library(200.0);
    history.end_undo_group(&library_v4);

    // Redo stack should be cleared by the group
    assert!(!history.can_redo());
    assert_eq!(history.redo_count(), 0);
}

#[test]
fn test_nested_begin_group_keeps_first() {
    let mut history = History::new();
    let library_a = create_test_library(0.0);
    let library_b = create_test_library(50.0);

    // First begin_undo_group captures state A
    history.begin_undo_group(&library_a, &default_sel());

    // Second begin_undo_group should be ignored (first wins)
    history.begin_undo_group(&library_b, &default_sel());

    let library_c = create_test_library(100.0);
    history.end_undo_group(&library_c);

    // Undo should restore A (from the first begin), not B
    let (restored, _) = history.undo(&library_c, &default_sel()).unwrap();
    let node = restored.root.child("ellipse1").unwrap();
    let x = node.input("x").unwrap().value.as_float().unwrap();
    assert!((x - 0.0).abs() < 0.001, "Expected x=0.0 (first begin), got x={}", x);
}

#[test]
fn test_group_followed_by_normal_saves() {
    let mut history = History::new();

    // Do a grouped operation
    let library_v1 = create_test_library(0.0);
    history.begin_undo_group(&library_v1, &default_sel());
    let library_v2 = create_test_library(50.0);
    history.end_undo_group(&library_v2);

    // Then do normal saves
    let library_v3 = create_test_library(75.0);
    history.save_state(&library_v3, &default_sel());

    // Should have 2 entries: the group + the normal save
    assert_eq!(history.undo_count(), 2);
}

#[test]
fn test_is_in_group() {
    let mut history = History::new();
    let library = create_test_library(0.0);

    assert!(!history.is_in_group());

    history.begin_undo_group(&library, &default_sel());
    assert!(history.is_in_group());

    history.end_undo_group(&library);
    assert!(!history.is_in_group());
}

// --- Selection snapshot tests ---

#[test]
fn test_undo_restores_selection_snapshot() {
    let mut history = History::new();
    let library_v1 = create_test_library(0.0);

    // Save state with ellipse1 selected
    let sel_v1 = SelectionSnapshot {
        selected_nodes: ["ellipse1".to_string()].into_iter().collect(),
        selected_node: Some("ellipse1".to_string()),
    };
    history.save_state(&library_v1, &sel_v1);

    // Current state: no selection
    let library_v2 = create_test_library(100.0);
    let sel_v2 = SelectionSnapshot::default();

    // Undo should restore the selection
    let (_, restored_sel) = history.undo(&library_v2, &sel_v2).unwrap();
    assert_eq!(restored_sel.selected_node, Some("ellipse1".to_string()));
    assert!(restored_sel.selected_nodes.contains("ellipse1"));
}

#[test]
fn test_redo_restores_selection_snapshot() {
    let mut history = History::new();
    let library_v1 = create_test_library(0.0);

    // Save state with no selection
    history.save_state(&library_v1, &default_sel());

    // Current state: ellipse1 selected
    let library_v2 = create_test_library(100.0);
    let sel_v2 = SelectionSnapshot {
        selected_nodes: ["ellipse1".to_string()].into_iter().collect(),
        selected_node: Some("ellipse1".to_string()),
    };

    // Undo (saves sel_v2 for redo)
    history.undo(&library_v2, &sel_v2);

    // Redo should restore sel_v2
    let (_, restored_sel) = history.redo(&library_v1, &default_sel()).unwrap();
    assert_eq!(restored_sel.selected_node, Some("ellipse1".to_string()));
}

#[test]
fn test_undo_group_restores_pre_group_selection() {
    let mut history = History::new();
    let library_v1 = create_test_library(0.0);

    // Begin group with ellipse1 selected
    let sel = SelectionSnapshot {
        selected_nodes: ["ellipse1".to_string()].into_iter().collect(),
        selected_node: Some("ellipse1".to_string()),
    };
    history.begin_undo_group(&library_v1, &sel);

    // End group with changed library
    let library_v2 = create_test_library(100.0);
    history.end_undo_group(&library_v2);

    // Undo should restore the pre-group selection
    let (_, restored_sel) = history.undo(&library_v2, &default_sel()).unwrap();
    assert_eq!(restored_sel.selected_node, Some("ellipse1".to_string()));
}
