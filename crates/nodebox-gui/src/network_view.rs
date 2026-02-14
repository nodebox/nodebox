//! Node network editor view.

use eframe::egui::{self, Color32, Pos2, Rect, Stroke, Vec2};
use nodebox_core::geometry::Point;
use nodebox_core::node::{Connection, Node, NodeLibrary, PortType};
use std::collections::{HashMap, HashSet};
use std::sync::Arc;

use crate::icon_cache::IconCache;
use crate::pan_zoom::PanZoom;
use crate::theme;

/// Actions that can be triggered by the network view.
#[derive(Debug, Clone)]
pub enum NetworkAction {
    /// No action.
    None,
    /// Open the node selection dialog at the given position (in grid units).
    OpenNodeDialog(Point),
    /// Open the node selection dialog filtered by type compatibility,
    /// for creating a node and connecting it to an existing output.
    OpenNodeDialogForConnection {
        /// Position where the new node should be created (in grid units).
        position: Point,
        /// The source node whose output is being connected.
        from_node: String,
        /// The output type of the source node (for filtering compatible nodes).
        output_type: PortType,
    },
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
    /// Whether space bar is currently pressed (for panning).
    is_space_pressed: bool,
    /// Whether we are currently panning with space+drag.
    is_panning: bool,
    /// Currently hovered input port (node_name, port_name).
    hovered_port: Option<(String, String)>,
    /// Currently hovered output port (node_name).
    hovered_output: Option<String>,
    /// Cache for node icons.
    icon_cache: IconCache,
    /// Whether we are currently rect-selecting (rubber band selection).
    is_rect_selecting: bool,
    /// Start point of the rect selection (screen coordinates).
    drag_select_start: Pos2,
    /// Current point of the rect selection (screen coordinates).
    drag_select_current: Pos2,
    /// Selection state before the rect selection started (for shift+drag additive selection).
    selection_before_drag: HashSet<String>,
    /// Whether the current drag started as an alt-drag copy operation.
    is_alt_copy_drag: bool,
}

/// State for dragging a new connection.
struct ConnectionDrag {
    /// The output node name.
    from_node: String,
    /// Output type of the source node (for compatibility checking).
    output_type: PortType,
    /// Current mouse position (end of wire).
    to_pos: Pos2,
}

/// Visual constants (matching NodeBox Java).
const GRID_CELL_SIZE: f32 = 48.0;
const NODE_MARGIN: f32 = 8.0;
const NODE_WIDTH: f32 = 128.0; // 48*3 - 8*2
const NODE_HEIGHT: f32 = 32.0; // 48 - 8*2
const NODE_ICON_SIZE: f32 = 24.0;
const NODE_PADDING: f32 = 4.0;
const PORT_WIDTH: f32 = 12.0;
const PORT_HEIGHT: f32 = 4.0;
const PORT_SPACING: f32 = 8.0;
/// Margin between ports for hit-area calculations.
const PORT_MARGIN: f32 = 6.0;
/// Hit area affordance for ports when not connecting.
const PORT_HEIGHT_AFFORDANCE: f32 = 6.0;


impl Default for NetworkView {
    fn default() -> Self {
        Self::new()
    }
}

impl NetworkView {
    /// Create a new network view.
    pub fn new() -> Self {
        // Start with an initial pan offset so the first grid lines don't hug the edges.
        let mut pan_zoom = PanZoom::with_zoom_limits(0.25, 4.0);
        pan_zoom.pan = Vec2::new(-1.0, -1.0);

        Self {
            pan_zoom,
            selected: HashSet::new(),
            is_dragging_selection: false,
            creating_connection: None,
            is_space_pressed: false,
            is_panning: false,
            hovered_port: None,
            hovered_output: None,
            icon_cache: IconCache::new(),
            is_rect_selecting: false,
            drag_select_start: Pos2::ZERO,
            drag_select_current: Pos2::ZERO,
            selection_before_drag: HashSet::new(),
            is_alt_copy_drag: false,
        }
    }

    /// Get the currently selected nodes.
    pub fn selected_nodes(&self) -> &HashSet<String> {
        &self.selected
    }

