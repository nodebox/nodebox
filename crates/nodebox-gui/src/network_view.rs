//! Node network editor view.

use eframe::egui::{self, Color32, Pos2, Rect, Stroke, Vec2};
use nodebox_core::node::{Connection, Node, NodeLibrary, PortType};
use std::collections::HashSet;

/// The visual state of the network view.
pub struct NetworkView {
    /// Current zoom level.
    zoom: f32,
    /// Current pan offset.
    pan: Vec2,
    /// Currently selected node names.
    selected: HashSet<String>,
    /// Node being dragged, if any.
    dragging_node: Option<String>,
    /// Drag start position.
    drag_start: Option<Pos2>,
    /// Connection being created, if any.
    creating_connection: Option<ConnectionDrag>,
    /// Index of hovered connection, if any.
    hovered_connection: Option<usize>,
}

/// State for dragging a new connection.
struct ConnectionDrag {
    /// The output node name.
    from_node: String,
    /// Current mouse position (end of wire).
    to_pos: Pos2,
}

/// Visual constants.
const NODE_WIDTH: f32 = 150.0;
const NODE_HEADER_HEIGHT: f32 = 24.0;
const PORT_HEIGHT: f32 = 20.0;
const PORT_RADIUS: f32 = 5.0;
const GRID_SIZE: f32 = 20.0;

impl Default for NetworkView {
    fn default() -> Self {
        Self::new()
    }
}

impl NetworkView {
    /// Create a new network view.
    pub fn new() -> Self {
        Self {
            zoom: 1.0,
            pan: Vec2::ZERO,
            selected: HashSet::new(),
            dragging_node: None,
            drag_start: None,
            creating_connection: None,
            hovered_connection: None,
        }
    }

    /// Get the currently selected nodes.
    pub fn selected_nodes(&self) -> &HashSet<String> {
        &self.selected
    }

