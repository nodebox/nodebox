//! Tabbed viewer pane with canvas and data views.

use eframe::egui::{self, Color32, Pos2, Rect, Stroke, Vec2};
use nodebox_core::geometry::{Color, Path, Point, PointType};
use crate::handles::HandleSet;
use crate::pan_zoom::PanZoom;
use crate::state::AppState;
use crate::theme;

/// Which tab is currently selected in the viewer.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum ViewerTab {
    Viewer,
    Data,
}

/// The tabbed viewer pane.
pub struct ViewerPane {
    /// Currently selected tab.
    current_tab: ViewerTab,
    /// Whether to show handles.
    pub show_handles: bool,
    /// Whether to show points.
    pub show_points: bool,
    /// Whether to show point numbers.
    pub show_point_numbers: bool,
    /// Whether to show origin crosshair.
    pub show_origin: bool,
    /// Whether to show geometry bounds.
    pub show_bounds: bool,
    /// Pan and zoom state.
    pan_zoom: PanZoom,
    /// Active handles for the selected node.
    handles: Option<HandleSet>,
    /// Index of handle being dragged.
    dragging_handle: Option<usize>,
    /// Whether space bar is currently pressed (for panning).
    is_space_pressed: bool,
    /// Whether we are currently panning with space+drag.
    is_panning: bool,
}

impl Default for ViewerPane {
    fn default() -> Self {
        Self::new()
    }
}

impl ViewerPane {
    /// Create a new viewer pane.
    pub fn new() -> Self {
        Self {
            current_tab: ViewerTab::Viewer,
            show_handles: true,
            show_points: false,
            show_point_numbers: false,
            show_origin: true,
            show_bounds: false,
            pan_zoom: PanZoom::with_zoom_limits(0.1, 10.0),
            handles: None,
            dragging_handle: None,
            is_space_pressed: false,
            is_panning: false,
        }
    }

    /// Get the current zoom level.
    pub fn zoom(&self) -> f32 {
        self.pan_zoom.zoom
    }

    /// Get the current pan offset.
    pub fn pan(&self) -> Vec2 {
        self.pan_zoom.pan
    }

    /// Zoom in by a step.
    pub fn zoom_in(&mut self) {
        self.pan_zoom.zoom_in();
    }

    /// Zoom out by a step.
    pub fn zoom_out(&mut self) {
        self.pan_zoom.zoom_out();
    }

    /// Fit the view to show all geometry.
    pub fn fit_to_window(&mut self) {
        self.pan_zoom.reset();
    }

    /// Get a mutable reference to the handles.
    pub fn handles_mut(&mut self) -> &mut Option<HandleSet> {
        &mut self.handles
    }

    /// Set handles.
    pub fn set_handles(&mut self, handles: Option<HandleSet>) {
        self.handles = handles;
    }

    /// Show the viewer pane with header tabs and toolbar.
    pub fn show(&mut self, ui: &mut egui::Ui, state: &AppState) {
        // Header with tabs and toolbar
        ui.horizontal(|ui| {
            // Tabs
            if ui
                .selectable_label(self.current_tab == ViewerTab::Viewer, "Viewer")
                .clicked()
            {
                self.current_tab = ViewerTab::Viewer;
            }
            if ui
                .selectable_label(self.current_tab == ViewerTab::Data, "Data")
                .clicked()
            {
                self.current_tab = ViewerTab::Data;
            }

            ui.separator();

            // Toolbar buttons (only show for Viewer tab)
            if self.current_tab == ViewerTab::Viewer {
                self.show_toolbar(ui);
            }
        });
        ui.separator();

        // Content area
        match self.current_tab {
            ViewerTab::Viewer => self.show_canvas(ui, state),
            ViewerTab::Data => self.show_data_view(ui, state),
        }
    }