    /// Clone all selected nodes and their internal/incoming connections.
    /// Updates `self.selected` to point to the new clones.
    fn perform_alt_copy(&mut self, library: &mut Arc<NodeLibrary>) {
        let lib = Arc::make_mut(library);

        // Collect existing names to track uniqueness across the batch.
        let mut used_names: HashSet<String> =
            lib.root.children.iter().map(|c| c.name.clone()).collect();

        // Phase 1: Build old_name -> new_name mapping.
        let mut name_map: HashMap<String, String> = HashMap::new();
        let selected_names: Vec<String> = self.selected.iter().cloned().collect();

        for old_name in &selected_names {
            let prefix = extract_name_prefix(old_name);
            let new_name = generate_unique_name(prefix, &used_names);
            used_names.insert(new_name.clone());
            name_map.insert(old_name.clone(), new_name);
        }

        // Phase 2: Clone nodes with new names.
        let mut new_nodes = Vec::new();
        for old_name in &selected_names {
            if let Some(original) = lib.root.child(old_name) {
                let mut cloned = original.clone();
                cloned.name = name_map[old_name].clone();
                new_nodes.push(cloned);
            }
        }

        // Phase 3: Duplicate connections.
        let mut new_connections = Vec::new();
        for conn in &lib.root.connections {
            let out_in_selection = name_map.contains_key(&conn.output_node);
            let in_in_selection = name_map.contains_key(&conn.input_node);

            if out_in_selection && in_in_selection {
                // Internal connection: remap both ends to clones.
                new_connections.push(Connection::new(
                    &name_map[&conn.output_node],
                    &name_map[&conn.input_node],
                    &conn.input_port,
                ));
            } else if in_in_selection {
                // Incoming connection: keep output node, remap input to clone.
                new_connections.push(Connection::new(
                    &conn.output_node,
                    &name_map[&conn.input_node],
                    &conn.input_port,
                ));
            }
            // Outgoing connections (out_in_selection && !in_in_selection): NOT duplicated.
        }

        // Phase 4: Mutate the network.
        for node in new_nodes {
            lib.root.children.push(node);
        }
        lib.root.connections.extend(new_connections);

        // Phase 5: Update selection to the clones.
        self.selected.clear();
        for new_name in name_map.values() {
            self.selected.insert(new_name.clone());
        }

        self.is_alt_copy_drag = true;
    }