    /// Show the network view.
    pub fn show(&mut self, ui: &mut egui::Ui, library: &mut NodeLibrary) {
        let (response, painter) =
            ui.allocate_painter(ui.available_size(), egui::Sense::click_and_drag());

        let rect = response.rect;

        // Handle zoom with scroll wheel
        if response.hovered() {
            let scroll = ui.input(|i| i.raw_scroll_delta.y);
            if scroll != 0.0 {
                let zoom_factor = 1.0 + scroll * 0.001;
                self.zoom = (self.zoom * zoom_factor).clamp(0.25, 4.0);
            }
        }

        // Handle panning with middle mouse button
        if response.dragged_by(egui::PointerButton::Middle) {
            self.pan += response.drag_delta();
        }

        // Draw background grid
        self.draw_grid(&painter, rect);

        // Calculate transform offset (center of view + pan)
        let offset = rect.center().to_vec2() + self.pan;

        // Get the current network (root for now)
        let network = &library.root;

        // Track connection interactions
        let mut connection_to_delete: Option<usize> = None;
        self.hovered_connection = None;

        // Draw connections first (behind nodes) and detect hover
        for (conn_idx, conn) in network.connections.iter().enumerate() {
            let is_hovered = self.is_connection_hovered(ui, network, conn, offset);
            if is_hovered {
                self.hovered_connection = Some(conn_idx);
            }
            self.draw_connection(&painter, network, conn, offset, is_hovered);
        }

        // Check for connection deletion (right-click on hovered connection)
        if let Some(conn_idx) = self.hovered_connection {
            if ui.input(|i| i.pointer.button_clicked(egui::PointerButton::Secondary)) {
                connection_to_delete = Some(conn_idx);
            }
        }

        // Draw connection being created
        if let Some(ref drag) = self.creating_connection {
            if let Some(from_node) = network.child(&drag.from_node) {
                let from_pos = self.node_output_pos(from_node, offset);
                self.draw_wire(&painter, from_pos, drag.to_pos, Color32::WHITE);
            }
        }

        // Draw nodes
        let mut node_to_select = None;
        let mut node_to_drag = None;
        let mut connection_to_create: Option<(String, String, String)> = None;

        for child in &network.children {
            let node_rect = self.node_rect(child, offset);

            // Check for node interactions
            let node_response = ui.interact(node_rect, ui.id().with(&child.name), egui::Sense::click_and_drag());

            if node_response.clicked() {
                node_to_select = Some(child.name.clone());
            }

            if node_response.drag_started() {
                node_to_drag = Some(child.name.clone());
                self.drag_start = ui.input(|i| i.pointer.hover_pos());
            }

            // Draw the node
            let is_selected = self.selected.contains(&child.name);
            let is_rendered = network.rendered_child.as_deref() == Some(&child.name);
            self.draw_node(&painter, ui, child, offset, is_selected, is_rendered);

            // Check for output port click (to start connection)
            let output_pos = self.node_output_pos(child, offset);
            let output_rect = Rect::from_center_size(output_pos, Vec2::splat(PORT_RADIUS * 3.0));
            let output_response = ui.interact(output_rect, ui.id().with(format!("{}_out", child.name)), egui::Sense::drag());

            if output_response.drag_started() {
                self.creating_connection = Some(ConnectionDrag {
                    from_node: child.name.clone(),
                    to_pos: output_pos,
                });
            }

            // Check for input port clicks (to complete connection)
            for (i, port) in child.inputs.iter().enumerate() {
                let port_pos = self.node_input_pos(child, i, offset);
                let port_rect = Rect::from_center_size(port_pos, Vec2::splat(PORT_RADIUS * 3.0));

                // If we're dragging a connection and hover over this port
                if self.creating_connection.is_some() {
                    let port_response = ui.interact(port_rect, ui.id().with(format!("{}_{}", child.name, port.name)), egui::Sense::hover());

                    if port_response.hovered() {
                        // Highlight the port
                        painter.circle_filled(port_pos, PORT_RADIUS + 2.0, Color32::YELLOW);
                    }
                }
            }
        }

        // Handle connection creation end
        if self.creating_connection.is_some() && ui.input(|i| i.pointer.any_released()) {
            if let Some(hover_pos) = ui.input(|i| i.pointer.hover_pos()) {
                // Find which input port we're over
                for child in &network.children {
                    for (i, port) in child.inputs.iter().enumerate() {
                        let port_pos = self.node_input_pos(child, i, offset);
                        if port_pos.distance(hover_pos) < PORT_RADIUS * 3.0 {
                            if let Some(ref drag) = self.creating_connection {
                                connection_to_create = Some((
                                    drag.from_node.clone(),
                                    child.name.clone(),
                                    port.name.clone(),
                                ));
                            }
                        }
                    }
                }
            }
            self.creating_connection = None;
        }

        // Update connection drag position
        if let Some(ref mut drag) = self.creating_connection {
            if let Some(pos) = ui.input(|i| i.pointer.hover_pos()) {
                drag.to_pos = pos;
            }
        }

        // Handle selection
        let had_node_selection = node_to_select.is_some();
        if let Some(name) = node_to_select {
            if ui.input(|i| i.modifiers.shift) {
                // Toggle selection with shift
                if self.selected.contains(&name) {
                    self.selected.remove(&name);
                } else {
                    self.selected.insert(name);
                }
            } else {
                // Replace selection
                self.selected.clear();
                self.selected.insert(name);
            }
        }

        // Handle node dragging
        if let Some(name) = node_to_drag {
            self.dragging_node = Some(name);
        }

        if self.dragging_node.is_some() {
            if ui.input(|i| i.pointer.any_released()) {
                self.dragging_node = None;
                self.drag_start = None;
            }
        }

        // Apply drag delta to dragged node
        if let Some(ref name) = self.dragging_node {
            let delta = response.drag_delta() / self.zoom;
            if delta != Vec2::ZERO {
                if let Some(node) = library.root.child_mut(name) {
                    node.position.x += delta.x as f64;
                    node.position.y += delta.y as f64;
                }
            }
        }

        // Create connection if needed
        if let Some((from, to, port)) = connection_to_create {
            library.root.connections.push(Connection::new(from, to, port));
        }

        // Delete connection if needed
        if let Some(conn_idx) = connection_to_delete {
            library.root.connections.remove(conn_idx);
        }

        // Handle delete key for selected nodes
        if ui.input(|i| i.key_pressed(egui::Key::Delete) || i.key_pressed(egui::Key::Backspace)) {
            // Delete selected nodes
            for name in &self.selected {
                // Remove node
                library.root.children.retain(|n| &n.name != name);
                // Remove connections involving this node
                library.root.connections.retain(|c| &c.output_node != name && &c.input_node != name);
            }
            self.selected.clear();
        }

        // Click on empty space clears selection
        if response.clicked() && !had_node_selection {
            self.selected.clear();
        }
    }

