//! Interactive handles for visual parameter editing on the canvas.

use eframe::egui::{self, Color32, Pos2, Stroke, Vec2};
use nodebox_core::geometry::Point;

/// The size of handle points.
const HANDLE_SIZE: f32 = 8.0;

/// Types of handles available.
#[derive(Clone, Copy, Debug, PartialEq)]
pub enum HandleType {
    /// A point that can be moved in any direction.
    Point,
    /// A horizontal line/bar.
    Horizontal,
    /// A vertical line/bar.
    Vertical,
    /// A rotation handle.
    Rotation,
    /// A scale handle.
    Scale,
}

/// A single handle on the canvas.
#[derive(Clone, Debug)]
pub struct Handle {
    /// The parameter name this handle controls.
    pub param_name: String,
    /// The handle type.
    pub handle_type: HandleType,
    /// The current position in world coordinates.
    pub position: Point,
    /// Whether this handle is being dragged.
    pub dragging: bool,
    /// Color of the handle.
    pub color: Color32,
}

impl Handle {
    /// Create a new point handle.
    pub fn point(param_name: impl Into<String>, position: Point) -> Self {
        Self {
            param_name: param_name.into(),
            handle_type: HandleType::Point,
            position,
            dragging: false,
            color: Color32::from_rgb(255, 100, 100),
        }
    }

    /// Create a new horizontal handle.
    pub fn horizontal(param_name: impl Into<String>, position: Point) -> Self {
        Self {
            param_name: param_name.into(),
            handle_type: HandleType::Horizontal,
            position,
            dragging: false,
            color: Color32::from_rgb(100, 255, 100),
        }
    }

    /// Create a new vertical handle.
    pub fn vertical(param_name: impl Into<String>, position: Point) -> Self {
        Self {
            param_name: param_name.into(),
            handle_type: HandleType::Vertical,
            position,
            dragging: false,
            color: Color32::from_rgb(100, 100, 255),
        }
    }

    /// Set the handle color.
    pub fn with_color(mut self, color: Color32) -> Self {
        self.color = color;
        self
    }
}

/// Manages a set of handles for a node.
pub struct HandleSet {
    /// The handles in this set.
    handles: Vec<Handle>,
    /// The node name these handles belong to.
    pub node_name: String,
}

impl HandleSet {
    /// Create a new empty handle set.
    pub fn new(node_name: impl Into<String>) -> Self {
        Self {
            handles: Vec::new(),
            node_name: node_name.into(),
        }
    }

    /// Add a handle to the set.
    pub fn add(&mut self, handle: Handle) {
        self.handles.push(handle);
    }

    /// Get handles mutably.
    pub fn handles_mut(&mut self) -> &mut Vec<Handle> {
        &mut self.handles
    }

    /// Get handles immutably.
    pub fn handles(&self) -> &[Handle] {
        &self.handles
    }

    /// Draw all handles on the canvas.
    pub fn draw(&self, painter: &egui::Painter, zoom: f32, pan: Vec2, center: Vec2) {
        for handle in &self.handles {
            let screen_pos = world_to_screen(handle.position, zoom, pan, center);
            self.draw_handle(painter, handle, screen_pos, zoom);
        }
    }

    /// Draw a single handle.
    fn draw_handle(&self, painter: &egui::Painter, handle: &Handle, pos: Pos2, zoom: f32) {
        let size = HANDLE_SIZE * zoom;
        let stroke_width = if handle.dragging { 2.0 } else { 1.0 };

        match handle.handle_type {
            HandleType::Point => {
                // Draw a filled circle with border
                painter.circle_filled(pos, size, handle.color);
                painter.circle_stroke(pos, size, Stroke::new(stroke_width, Color32::WHITE));
            }
            HandleType::Horizontal => {
                // Draw a horizontal bar
                let half_width = size * 2.0;
                let half_height = size * 0.5;
                let rect = egui::Rect::from_center_size(pos, Vec2::new(half_width * 2.0, half_height * 2.0));
                painter.rect_filled(rect, 2.0, handle.color);
                painter.rect_stroke(rect, 2.0, Stroke::new(stroke_width, Color32::WHITE));
            }
            HandleType::Vertical => {
                // Draw a vertical bar
                let half_width = size * 0.5;
                let half_height = size * 2.0;
                let rect = egui::Rect::from_center_size(pos, Vec2::new(half_width * 2.0, half_height * 2.0));
                painter.rect_filled(rect, 2.0, handle.color);
                painter.rect_stroke(rect, 2.0, Stroke::new(stroke_width, Color32::WHITE));
            }
            HandleType::Rotation => {
                // Draw a circular arc indicator
                painter.circle_stroke(pos, size * 1.5, Stroke::new(2.0, handle.color));
                painter.circle_filled(pos, size * 0.5, handle.color);
            }
            HandleType::Scale => {
                // Draw a diamond/square rotated 45 degrees
                let d = size;
                let points = vec![
                    Pos2::new(pos.x, pos.y - d),
                    Pos2::new(pos.x + d, pos.y),
                    Pos2::new(pos.x, pos.y + d),
                    Pos2::new(pos.x - d, pos.y),
                    Pos2::new(pos.x, pos.y - d),
                ];
                painter.add(egui::Shape::convex_polygon(
                    points.clone(),
                    handle.color,
                    Stroke::new(stroke_width, Color32::WHITE),
                ));
            }
        }
    }