    /// Show the toolbar buttons.
    fn show_toolbar(&mut self, ui: &mut egui::Ui) {
        // Toggle buttons styled as small selectable labels
        if ui
            .selectable_label(self.show_handles, "Handles")
            .on_hover_text("Show/hide handles")
            .clicked()
        {
            self.show_handles = !self.show_handles;
        }

        if ui
            .selectable_label(self.show_points, "Points")
            .on_hover_text("Show/hide path points")
            .clicked()
        {
            self.show_points = !self.show_points;
        }

        if ui
            .selectable_label(self.show_point_numbers, "Pt#")
            .on_hover_text("Show/hide point numbers")
            .clicked()
        {
            self.show_point_numbers = !self.show_point_numbers;
        }

        if ui
            .selectable_label(self.show_origin, "Origin")
            .on_hover_text("Show/hide origin crosshair")
            .clicked()
        {
            self.show_origin = !self.show_origin;
        }

        if ui
            .selectable_label(self.show_bounds, "Bounds")
            .on_hover_text("Show/hide geometry bounds")
            .clicked()
        {
            self.show_bounds = !self.show_bounds;
        }
    }

    /// Show the canvas viewer.
    fn show_canvas(&mut self, ui: &mut egui::Ui, state: &AppState) {
        let (response, painter) =
            ui.allocate_painter(ui.available_size(), egui::Sense::click_and_drag());

        let rect = response.rect;
        let center = rect.center().to_vec2();

        // Handle zoom with scroll wheel, centered on mouse position
        self.pan_zoom.handle_scroll_zoom(rect, ui, center);

        // Track space bar state for Photoshop-style panning
        if ui.input(|i| i.key_pressed(egui::Key::Space)) {
            self.is_space_pressed = true;
        }
        if ui.input(|i| i.key_released(egui::Key::Space)) {
            self.is_space_pressed = false;
            self.is_panning = false;
        }

        // Handle panning with space+drag, middle mouse button, or right drag
        if self.is_space_pressed && response.dragged_by(egui::PointerButton::Primary) {
            self.pan_zoom.pan += response.drag_delta();
            self.is_panning = true;
        }
        self.pan_zoom.handle_drag_pan(&response, egui::PointerButton::Middle);
        self.pan_zoom.handle_drag_pan(&response, egui::PointerButton::Secondary);

        // Change cursor when space is held (panning mode)
        if self.is_space_pressed && response.hovered() {
            if self.is_panning {
                ui.ctx().set_cursor_icon(egui::CursorIcon::Grabbing);
            } else {
                ui.ctx().set_cursor_icon(egui::CursorIcon::Grab);
            }
        }

        // Draw background
        let bg_color = egui::Color32::from_rgb(
            (state.background_color.r * 255.0) as u8,
            (state.background_color.g * 255.0) as u8,
            (state.background_color.b * 255.0) as u8,
        );
        painter.rect_filled(rect, 0.0, bg_color);

        // Draw a subtle grid
        self.draw_grid(&painter, rect);

        // Draw all geometry
        for path in &state.geometry {
            self.draw_path(&painter, path, center);

            // Draw bounds if enabled
            if self.show_bounds {
                self.draw_bounds(&painter, path, center);
            }

            // Draw points if enabled
            if self.show_points {
                self.draw_points(&painter, path, center);
            }
        }

        // Draw origin crosshair
        if self.show_origin {
            let origin = self.pan_zoom.world_to_screen(Pos2::ZERO, center);
            if rect.contains(origin) {
                let crosshair_size = 10.0;
                painter.line_segment(
                    [
                        origin - Vec2::new(crosshair_size, 0.0),
                        origin + Vec2::new(crosshair_size, 0.0),
                    ],
                    Stroke::new(1.0, egui::Color32::GRAY),
                );
                painter.line_segment(
                    [
                        origin - Vec2::new(0.0, crosshair_size),
                        origin + Vec2::new(0.0, crosshair_size),
                    ],
                    Stroke::new(1.0, egui::Color32::GRAY),
                );
            }
        }

        // Draw and handle interactive handles
        if self.show_handles {
            if let Some(ref handles) = self.handles {
                handles.draw(&painter, self.pan_zoom.zoom, self.pan_zoom.pan, center);
            }
        }
    }