    /// Draw the background grid.
    fn draw_grid(&self, painter: &egui::Painter, rect: Rect) {
        let grid_size = GRID_SIZE * self.zoom;
        let grid_color = Color32::from_rgba_unmultiplied(100, 100, 100, 40);

        let offset = self.pan;
        let offset_x = offset.x % grid_size;
        let offset_y = offset.y % grid_size;

        // Vertical lines
        let mut x = rect.left() + offset_x;
        while x < rect.right() {
            painter.line_segment(
                [Pos2::new(x, rect.top()), Pos2::new(x, rect.bottom())],
                Stroke::new(1.0, grid_color),
            );
            x += grid_size;
        }

        // Horizontal lines
        let mut y = rect.top() + offset_y;
        while y < rect.bottom() {
            painter.line_segment(
                [Pos2::new(rect.left(), y), Pos2::new(rect.right(), y)],
                Stroke::new(1.0, grid_color),
            );
            y += grid_size;
        }
    }

    /// Get the screen rectangle for a node.
    fn node_rect(&self, node: &Node, offset: Vec2) -> Rect {
        let pos = Pos2::new(
            node.position.x as f32 * self.zoom + offset.x,
            node.position.y as f32 * self.zoom + offset.y,
        );
        let height = NODE_HEADER_HEIGHT + node.inputs.len() as f32 * PORT_HEIGHT + 10.0;
        Rect::from_min_size(pos, Vec2::new(NODE_WIDTH * self.zoom, height * self.zoom))
    }

    /// Get the screen position of a node's output port.
    fn node_output_pos(&self, node: &Node, offset: Vec2) -> Pos2 {
        let rect = self.node_rect(node, offset);
        Pos2::new(rect.right(), rect.top() + NODE_HEADER_HEIGHT * 0.5 * self.zoom)
    }

    /// Get the screen position of a node's input port.
    fn node_input_pos(&self, node: &Node, port_index: usize, offset: Vec2) -> Pos2 {
        let rect = self.node_rect(node, offset);
        Pos2::new(
            rect.left(),
            rect.top() + (NODE_HEADER_HEIGHT + port_index as f32 * PORT_HEIGHT + PORT_HEIGHT * 0.5) * self.zoom,
        )
    }