    /// Check if a screen position is over any handle, returns the handle index.
    pub fn hit_test(&self, screen_pos: Pos2, zoom: f32, pan: Vec2, center: Vec2) -> Option<usize> {
        let hit_radius = HANDLE_SIZE * zoom * 1.5;

        for (i, handle) in self.handles.iter().enumerate() {
            let handle_screen_pos = world_to_screen(handle.position, zoom, pan, center);
            if handle_screen_pos.distance(screen_pos) < hit_radius {
                return Some(i);
            }
        }
        None
    }

    /// Update handle position from screen coordinates.
    pub fn update_handle_position(&mut self, index: usize, screen_pos: Pos2, zoom: f32, pan: Vec2, center: Vec2) {
        if let Some(handle) = self.handles.get_mut(index) {
            let world_pos = screen_to_world(screen_pos, zoom, pan, center);

            // Constrain based on handle type
            match handle.handle_type {
                HandleType::Horizontal => {
                    handle.position.x = world_pos.x;
                    // Keep y unchanged
                }
                HandleType::Vertical => {
                    // Keep x unchanged
                    handle.position.y = world_pos.y;
                }
                _ => {
                    handle.position = world_pos;
                }
            }
        }
    }
}

/// Convert world coordinates to screen coordinates.
pub fn world_to_screen(world: Point, zoom: f32, pan: Vec2, center: Vec2) -> Pos2 {
    Pos2::new(
        world.x as f32 * zoom + pan.x + center.x,
        world.y as f32 * zoom + pan.y + center.y,
    )
}

/// Convert screen coordinates to world coordinates.
pub fn screen_to_world(screen: Pos2, zoom: f32, pan: Vec2, center: Vec2) -> Point {
    Point::new(
        ((screen.x - pan.x - center.x) / zoom) as f64,
        ((screen.y - pan.y - center.y) / zoom) as f64,
    )
}

/// Create handles for an ellipse node.
pub fn ellipse_handles(x: f64, y: f64, width: f64, height: f64) -> Vec<Handle> {
    vec![
        Handle::point("position", Point::new(x, y))
            .with_color(Color32::from_rgb(255, 200, 100)),
        Handle::horizontal("width", Point::new(x + width / 2.0, y))
            .with_color(Color32::from_rgb(100, 200, 255)),
        Handle::vertical("height", Point::new(x, y + height / 2.0))
            .with_color(Color32::from_rgb(100, 255, 200)),
    ]
}

/// Create handles for a rect node.
pub fn rect_handles(x: f64, y: f64, width: f64, height: f64) -> Vec<Handle> {
    vec![
        Handle::point("position", Point::new(x, y))
            .with_color(Color32::from_rgb(255, 200, 100)),
        Handle::point("size", Point::new(x + width, y + height))
            .with_color(Color32::from_rgb(100, 200, 255)),
    ]
}

/// Create handles for a line node.
pub fn line_handles(point1: Point, point2: Point) -> Vec<Handle> {
    vec![
        Handle::point("point1", point1)
            .with_color(Color32::from_rgb(255, 100, 100)),
        Handle::point("point2", point2)
            .with_color(Color32::from_rgb(100, 255, 100)),
    ]
}

/// Create handles for transform operations.
pub fn transform_handles(center: Point, tx: f64, ty: f64) -> Vec<Handle> {
    vec![
        Handle::point("offset", Point::new(center.x + tx, center.y + ty))
            .with_color(Color32::from_rgb(255, 150, 100)),
    ]
}