    /// Show the data view (placeholder for now).
    fn show_data_view(&mut self, ui: &mut egui::Ui, state: &AppState) {
        ui.vertical_centered(|ui| {
            ui.add_space(50.0);
            ui.label(
                egui::RichText::new("Data View")
                    .color(theme::TEXT_DISABLED)
                    .size(16.0),
            );
            ui.add_space(10.0);
            ui.label(
                egui::RichText::new("Tabular view of geometry data coming soon.")
                    .color(theme::TEXT_DISABLED)
                    .size(12.0),
            );
            ui.add_space(20.0);

            // Show some basic stats
            ui.label(
                egui::RichText::new(format!("Paths: {}", state.geometry.len()))
                    .color(theme::TEXT_NORMAL)
                    .size(12.0),
            );

            let total_points: usize = state
                .geometry
                .iter()
                .flat_map(|p| &p.contours)
                .map(|c| c.points.len())
                .sum();
            ui.label(
                egui::RichText::new(format!("Total points: {}", total_points))
                    .color(theme::TEXT_NORMAL)
                    .size(12.0),
            );
        });
    }

    /// Handle interaction and return any parameter changes.
    pub fn handle_interaction(&mut self, ui: &egui::Ui, rect: Rect) -> Option<(String, Point)> {
        if !self.show_handles {
            return None;
        }

        let center = rect.center().to_vec2();

        // Check for handle dragging
        if let Some(ref mut handles) = self.handles {
            let mouse_pos = ui.input(|i| i.pointer.hover_pos());

            // Check for drag start
            if ui.input(|i| i.pointer.button_pressed(egui::PointerButton::Primary)) {
                if let Some(pos) = mouse_pos {
                    if let Some(idx) = handles.hit_test(pos, self.pan_zoom.zoom, self.pan_zoom.pan, center) {
                        self.dragging_handle = Some(idx);
                        if let Some(handle) = handles.handles_mut().get_mut(idx) {
                            handle.dragging = true;
                        }
                    }
                }
            }

            // Handle dragging
            if let Some(idx) = self.dragging_handle {
                if ui.input(|i| i.pointer.button_down(egui::PointerButton::Primary)) {
                    if let Some(pos) = mouse_pos {
                        handles.update_handle_position(idx, pos, self.pan_zoom.zoom, self.pan_zoom.pan, center);
                    }
                } else {
                    // Drag ended
                    if let Some(handle) = handles.handles_mut().get_mut(idx) {
                        handle.dragging = false;
                        let param_name = handle.param_name.clone();
                        let position = handle.position;
                        self.dragging_handle = None;
                        return Some((param_name, position));
                    }
                    self.dragging_handle = None;
                }
            }
        }

        None
    }