    /// Draw a node.
    fn draw_node(
        &self,
        painter: &egui::Painter,
        _ui: &egui::Ui,
        node: &Node,
        offset: Vec2,
        is_selected: bool,
        is_rendered: bool,
    ) {
        let rect = self.node_rect(node, offset);

        // Node background
        let bg_color = if is_selected {
            Color32::from_rgb(60, 80, 100)
        } else {
            Color32::from_rgb(40, 44, 52)
        };
        painter.rect_filled(rect, 4.0 * self.zoom, bg_color);

        // Node border
        let border_color = if is_rendered {
            Color32::from_rgb(255, 200, 100)
        } else if is_selected {
            Color32::from_rgb(100, 150, 200)
        } else {
            Color32::from_rgb(80, 80, 80)
        };
        painter.rect_stroke(rect, 4.0 * self.zoom, Stroke::new(2.0 * self.zoom, border_color));

        // Node header
        let header_rect = Rect::from_min_size(rect.min, Vec2::new(rect.width(), NODE_HEADER_HEIGHT * self.zoom));
        let header_color = self.category_color(&node.category);
        painter.rect_filled(
            Rect::from_min_max(header_rect.min, Pos2::new(header_rect.max.x, header_rect.max.y)),
            egui::Rounding {
                nw: 4.0 * self.zoom,
                ne: 4.0 * self.zoom,
                sw: 0.0,
                se: 0.0,
            },
            header_color,
        );

        // Node name
        let text_pos = header_rect.center();
        painter.text(
            text_pos,
            egui::Align2::CENTER_CENTER,
            &node.name,
            egui::FontId::proportional(12.0 * self.zoom),
            Color32::WHITE,
        );

        // Output port
        let output_pos = self.node_output_pos(node, offset);
        painter.circle_filled(output_pos, PORT_RADIUS * self.zoom, self.port_type_color(&node.output_type));
        painter.circle_stroke(output_pos, PORT_RADIUS * self.zoom, Stroke::new(1.0, Color32::WHITE));

        // Input ports
        for (i, port) in node.inputs.iter().enumerate() {
            let port_pos = self.node_input_pos(node, i, offset);

            // Port circle
            painter.circle_filled(port_pos, PORT_RADIUS * self.zoom, self.port_type_color(&port.port_type));
            painter.circle_stroke(port_pos, PORT_RADIUS * self.zoom, Stroke::new(1.0, Color32::WHITE));

            // Port label
            let label_pos = Pos2::new(port_pos.x + 10.0 * self.zoom, port_pos.y);
            painter.text(
                label_pos,
                egui::Align2::LEFT_CENTER,
                &port.name,
                egui::FontId::proportional(10.0 * self.zoom),
                Color32::LIGHT_GRAY,
            );
        }
    }

    /// Check if a connection is being hovered.
    fn is_connection_hovered(&self, ui: &egui::Ui, network: &Node, conn: &Connection, offset: Vec2) -> bool {
        let from_node = network.child(&conn.output_node);
        let to_node = network.child(&conn.input_node);

        if let (Some(from), Some(to)) = (from_node, to_node) {
            let from_pos = self.node_output_pos(from, offset);
            let port_index = to.inputs.iter().position(|p| p.name == conn.input_port).unwrap_or(0);
            let to_pos = self.node_input_pos(to, port_index, offset);

            // Check if mouse is near the bezier curve
            if let Some(mouse_pos) = ui.input(|i| i.pointer.hover_pos()) {
                // Sample the bezier curve and check distance
                let control_distance = ((to_pos.x - from_pos.x).abs() * 0.5).max(50.0 * self.zoom);
                let ctrl1 = Pos2::new(from_pos.x + control_distance, from_pos.y);
                let ctrl2 = Pos2::new(to_pos.x - control_distance, to_pos.y);

                for i in 0..32 {
                    let t = i as f32 / 31.0;
                    let pt = cubic_bezier(from_pos, ctrl1, ctrl2, to_pos, t);
                    if pt.distance(mouse_pos) < 8.0 * self.zoom {
                        return true;
                    }
                }
            }
        }
        false
    }

