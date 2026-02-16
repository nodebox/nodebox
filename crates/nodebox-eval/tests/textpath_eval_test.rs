//! Integration test: evaluate a textpath node through the full evaluation pipeline.

use std::sync::Arc;
use nodebox_core::geometry::Point;
use nodebox_core::node::{Node, NodeLibrary, Port};
use nodebox_core::platform::{ProjectContext, TestPlatform};
use nodebox_eval::eval::evaluate_network;
use nodebox_eval::NodeOutput;

fn make_textpath_library(text: &str, font_size: f64, position: Point) -> NodeLibrary {
    let mut lib = NodeLibrary::new("test");
    lib.root = Node::network("root")
        .with_child(
            Node::new("textpath1")
                .with_prototype("corevector.textpath")
                .with_input(Port::string("text", text))
                .with_input(Port::string("font_name", "Inter"))
                .with_input(Port::float("font_size", font_size))
                .with_input(Port::point("position", position))
                .with_input(Port::float("width", 0.0)),
        )
        .with_rendered_child("textpath1");
    lib
}

#[test]
fn test_textpath_produces_non_empty_path() {
    let lib = make_textpath_library("hello", 24.0, Point::ZERO);
    let platform: Arc<dyn nodebox_core::platform::Platform> = Arc::new(TestPlatform::new());
    let ctx = ProjectContext::new_unsaved();

    let (paths, output, errors) = evaluate_network(&lib, &platform, &ctx);

    assert!(errors.is_empty(), "textpath should not error: {:?}", errors);
    assert!(!paths.is_empty(), "textpath should produce geometry");
    assert!(matches!(output, NodeOutput::Path(_)), "output should be a Path");
}

#[test]
fn test_textpath_bounds_are_reasonable() {
    let lib = make_textpath_library("hello", 48.0, Point::ZERO);
    let platform: Arc<dyn nodebox_core::platform::Platform> = Arc::new(TestPlatform::new());
    let ctx = ProjectContext::new_unsaved();

    let (paths, _, errors) = evaluate_network(&lib, &platform, &ctx);
    assert!(errors.is_empty(), "errors: {:?}", errors);

    let path = &paths[0];
    let bounds = path.bounds().expect("path should have bounds");
    assert!(bounds.width > 10.0, "text should have reasonable width, got {}", bounds.width);
    assert!(bounds.height > 5.0, "text should have reasonable height, got {}", bounds.height);
}

#[test]
fn test_textpath_position_offset() {
    let lib_origin = make_textpath_library("X", 48.0, Point::ZERO);
    let lib_offset = make_textpath_library("X", 48.0, Point::new(100.0, 200.0));
    let platform: Arc<dyn nodebox_core::platform::Platform> = Arc::new(TestPlatform::new());
    let ctx = ProjectContext::new_unsaved();

    let (paths_origin, _, _) = evaluate_network(&lib_origin, &platform, &ctx);
    let (paths_offset, _, _) = evaluate_network(&lib_offset, &platform, &ctx);

    let b0 = paths_origin[0].bounds().unwrap();
    let b1 = paths_offset[0].bounds().unwrap();
    assert!((b1.x - b0.x - 100.0).abs() < 1.0, "X offset should be ~100");
    assert!((b1.y - b0.y - 200.0).abs() < 1.0, "Y offset should be ~200");
}

#[test]
fn test_textpath_empty_text() {
    let lib = make_textpath_library("", 24.0, Point::ZERO);
    let platform: Arc<dyn nodebox_core::platform::Platform> = Arc::new(TestPlatform::new());
    let ctx = ProjectContext::new_unsaved();

    let (paths, _, errors) = evaluate_network(&lib, &platform, &ctx);
    assert!(errors.is_empty());
    // Empty text produces an empty path
    assert!(paths.is_empty() || paths[0].contours.is_empty());
}