    /// Show the network view. Returns any action that should be handled by the app.
    ///
    /// The `node_errors` map contains per-node error messages for visual feedback.
    pub fn show(&mut self, ui: &mut egui::Ui, library: &mut Arc<NodeLibrary>, node_errors: &HashMap<String, String>) -> NetworkAction {
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

        // Draw connections first (behind nodes)
        for conn in network.connections.iter() {
            self.draw_connection(&painter, network, conn, offset);
        }

        // Draw connection being created
        if let Some(ref drag) = self.creating_connection {
            if let Some(from_node) = network.child(&drag.from_node) {
                let from_pos = self.node_output_center(from_node, offset);
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
        let mut disconnect_and_reroute: Option<(usize, String, PortType)> = None;

        // Detect port hover states using the new hit-testing system
        let is_connecting = self.creating_connection.is_some();
        if let Some(mouse_pos) = ui.input(|i| i.pointer.hover_pos()) {
            // Check output port hover (only when NOT connecting, for start-drag feedback)
            if !is_connecting {
                if let Some(node_name) = self.find_output_port_at(network, mouse_pos, offset, false) {
                    self.hovered_output = Some(node_name);
                }
            }

            // Check input port hover (use expanded hit area when connecting)
            if let Some((node_name, port_name, _)) =
                self.find_input_port_at(network, mouse_pos, offset, is_connecting)
            {
                self.hovered_port = Some((node_name, port_name));
            }
        }

        // Track node to set as rendered (on double-click)
        let mut node_to_render: Option<String> = None;

        for child in &network.children {
            let node_rect = self.node_rect(child, offset);

            // Check for node interactions
            let node_response =
                ui.interact(node_rect, ui.id().with(&child.name), egui::Sense::click_and_drag());

            // Skip node interactions if hovering a port (tooltip visible = port interaction)
            let hovering_port = self.hovered_port.is_some() || self.hovered_output.is_some();

            if node_response.clicked() && !hovering_port {
                node_to_select = Some(child.name.clone());
            }

            // Double-click sets the node as rendered
            if node_response.double_clicked() && !hovering_port {
                node_to_render = Some(child.name.clone());
            }

            if node_response.drag_started() && !self.is_panning && !hovering_port {
                start_dragging_node = Some(child.name.clone());
            }

            // Draw the node (with connection drag feedback if applicable)
            let is_selected = self.selected.contains(&child.name);
            let is_rendered = network.rendered_child.as_deref() == Some(&child.name);
            let drag_output_type = self.creating_connection.as_ref().map(|c| c.output_type.clone());
            let error_msg = node_errors.get(&child.name);
            self.draw_node(ui.ctx(), &painter, network, child, offset, is_selected, is_rendered, drag_output_type.as_ref(), error_msg);

            // Show error tooltip on hover
            if let Some(msg) = error_msg {
                if node_response.hovered() {
                    node_response.on_hover_text(format!("{}: {}", child.name, msg));
                }
            }

            // Check for output port click (to start connection)
            // Use normal-sized hit area (no is_connecting inflation for starting)
            let output_rect = self.output_rect(child, offset, false);
            let output_response = ui.interact(
                output_rect,
                ui.id().with(format!("{}_out", child.name)),
                egui::Sense::drag(),
            );

            if output_response.drag_started() && !is_connecting {
                let output_pos = self.node_output_pos(child, offset);
                self.creating_connection = Some(ConnectionDrag {
                    from_node: child.name.clone(),
                    output_type: child.output_type.clone(),
                    to_pos: output_pos,
                });
            }

            // Check for input port clicks (disconnect-and-reroute)
            // Use normal-sized hit areas for clicking (not completing a connection)
            if !is_connecting {
                for (i, port) in child.inputs.iter().enumerate() {
                    if is_hidden_port(&port.port_type) {
                        continue;
                    }
                    let port_rect = self.input_rect(child, i, offset, false);
                    let port_response = ui.interact(
                        port_rect,
                        ui.id().with(format!("{}_{}_click", child.name, port.name)),
                        egui::Sense::drag(),
                    );

                    // If user starts dragging from an occupied input port,
                    // disconnect the existing connection and start a new drag
                    // from the upstream output port
                    if port_response.drag_started() {
                        if let Some(conn_idx) =
                            self.find_connection_at_input(network, &child.name, &port.name)
                        {
                            let conn = &network.connections[conn_idx];
                            if let Some(upstream_node) = network.child(&conn.output_node) {
                                // Queue disconnect and start new connection from upstream
                                disconnect_and_reroute = Some((
                                    conn_idx,
                                    upstream_node.name.clone(),
                                    upstream_node.output_type.clone(),
                                ));
                            }
                        }
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
                        let galley = painter.layout_no_wrap(tooltip_text, font, theme::TOOLTIP_TEXT);
                        let tooltip_rect = Rect::from_min_size(
                            tooltip_pos,
                            galley.size() + Vec2::splat(8.0),
                        );
                        painter.rect_filled(tooltip_rect, 4.0, theme::TOOLTIP_BG);
                        painter.galley(tooltip_pos + Vec2::splat(4.0), galley, theme::TOOLTIP_TEXT);
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
                        let galley = painter.layout_no_wrap(tooltip_text, font, theme::TOOLTIP_TEXT);
                        let tooltip_rect = Rect::from_min_size(
                            tooltip_pos,
                            galley.size() + Vec2::splat(8.0),
                        );
                        painter.rect_filled(tooltip_rect, 4.0, theme::TOOLTIP_BG);
                        painter.galley(tooltip_pos + Vec2::splat(4.0), galley, theme::TOOLTIP_TEXT);
                    }
                }
            }
        }

        // Handle connection creation end (use inflated hit areas for easy drop)
        if self.creating_connection.is_some() && ui.input(|i| i.pointer.any_released()) {
            let mut connection_made = false;
            if let Some(hover_pos) = ui.input(|i| i.pointer.hover_pos()) {
                // Find which input port we're over using inflated hit areas (is_connecting=true)
                if let Some((node_name, port_name, _)) =
                    self.find_input_port_at(network, hover_pos, offset, true)
                {
                    if let Some(ref drag) = self.creating_connection {
                        // Check type compatibility before creating connection
                        if let Some(target_node) = network.child(&node_name) {
                            if let Some(target_port) = target_node.input(&port_name) {
                                if PortType::is_compatible(&drag.output_type, &target_port.port_type)
                                {
                                    connection_to_create = Some((
                                        drag.from_node.clone(),
                                        node_name,
                                        port_name,
                                    ));
                                    connection_made = true;
                                }
                            }
                        }
                    }
                }

                // If no connection was made and cursor is not over any node, open dialog
                if !connection_made {
                    let over_any_node = network.children.iter().any(|child| {
                        self.node_rect(child, offset).contains(hover_pos)
                    });
                    if !over_any_node {
                        if let Some(ref drag) = self.creating_connection {
                            let grid_pos = self.screen_to_grid(hover_pos, offset);
                            action = NetworkAction::OpenNodeDialogForConnection {
                                position: Point::new(
                                    grid_pos.x.round() as f64,
                                    grid_pos.y.round() as f64,
                                ),
                                from_node: drag.from_node.clone(),
                                output_type: drag.output_type.clone(),
                            };
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

        // Handle disconnect-and-reroute (remove old connection, start new drag from upstream)
        if let Some((conn_idx, from_node_name, output_type)) = disconnect_and_reroute {
            // Remove the old connection
            Arc::make_mut(library).root.connections.remove(conn_idx);
            // Start a new connection drag from the upstream node
            if let Some(from_node) = library.root.child(&from_node_name) {
                let output_pos = self.node_output_pos(from_node, offset);
                self.creating_connection = Some(ConnectionDrag {
                    from_node: from_node_name,
                    output_type,
                    to_pos: output_pos,
                });
            }
        }

        // --- Drag Selection (rubber band) ---
        // Start drag selection when primary-dragging on empty space
        if !self.is_rect_selecting
            && !self.is_panning
            && !self.is_space_pressed
            && start_dragging_node.is_none()
            && self.creating_connection.is_none()
            && response.drag_started_by(egui::PointerButton::Primary)
        {
            if let Some(pos) = ui.input(|i| i.pointer.interact_pos()) {
                self.is_rect_selecting = true;
                self.drag_select_start = pos;
                self.drag_select_current = pos;
                if ui.input(|i| i.modifiers.shift) {
                    self.selection_before_drag = self.selected.clone();
                } else {
                    self.selection_before_drag.clear();
                    self.selected.clear();
                }
            }
        }

        // Update drag selection rectangle and compute selected nodes
        if self.is_rect_selecting {
            if let Some(pos) = ui.input(|i| i.pointer.hover_pos()) {
                self.drag_select_current = pos;
            }
            let selection_rect =
                Rect::from_two_pos(self.drag_select_start, self.drag_select_current);
            self.selected = self.selection_before_drag.clone();
            for child in &library.root.children {
                let node_rect = self.node_rect(child, offset);
                if selection_rect.intersects(node_rect) {
                    self.selected.insert(child.name.clone());
                }
            }
        }

        // End drag selection
        if self.is_rect_selecting && ui.input(|i| i.pointer.any_released()) {
            self.is_rect_selecting = false;
        }

        // Draw drag selection rectangle
        if self.is_rect_selecting {
            let r = Rect::from_two_pos(self.drag_select_start, self.drag_select_current);
            painter.rect_filled(r, 0.0, Color32::from_white_alpha(100));
            painter.rect_stroke(r, 0.0, Stroke::new(1.0, Color32::from_white_alpha(100)), egui::StrokeKind::Inside);
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

            // Alt/Option-drag: clone selected nodes (originals stay, clones get dragged)
            if ui.input(|i| i.modifiers.alt) {
                self.perform_alt_copy(library);
            }

            self.is_dragging_selection = true;
        }

        // Apply drag delta to all selected nodes
        if self.is_dragging_selection {
            // Show copy cursor if this was an alt-copy drag
            if self.is_alt_copy_drag {
                ui.ctx().set_cursor_icon(egui::CursorIcon::Copy);
            }
            let pointer_delta = ui.input(|i| {
                if i.pointer.is_decidedly_dragging() {
                    i.pointer.delta()
                } else {
                    Vec2::ZERO
                }
            });
            let delta = pointer_delta / (self.pan_zoom.zoom * GRID_CELL_SIZE);
            if delta != Vec2::ZERO {
                let lib = Arc::make_mut(library);
                for name in &self.selected {
                    if let Some(node) = lib.root.child_mut(name) {
                        node.position.x += delta.x as f64;
                        node.position.y += delta.y as f64;
                    }
                }
            }
        }

        // Snap all selected nodes to grid when drag ends
        if self.is_dragging_selection && ui.input(|i| i.pointer.any_released()) {
            let lib = Arc::make_mut(library);
            for name in &self.selected {
                if let Some(node) = lib.root.child_mut(name) {
                    node.position.x = node.position.x.round();
                    node.position.y = node.position.y.round();
                }
            }
            self.is_dragging_selection = false;
            self.is_alt_copy_drag = false;
        }

        // Set rendered node (on double-click)
        if let Some(name) = node_to_render {
            Arc::make_mut(library).root.rendered_child = Some(name);
        }

        // Create connection if needed (replaces any existing connection to the same input port)
        if let Some((from, to, port)) = connection_to_create {
            Arc::make_mut(library).root.connect(Connection::new(from, to, port));
        }

        // Handle delete key for selected nodes (but not when editing text)
        let wants_keyboard = ui.ctx().wants_keyboard_input();
        if !wants_keyboard && ui.input(|i| i.key_pressed(egui::Key::Delete) || i.key_pressed(egui::Key::Backspace)) {
            // Delete selected nodes
            let lib = Arc::make_mut(library);
            for name in &self.selected {
                // Remove node
                lib.root.children.retain(|n| &n.name != name);
                // Remove connections involving this node
                lib.root.connections.retain(|c| &c.output_node != name && &c.input_node != name);
                // If the deleted node was the rendered node, clear the rendered child
                if lib.root.rendered_child.as_deref() == Some(name.as_str()) {
                    lib.root.rendered_child = None;
                }
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

    // ========================================================================
    // Hit Testing Affordances
    // ========================================================================

    /// Get the hit rectangle for an input port, with contextual inflation.
    ///
    /// When `is_connecting` is true, the hit area expands dramatically to make
    /// dropping connections easier - the entire node body becomes a valid target.
    fn input_rect(&self, node: &Node, port_index: usize, offset: Vec2, is_connecting: bool) -> Rect {
        let node_rect = self.node_rect(node, offset);
        let z = self.pan_zoom.zoom;
        let port_x = (PORT_WIDTH + PORT_SPACING) * port_index as f32;
        let port_screen_x = node_rect.left() + port_x * z;

        if is_connecting {
            // When completing a connection: huge hit area
            // Width expands to include margin (closing gaps between ports)
            // Height extends from top of port through entire node body
            let width = (PORT_WIDTH + PORT_MARGIN) * z;
            let height = (PORT_HEIGHT + NODE_HEIGHT) * z;
            Rect::from_min_size(
                Pos2::new(port_screen_x - (PORT_MARGIN / 2.0) * z, node_rect.top() - PORT_HEIGHT * z),
                Vec2::new(width, height),
            )
        } else {
            // Normal mode: small hit area with basic affordance
            Rect::from_min_size(
                Pos2::new(port_screen_x, node_rect.top() - PORT_HEIGHT * z),
                Vec2::new(PORT_WIDTH * z, PORT_HEIGHT_AFFORDANCE * z),
            )
        }
    }

    /// Get the hit rectangle for an output port, with contextual inflation.
    ///
    /// When `is_connecting` is true, the hit area expands to cover the bottom
    /// half of the node for hover feedback.
    fn output_rect(&self, node: &Node, offset: Vec2, is_connecting: bool) -> Rect {
        let node_rect = self.node_rect(node, offset);
        let z = self.pan_zoom.zoom;

        if is_connecting {
            // When connecting: expand to bottom half of node (for hover feedback)
            Rect::from_min_max(
                Pos2::new(node_rect.left(), node_rect.center().y),
                Pos2::new(node_rect.left() + PORT_WIDTH * z, node_rect.bottom() + PORT_HEIGHT * z),
            )
        } else {
            // Normal mode: small hit area with basic affordance
            Rect::from_min_size(
                Pos2::new(node_rect.left(), node_rect.bottom()),
                Vec2::new(PORT_WIDTH * z, PORT_HEIGHT_AFFORDANCE * z),
            )
        }
    }

    /// Multi-point hit test: checks if a point is near a rectangle.
    ///
    /// Tests the cursor at four offset positions (±PORT_MARGIN/2 in each axis),
    /// equivalent to inflating the rect by that amount but without allocating.
    fn contains_point_multipoint(&self, rect: Rect, point: Pos2) -> bool {
        let half_margin = (PORT_MARGIN / 2.0) * self.pan_zoom.zoom;
        let offsets = [
            Vec2::new(-half_margin, -half_margin),
            Vec2::new(half_margin, -half_margin),
            Vec2::new(-half_margin, half_margin),
            Vec2::new(half_margin, half_margin),
        ];
        for offset in offsets {
            if rect.contains(point + offset) {
                return true;
            }
        }
        false
    }

    /// Find the input port at a given screen position using mathematical indexing.
    ///
    /// Returns `Some((node_name, port_name, port_index))` if found.
    fn find_input_port_at(
        &self,
        network: &Node,
        screen_pos: Pos2,
        offset: Vec2,
        is_connecting: bool,
    ) -> Option<(String, String, usize)> {
        let z = self.pan_zoom.zoom;

        for child in &network.children {
            let node_rect = self.node_rect(child, offset);

            // Broad-phase rejection: check if we're anywhere near the input port area
            let port_strip_height = if is_connecting {
                (PORT_HEIGHT + NODE_HEIGHT) * z
            } else {
                PORT_HEIGHT_AFFORDANCE * z
            };
            let port_strip = Rect::from_min_size(
                Pos2::new(node_rect.left(), node_rect.top() - PORT_HEIGHT * z),
                Vec2::new(node_rect.width(), port_strip_height),
            );

            // Use multi-point hit test for broad phase
            if !self.contains_point_multipoint(port_strip, screen_pos) {
                continue;
            }

            // Mathematical port indexing: calculate which port cell we're in
            let local_x = screen_pos.x - node_rect.left() + (PORT_MARGIN / 2.0) * z;
            let cell_width = (PORT_WIDTH + PORT_SPACING) * z;
            let port_index = (local_x / cell_width).floor() as usize;

            // Validate against actual port list (skip hidden ports)
            let visible_ports: Vec<_> = child
                .inputs
                .iter()
                .enumerate()
                .filter(|(_, p)| !is_hidden_port(&p.port_type))
                .collect();

            if port_index < visible_ports.len() {
                let (actual_index, port) = visible_ports[port_index];
                // Final check: verify the point is within this port's hit rect
                let port_rect = self.input_rect(child, actual_index, offset, is_connecting);
                if self.contains_point_multipoint(port_rect, screen_pos) {
                    return Some((child.name.clone(), port.name.clone(), actual_index));
                }
            }
        }
        None
    }

    /// Find the output port at a given screen position.
    ///
    /// Returns `Some(node_name)` if found (nodes have only one output).
    fn find_output_port_at(
        &self,
        network: &Node,
        screen_pos: Pos2,
        offset: Vec2,
        is_connecting: bool,
    ) -> Option<String> {
        for child in &network.children {
            let output_rect = self.output_rect(child, offset, is_connecting);
            if self.contains_point_multipoint(output_rect, screen_pos) {
                return Some(child.name.clone());
            }
        }
        None
    }

    /// Find which connection is occupying an input port (if any).
    fn find_connection_at_input(
        &self,
        network: &Node,
        node_name: &str,
        port_name: &str,
    ) -> Option<usize> {
        network
            .connections
            .iter()
            .position(|c| c.input_node == node_name && c.input_port == port_name)
    }

    /// Draw the background grid (Java NodeBox style).
    fn draw_grid(&self, painter: &egui::Painter, rect: Rect) {
        // Background color
        painter.rect_filled(rect, 0.0, theme::NETWORK_BACKGROUND);

        // Grid lines
        let grid_size = GRID_CELL_SIZE * self.pan_zoom.zoom;
        let grid_color = theme::NETWORK_GRID;

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

    /// Get the screen position of a node's output port (top-left corner of port rect).
    fn node_output_pos(&self, node: &Node, offset: Vec2) -> Pos2 {
        let rect = self.node_rect(node, offset);
        Pos2::new(rect.left(), rect.bottom())
    }

    /// Get the screen position of a node's input port (top-left corner of port rect).
    fn node_input_pos(&self, node: &Node, port_index: usize, offset: Vec2) -> Pos2 {
        let rect = self.node_rect(node, offset);
        let port_x = (PORT_WIDTH + PORT_SPACING) * port_index as f32;
        Pos2::new(
            rect.left() + port_x * self.pan_zoom.zoom,
            rect.top() - PORT_HEIGHT * self.pan_zoom.zoom, // Above the node
        )
    }

    /// Get the center position of a node's output port (for wire connections).
    fn node_output_center(&self, node: &Node, offset: Vec2) -> Pos2 {
        let pos = self.node_output_pos(node, offset);
        Pos2::new(
            pos.x + PORT_WIDTH / 2.0 * self.pan_zoom.zoom,
            pos.y + PORT_HEIGHT / 2.0 * self.pan_zoom.zoom,
        )
    }

    /// Get the center position of a node's input port (for wire connections).
    fn node_input_center(&self, node: &Node, port_index: usize, offset: Vec2) -> Pos2 {
        let pos = self.node_input_pos(node, port_index, offset);
        Pos2::new(
            pos.x + PORT_WIDTH / 2.0 * self.pan_zoom.zoom,
            pos.y + PORT_HEIGHT / 2.0 * self.pan_zoom.zoom,
        )
    }

    /// Draw a node (Java NodeBox style).
    ///
    /// If `drag_output_type` is provided, input ports will show visual feedback
    /// indicating type compatibility with the dragged connection.
    ///
    /// If `error_msg` is provided, the node will be drawn with an error background color.
    fn draw_node(
        &mut self,
        ctx: &egui::Context,
        painter: &egui::Painter,
        _network: &Node,
        node: &Node,
        offset: Vec2,
        is_selected: bool,
        is_rendered: bool,
        drag_output_type: Option<&PortType>,
        error_msg: Option<&String>,
    ) {
        let rect = self.node_rect(node, offset);
        // Use error color if node has an error, otherwise use output type color
        let body_color = if error_msg.is_some() {
            theme::ERROR_RED
        } else {
            self.output_type_color(&node.output_type)
        };
        let z = self.pan_zoom.zoom;

        // 1. Selection ring (white fill behind, 2px inset)
        if is_selected {
            painter.rect_filled(rect, 0.0, Color32::WHITE);
            let inset = Rect::from_min_max(
                rect.min + Vec2::splat(2.0 * z),
                rect.max - Vec2::splat(2.0 * z),
            );
            painter.rect_filled(inset, 0.0, body_color);
        } else {
            // Node body colored by output type
            painter.rect_filled(rect, 0.0, body_color);
        }

        // 2. Rendered indicator (white triangle in bottom-right corner)
        if is_rendered {
            let points = vec![
                Pos2::new(rect.right() - 2.0 * z, rect.bottom() - 20.0 * z),
                Pos2::new(rect.right() - 2.0 * z, rect.bottom() - 2.0 * z),
                Pos2::new(rect.right() - 20.0 * z, rect.bottom() - 2.0 * z),
            ];
            painter.add(egui::Shape::convex_polygon(points, Color32::WHITE, Stroke::NONE));
        }

        // 3. Draw icon (26x26 at padding offset)
        let icon_pos = Pos2::new(
            rect.left() + NODE_PADDING * z,
            rect.top() + NODE_PADDING * z,
        );
        self.draw_node_icon(ctx, painter, icon_pos, node.function.as_deref(), &node.category);

        // 4. Draw name (after icon, vertically centered)
        let name_x = rect.left() + (NODE_ICON_SIZE + NODE_PADDING * 2.0) * z;
        let name_y = rect.center().y;
        painter.text(
            Pos2::new(name_x, name_y),
            egui::Align2::LEFT_CENTER,
            &node.name,
            egui::FontId::proportional(11.0 * z),
            theme::TEXT_STRONG,
        );

        // 5. Input ports (small rects on top edge) with connection-drag feedback
        for (i, port) in node.inputs.iter().enumerate() {
            if is_hidden_port(&port.port_type) {
                continue;
            }
            let port_pos = self.node_input_pos(node, i, offset);

            // Determine if this port is hovered
            let is_hovered = self
                .hovered_port
                .as_ref()
                .is_some_and(|(n, p)| n == &node.name && p == &port.name);

            // Calculate port size and color based on drag state
            // Heights change during connection mode, colors stay the same
            // Ports "hug" the node - bottom edge stays at node top
            let (port_height, color) = if let Some(output_type) = drag_output_type {
                // We're dragging a connection - show type compatibility via height
                let is_compatible = PortType::is_compatible(output_type, &port.port_type);

                if is_hovered && is_compatible {
                    // Hovered and compatible: accent color, normal size
                    (PORT_HEIGHT, theme::PORT_HOVER)
                } else if is_compatible {
                    // Compatible: taller port (2px extra)
                    (PORT_HEIGHT + 2.0, self.port_type_color(&port.port_type))
                } else {
                    // Incompatible: minimal height (1px)
                    (1.0, self.port_type_color(&port.port_type))
                }
            } else if is_hovered {
                // Not dragging, but hovered: accent highlight
                (PORT_HEIGHT, theme::PORT_HOVER)
            } else {
                // Normal state: standard port color
                (PORT_HEIGHT, self.port_type_color(&port.port_type))
            };

            // Adjust Y position so port bottom stays at node top (hugs the node)
            let port_y = rect.top() - port_height * z;
            let adjusted_port_pos = Pos2::new(port_pos.x, port_y);

            let port_rect = Rect::from_min_size(
                adjusted_port_pos,
                Vec2::new(PORT_WIDTH * z, port_height * z),
            );
            painter.rect_filled(port_rect, 0.0, color);
        }

        // 6. Output port (small rect at bottom left)
        // Only show hover highlight when NOT dragging a connection
        let out_pos = self.node_output_pos(node, offset);
        let out_rect = Rect::from_min_size(
            out_pos,
            Vec2::new(PORT_WIDTH * z, PORT_HEIGHT * z),
        );
        let out_color = if self.hovered_output.as_ref() == Some(&node.name)
            && drag_output_type.is_none()
        {
            theme::PORT_HOVER
        } else {
            self.port_type_color(&node.output_type)
        };
        painter.rect_filled(out_rect, 0.0, out_color);
    }

    /// Draw a node icon, loading from libraries if available.
    fn draw_node_icon(&mut self, ctx: &egui::Context, painter: &egui::Painter, pos: Pos2, function: Option<&str>, category: &str) {
        let size = NODE_ICON_SIZE * self.pan_zoom.zoom;
        self.icon_cache.draw_icon_zoomed(ctx, painter, pos, size, self.pan_zoom.zoom, function, category);
    }
    /// Draw a connection between nodes.
    fn draw_connection(&self, painter: &egui::Painter, network: &Node, conn: &Connection, offset: Vec2) {
        // Find the source and target nodes
        let from_node = network.child(&conn.output_node);
        let to_node = network.child(&conn.input_node);

        if let (Some(from), Some(to)) = (from_node, to_node) {
            let from_pos = self.node_output_center(from, offset);

            // Find the input port index
            let port_index = to.inputs.iter().position(|p| p.name == conn.input_port).unwrap_or(0);
            let to_pos = self.node_input_center(to, port_index, offset);

            // Get the port type for coloring
            let port_type = to.input(conn.input_port.as_str())
                .map(|p| &p.port_type)
                .unwrap_or(&PortType::Geometry);

            let color = self.port_type_color(port_type);
            self.draw_wire_with_width(painter, from_pos, to_pos, color, 2.0);
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
            "geometry" | "corevector" => theme::CATEGORY_GEOMETRY,
            "transform" => theme::CATEGORY_TRANSFORM,
            "color" => theme::CATEGORY_COLOR,
            "math" => theme::CATEGORY_MATH,
            "list" => theme::CATEGORY_LIST,
            "string" => theme::CATEGORY_STRING,
            "data" => theme::CATEGORY_DATA,
            _ => theme::CATEGORY_DEFAULT,
        }
    }

    /// Get a color for a port type (matching Java Theme).
    fn port_type_color(&self, port_type: &PortType) -> Color32 {
        match port_type {
            PortType::Int => theme::PORT_COLOR_INT,
            PortType::Float => theme::PORT_COLOR_FLOAT,
            PortType::String => theme::PORT_COLOR_STRING,
            PortType::Boolean => theme::PORT_COLOR_BOOLEAN,
            PortType::Point => theme::PORT_COLOR_POINT,
            PortType::Color => theme::PORT_COLOR_COLOR,
            PortType::Geometry => theme::PORT_COLOR_GEOMETRY,
            PortType::List => theme::PORT_COLOR_LIST,
            _ => theme::PORT_COLOR_DATA,
        }
    }

    /// Get a color for a node's body based on output type (muted tints).
    fn output_type_color(&self, output_type: &PortType) -> Color32 {
        match output_type {
            PortType::Int => theme::NODE_BODY_INT,
            PortType::Float => theme::NODE_BODY_FLOAT,
            PortType::String => theme::NODE_BODY_STRING,
            PortType::Boolean => theme::NODE_BODY_BOOLEAN,
            PortType::Point => theme::NODE_BODY_POINT,
            PortType::Color => theme::NODE_BODY_COLOR,
            PortType::Geometry => theme::NODE_BODY_GEOMETRY,
            PortType::List => theme::NODE_BODY_LIST,
            _ => theme::NODE_BODY_DEFAULT,
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

/// Strip trailing digits from a node name to get the base prefix.
///
/// Examples: `"rect1"` → `"rect"`, `"ellipse"` → `"ellipse"`, `"a1b2"` → `"a1b"`
fn extract_name_prefix(name: &str) -> &str {
    let prefix_end = name.trim_end_matches(|c: char| c.is_ascii_digit()).len();
    if prefix_end == 0 {
        name
    } else {
        &name[..prefix_end]
    }
}

/// Generate a unique name given a prefix and a set of existing names.
///
/// Always appends a numeric index: `prefix1`, `prefix2`, etc.
fn generate_unique_name(prefix: &str, existing: &HashSet<String>) -> String {
    for i in 1..1000 {
        let name = format!("{}{}", prefix, i);
        if !existing.contains(&name) {
            return name;
        }
    }
    // Fallback (shouldn't happen in practice)
    format!("{}{}", prefix, 1000)
}

#[cfg(test)]
mod tests {
    use super::*;
    use nodebox_core::node::{Node, NodeLibrary};

    #[test]
    fn test_extract_name_prefix() {
        assert_eq!(extract_name_prefix("rect1"), "rect");
        assert_eq!(extract_name_prefix("rect"), "rect");
        assert_eq!(extract_name_prefix("ellipse42"), "ellipse");
        assert_eq!(extract_name_prefix("a1b2"), "a1b");
        assert_eq!(extract_name_prefix(""), "");
        // All-digit name: returns the whole name (prefix_end == 0)
        assert_eq!(extract_name_prefix("123"), "123");
    }

    #[test]
    fn test_generate_unique_name_available() {
        let existing = HashSet::new();
        assert_eq!(generate_unique_name("rect", &existing), "rect1");
    }

    #[test]
    fn test_generate_unique_name_increments() {
        let existing: HashSet<String> = ["rect".into()].into();
        assert_eq!(generate_unique_name("rect", &existing), "rect1");

        let existing: HashSet<String> = ["rect".into(), "rect1".into()].into();
        assert_eq!(generate_unique_name("rect", &existing), "rect2");
    }

    #[test]
    fn test_alt_copy_single_node() {
        let mut library = Arc::new(NodeLibrary::new("test"));
        {
            let lib = Arc::make_mut(&mut library);
            lib.root.children.push(Node::new("rect").with_position(1.0, 2.0));
            lib.root.children.push(Node::new("rect1")); // Take up "rect1" so clone becomes "rect2"
        }

        let mut view = NetworkView::new();
        view.selected.insert("rect".into());

        view.perform_alt_copy(&mut library);

        // Original should still exist
        assert!(library.root.child("rect").is_some());
        // Clone should exist with incremented name (skips "rect1" which is taken)
        assert!(library.root.child("rect2").is_some());
        // Clone should have same position as original
        let clone = library.root.child("rect2").unwrap();
        assert_eq!(clone.position.x, 1.0);
        assert_eq!(clone.position.y, 2.0);
        // Selection should point to clone
        assert!(view.selected.contains("rect2"));
        assert!(!view.selected.contains("rect"));
        assert_eq!(view.selected.len(), 1);
    }

    #[test]
    fn test_alt_copy_multiple_nodes_with_internal_connections() {
        let mut library = Arc::new(NodeLibrary::new("test"));
        {
            let lib = Arc::make_mut(&mut library);
            lib.root.children.push(Node::new("rect1"));
            lib.root.children.push(Node::new("translate1"));
            lib.root.connections.push(Connection::new("rect1", "translate1", "shape"));
        }

        let mut view = NetworkView::new();
        view.selected.insert("rect1".into());
        view.selected.insert("translate1".into());

        view.perform_alt_copy(&mut library);

        // Should have 4 nodes total
        assert_eq!(library.root.children.len(), 4);
        // Original connection should still exist
        assert!(library.root.connections.iter().any(|c|
            c.output_node == "rect1" && c.input_node == "translate1" && c.input_port == "shape"
        ));
        // Internal connection should be duplicated with new names
        // (rect2 -> translate2 or similar)
        let new_names: Vec<String> = view.selected.iter().cloned().collect();
        let has_internal_conn = library.root.connections.iter().any(|c|
            new_names.contains(&c.output_node) && new_names.contains(&c.input_node) && c.input_port == "shape"
        );
        assert!(has_internal_conn, "Internal connection should be duplicated");
    }

    #[test]
    fn test_alt_copy_incoming_connections_preserved() {
        let mut library = Arc::new(NodeLibrary::new("test"));
        {
            let lib = Arc::make_mut(&mut library);
            lib.root.children.push(Node::new("source1")); // Not selected
            lib.root.children.push(Node::new("rect1")); // Selected
            lib.root.connections.push(Connection::new("source1", "rect1", "shape"));
        }

        let mut view = NetworkView::new();
        view.selected.insert("rect1".into());

        view.perform_alt_copy(&mut library);

        let clone_name: String = view.selected.iter().next().unwrap().clone();
        // Incoming connection from non-selected node should be duplicated
        assert!(library.root.connections.iter().any(|c|
            c.output_node == "source1" && c.input_node == clone_name && c.input_port == "shape"
        ));
    }

    #[test]
    fn test_alt_copy_outgoing_connections_not_duplicated() {
        let mut library = Arc::new(NodeLibrary::new("test"));
        {
            let lib = Arc::make_mut(&mut library);
            lib.root.children.push(Node::new("rect1")); // Selected
            lib.root.children.push(Node::new("target1")); // Not selected
            lib.root.connections.push(Connection::new("rect1", "target1", "shape"));
        }

        let mut view = NetworkView::new();
        view.selected.insert("rect1".into());

        view.perform_alt_copy(&mut library);

        let clone_name: String = view.selected.iter().next().unwrap().clone();
        // Outgoing connection should NOT be duplicated
        assert!(!library.root.connections.iter().any(|c|
            c.output_node == clone_name && c.input_node == "target1"
        ));
        // Original connection should still exist
        assert_eq!(library.root.connections.len(), 1);
    }

    #[test]
    fn test_alt_copy_name_collision_across_batch() {
        let mut library = Arc::new(NodeLibrary::new("test"));
        {
            let lib = Arc::make_mut(&mut library);
            lib.root.children.push(Node::new("rect"));
            lib.root.children.push(Node::new("rect1"));
        }

        let mut view = NetworkView::new();
        view.selected.insert("rect".into());
        view.selected.insert("rect1".into());

        view.perform_alt_copy(&mut library);

        // Should have 4 nodes, each with a unique name
        assert_eq!(library.root.children.len(), 4);
        let names: HashSet<String> = library.root.children.iter().map(|c| c.name.clone()).collect();
        assert_eq!(names.len(), 4);
        // rect and rect1 still exist, two new names generated
        assert!(names.contains("rect"));
        assert!(names.contains("rect1"));
    }
}