    /// Draw a background grid.
    fn draw_grid(&self, painter: &egui::Painter, rect: Rect) {
        let grid_size = 50.0 * self.pan_zoom.zoom;
        let grid_color = egui::Color32::from_rgba_unmultiplied(200, 200, 200, 30);

        let center = rect.center().to_vec2();
        let origin = self.pan_zoom.pan + center;

        // Calculate grid offset
        let offset_x = origin.x % grid_size;
        let offset_y = origin.y % grid_size;

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

    /// Draw geometry bounds.
    fn draw_bounds(&self, painter: &egui::Painter, path: &Path, center: Vec2) {
        if let Some(bounds) = path.bounds() {
            let min = Pos2::new(bounds.x as f32, bounds.y as f32);
            let max = Pos2::new(
                (bounds.x + bounds.width) as f32,
                (bounds.y + bounds.height) as f32,
            );

            let screen_min = self.pan_zoom.world_to_screen(min, center);
            let screen_max = self.pan_zoom.world_to_screen(max, center);

            let bounds_rect = Rect::from_min_max(screen_min, screen_max);
            painter.rect_stroke(
                bounds_rect,
                0.0,
                Stroke::new(1.0, Color32::from_rgba_unmultiplied(255, 255, 0, 100)),
            );
        }
    }

    /// Draw path points.
    fn draw_points(&self, painter: &egui::Painter, path: &Path, center: Vec2) {
        for contour in &path.contours {
            for (i, pp) in contour.points.iter().enumerate() {
                let world_pt = Pos2::new(pp.point.x as f32, pp.point.y as f32);
                let screen_pt = self.pan_zoom.world_to_screen(world_pt, center);

                // Draw point marker
                let color = match pp.point_type {
                    PointType::LineTo => Color32::from_rgb(100, 200, 100),
                    PointType::CurveTo => Color32::from_rgb(200, 100, 100),
                    PointType::CurveData => Color32::from_rgb(100, 100, 200),
                };
                painter.circle_filled(screen_pt, 3.0, color);

                // Draw point number if enabled
                if self.show_point_numbers {
                    painter.text(
                        screen_pt + Vec2::new(5.0, -5.0),
                        egui::Align2::LEFT_BOTTOM,
                        i.to_string(),
                        egui::FontId::proportional(9.0),
                        Color32::WHITE,
                    );
                }
            }
        }
    }

    /// Draw a path on the canvas.
    fn draw_path(&self, painter: &egui::Painter, path: &Path, center: Vec2) {
        for contour in &path.contours {
            if contour.points.is_empty() {
                continue;
            }

            // Build the path points
            let mut egui_points: Vec<Pos2> = Vec::new();
            let mut i = 0;

            while i < contour.points.len() {
                let pp = &contour.points[i];
                let world_pt = Pos2::new(pp.point.x as f32, pp.point.y as f32);
                let screen_pt = self.pan_zoom.world_to_screen(world_pt, center);

                match pp.point_type {
                    PointType::LineTo => {
                        egui_points.push(screen_pt);
                        i += 1;
                    }
                    PointType::CurveData => {
                        // CurveData is a control point - look ahead for the full cubic bezier
                        // Structure: CurveData (ctrl1), CurveData (ctrl2), CurveTo (end)
                        if i + 2 < contour.points.len() {
                            let ctrl1 = &contour.points[i];
                            let ctrl2 = &contour.points[i + 1];
                            let end = &contour.points[i + 2];

                            // Get start point (last point in egui_points, or first point of contour)
                            let start = egui_points.last().copied().unwrap_or(screen_pt);

                            let c1 = self.world_to_screen(ctrl1.point, center);
                            let c2 = self.world_to_screen(ctrl2.point, center);
                            let e = self.world_to_screen(end.point, center);

                            // Sample the cubic bezier
                            for t in 1..=10 {
                                let t = t as f32 / 10.0;
                                let pt = cubic_bezier(start, c1, c2, e, t);
                                egui_points.push(pt);
                            }

                            i += 3; // Skip ctrl1, ctrl2, end
                        } else {
                            i += 1;
                        }
                    }
                    PointType::CurveTo => {
                        // Standalone CurveTo without preceding CurveData - treat as line
                        egui_points.push(screen_pt);
                        i += 1;
                    }
                }
            }

            if egui_points.len() < 2 {
                continue;
            }

            // Close the path if needed
            if contour.closed && !egui_points.is_empty() {
                egui_points.push(egui_points[0]);
            }

            // Draw fill
            if let Some(fill) = path.fill {
                let fill_color = color_to_egui(fill);
                if egui_points.len() >= 3 {
                    painter.add(egui::Shape::convex_polygon(
                        egui_points.clone(),
                        fill_color,
                        Stroke::NONE,
                    ));
                }
            }

            // Draw stroke
            if let Some(stroke_color) = path.stroke {
                let stroke = Stroke::new(
                    path.stroke_width as f32 * self.pan_zoom.zoom,
                    color_to_egui(stroke_color),
                );
                painter.add(egui::Shape::line(egui_points, stroke));
            } else if path.fill.is_none() {
                // If no fill and no stroke, draw a default stroke
                let stroke = Stroke::new(1.0, egui::Color32::BLACK);
                painter.add(egui::Shape::line(egui_points, stroke));
            }
        }
    }

    /// Convert a world point to screen coordinates.
    fn world_to_screen(&self, point: Point, center: Vec2) -> Pos2 {
        let world_pt = Pos2::new(point.x as f32, point.y as f32);
        self.pan_zoom.world_to_screen(world_pt, center)
    }

    /// Update handles for the selected node.
    pub fn update_handles_for_node(&mut self, node_name: Option<&str>, state: &AppState) {
        use crate::handles::{ellipse_handles, rect_handles, Handle};

        match node_name {
            Some(name) => {
                if let Some(node) = state.library.root.child(name) {
                    let mut handle_set = HandleSet::new(name);

                    if let Some(ref proto) = node.prototype {
                        match proto.as_str() {
                            "corevector.ellipse" => {
                                let x = node
                                    .input("x")
                                    .and_then(|p| p.value.as_float())
                                    .unwrap_or(0.0);
                                let y = node
                                    .input("y")
                                    .and_then(|p| p.value.as_float())
                                    .unwrap_or(0.0);
                                let width = node
                                    .input("width")
                                    .and_then(|p| p.value.as_float())
                                    .unwrap_or(100.0);
                                let height = node
                                    .input("height")
                                    .and_then(|p| p.value.as_float())
                                    .unwrap_or(100.0);

                                for h in ellipse_handles(x, y, width, height) {
                                    handle_set.add(h);
                                }
                            }
                            "corevector.rect" => {
                                let x = node
                                    .input("x")
                                    .and_then(|p| p.value.as_float())
                                    .unwrap_or(0.0);
                                let y = node
                                    .input("y")
                                    .and_then(|p| p.value.as_float())
                                    .unwrap_or(0.0);
                                let width = node
                                    .input("width")
                                    .and_then(|p| p.value.as_float())
                                    .unwrap_or(100.0);
                                let height = node
                                    .input("height")
                                    .and_then(|p| p.value.as_float())
                                    .unwrap_or(100.0);

                                for h in rect_handles(x, y, width, height) {
                                    handle_set.add(h);
                                }
                            }
                            "corevector.line" => {
                                let p1 = node
                                    .input("point1")
                                    .and_then(|p| p.value.as_point().cloned())
                                    .unwrap_or(Point::ZERO);
                                let p2 = node
                                    .input("point2")
                                    .and_then(|p| p.value.as_point().cloned())
                                    .unwrap_or(Point::new(100.0, 100.0));

                                handle_set.add(
                                    Handle::point("point1", p1)
                                        .with_color(Color32::from_rgb(255, 100, 100)),
                                );
                                handle_set.add(
                                    Handle::point("point2", p2)
                                        .with_color(Color32::from_rgb(100, 255, 100)),
                                );
                            }
                            "corevector.polygon" | "corevector.star" => {
                                let x = node
                                    .input("x")
                                    .and_then(|p| p.value.as_float())
                                    .unwrap_or(0.0);
                                let y = node
                                    .input("y")
                                    .and_then(|p| p.value.as_float())
                                    .unwrap_or(0.0);

                                handle_set.add(Handle::point("position", Point::new(x, y)));
                            }
                            _ => {}
                        }
                    }

                    if !handle_set.handles().is_empty() {
                        self.handles = Some(handle_set);
                    } else {
                        self.handles = None;
                    }
                } else {
                    self.handles = None;
                }
            }
            None => {
                self.handles = None;
            }
        }
    }
}

/// Convert a NodeBox color to an egui color.
fn color_to_egui(color: Color) -> egui::Color32 {
    egui::Color32::from_rgba_unmultiplied(
        (color.r * 255.0) as u8,
        (color.g * 255.0) as u8,
        (color.b * 255.0) as u8,
        (color.a * 255.0) as u8,
    )
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