    /// Draw a connection between nodes.
    fn draw_connection(&self, painter: &egui::Painter, network: &Node, conn: &Connection, offset: Vec2, is_hovered: bool) {
        // Find the source and target nodes
        let from_node = network.child(&conn.output_node);
        let to_node = network.child(&conn.input_node);

        if let (Some(from), Some(to)) = (from_node, to_node) {
            let from_pos = self.node_output_pos(from, offset);

            // Find the input port index
            let port_index = to.inputs.iter().position(|p| p.name == conn.input_port).unwrap_or(0);
            let to_pos = self.node_input_pos(to, port_index, offset);

            // Get the port type for coloring
            let port_type = to.input(conn.input_port.as_str())
                .map(|p| &p.port_type)
                .unwrap_or(&PortType::Geometry);

            let color = if is_hovered {
                Color32::from_rgb(255, 100, 100) // Red for hovered (indicating can delete)
            } else {
                self.port_type_color(port_type)
            };

            let width = if is_hovered { 3.0 } else { 2.0 };
            self.draw_wire_with_width(painter, from_pos, to_pos, color, width);
        }
    }

    /// Draw a wire (bezier curve) between two points.
    fn draw_wire(&self, painter: &egui::Painter, from: Pos2, to: Pos2, color: Color32) {
        self.draw_wire_with_width(painter, from, to, color, 2.0);
    }

    /// Draw a wire (bezier curve) between two points with custom width.
    fn draw_wire_with_width(&self, painter: &egui::Painter, from: Pos2, to: Pos2, color: Color32, width: f32) {
        let control_distance = ((to.x - from.x).abs() * 0.5).max(50.0 * self.zoom);
        let ctrl1 = Pos2::new(from.x + control_distance, from.y);
        let ctrl2 = Pos2::new(to.x - control_distance, to.y);

        // Sample the bezier curve
        let mut points = Vec::with_capacity(32);
        for i in 0..=31 {
            let t = i as f32 / 31.0;
            let pt = cubic_bezier(from, ctrl1, ctrl2, to, t);
            points.push(pt);
        }

        painter.add(egui::Shape::line(points, Stroke::new(width * self.zoom, color)));
    }

    /// Get a color for a category.
    fn category_color(&self, category: &str) -> Color32 {
        match category.to_lowercase().as_str() {
            "geometry" | "corevector" => Color32::from_rgb(80, 120, 200),
            "transform" => Color32::from_rgb(200, 120, 80),
            "color" => Color32::from_rgb(200, 80, 120),
            "math" => Color32::from_rgb(120, 200, 80),
            "list" => Color32::from_rgb(200, 200, 80),
            "string" => Color32::from_rgb(180, 80, 200),
            "data" => Color32::from_rgb(80, 200, 200),
            _ => Color32::from_rgb(100, 100, 100),
        }
    }

    /// Get a color for a port type.
    fn port_type_color(&self, port_type: &PortType) -> Color32 {
        match port_type {
            PortType::Int => Color32::from_rgb(100, 200, 255),
            PortType::Float => Color32::from_rgb(100, 200, 255),
            PortType::String => Color32::from_rgb(255, 200, 100),
            PortType::Boolean => Color32::from_rgb(255, 100, 100),
            PortType::Point => Color32::from_rgb(200, 255, 100),
            PortType::Color => Color32::from_rgb(255, 100, 200),
            PortType::Geometry => Color32::from_rgb(200, 200, 200),
            PortType::List => Color32::from_rgb(255, 200, 150),
            _ => Color32::from_rgb(150, 150, 150),
        }
    }
}

/// Evaluate a cubic bezier curve at parameter t.
fn cubic_bezier(p0: Pos2, p1: Pos2, p2: Pos2, p3: Pos2, t: f32) -> Pos2 {
    let t2 = t * t;
    let t3 = t2 * t;
    let mt = 1.0 - t;
    let mt2 = mt * mt;
    let mt3 = mt2 * mt;

    Pos2::new(
        mt3 * p0.x + 3.0 * mt2 * t * p1.x + 3.0 * mt * t2 * p2.x + t3 * p3.x,
        mt3 * p0.y + 3.0 * mt2 * t * p1.y + 3.0 * mt * t2 * p2.y + t3 * p3.y,
    )
}
