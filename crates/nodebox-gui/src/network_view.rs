//! Node network editor view.

use eframe::egui::{self, Color32, Pos2, Rect, Stroke, Vec2};
use nodebox_core::geometry::Point;
use nodebox_core::node::{Connection, Node, NodeLibrary, PortType};
use std::collections::HashSet;

use crate::pan_zoom::PanZoom;

/// Actions that can be triggered by the network view.
#[derive(Debug, Clone)]
pub enum NetworkAction {
    /// No action.
    None,
    /// Open the node selection dialog at the given position (in grid units).
    OpenNodeDialog(Point),
}

/// The visual state of the network view.
pub struct NetworkView {
    /// Pan and zoom state.
    pan_zoom: PanZoom,
    /// Currently selected node names.
    selected: HashSet<String>,
    /// Whether we are currently dragging the selection.
    is_dragging_selection: bool,
    /// Connection being created, if any.
    creating_connection: Option<ConnectionDrag>,
    /// Index of hovered connection, if any.
    hovered_connection: Option<usize>,
    /// Whether space bar is currently pressed (for panning).
    is_space_pressed: bool,
    /// Whether we are currently panning with space+drag.
    is_panning: bool,
    /// Currently hovered input port (node_name, port_name).
    hovered_port: Option<(String, String)>,
    /// Currently hovered output port (node_name).
    hovered_output: Option<String>,
}

/// State for dragging a new connection.
struct ConnectionDrag {
    /// The output node name.
    from_node: String,
    /// Current mouse position (end of wire).
    to_pos: Pos2,
}

/// Visual constants (matching NodeBox Java).
const GRID_CELL_SIZE: f32 = 48.0;
const NODE_MARGIN: f32 = 6.0;
const NODE_WIDTH: f32 = 132.0; // 48*3 - 6*2
const NODE_HEIGHT: f32 = 36.0; // 48 - 6*2
const NODE_ICON_SIZE: f32 = 26.0;
const NODE_PADDING: f32 = 5.0;
const PORT_WIDTH: f32 = 10.0;
const PORT_HEIGHT: f32 = 3.0;
const PORT_SPACING: f32 = 10.0;

/// Colors matching NodeBox Java Theme.
const NETWORK_BACKGROUND_COLOR: (u8, u8, u8) = (69, 69, 69);
const NETWORK_GRID_COLOR: (u8, u8, u8) = (85, 85, 85);
#[allow(dead_code)]
const CONNECTION_DEFAULT_COLOR: (u8, u8, u8) = (200, 200, 200);

impl Default for NetworkView {
    fn default() -> Self {
        Self::new()
    }
}

impl NetworkView {
    /// Create a new network view.
    pub fn new() -> Self {
        Self {
            pan_zoom: PanZoom::with_zoom_limits(0.25, 4.0),
            selected: HashSet::new(),
            is_dragging_selection: false,
            creating_connection: None,
            hovered_connection: None,
            is_space_pressed: false,
            is_panning: false,
            hovered_port: None,
            hovered_output: None,
        }
    }

    /// Get the currently selected nodes.
    pub fn selected_nodes(&self) -> &HashSet<String> {
        &self.selected
    }

