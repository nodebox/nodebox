//! Interactive handles for visual parameter editing on the canvas.

use eframe::egui::{self, Color32, Pos2, Stroke, Vec2};
use nodebox_core::geometry::Point;

/// The size of handle points (width/height of the square).
const HANDLE_SIZE: f32 = 6.0;

/// Blue color for handles (#3366ff).
pub const HANDLE_COLOR: Color32 = Color32::from_rgb(0x33, 0x66, 0xff);

/// Drag state for FourPointHandle.
#[derive(Clone, Copy, Debug, PartialEq, Default)]
pub enum FourPointDragState {
    #[default]
    None,
    TopLeft,
    TopRight,
    BottomRight,
    BottomLeft,
    Center,
}

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
    /// Handle sizes are fixed in screen space (don't scale with zoom).
    pub fn draw(&self, painter: &egui::Painter, zoom: f32, pan: Vec2, center: Vec2) {
        for handle in &self.handles {
            let screen_pos = world_to_screen(handle.position, zoom, pan, center);
            self.draw_handle(painter, handle, screen_pos);
        }
    }

    /// Draw a single handle as a blue square (consistent with FourPointHandle style).
    fn draw_handle(&self, painter: &egui::Painter, _handle: &Handle, pos: Pos2) {
        // All handles are drawn as blue squares with fixed screen size
        let rect = egui::Rect::from_center_size(pos, Vec2::splat(HANDLE_SIZE));
        painter.rect_filled(rect, 0.0, HANDLE_COLOR);
    }

    /// Check if a screen position is over any handle, returns the handle index.
    /// Uses fixed screen-space hit radius (doesn't scale with zoom).
    pub fn hit_test(&self, screen_pos: Pos2, zoom: f32, pan: Vec2, center: Vec2) -> Option<usize> {
        // Hit target is visual size plus 8 pixels padding (fixed screen size)
        let hit_radius = HANDLE_SIZE * 2.0;

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

/// Adobe Illustrator-style handle with bounding box and corner/center handles.
#[derive(Clone, Debug)]
pub struct FourPointHandle {
    /// The node name this handle controls.
    pub node_name: String,
    /// Center position of the rect.
    pub center: Point,
    /// Width of the rect.
    pub width: f64,
    /// Height of the rect.
    pub height: f64,
    /// Current drag state.
    pub drag_state: FourPointDragState,
    /// Position where drag started (world coordinates).
    pub drag_start: Option<Point>,
}

impl FourPointHandle {
    /// Create a new FourPointHandle.
    pub fn new(node_name: impl Into<String>, center: Point, width: f64, height: f64) -> Self {
        Self {
            node_name: node_name.into(),
            center,
            width,
            height,
            drag_state: FourPointDragState::None,
            drag_start: None,
        }
    }

    /// Compute corner positions from center/width/height.
    /// Returns [top_left, top_right, bottom_right, bottom_left].
    pub fn corners(&self) -> [Point; 4] {
        let half_w = self.width / 2.0;
        let half_h = self.height / 2.0;
        [
            Point::new(self.center.x - half_w, self.center.y - half_h), // top-left
            Point::new(self.center.x + half_w, self.center.y - half_h), // top-right
            Point::new(self.center.x + half_w, self.center.y + half_h), // bottom-right
            Point::new(self.center.x - half_w, self.center.y + half_h), // bottom-left
        ]
    }

    /// Draw the handle on the canvas.
    /// Handle sizes are fixed in screen space (don't scale with zoom).
    pub fn draw(&self, painter: &egui::Painter, zoom: f32, pan: Vec2, center: Vec2) {
        let corners = self.corners();
        let screen_corners: Vec<Pos2> = corners
            .iter()
            .map(|p| world_to_screen(*p, zoom, pan, center))
            .collect();
        let screen_center = world_to_screen(self.center, zoom, pan, center);

        // Draw bounding box lines
        let stroke = Stroke::new(1.0, HANDLE_COLOR);
        for i in 0..4 {
            let next = (i + 1) % 4;
            painter.line_segment([screen_corners[i], screen_corners[next]], stroke);
        }

        // Draw corner handles (6x6 squares) - fixed screen size, not affected by zoom
        let handle_size = HANDLE_SIZE;
        for corner in &screen_corners {
            let rect = egui::Rect::from_center_size(*corner, Vec2::splat(handle_size));
            painter.rect_filled(rect, 0.0, HANDLE_COLOR);
        }

        // Draw center handle
        let center_rect = egui::Rect::from_center_size(screen_center, Vec2::splat(handle_size));
        painter.rect_filled(center_rect, 0.0, HANDLE_COLOR);
    }

    /// Check which corner/center was clicked.
    /// Returns the drag state for the hit point, or None if no hit.
    /// Corners have expanded hit targets (5px padding). Interior clicks drag the center.
    pub fn hit_test(&self, screen_pos: Pos2, zoom: f32, pan: Vec2, center: Vec2) -> Option<FourPointDragState> {
        // Hit target is visual size plus 8 pixels padding on each side (fixed screen size)
        let hit_radius = HANDLE_SIZE * 2.0;
        let corners = self.corners();

        // Check corners first (in order: top-left, top-right, bottom-right, bottom-left)
        let states = [
            FourPointDragState::TopLeft,
            FourPointDragState::TopRight,
            FourPointDragState::BottomRight,
            FourPointDragState::BottomLeft,
        ];

        for (corner, state) in corners.iter().zip(states.iter()) {
            let screen_corner = world_to_screen(*corner, zoom, pan, center);
            if screen_corner.distance(screen_pos) < hit_radius {
                return Some(*state);
            }
        }

        // Check if inside the bounding box - drag center (like Illustrator)
        let screen_corners: Vec<Pos2> = corners
            .iter()
            .map(|p| world_to_screen(*p, zoom, pan, center))
            .collect();

        // Build bounding rect from screen corners
        let min_x = screen_corners.iter().map(|p| p.x).fold(f32::INFINITY, f32::min);
        let max_x = screen_corners.iter().map(|p| p.x).fold(f32::NEG_INFINITY, f32::max);
        let min_y = screen_corners.iter().map(|p| p.y).fold(f32::INFINITY, f32::min);
        let max_y = screen_corners.iter().map(|p| p.y).fold(f32::NEG_INFINITY, f32::max);

        if screen_pos.x >= min_x && screen_pos.x <= max_x &&
           screen_pos.y >= min_y && screen_pos.y <= max_y {
            return Some(FourPointDragState::Center);
        }

        None
    }

    /// Start dragging from the given world position.
    pub fn start_drag(&mut self, state: FourPointDragState, world_pos: Point) {
        self.drag_state = state;
        self.drag_start = Some(world_pos);
    }

    /// Update during drag. Returns the delta from drag start.
    pub fn update_drag(&mut self, world_pos: Point) {
        if let Some(start) = self.drag_start {
            let dx = world_pos.x - start.x;
            let dy = world_pos.y - start.y;

            match self.drag_state {
                FourPointDragState::TopLeft => {
                    // width -= dx*2, height -= dy*2
                    self.width = (self.width - dx * 2.0).max(1.0);
                    self.height = (self.height - dy * 2.0).max(1.0);
                }
                FourPointDragState::TopRight => {
                    // width += dx*2, height -= dy*2
                    self.width = (self.width + dx * 2.0).max(1.0);
                    self.height = (self.height - dy * 2.0).max(1.0);
                }
                FourPointDragState::BottomRight => {
                    // width += dx*2, height += dy*2
                    self.width = (self.width + dx * 2.0).max(1.0);
                    self.height = (self.height + dy * 2.0).max(1.0);
                }
                FourPointDragState::BottomLeft => {
                    // width -= dx*2, height += dy*2
                    self.width = (self.width - dx * 2.0).max(1.0);
                    self.height = (self.height + dy * 2.0).max(1.0);
                }
                FourPointDragState::Center => {
                    // Move position
                    self.center.x += dx;
                    self.center.y += dy;
                }
                FourPointDragState::None => {}
            }

            // Update drag start for continuous dragging
            self.drag_start = Some(world_pos);
        }
    }

    /// End dragging and return the final values.
    pub fn end_drag(&mut self) -> (f64, f64, f64, f64) {
        self.drag_state = FourPointDragState::None;
        self.drag_start = None;
        (self.center.x, self.center.y, self.width, self.height)
    }

    /// Check if currently dragging.
    pub fn is_dragging(&self) -> bool {
        self.drag_state != FourPointDragState::None
    }
}

/// Create a FourPointHandle for a rect node.
pub fn rect_four_point_handle(node_name: &str, x: f64, y: f64, width: f64, height: f64) -> FourPointHandle {
    FourPointHandle::new(node_name, Point::new(x, y), width, height)
}