    /// Show the network view. Returns any action that should be handled by the app.
    pub fn show(&mut self, ui: &mut egui::Ui, library: &mut NodeLibrary) -> NetworkAction {
        let mut action = NetworkAction::None;

        let (response, painter) =
            ui.allocate_painter(ui.available_size(), egui::Sense::click_and_drag());

        let rect = response.rect;

        // Handle zoom with scroll wheel, centered on mouse position
        // Origin is at top-left of the view (rect.min)
        let origin = rect.min.to_vec2();
        self.pan_zoom.handle_scroll_zoom(rect, ui, origin);

        // Track space bar state for Photoshop-style panning
        if ui.input(|i| i.key_pressed(egui::Key::Space)) {
            self.is_space_pressed = true;
        }
        if ui.input(|i| i.key_released(egui::Key::Space)) {
            self.is_space_pressed = false;
            self.is_panning = false;
        }

        // Handle panning with space+drag OR middle mouse button
        if self.is_space_pressed && response.dragged_by(egui::PointerButton::Primary) {
            self.pan_zoom.pan += response.drag_delta();
            self.is_panning = true;
        } else if response.dragged_by(egui::PointerButton::Middle) {
            self.pan_zoom.pan += response.drag_delta();
        }

        // Change cursor when space is held (panning mode)
        if self.is_space_pressed && response.hovered() {
            if self.is_panning {
                ui.ctx().set_cursor_icon(egui::CursorIcon::Grabbing);
            } else {
                ui.ctx().set_cursor_icon(egui::CursorIcon::Grab);
            }
        }

        // Draw background grid
        self.draw_grid(&painter, rect);

        // Calculate transform offset (top-left of view + pan)
        let offset = rect.min.to_vec2() + self.pan_zoom.pan;

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

        // Reset hover state
        self.hovered_port = None;
        self.hovered_output = None;

        // Draw nodes
        let mut node_to_select = None;
        let mut start_dragging_node: Option<String> = None;
        let mut connection_to_create: Option<(String, String, String)> = None;

        // First pass: detect port hover states
        if let Some(mouse_pos) = ui.input(|i| i.pointer.hover_pos()) {
            for child in &network.children {
                // Check output port hover
                let output_pos = self.node_output_pos(child, offset);
                let output_rect = Rect::from_min_size(
                    output_pos,
                    Vec2::new(PORT_WIDTH * self.pan_zoom.zoom, PORT_HEIGHT * self.pan_zoom.zoom),
                )
                .expand(4.0 * self.pan_zoom.zoom);
                if output_rect.contains(mouse_pos) {
                    self.hovered_output = Some(child.name.clone());
                }

                // Check input port hover
                for (i, port) in child.inputs.iter().enumerate() {
                    if is_hidden_port(&port.port_type) {
                        continue;
                    }
                    let port_pos = self.node_input_pos(child, i, offset);
                    let port_rect = Rect::from_min_size(
                        port_pos,
                        Vec2::new(PORT_WIDTH * self.pan_zoom.zoom, PORT_HEIGHT * self.pan_zoom.zoom),
                    )
                    .expand(4.0 * self.pan_zoom.zoom);
                    if port_rect.contains(mouse_pos) {
                        self.hovered_port = Some((child.name.clone(), port.name.clone()));
                    }
                }
            }
        }

        // Track node to set as rendered (on double-click)
        let mut node_to_render: Option<String> = None;

        for child in &network.children {
            let node_rect = self.node_rect(child, offset);

            // Check for node interactions
            let node_response =
                ui.interact(node_rect, ui.id().with(&child.name), egui::Sense::click_and_drag());

            if node_response.clicked() {
                node_to_select = Some(child.name.clone());
            }

            // Double-click sets the node as rendered
            if node_response.double_clicked() {
                node_to_render = Some(child.name.clone());
            }

            if node_response.drag_started() && !self.is_panning {
                start_dragging_node = Some(child.name.clone());
            }

            // Draw the node
            let is_selected = self.selected.contains(&child.name);
            let is_rendered = network.rendered_child.as_deref() == Some(&child.name);
            self.draw_node(&painter, ui, child, offset, is_selected, is_rendered);

            // Check for output port click (to start connection)
            let output_pos = self.node_output_pos(child, offset);
            let output_rect = Rect::from_min_size(
                output_pos,
                Vec2::new(PORT_WIDTH * self.pan_zoom.zoom, PORT_HEIGHT * self.pan_zoom.zoom),
            )
            .expand(4.0 * self.pan_zoom.zoom);
            let output_response = ui.interact(
                output_rect,
                ui.id().with(format!("{}_out", child.name)),
                egui::Sense::drag(),
            );

            if output_response.drag_started() {
                self.creating_connection = Some(ConnectionDrag {
                    from_node: child.name.clone(),
                    to_pos: output_pos,
                });
            }

            // Check for input port clicks (to complete connection)
            for (i, port) in child.inputs.iter().enumerate() {
                if is_hidden_port(&port.port_type) {
                    continue;
                }
                let port_pos = self.node_input_pos(child, i, offset);
                let port_rect = Rect::from_min_size(
                    port_pos,
                    Vec2::new(PORT_WIDTH * self.pan_zoom.zoom, PORT_HEIGHT * self.pan_zoom.zoom),
                )
                .expand(4.0 * self.pan_zoom.zoom);

                // If we're dragging a connection and hover over this port
                if self.creating_connection.is_some() {
                    let port_response = ui.interact(
                        port_rect,
                        ui.id().with(format!("{}_{}", child.name, port.name)),
                        egui::Sense::hover(),
                    );

                    if port_response.hovered() {
                        // Highlight indicator is now handled in draw_node via hovered_port
                    }
                }
            }
        }

        // Draw port tooltip if hovering
        if let Some((node_name, port_name)) = &self.hovered_port {
            if let Some(node) = network.child(node_name) {
                if let Some(port) = node.input(port_name) {
                    if let Some(mouse_pos) = ui.input(|i| i.pointer.hover_pos()) {
                        let tooltip_text = format!("{} ({:?})", port_name, port.port_type);
                        let tooltip_pos = Pos2::new(mouse_pos.x + 10.0, mouse_pos.y - 20.0);
                        let font = egui::FontId::proportional(11.0);
                        let galley = painter.layout_no_wrap(tooltip_text, font, Color32::WHITE);
                        let tooltip_rect = Rect::from_min_size(
                            tooltip_pos,
                            galley.size() + Vec2::splat(8.0),
                        );
                        painter.rect_filled(tooltip_rect, 4.0, Color32::from_rgb(50, 50, 50));
                        painter.galley(tooltip_pos + Vec2::splat(4.0), galley, Color32::WHITE);
                    }
                }
            }
        }

        // Draw output tooltip if hovering
        if let Some(node_name) = &self.hovered_output {
            if let Some(node) = network.child(node_name) {
                if self.creating_connection.is_none() {
                    if let Some(mouse_pos) = ui.input(|i| i.pointer.hover_pos()) {
                        let tooltip_text = format!("output ({:?})", node.output_type);
                        let tooltip_pos = Pos2::new(mouse_pos.x + 10.0, mouse_pos.y - 20.0);
                        let font = egui::FontId::proportional(11.0);
                        let galley = painter.layout_no_wrap(tooltip_text, font, Color32::WHITE);
                        let tooltip_rect = Rect::from_min_size(
                            tooltip_pos,
                            galley.size() + Vec2::splat(8.0),
                        );
                        painter.rect_filled(tooltip_rect, 4.0, Color32::from_rgb(50, 50, 50));
                        painter.galley(tooltip_pos + Vec2::splat(4.0), galley, Color32::WHITE);
                    }
                }
            }
        }

        // Handle connection creation end
        if self.creating_connection.is_some() && ui.input(|i| i.pointer.any_released()) {
            if let Some(hover_pos) = ui.input(|i| i.pointer.hover_pos()) {
                // Find which input port we're over (using rectangular hit detection)
                for child in &network.children {
                    for (i, port) in child.inputs.iter().enumerate() {
                        if is_hidden_port(&port.port_type) {
                            continue;
                        }
                        let port_pos = self.node_input_pos(child, i, offset);
                        let port_rect = Rect::from_min_size(
                            port_pos,
                            Vec2::new(PORT_WIDTH * self.pan_zoom.zoom, PORT_HEIGHT * self.pan_zoom.zoom),
                        )
                        .expand(4.0 * self.pan_zoom.zoom);
                        if port_rect.contains(hover_pos) {
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

        // Handle selection dragging
        if let Some(name) = start_dragging_node {
            // If the node is not already selected, select only this node
            if !self.selected.contains(&name) {
                self.selected.clear();
                self.selected.insert(name);
            }
            self.is_dragging_selection = true;
        }

        // Apply drag delta to all selected nodes
        if self.is_dragging_selection {
            let pointer_delta = ui.input(|i| {
                if i.pointer.is_decidedly_dragging() {
                    i.pointer.delta()
                } else {
                    Vec2::ZERO
                }
            });
            let delta = pointer_delta / (self.pan_zoom.zoom * GRID_CELL_SIZE);
            if delta != Vec2::ZERO {
                for name in &self.selected {
                    if let Some(node) = library.root.child_mut(name) {
                        node.position.x += delta.x as f64;
                        node.position.y += delta.y as f64;
                    }
                }
            }
        }

        // Snap all selected nodes to grid when drag ends
        if self.is_dragging_selection && ui.input(|i| i.pointer.any_released()) {
            for name in &self.selected {
                if let Some(node) = library.root.child_mut(name) {
                    node.position.x = node.position.x.round();
                    node.position.y = node.position.y.round();
                }
            }
            self.is_dragging_selection = false;
        }

        // Set rendered node (on double-click)
        if let Some(name) = node_to_render {
            library.root.rendered_child = Some(name);
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

        // Double-click on empty space opens node dialog
        if response.double_clicked() && !had_node_selection {
            // Convert screen position to grid position
            if let Some(mouse_pos) = ui.input(|i| i.pointer.hover_pos()) {
                let grid_pos = self.screen_to_grid(mouse_pos, offset);
                action = NetworkAction::OpenNodeDialog(Point::new(
                    grid_pos.x.round() as f64,
                    grid_pos.y.round() as f64,
                ));
            }
        }

        action
    }

    /// Convert screen position to grid position.
    fn screen_to_grid(&self, screen_pos: Pos2, offset: Vec2) -> Pos2 {
        let local = screen_pos - offset;
        Pos2::new(
            local.x / (self.pan_zoom.zoom * GRID_CELL_SIZE),
            local.y / (self.pan_zoom.zoom * GRID_CELL_SIZE),
        )
    }

    /// Draw the background grid (Java NodeBox style).
    fn draw_grid(&self, painter: &egui::Painter, rect: Rect) {
        // Background color
        let bg_color = Color32::from_rgb(
            NETWORK_BACKGROUND_COLOR.0,
            NETWORK_BACKGROUND_COLOR.1,
            NETWORK_BACKGROUND_COLOR.2,
        );
        painter.rect_filled(rect, 0.0, bg_color);

        // Grid lines
        let grid_size = GRID_CELL_SIZE * self.pan_zoom.zoom;
        let grid_color = Color32::from_rgb(
            NETWORK_GRID_COLOR.0,
            NETWORK_GRID_COLOR.1,
            NETWORK_GRID_COLOR.2,
        );

        // Grid origin is at top-left + pan (same as node coordinate system origin)
        let origin_x = rect.left() + self.pan_zoom.pan.x;
        let origin_y = rect.top() + self.pan_zoom.pan.y;

        // Find offset from rect edge to first grid line (using rem_euclid for correct modulo)
        let offset_x = (origin_x - rect.left()).rem_euclid(grid_size);
        let offset_y = (origin_y - rect.top()).rem_euclid(grid_size);

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

    /// Get the screen rectangle for a node (fixed size, grid-positioned with margin).
    fn node_rect(&self, node: &Node, offset: Vec2) -> Rect {
        // Position on grid using GRID_CELL_SIZE, with NODE_MARGIN inset
        let grid_x = node.position.x as f32 * GRID_CELL_SIZE + NODE_MARGIN;
        let grid_y = node.position.y as f32 * GRID_CELL_SIZE + NODE_MARGIN;
        let pos = Pos2::new(
            grid_x * self.pan_zoom.zoom + offset.x,
            grid_y * self.pan_zoom.zoom + offset.y,
        );
        Rect::from_min_size(pos, Vec2::new(NODE_WIDTH * self.pan_zoom.zoom, NODE_HEIGHT * self.pan_zoom.zoom))
    }

    /// Get the screen position of a node's output port (bottom left).
    fn node_output_pos(&self, node: &Node, offset: Vec2) -> Pos2 {
        let rect = self.node_rect(node, offset);
        Pos2::new(rect.left(), rect.bottom())
    }

    /// Get the screen position of a node's input port (distributed along top edge).
    fn node_input_pos(&self, node: &Node, port_index: usize, offset: Vec2) -> Pos2 {
        let rect = self.node_rect(node, offset);
        let port_x = (PORT_WIDTH + PORT_SPACING) * port_index as f32;
        Pos2::new(
            rect.left() + port_x * self.pan_zoom.zoom,
            rect.top() - PORT_HEIGHT * self.pan_zoom.zoom, // Above the node
        )
    }

    /// Draw a node (Java NodeBox style).
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
        let body_color = self.output_type_color(&node.output_type);

        // 1. Selection ring (white fill behind, 2px inset)
        if is_selected {
            painter.rect_filled(rect, 0.0, Color32::WHITE);
            let inset = Rect::from_min_max(
                rect.min + Vec2::splat(2.0 * self.pan_zoom.zoom),
                rect.max - Vec2::splat(2.0 * self.pan_zoom.zoom),
            );
            painter.rect_filled(inset, 0.0, body_color);
        } else {
            // Node body colored by output type
            painter.rect_filled(rect, 0.0, body_color);
        }

        // 2. Rendered indicator (white triangle in bottom-right corner)
        if is_rendered {
            let points = vec![
                Pos2::new(rect.right() - 2.0 * self.pan_zoom.zoom, rect.bottom() - 20.0 * self.pan_zoom.zoom),
                Pos2::new(rect.right() - 2.0 * self.pan_zoom.zoom, rect.bottom() - 2.0 * self.pan_zoom.zoom),
                Pos2::new(rect.right() - 20.0 * self.pan_zoom.zoom, rect.bottom() - 2.0 * self.pan_zoom.zoom),
            ];
            painter.add(egui::Shape::convex_polygon(points, Color32::WHITE, Stroke::NONE));
        }

        // 3. Draw icon (26x26 at padding offset)
        let icon_pos = Pos2::new(
            rect.left() + NODE_PADDING * self.pan_zoom.zoom,
            rect.top() + NODE_PADDING * self.pan_zoom.zoom,
        );
        self.draw_node_icon(painter, icon_pos, &node.category);

        // 4. Draw name (after icon, vertically centered)
        let name_x = rect.left() + (NODE_ICON_SIZE + NODE_PADDING * 2.0) * self.pan_zoom.zoom;
        let name_y = rect.center().y;
        painter.text(
            Pos2::new(name_x, name_y),
            egui::Align2::LEFT_CENTER,
            &node.name,
            egui::FontId::proportional(11.0 * self.pan_zoom.zoom),
            Color32::WHITE,
        );

        // 5. Input ports (small rects on top edge)
        for (i, port) in node.inputs.iter().enumerate() {
            if is_hidden_port(&port.port_type) {
                continue;
            }
            let port_pos = self.node_input_pos(node, i, offset);
            let port_rect = Rect::from_min_size(
                port_pos,
                Vec2::new(PORT_WIDTH * self.pan_zoom.zoom, PORT_HEIGHT * self.pan_zoom.zoom),
            );
            let color = if self
                .hovered_port
                .as_ref()
                .is_some_and(|(n, p)| n == &node.name && p == &port.name)
            {
                Color32::YELLOW
            } else {
                self.port_type_color(&port.port_type)
            };
            painter.rect_filled(port_rect, 0.0, color);
        }

        // 6. Output port (small rect at bottom left)
        let out_pos = self.node_output_pos(node, offset);
        let out_rect = Rect::from_min_size(
            out_pos,
            Vec2::new(PORT_WIDTH * self.pan_zoom.zoom, PORT_HEIGHT * self.pan_zoom.zoom),
        );
        let out_color = if self.hovered_output.as_ref() == Some(&node.name)
            && self.creating_connection.is_none()
        {
            Color32::YELLOW
        } else {
            self.port_type_color(&node.output_type)
        };
        painter.rect_filled(out_rect, 0.0, out_color);
    }

    /// Draw a node icon placeholder.
    fn draw_node_icon(&self, painter: &egui::Painter, pos: Pos2, category: &str) {
        let size = NODE_ICON_SIZE * self.pan_zoom.zoom;
        let rect = Rect::from_min_size(pos, Vec2::splat(size));
        let icon_color = Color32::from_rgba_unmultiplied(255, 255, 255, 200);

        match category.to_lowercase().as_str() {
            "corevector" | "geometry" => {
                // Diamond shape
                let center = rect.center();
                let r = size * 0.35;
                let points = vec![
                    Pos2::new(center.x, center.y - r),
                    Pos2::new(center.x + r, center.y),
                    Pos2::new(center.x, center.y + r),
                    Pos2::new(center.x - r, center.y),
                ];
                painter.add(egui::Shape::convex_polygon(points, icon_color, Stroke::NONE));
            }
            "transform" => {
                // Arrow shape
                let center = rect.center();
                let r = size * 0.3;
                let points = vec![
                    Pos2::new(center.x, center.y - r),
                    Pos2::new(center.x + r, center.y + r * 0.5),
                    Pos2::new(center.x, center.y),
                    Pos2::new(center.x - r, center.y + r * 0.5),
                ];
                painter.add(egui::Shape::convex_polygon(points, icon_color, Stroke::NONE));
            }
            "color" => {
                // Circle
                let center = rect.center();
                painter.circle_filled(center, size * 0.35, icon_color);
            }
            "math" => {
                // Plus sign
                let center = rect.center();
                let r = size * 0.3;
                let stroke = Stroke::new(2.0 * self.pan_zoom.zoom, icon_color);
                painter.line_segment(
                    [
                        Pos2::new(center.x - r, center.y),
                        Pos2::new(center.x + r, center.y),
                    ],
                    stroke,
                );
                painter.line_segment(
                    [
                        Pos2::new(center.x, center.y - r),
                        Pos2::new(center.x, center.y + r),
                    ],
                    stroke,
                );
            }
            "list" => {
                // Three horizontal lines
                let stroke = Stroke::new(2.0 * self.pan_zoom.zoom, icon_color);
                for i in 0..3 {
                    let y = rect.top() + size * (0.3 + 0.2 * i as f32);
                    painter.line_segment(
                        [
                            Pos2::new(rect.left() + size * 0.2, y),
                            Pos2::new(rect.right() - size * 0.2, y),
                        ],
                        stroke,
                    );
                }
            }
            "string" => {
                // "A" letter outline
                let center = rect.center();
                painter.text(
                    center,
                    egui::Align2::CENTER_CENTER,
                    "A",
                    egui::FontId::proportional(size * 0.6),
                    icon_color,
                );
            }
            "data" => {
                // Database cylinder (simplified as stacked ovals)
                let cx = rect.center().x;
                let top = rect.top() + size * 0.25;
                let bot = rect.bottom() - size * 0.25;
                let w = size * 0.35;
                let stroke = Stroke::new(1.5 * self.pan_zoom.zoom, icon_color);
                painter.line_segment([Pos2::new(cx - w, top), Pos2::new(cx - w, bot)], stroke);
                painter.line_segment([Pos2::new(cx + w, top), Pos2::new(cx + w, bot)], stroke);
                painter.line_segment([Pos2::new(cx - w, top), Pos2::new(cx + w, top)], stroke);
                painter.line_segment([Pos2::new(cx - w, bot), Pos2::new(cx + w, bot)], stroke);
            }
            _ => {
                // Default: small filled rounded rect
                painter.rect_filled(rect.shrink(size * 0.2), 2.0 * self.pan_zoom.zoom, icon_color);
            }
        }
    }

    /// Check if a connection is being hovered (using vertical bezier curve).
    fn is_connection_hovered(
        &self,
        ui: &egui::Ui,
        network: &Node,
        conn: &Connection,
        offset: Vec2,
    ) -> bool {
        let from_node = network.child(&conn.output_node);
        let to_node = network.child(&conn.input_node);

        if let (Some(from), Some(to)) = (from_node, to_node) {
            let from_pos = self.node_output_pos(from, offset);
            let port_index = to
                .inputs
                .iter()
                .position(|p| p.name == conn.input_port)
                .unwrap_or(0);
            let to_pos = self.node_input_pos(to, port_index, offset);

            // Check if mouse is near the bezier curve
            if let Some(mouse_pos) = ui.input(|i| i.pointer.hover_pos()) {
                let dy = (to_pos.y - from_pos.y).abs();
                if dy < GRID_CELL_SIZE * self.pan_zoom.zoom {
                    // Short connection: check distance to line segment
                    let line_dist = point_to_line_distance(mouse_pos, from_pos, to_pos);
                    if line_dist < 8.0 * self.pan_zoom.zoom {
                        return true;
                    }
                } else {
                    // Vertical bezier curve - sample and check distance
                    let half_dx = (to_pos.x - from_pos.x).abs() / 2.0;
                    let ctrl1 = Pos2::new(from_pos.x, from_pos.y + half_dx.max(30.0 * self.pan_zoom.zoom));
                    let ctrl2 = Pos2::new(to_pos.x, to_pos.y - half_dx.max(30.0 * self.pan_zoom.zoom));

                    for i in 0..32 {
                        let t = i as f32 / 31.0;
                        let pt = cubic_bezier(from_pos, ctrl1, ctrl2, to_pos, t);
                        if pt.distance(mouse_pos) < 8.0 * self.pan_zoom.zoom {
                            return true;
                        }
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

    /// Draw a wire (bezier curve) between two points with custom width (vertical flow).
    fn draw_wire_with_width(
        &self,
        painter: &egui::Painter,
        from: Pos2,
        to: Pos2,
        color: Color32,
        width: f32,
    ) {
        let dy = (to.y - from.y).abs();
        if dy < GRID_CELL_SIZE * self.pan_zoom.zoom {
            // Short connection: straight line
            painter.line_segment([from, to], Stroke::new(width * self.pan_zoom.zoom, color));
        } else {
            // Longer connection: vertical bezier curve
            let half_dx = (to.x - from.x).abs() / 2.0;
            let ctrl1 = Pos2::new(from.x, from.y + half_dx.max(30.0 * self.pan_zoom.zoom));
            let ctrl2 = Pos2::new(to.x, to.y - half_dx.max(30.0 * self.pan_zoom.zoom));

            // Sample the bezier curve
            let mut points = Vec::with_capacity(32);
            for i in 0..=31 {
                let t = i as f32 / 31.0;
                let pt = cubic_bezier(from, ctrl1, ctrl2, to, t);
                points.push(pt);
            }

            painter.add(egui::Shape::line(points, Stroke::new(width * self.pan_zoom.zoom, color)));
        }
    }

    /// Get a color for a category (used for icons, not node body).
    #[allow(dead_code)]
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

    /// Get a color for a port type (matching Java Theme).
    fn port_type_color(&self, port_type: &PortType) -> Color32 {
        match port_type {
            PortType::Int | PortType::Float => Color32::from_rgb(116, 119, 121),
            PortType::String | PortType::Boolean => Color32::from_rgb(92, 90, 91),
            PortType::Point => Color32::from_rgb(119, 154, 173),
            PortType::Color => Color32::from_rgb(94, 85, 112),
            PortType::Geometry => Color32::from_rgb(20, 20, 20),
            PortType::List => Color32::from_rgb(76, 137, 174),
            _ => Color32::from_rgb(52, 85, 129), // data/default
        }
    }

    /// Get a color for a node's output type (colors the entire node body).
    fn output_type_color(&self, output_type: &PortType) -> Color32 {
        match output_type {
            PortType::Int | PortType::Float => Color32::from_rgb(116, 119, 121),
            PortType::String | PortType::Boolean => Color32::from_rgb(92, 90, 91),
            PortType::Point => Color32::from_rgb(119, 154, 173),
            PortType::Color => Color32::from_rgb(94, 85, 112),
            PortType::Geometry => Color32::from_rgb(20, 20, 20),
            PortType::List => Color32::from_rgb(76, 137, 174),
            _ => Color32::from_rgb(52, 85, 129), // data/default
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

/// Check if a port type should be hidden (e.g., internal/context ports).
fn is_hidden_port(_port_type: &PortType) -> bool {
    // For now, show all ports. Can be extended to hide certain types.
    false
}

/// Calculate the distance from a point to a line segment.
fn point_to_line_distance(point: Pos2, line_start: Pos2, line_end: Pos2) -> f32 {
    let line = line_end - line_start;
    let len_sq = line.length_sq();
    if len_sq == 0.0 {
        return point.distance(line_start);
    }
    let t = ((point - line_start).dot(line) / len_sq).clamp(0.0, 1.0);
    let projection = line_start + line * t;
    point.distance(projection)
}
