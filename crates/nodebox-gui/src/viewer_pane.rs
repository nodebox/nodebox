//! Tabbed viewer pane with canvas and data views.

use eframe::egui::{self, Color32, ColorImage, Pos2, Rect, Stroke, TextureHandle, TextureOptions, Vec2};
use egui_extras::{Column, TableBuilder};
use nodebox_core::geometry::{Color, Path, PathPoint, Point, PointType};
use std::collections::HashMap;
use nodebox_ops::data::DataValue;
use crate::components;
use crate::eval::NodeOutput;
use crate::handles::{FourPointHandle, HandleSet, HANDLE_COLOR};
use crate::pan_zoom::PanZoom;
use crate::state::AppState;
use crate::theme;

#[cfg(feature = "gpu-rendering")]
use crate::vello_viewer::VelloViewer;
#[cfg(feature = "gpu-rendering")]
use std::hash::{Hash, Hasher};

/// Re-export or define RenderState type for unified API.
/// When gpu-rendering is enabled, this is egui_wgpu::RenderState.
/// When disabled, we use a unit type placeholder.
#[cfg(feature = "gpu-rendering")]
pub type RenderState = egui_wgpu::RenderState;

#[cfg(not(feature = "gpu-rendering"))]
pub type RenderState = ();

/// Result of handle interaction.
#[derive(Clone, Debug)]
pub enum HandleResult {
    /// No interaction occurred.
    None,
    /// A single point changed (for regular handles).
    PointChange { param: String, value: Point },
    /// FourPointHandle changed (x, y, width, height).
    FourPointChange { x: f64, y: f64, width: f64, height: f64 },
}

/// Which tab is currently selected in the viewer.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum ViewerTab {
    Viewer,
    Data,
}

/// Which sub-view is selected in the Data tab.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum DataViewMode {
    Paths,
    Points,
}

/// Cached textures for outlined digit rendering (Houdini-style).
struct DigitCache {
    /// Texture handles for digits 0-9.
    textures: [Option<TextureHandle>; 10],
    /// Width of each digit texture.
    digit_width: f32,
    /// Height of each digit texture.
    digit_height: f32,
}

impl DigitCache {
    fn new() -> Self {
        Self {
            textures: Default::default(),
            digit_width: 0.0,
            digit_height: 0.0,
        }
    }

    /// Ensure digit textures are created.
    fn ensure_initialized(&mut self, ctx: &egui::Context) {
        if self.textures[0].is_some() {
            return; // Already initialized
        }

        // Create outlined digit textures
        const FONT_SIZE: f32 = 12.0;
        const PADDING: usize = 2; // For outline

        for digit in 0..10 {
            let digit_char = char::from_digit(digit as u32, 10).unwrap();
            let image = Self::render_outlined_digit(ctx, digit_char, FONT_SIZE, PADDING);

            if digit == 0 {
                self.digit_width = image.width() as f32;
                self.digit_height = image.height() as f32;
            }

            let texture = ctx.load_texture(
                format!("digit_{}", digit),
                image,
                TextureOptions::LINEAR,
            );
            self.textures[digit] = Some(texture);
        }
    }

    /// Render a single digit with white outline and blue fill.
    fn render_outlined_digit(ctx: &egui::Context, digit: char, font_size: f32, padding: usize) -> ColorImage {
        // Use egui's font system to get glyph info
        let font_id = egui::FontId::proportional(font_size);

        // Get the galley for measuring
        let galley = ctx.fonts_mut(|f| {
            f.layout_no_wrap(digit.to_string(), font_id.clone(), Color32::WHITE)
        });

        let glyph_width = galley.rect.width().ceil() as usize;
        let glyph_height = galley.rect.height().ceil() as usize;

        // Image size with padding for outline
        let width = glyph_width + padding * 2 + 2;
        let height = glyph_height + padding * 2;

        // Create image buffer
        let mut pixels = vec![Color32::TRANSPARENT; width * height];

        // Render outline (white) by sampling at offsets
        let outline_color = Color32::WHITE;
        let fill_color = HANDLE_COLOR;

        // Get font texture and UV info for the glyph
        // Since we can't easily access raw glyph data, use a simpler approach:
        // Render using a pre-defined bitmap font pattern for digits
        let bitmap = get_digit_bitmap(digit);

        let scale = (font_size / 8.0).max(1.0) as usize; // Scale factor
        let bmp_width = 5 * scale;
        let bmp_height = 7 * scale;

        // Center the bitmap in the image
        let offset_x = (width - bmp_width) / 2;
        let offset_y = (height - bmp_height) / 2;

        // Draw outline first (white, offset in 8 directions)
        for dy in -1i32..=1 {
            for dx in -1i32..=1 {
                if dx == 0 && dy == 0 {
                    continue;
                }
                draw_digit_bitmap(&mut pixels, width, &bitmap, scale,
                    (offset_x as i32 + dx) as usize,
                    (offset_y as i32 + dy) as usize,
                    outline_color);
            }
        }

        // Draw fill (blue)
        draw_digit_bitmap(&mut pixels, width, &bitmap, scale, offset_x, offset_y, fill_color);

        ColorImage {
            size: [width, height],
            pixels,
            source_size: egui::Vec2::new(width as f32, height as f32),
        }
    }

    /// Get texture for a digit.
    fn get(&self, digit: usize) -> Option<&TextureHandle> {
        self.textures.get(digit).and_then(|t| t.as_ref())
    }
}

/// 5x7 bitmap font for digits 0-9.
fn get_digit_bitmap(digit: char) -> [u8; 7] {
    match digit {
        '0' => [0b01110, 0b10001, 0b10011, 0b10101, 0b11001, 0b10001, 0b01110],
        '1' => [0b00100, 0b01100, 0b00100, 0b00100, 0b00100, 0b00100, 0b01110],
        '2' => [0b01110, 0b10001, 0b00001, 0b00010, 0b00100, 0b01000, 0b11111],
        '3' => [0b11111, 0b00010, 0b00100, 0b00010, 0b00001, 0b10001, 0b01110],
        '4' => [0b00010, 0b00110, 0b01010, 0b10010, 0b11111, 0b00010, 0b00010],
        '5' => [0b11111, 0b10000, 0b11110, 0b00001, 0b00001, 0b10001, 0b01110],
        '6' => [0b00110, 0b01000, 0b10000, 0b11110, 0b10001, 0b10001, 0b01110],
        '7' => [0b11111, 0b00001, 0b00010, 0b00100, 0b01000, 0b01000, 0b01000],
        '8' => [0b01110, 0b10001, 0b10001, 0b01110, 0b10001, 0b10001, 0b01110],
        '9' => [0b01110, 0b10001, 0b10001, 0b01111, 0b00001, 0b00010, 0b01100],
        _ => [0; 7],
    }
}

/// Draw a digit bitmap to the pixel buffer.
fn draw_digit_bitmap(pixels: &mut [Color32], img_width: usize, bitmap: &[u8; 7], scale: usize, x_off: usize, y_off: usize, color: Color32) {
    for (row, bits) in bitmap.iter().enumerate() {
        for col in 0..5 {
            if (bits >> (4 - col)) & 1 == 1 {
                // Draw scaled pixel
                for sy in 0..scale {
                    for sx in 0..scale {
                        let px = x_off + col * scale + sx;
                        let py = y_off + row * scale + sy;
                        if px < img_width && py < pixels.len() / img_width {
                            pixels[py * img_width + px] = color;
                        }
                    }
                }
            }
        }
    }
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
    /// Whether to show the canvas border.
    pub show_canvas_border: bool,
    /// Pan and zoom state.
    pan_zoom: PanZoom,
    /// Active handles for the selected node.
    handles: Option<HandleSet>,
    /// FourPointHandle for rect nodes.
    four_point_handle: Option<FourPointHandle>,
    /// Index of handle being dragged.
    dragging_handle: Option<usize>,
    /// Whether space bar is currently pressed (for panning).
    is_space_pressed: bool,
    /// Whether we are currently panning with space+drag.
    is_panning: bool,
    /// Cached digit textures for point numbers.
    digit_cache: DigitCache,
    /// GPU-accelerated Vello viewer (when gpu-rendering feature is enabled).
    #[cfg(feature = "gpu-rendering")]
    vello_viewer: VelloViewer,
    /// Whether to use GPU rendering (can be toggled at runtime).
    #[cfg(feature = "gpu-rendering")]
    pub use_gpu_rendering: bool,
    /// Current data view mode (paths or points).
    data_view_mode: DataViewMode,
    /// The user's preferred tab when a Geometry node is rendered.
    preferred_geometry_tab: ViewerTab,
    /// Whether the Visual tab is available (rendered node outputs Geometry/Point).
    visual_tab_available: bool,
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
            show_canvas_border: true,
            pan_zoom: PanZoom::with_zoom_limits(0.1, 10.0),
            handles: None,
            four_point_handle: None,
            dragging_handle: None,
            is_space_pressed: false,
            is_panning: false,
            digit_cache: DigitCache::new(),
            #[cfg(feature = "gpu-rendering")]
            vello_viewer: VelloViewer::new(),
            #[cfg(feature = "gpu-rendering")]
            use_gpu_rendering: true, // Default to GPU rendering when available
            data_view_mode: DataViewMode::Points,
            preferred_geometry_tab: ViewerTab::Viewer,
            visual_tab_available: true,
        }
    }

    /// Get the current pan offset.
    #[allow(dead_code)]
    pub fn pan(&self) -> Vec2 {
        self.pan_zoom.pan
    }

    /// Zoom in by a step.
    #[allow(dead_code)]
    pub fn zoom_in(&mut self) {
        self.pan_zoom.zoom_in();
    }

    /// Zoom out by a step.
    #[allow(dead_code)]
    pub fn zoom_out(&mut self) {
        self.pan_zoom.zoom_out();
    }

    /// Fit the view to show all geometry.
    #[allow(dead_code)]
    pub fn fit_to_window(&mut self) {
        self.pan_zoom.reset();
    }

    /// Reset zoom to 100% (actual size).
    pub fn reset_zoom(&mut self) {
        self.pan_zoom.reset();
    }

    /// Compute a hash of the geometry for cache invalidation.
    #[cfg(feature = "gpu-rendering")]
    fn hash_geometry(geometry: &[Path]) -> u64 {
        use std::collections::hash_map::DefaultHasher;
        let mut hasher = DefaultHasher::new();
        // Hash path count and basic properties
        geometry.len().hash(&mut hasher);
        for path in geometry {
            path.contours.len().hash(&mut hasher);
            for contour in &path.contours {
                contour.points.len().hash(&mut hasher);
                contour.closed.hash(&mut hasher);
                // Hash actual point coordinates (critical for cache invalidation!)
                for point in &contour.points {
                    point.point.x.to_bits().hash(&mut hasher);
                    point.point.y.to_bits().hash(&mut hasher);
                    std::mem::discriminant(&point.point_type).hash(&mut hasher);
                }
            }
            // Hash fill color
            if let Some(fill) = path.fill {
                fill.r.to_bits().hash(&mut hasher);
                fill.g.to_bits().hash(&mut hasher);
                fill.b.to_bits().hash(&mut hasher);
                fill.a.to_bits().hash(&mut hasher);
            }
            // Hash stroke color
            if let Some(stroke) = path.stroke {
                stroke.r.to_bits().hash(&mut hasher);
                stroke.g.to_bits().hash(&mut hasher);
                stroke.b.to_bits().hash(&mut hasher);
                stroke.a.to_bits().hash(&mut hasher);
            }
            // Hash stroke width
            path.stroke_width.to_bits().hash(&mut hasher);
        }
        hasher.finish()
    }

    /// Get a mutable reference to the handles.
    #[allow(dead_code)]
    pub fn handles_mut(&mut self) -> &mut Option<HandleSet> {
        &mut self.handles
    }

    /// Set handles.
    #[allow(dead_code)]
    pub fn set_handles(&mut self, handles: Option<HandleSet>) {
        self.handles = handles;
    }

    /// Show the viewer pane with header tabs and toolbar.
    /// Returns any handle interaction result.
    ///
    /// Pass `render_state` for GPU-accelerated rendering when available.
    /// When `gpu-rendering` feature is disabled, pass `None`.
    pub fn show(&mut self, ui: &mut egui::Ui, state: &AppState, render_state: Option<&RenderState>) -> HandleResult {
        // Auto-switch tab based on whether the rendered node outputs geometry.
        // When there's no rendered node at all, keep the current tab as-is
        // so the user stays in Viewer mode (showing an empty canvas) if that
        // was their preferred mode.
        let has_rendered = state.library.root.rendered_child.is_some();
        let is_geometry = state.library.is_rendered_output_geometry();
        let was_available = self.visual_tab_available;

        if !has_rendered {
            // No rendered node: keep current tab, but mark visual as available
            // so the Viewer tab remains selectable (shows empty canvas).
            self.visual_tab_available = true;
        } else if is_geometry && !was_available {
            // Switching back to geometry: restore preferred tab
            self.visual_tab_available = true;
            self.current_tab = self.preferred_geometry_tab;
        } else if !is_geometry && was_available {
            // Switching to non-geometry: save preference, force Data
            self.preferred_geometry_tab = self.current_tab;
            self.visual_tab_available = false;
            self.current_tab = ViewerTab::Data;
        } else if !is_geometry {
            // Staying on non-geometry: keep Data forced
            self.current_tab = ViewerTab::Data;
        }

        // Remove spacing so content is snug against header
        ui.spacing_mut().item_spacing = egui::vec2(0.0, 0.0);

        // Draw header with "VIEWER" title and separator
        let (header_rect, mut x) = components::draw_pane_header_with_title(ui, "Viewer");

        // Segmented control for Visual/Data toggle
        let selected_index = if self.current_tab == ViewerTab::Viewer { 0 } else { 1 };
        let disabled = if self.visual_tab_available { None } else { Some(0) };
        let (clicked_index, new_x) = components::header_segmented_control(
            ui,
            header_rect,
            x,
            ["Visual", "Data"],
            selected_index,
            disabled,
        );
        if let Some(index) = clicked_index {
            self.current_tab = if index == 0 { ViewerTab::Viewer } else { ViewerTab::Data };
            if self.visual_tab_available {
                self.preferred_geometry_tab = self.current_tab;
            }
        }
        x = new_x + theme::PADDING_XL; // 16px spacing after segmented control

        if self.current_tab == ViewerTab::Viewer {
            // Visual tab: show toggle buttons for visual options
            let (clicked, new_x) = components::header_tab_button(
                ui,
                header_rect,
                x,
                "Handles",
                self.show_handles,
            );
            if clicked {
                self.show_handles = !self.show_handles;
            }
            x = new_x;

            let (clicked, new_x) = components::header_tab_button(
                ui,
                header_rect,
                x,
                "Points",
                self.show_points,
            );
            if clicked {
                self.show_points = !self.show_points;
            }
            x = new_x;

            let (clicked, new_x) = components::header_tab_button(
                ui,
                header_rect,
                x,
                "Pt#",
                self.show_point_numbers,
            );
            if clicked {
                self.show_point_numbers = !self.show_point_numbers;
            }
            x = new_x;

            let (clicked, new_x) = components::header_tab_button(
                ui,
                header_rect,
                x,
                "Origin",
                self.show_origin,
            );
            if clicked {
                self.show_origin = !self.show_origin;
            }
            x = new_x;

            let (clicked, new_x) = components::header_tab_button(
                ui,
                header_rect,
                x,
                "Canvas",
                self.show_canvas_border,
            );
            if clicked {
                self.show_canvas_border = !self.show_canvas_border;
            }
            x = new_x;
        } else {
            // Data tab: show data view mode selector
            if state.node_output.is_geometry() {
                // Geometry output: Paths/Points segmented control
                let selected_index = if self.data_view_mode == DataViewMode::Paths { 0 } else { 1 };
                let (clicked_index, new_x) = components::header_segmented_control(
                    ui,
                    header_rect,
                    x,
                    ["Paths", "Points"],
                    selected_index,
                    None,
                );
                if let Some(index) = clicked_index {
                    self.data_view_mode = if index == 0 { DataViewMode::Paths } else { DataViewMode::Points };
                }
                x = new_x;
            } else {
                // Non-geometry output: show type label
                let type_label = {
                    let label = state.node_output.type_label();
                    let mut s = label.to_string();
                    if let Some(first) = s.get_mut(0..1) {
                        first.make_ascii_uppercase();
                    }
                    s
                };
                ui.painter().text(
                    egui::pos2(x + 6.0, header_rect.center().y),
                    egui::Align2::LEFT_CENTER,
                    &type_label,
                    egui::FontId::proportional(11.0),
                    theme::TEXT_SUBDUED,
                );
                // Approximate width for spacing
                x += 60.0;
            }
        }

        // Zoom controls on the right side: [-] [100%] [+]
        let zoom_controls_width = 24.0 + 48.0 + 24.0 + theme::PADDING; // minus + percent + plus + right padding
        let spacer_width = header_rect.right() - x - zoom_controls_width;
        x += spacer_width.max(0.0);

        // Zoom out button (-)
        let (clicked, new_x) = components::header_icon_button(ui, header_rect, x, "−");
        if clicked {
            self.pan_zoom.zoom_out();
        }
        x = new_x;

        // Zoom percentage DragValue
        let (new_zoom, new_x) = components::header_zoom_control(ui, header_rect, x, self.pan_zoom.zoom);
        if let Some(zoom) = new_zoom {
            self.pan_zoom.zoom = zoom.clamp(0.1, 10.0);
        }
        x = new_x;

        // Zoom in button (+)
        let (clicked, _) = components::header_icon_button(ui, header_rect, x, "+");
        if clicked {
            self.pan_zoom.zoom_in();
        }

        // Content area (directly after header, no extra spacing)
        match self.current_tab {
            ViewerTab::Viewer => self.show_canvas(ui, state, render_state),
            ViewerTab::Data => {
                self.show_data_view(ui, state);
                HandleResult::None
            }
        }
    }

    /// Show the canvas viewer.
    /// Uses GPU rendering when available (gpu-rendering feature + valid render_state + use_gpu_rendering enabled).
    /// Falls back to CPU rendering otherwise.
    fn show_canvas(&mut self, ui: &mut egui::Ui, state: &AppState, render_state: Option<&RenderState>) -> HandleResult {
        use crate::handles::{screen_to_world, FourPointDragState};

        // Initialize digit cache if needed
        self.digit_cache.ensure_initialized(ui.ctx());

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
        let is_panning = self.is_space_pressed && response.dragged_by(egui::PointerButton::Primary);
        if is_panning {
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

        // Draw all geometry (using GPU or CPU rendering)
        self.render_geometry(ui, &painter, state, render_state, rect, center);

        // Draw canvas border (uses document width/height)
        // Drawn after geometry so it's visible on top of the Vello GPU texture.
        if self.show_canvas_border {
            self.draw_canvas_border(&painter, center, state.library.width(), state.library.height());
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
                    Stroke::new(1.0, theme::VIEWER_CROSSHAIR),
                );
                painter.line_segment(
                    [
                        origin - Vec2::new(0.0, crosshair_size),
                        origin + Vec2::new(0.0, crosshair_size),
                    ],
                    Stroke::new(1.0, theme::VIEWER_CROSSHAIR),
                );
            }
        }

        // Draw and handle interactive handles
        if self.show_handles {
            if let Some(ref handles) = self.handles {
                handles.draw(&painter, self.pan_zoom.zoom, self.pan_zoom.pan, center);
            }
            if let Some(ref handle) = self.four_point_handle {
                handle.draw(&painter, self.pan_zoom.zoom, self.pan_zoom.pan, center);
            }
        }

        // Draw point numbers on top of everything (including handles)
        if self.show_point_numbers {
            let mut point_index = 0usize;
            for path in &state.geometry {
                point_index = self.draw_point_numbers(&painter, path, center, point_index);
            }
        }

        // Handle interactions (only if not panning)
        if !self.is_space_pressed && self.show_handles {
            let mouse_pos = ui.input(|i| i.pointer.hover_pos());

            // Handle FourPointHandle first (takes priority)
            if let Some(ref mut four_point) = self.four_point_handle {
                // Check for drag start
                if response.drag_started_by(egui::PointerButton::Primary) {
                    if let Some(pos) = mouse_pos {
                        if let Some(hit_state) = four_point.hit_test(pos, self.pan_zoom.zoom, self.pan_zoom.pan, center) {
                            let world_pos = screen_to_world(pos, self.pan_zoom.zoom, self.pan_zoom.pan, center);
                            four_point.start_drag(hit_state, world_pos);
                        }
                    }
                }

                // Handle dragging
                if four_point.is_dragging() {
                    if response.drag_stopped_by(egui::PointerButton::Primary) {
                        // Drag ended - return final values
                        let (x, y, width, height) = four_point.end_drag();
                        return HandleResult::FourPointChange { x, y, width, height };
                    } else if response.dragged_by(egui::PointerButton::Primary) {
                        // Still dragging - update and return current values for live preview
                        if let Some(pos) = mouse_pos {
                            let world_pos = screen_to_world(pos, self.pan_zoom.zoom, self.pan_zoom.pan, center);
                            four_point.update_drag(world_pos);
                        }
                        // Return current values to trigger re-render
                        return HandleResult::FourPointChange {
                            x: four_point.center.x,
                            y: four_point.center.y,
                            width: four_point.width,
                            height: four_point.height,
                        };
                    }
                }

                // If FourPointHandle is dragging, don't process regular handles
                if four_point.drag_state != FourPointDragState::None {
                    return HandleResult::None;
                }
            }

            // Check for regular handle dragging
            if let Some(ref mut handles) = self.handles {
                // Check for drag start
                if response.drag_started_by(egui::PointerButton::Primary) {
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
                    if response.drag_stopped_by(egui::PointerButton::Primary) {
                        // Drag ended
                        if let Some(handle) = handles.handles_mut().get_mut(idx) {
                            handle.dragging = false;
                            let param_name = handle.param_name.clone();
                            let position = handle.position;
                            self.dragging_handle = None;
                            return HandleResult::PointChange { param: param_name, value: position };
                        }
                        self.dragging_handle = None;
                    } else if response.dragged_by(egui::PointerButton::Primary) {
                        // Still dragging - update and return current values for live preview
                        if let Some(pos) = mouse_pos {
                            handles.update_handle_position(idx, pos, self.pan_zoom.zoom, self.pan_zoom.pan, center);
                        }
                        // Return current values to trigger re-render
                        if let Some(handle) = handles.handles().get(idx) {
                            return HandleResult::PointChange {
                                param: handle.param_name.clone(),
                                value: handle.position,
                            };
                        }
                    }
                }
            }
        }

        HandleResult::None
    }

    /// Render geometry using GPU when available, falling back to CPU.
    #[cfg(feature = "gpu-rendering")]
    fn render_geometry(
        &mut self,
        ui: &mut egui::Ui,
        painter: &egui::Painter,
        state: &AppState,
        render_state: Option<&RenderState>,
        rect: Rect,
        center: Vec2,
    ) {
        let use_gpu = render_state.is_some()
            && self.use_gpu_rendering
            && self.vello_viewer.is_available();

        if use_gpu {
            let render_state = render_state.unwrap();

            // Compute geometry hash for cache invalidation
            let geometry_hash = Self::hash_geometry(&state.geometry);

            // Set background color (Vello will render the background)
            self.vello_viewer.set_background_color(state.background_color);

            // Render with Vello using shared wgpu device
            self.vello_viewer.render(
                render_state,
                ui,
                &state.geometry,
                self.pan_zoom.pan,
                self.pan_zoom.zoom,
                rect,
                geometry_hash,
            );

            // Draw points overlay if enabled or if output is from a Point-type node
            if self.show_points || state.library.is_rendered_output_point() {
                for path in &state.geometry {
                    self.draw_points(painter, path, center);
                }
            }
        } else {
            self.render_geometry_cpu(painter, state, center);
        }
    }

    /// Render geometry using CPU (when gpu-rendering feature is disabled).
    #[cfg(not(feature = "gpu-rendering"))]
    fn render_geometry(
        &mut self,
        _ui: &mut egui::Ui,
        painter: &egui::Painter,
        state: &AppState,
        _render_state: Option<&RenderState>,
        _rect: Rect,
        center: Vec2,
    ) {
        self.render_geometry_cpu(painter, state, center);
    }

    /// CPU-based geometry rendering (used as fallback or when GPU is unavailable).
    fn render_geometry_cpu(&self, painter: &egui::Painter, state: &AppState, center: Vec2) {
        for path in &state.geometry {
            self.draw_path(painter, path, center);

            if self.show_points || state.library.is_rendered_output_point() {
                self.draw_points(painter, path, center);
            }
        }
    }

    /// Show the data view with spreadsheet table.
    fn show_data_view(&mut self, ui: &mut egui::Ui, state: &AppState) {
        if state.node_output.is_geometry() {
            // Geometry output: show Paths or Points table
            if state.geometry.is_empty() {
                Self::show_data_empty(ui);
                return;
            }

            match self.data_view_mode {
                DataViewMode::Paths => Self::show_paths_table(ui, state),
                DataViewMode::Points => Self::show_points_table(ui, state),
            }
        } else if state.node_output.is_data_rows() {
            // Data rows output: show multi-column data table
            if state.node_output.item_count() == 0 {
                Self::show_data_empty(ui);
            } else {
                Self::show_data_rows_table(ui, &state.node_output);
            }
        } else {
            // Non-geometry output: show values table
            if state.node_output.item_count() == 0 {
                Self::show_data_empty(ui);
            } else {
                Self::show_values_table(ui, &state.node_output);
            }
        }
    }

    /// Show a table of non-geometry values.
    fn show_values_table(ui: &mut egui::Ui, output: &crate::eval::NodeOutput) {
        let text_height = theme::ROW_HEIGHT;
        let values = output.to_display_strings();
        let is_color = output.is_color();
        let type_label = output.type_label();

        let mut table = TableBuilder::new(ui)
            .striped(false)
            .resizable(true)
            .cell_layout(egui::Layout::left_to_right(egui::Align::Center))
            .min_scrolled_height(0.0)
            .max_scroll_height(f32::INFINITY)
            .column(Column::exact(60.0));                              // Index
        if is_color {
            table = table.column(Column::exact(24.0));                 // Swatch
        }
        table = table.column(Column::remainder().at_least(100.0).clip(true)); // Value

        // Capitalize first letter of type label for header
        let value_header = {
            let mut s = type_label.to_string();
            if let Some(first) = s.get_mut(0..1) {
                first.make_ascii_uppercase();
            }
            s
        };

        table
            .header(theme::TABLE_HEADER_HEIGHT, |mut header| {
                header.col(|ui| {
                    ui.painter().rect_filled(ui.max_rect(), 0.0, theme::TABLE_HEADER_BG);
                    ui.add_space(8.0);
                    ui.label(
                        egui::RichText::new("Index")
                            .color(theme::TABLE_HEADER_TEXT)
                            .size(11.0),
                    );
                });
                if is_color {
                    header.col(|ui| {
                        ui.painter().rect_filled(ui.max_rect(), 0.0, theme::TABLE_HEADER_BG);
                    });
                }
                header.col(|ui| {
                    ui.painter().rect_filled(ui.max_rect(), 0.0, theme::TABLE_HEADER_BG);
                    ui.add_space(8.0);
                    ui.label(
                        egui::RichText::new(&value_header)
                            .color(theme::TABLE_HEADER_TEXT)
                            .size(11.0),
                    );
                });
            })
            .body(|body| {
                body.rows(text_height, values.len(), |mut row| {
                    let row_index = row.index();
                    let row_bg = if row_index % 2 == 0 {
                        theme::TABLE_ROW_EVEN
                    } else {
                        theme::TABLE_ROW_ODD
                    };

                    // Index
                    row.col(|ui| {
                        ui.painter().rect_filled(ui.max_rect(), 0.0, row_bg);
                        ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                            ui.add_space(4.0);
                            ui.label(
                                egui::RichText::new(format!("{}", row_index))
                                    .color(theme::TABLE_INDEX_TEXT)
                                    .size(11.0),
                            );
                        });
                    });

                    // Swatch (color types only)
                    if is_color {
                        row.col(|ui| {
                            ui.painter().rect_filled(ui.max_rect(), 0.0, row_bg);
                            if let Some(c) = output.color_at(row_index) {
                                let swatch_size = 14.0;
                                let rect = ui.max_rect();
                                let swatch_rect = egui::Rect::from_min_size(
                                    egui::pos2(
                                        rect.center().x - swatch_size / 2.0,
                                        rect.center().y - swatch_size / 2.0,
                                    ),
                                    egui::vec2(swatch_size, swatch_size),
                                );
                                let egui_color = Color32::from_rgba_unmultiplied(
                                    (c.r * 255.0) as u8,
                                    (c.g * 255.0) as u8,
                                    (c.b * 255.0) as u8,
                                    (c.a * 255.0) as u8,
                                );
                                if c.a < 1.0 {
                                    ui.painter().rect_filled(swatch_rect, 0.0, Color32::WHITE);
                                }
                                ui.painter().rect_filled(swatch_rect, 0.0, egui_color);
                                ui.painter().rect_stroke(
                                    swatch_rect,
                                    0.0,
                                    Stroke::new(1.0, theme::ZINC_500),
                                    egui::StrokeKind::Inside,
                                );
                            }
                        });
                    }

                    // Value
                    row.col(|ui| {
                        ui.painter().rect_filled(ui.max_rect(), 0.0, row_bg);
                        ui.add_space(8.0);
                        ui.label(
                            egui::RichText::new(&values[row_index])
                                .color(theme::TABLE_CELL_TEXT)
                                .size(11.0),
                        );
                    });
                });
            });
    }

    /// Show a multi-column table for DataRow/DataRows output.
    fn show_data_rows_table(ui: &mut egui::Ui, output: &NodeOutput) {
        let text_height = theme::ROW_HEIGHT;

        // Collect all rows into a reference list
        let rows: Vec<&HashMap<String, DataValue>> = match output {
            NodeOutput::DataRows(rs) => rs.iter().collect(),
            NodeOutput::DataRow(r) => vec![r],
            _ => return,
        };

        if rows.is_empty() {
            return;
        }

        // Determine column headers: collect all keys across all rows, sorted for stable display
        let mut column_set = std::collections::BTreeSet::new();
        for row in &rows {
            for key in row.keys() {
                column_set.insert(key.clone());
            }
        }
        let columns: Vec<String> = column_set.into_iter().collect();

        if columns.is_empty() {
            return;
        }

        // Build table with Index + one column per data key
        let mut table = TableBuilder::new(ui)
            .striped(false)
            .resizable(true)
            .cell_layout(egui::Layout::left_to_right(egui::Align::Center))
            .min_scrolled_height(0.0)
            .max_scroll_height(f32::INFINITY)
            .column(Column::exact(60.0)); // Index

        for _ in &columns {
            table = table.column(Column::initial(120.0).at_least(60.0).clip(true));
        }

        table
            .header(theme::TABLE_HEADER_HEIGHT, |mut header| {
                // Index header
                header.col(|ui| {
                    ui.painter().rect_filled(ui.max_rect(), 0.0, theme::TABLE_HEADER_BG);
                    ui.add_space(8.0);
                    ui.label(
                        egui::RichText::new("Index")
                            .color(theme::TABLE_HEADER_TEXT)
                            .size(11.0),
                    );
                });
                // Data column headers
                for col_name in &columns {
                    header.col(|ui| {
                        ui.painter().rect_filled(ui.max_rect(), 0.0, theme::TABLE_HEADER_BG);
                        ui.add_space(8.0);
                        ui.label(
                            egui::RichText::new(col_name)
                                .color(theme::TABLE_HEADER_TEXT)
                                .size(11.0),
                        );
                    });
                }
            })
            .body(|body| {
                body.rows(text_height, rows.len(), |mut row| {
                    let row_index = row.index();
                    let data_row = rows[row_index];
                    let row_bg = if row_index % 2 == 0 {
                        theme::TABLE_ROW_EVEN
                    } else {
                        theme::TABLE_ROW_ODD
                    };

                    // Index column
                    row.col(|ui| {
                        ui.painter().rect_filled(ui.max_rect(), 0.0, row_bg);
                        ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                            ui.add_space(4.0);
                            ui.label(
                                egui::RichText::new(format!("{}", row_index))
                                    .color(theme::TABLE_INDEX_TEXT)
                                    .size(11.0),
                            );
                        });
                    });

                    // Data columns
                    for col_name in &columns {
                        row.col(|ui| {
                            ui.painter().rect_filled(ui.max_rect(), 0.0, row_bg);
                            ui.add_space(8.0);
                            let cell_text = match data_row.get(col_name) {
                                Some(val) => val.as_string(),
                                None => String::new(),
                            };
                            ui.label(
                                egui::RichText::new(cell_text)
                                    .color(theme::TABLE_CELL_TEXT)
                                    .size(11.0),
                            );
                        });
                    }
                });
            });
    }

    /// Show empty state when no data is available.
    fn show_data_empty(ui: &mut egui::Ui) {
        ui.vertical_centered(|ui| {
            ui.add_space(50.0);
            ui.label(
                egui::RichText::new("No data to display")
                    .color(theme::TEXT_DISABLED)
                    .size(14.0),
            );
            ui.add_space(8.0);
            ui.label(
                egui::RichText::new("Render a node to see its data here.")
                    .color(theme::TEXT_DISABLED)
                    .size(11.0),
            );
        });
    }

    /// Show the path-level data table.
    fn show_paths_table(ui: &mut egui::Ui, state: &AppState) {
        let text_height = theme::ROW_HEIGHT;

        let table = TableBuilder::new(ui)
            .striped(false) // Custom zebra striping
            .resizable(true)
            .cell_layout(egui::Layout::left_to_right(egui::Align::Center))
            .min_scrolled_height(0.0)
            .max_scroll_height(f32::INFINITY)
            .column(Column::exact(60.0))                            // Index
            .column(Column::initial(120.0).at_least(80.0))          // Fill
            .column(Column::initial(120.0).at_least(80.0))          // Stroke
            .column(Column::initial(90.0).at_least(60.0))           // Stroke Width
            .column(Column::initial(70.0).at_least(50.0))           // Contours
            .column(Column::initial(70.0).at_least(50.0).clip(true)); // Points

        table
            .header(theme::TABLE_HEADER_HEIGHT, |mut header| {
                let headers = ["Index", "Fill", "Stroke", "Stroke Width", "Contours", "Points"];
                for h in headers {
                    header.col(|ui| {
                        ui.painter().rect_filled(ui.max_rect(), 0.0, theme::TABLE_HEADER_BG);
                        ui.add_space(8.0);
                        ui.label(
                            egui::RichText::new(h)
                                .color(theme::TABLE_HEADER_TEXT)
                                .size(11.0),
                        );
                    });
                }
            })
            .body(|body| {
                body.rows(text_height, state.geometry.len(), |mut row| {
                    let row_index = row.index();
                    let path = &state.geometry[row_index];
                    let row_bg = if row_index % 2 == 0 {
                        theme::TABLE_ROW_EVEN
                    } else {
                        theme::TABLE_ROW_ODD
                    };

                    // Index
                    row.col(|ui| {
                        ui.painter().rect_filled(ui.max_rect(), 0.0, row_bg);
                        ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                            ui.add_space(4.0);
                            ui.label(
                                egui::RichText::new(format!("{}", row_index))
                                    .color(theme::TABLE_INDEX_TEXT)
                                    .size(11.0),
                            );
                        });
                    });

                    // Fill
                    row.col(|ui| {
                        ui.painter().rect_filled(ui.max_rect(), 0.0, row_bg);
                        Self::draw_color_cell(ui, path.fill);
                    });

                    // Stroke
                    row.col(|ui| {
                        ui.painter().rect_filled(ui.max_rect(), 0.0, row_bg);
                        Self::draw_color_cell(ui, path.stroke);
                    });

                    // Stroke Width
                    row.col(|ui| {
                        ui.painter().rect_filled(ui.max_rect(), 0.0, row_bg);
                        ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                            ui.add_space(4.0);
                            ui.label(
                                egui::RichText::new(format!("{:.2}", path.stroke_width))
                                    .color(theme::TABLE_CELL_TEXT)
                                    .size(11.0),
                            );
                        });
                    });

                    // Contours
                    row.col(|ui| {
                        ui.painter().rect_filled(ui.max_rect(), 0.0, row_bg);
                        ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                            ui.add_space(4.0);
                            ui.label(
                                egui::RichText::new(format!("{}", path.contours.len()))
                                    .color(theme::TABLE_CELL_TEXT)
                                    .size(11.0),
                            );
                        });
                    });

                    // Points
                    row.col(|ui| {
                        ui.painter().rect_filled(ui.max_rect(), 0.0, row_bg);
                        ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                            ui.add_space(4.0);
                            ui.label(
                                egui::RichText::new(format!("{}", path.point_count()))
                                    .color(theme::TABLE_CELL_TEXT)
                                    .size(11.0),
                            );
                        });
                    });
                });
            });
    }

    /// Show the point-level data table.
    fn show_points_table(ui: &mut egui::Ui, state: &AppState) {
        let text_height = theme::ROW_HEIGHT;

        // Build flat list of (path_idx, contour_idx, point)
        let flat_points: Vec<(usize, usize, &PathPoint)> = state.geometry.iter()
            .enumerate()
            .flat_map(|(pi, path)| {
                path.contours.iter().enumerate().flat_map(move |(ci, contour)| {
                    contour.points.iter().map(move |pp| (pi, ci, pp))
                })
            })
            .collect();

        let total_rows = flat_points.len();

        let table = TableBuilder::new(ui)
            .striped(false)
            .resizable(true)
            .cell_layout(egui::Layout::left_to_right(egui::Align::Center))
            .min_scrolled_height(0.0)
            .max_scroll_height(f32::INFINITY)
            .column(Column::exact(60.0))                             // Index
            .column(Column::initial(100.0).at_least(70.0))           // X
            .column(Column::initial(100.0).at_least(70.0))           // Y
            .column(Column::initial(80.0).at_least(60.0))            // Type
            .column(Column::initial(50.0).at_least(40.0))            // Path
            .column(Column::initial(60.0).at_least(40.0).clip(true)); // Contour

        table
            .header(theme::TABLE_HEADER_HEIGHT, |mut header| {
                let headers = ["Index", "X", "Y", "Type", "Path", "Contour"];
                for h in headers {
                    header.col(|ui| {
                        ui.painter().rect_filled(ui.max_rect(), 0.0, theme::TABLE_HEADER_BG);
                        ui.add_space(8.0);
                        ui.label(
                            egui::RichText::new(h)
                                .color(theme::TABLE_HEADER_TEXT)
                                .size(11.0),
                        );
                    });
                }
            })
            .body(|body| {
                body.rows(text_height, total_rows, |mut row| {
                    let row_index = row.index();
                    let (path_idx, contour_idx, pp) = flat_points[row_index];
                    let row_bg = if row_index % 2 == 0 {
                        theme::TABLE_ROW_EVEN
                    } else {
                        theme::TABLE_ROW_ODD
                    };

                    // Index
                    row.col(|ui| {
                        ui.painter().rect_filled(ui.max_rect(), 0.0, row_bg);
                        ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                            ui.add_space(4.0);
                            ui.label(
                                egui::RichText::new(format!("{}", row_index))
                                    .color(theme::TABLE_INDEX_TEXT)
                                    .size(11.0),
                            );
                        });
                    });

                    // X
                    row.col(|ui| {
                        ui.painter().rect_filled(ui.max_rect(), 0.0, row_bg);
                        ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                            ui.add_space(4.0);
                            ui.label(
                                egui::RichText::new(format!("{:.2}", pp.point.x))
                                    .color(theme::TABLE_CELL_TEXT)
                                    .size(11.0),
                            );
                        });
                    });

                    // Y
                    row.col(|ui| {
                        ui.painter().rect_filled(ui.max_rect(), 0.0, row_bg);
                        ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                            ui.add_space(4.0);
                            ui.label(
                                egui::RichText::new(format!("{:.2}", pp.point.y))
                                    .color(theme::TABLE_CELL_TEXT)
                                    .size(11.0),
                            );
                        });
                    });

                    // Type
                    row.col(|ui| {
                        ui.painter().rect_filled(ui.max_rect(), 0.0, row_bg);
                        ui.label(
                            egui::RichText::new(Self::point_type_label(pp.point_type))
                                .color(theme::TABLE_CELL_TEXT)
                                .size(11.0),
                        );
                    });

                    // Path
                    row.col(|ui| {
                        ui.painter().rect_filled(ui.max_rect(), 0.0, row_bg);
                        ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                            ui.add_space(4.0);
                            ui.label(
                                egui::RichText::new(format!("{}", path_idx))
                                    .color(theme::TABLE_INDEX_TEXT)
                                    .size(11.0),
                            );
                        });
                    });

                    // Contour
                    row.col(|ui| {
                        ui.painter().rect_filled(ui.max_rect(), 0.0, row_bg);
                        ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                            ui.add_space(4.0);
                            ui.label(
                                egui::RichText::new(format!("{}", contour_idx))
                                    .color(theme::TABLE_INDEX_TEXT)
                                    .size(11.0),
                            );
                        });
                    });
                });
            });
    }

    /// Display label for a PointType.
    fn point_type_label(pt: PointType) -> &'static str {
        match pt {
            PointType::LineTo => "Line",
            PointType::CurveTo => "Curve",
            PointType::CurveData => "CurveCtl",
            PointType::QuadTo => "Quad",
            PointType::QuadData => "QuadCtl",
        }
    }

    /// Draw a color cell with swatch + hex text, or "--" for None.
    fn draw_color_cell(ui: &mut egui::Ui, color: Option<Color>) {
        match color {
            Some(c) => {
                let swatch_size = 12.0;
                let rect = ui.available_rect_before_wrap();

                // Draw color swatch
                let swatch_rect = egui::Rect::from_min_size(
                    egui::pos2(
                        rect.left() + 4.0,
                        rect.center().y - swatch_size / 2.0,
                    ),
                    egui::vec2(swatch_size, swatch_size),
                );
                let egui_color = Color32::from_rgba_unmultiplied(
                    (c.r * 255.0) as u8,
                    (c.g * 255.0) as u8,
                    (c.b * 255.0) as u8,
                    (c.a * 255.0) as u8,
                );

                // Checkerboard background for transparency
                if c.a < 1.0 {
                    ui.painter().rect_filled(swatch_rect, 0.0, Color32::WHITE);
                }
                ui.painter().rect_filled(swatch_rect, 0.0, egui_color);
                ui.painter().rect_stroke(
                    swatch_rect,
                    0.0,
                    Stroke::new(1.0, theme::ZINC_500),
                    egui::StrokeKind::Inside,
                );

                // Hex text after swatch
                let hex = c.to_hex();
                ui.painter().text(
                    egui::pos2(swatch_rect.right() + 6.0, rect.center().y),
                    egui::Align2::LEFT_CENTER,
                    &hex,
                    egui::FontId::proportional(11.0),
                    theme::TABLE_CELL_TEXT,
                );
                // Allocate space for layout
                ui.allocate_exact_size(
                    egui::vec2(swatch_size + 6.0 + 80.0, swatch_size),
                    egui::Sense::hover(),
                );
            }
            None => {
                ui.label(
                    egui::RichText::new("--")
                        .color(theme::TEXT_DISABLED)
                        .size(11.0),
                );
            }
        }
    }

    /// Draw the canvas border (document bounds).
    /// The border is drawn in screen space (constant 1px line width regardless of zoom).
    fn draw_canvas_border(&self, painter: &egui::Painter, center: Vec2, width: f64, height: f64) {
        // Canvas is centered at origin, so bounds are from -width/2 to +width/2
        let half_width = width as f32 / 2.0;
        let half_height = height as f32 / 2.0;

        let top_left = Pos2::new(-half_width, -half_height);
        let bottom_right = Pos2::new(half_width, half_height);

        let screen_top_left = self.pan_zoom.world_to_screen(top_left, center);
        let screen_bottom_right = self.pan_zoom.world_to_screen(bottom_right, center);

        let canvas_rect = Rect::from_min_max(screen_top_left, screen_bottom_right);

        // Draw border with constant 1px line width (screen space)
        let border_color = Color32::from_rgba_unmultiplied(128, 128, 128, 180);
        painter.rect_stroke(canvas_rect, 0.0, Stroke::new(1.0, border_color), egui::StrokeKind::Inside);
    }

    /// Draw a background grid.
    fn draw_grid(&self, painter: &egui::Painter, rect: Rect) {
        let grid_size = 50.0 * self.pan_zoom.zoom;
        let grid_color = theme::viewer_grid();

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

    /// Draw path points.
    fn draw_points(&self, painter: &egui::Painter, path: &Path, center: Vec2) {
        for contour in &path.contours {
            for pp in contour.points.iter() {
                let world_pt = Pos2::new(pp.point.x as f32, pp.point.y as f32);
                let screen_pt = self.pan_zoom.world_to_screen(world_pt, center);

                // Draw point marker
                let color = match pp.point_type {
                    PointType::LineTo => theme::POINT_LINE_TO,
                    PointType::CurveTo | PointType::QuadTo => theme::POINT_CURVE_TO,
                    PointType::CurveData | PointType::QuadData => theme::POINT_CURVE_DATA,
                };
                painter.circle_filled(screen_pt, 3.0, color);
            }
        }
    }

    /// Draw point numbers using cached outlined digit textures (Houdini-style: bottom-right of point).
    /// Returns the next point index to use (for tracking across multiple paths).
    fn draw_point_numbers(&self, painter: &egui::Painter, path: &Path, center: Vec2, start_index: usize) -> usize {
        // Tight spacing between digits (characters are ~7px wide in the texture)
        let digit_spacing = 7.0;
        let mut point_index = start_index;

        for contour in &path.contours {
            for pp in contour.points.iter() {
                let world_pt = Pos2::new(pp.point.x as f32, pp.point.y as f32);
                let screen_pt = self.pan_zoom.world_to_screen(world_pt, center);

                // Position to the bottom-right of the point (like Houdini)
                let mut x = screen_pt.x + 3.0;
                let y = screen_pt.y + 2.0;

                // Draw each digit of the number
                let num_str = point_index.to_string();
                for ch in num_str.chars() {
                    if let Some(digit) = ch.to_digit(10) {
                        if let Some(texture) = self.digit_cache.get(digit as usize) {
                            let rect = Rect::from_min_size(
                                Pos2::new(x, y),
                                Vec2::new(self.digit_cache.digit_width, self.digit_cache.digit_height),
                            );
                            painter.image(
                                texture.id(),
                                rect,
                                Rect::from_min_max(Pos2::ZERO, Pos2::new(1.0, 1.0)),
                                Color32::WHITE,
                            );
                            x += digit_spacing;
                        }
                    }
                }
                point_index += 1;
            }
        }
        point_index
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
                    PointType::QuadData => {
                        // QuadData is a control point - look ahead for the full quadratic bezier
                        // Structure: QuadData (ctrl), QuadTo (end)
                        if i + 1 < contour.points.len() {
                            let ctrl = &contour.points[i];
                            let end = &contour.points[i + 1];

                            // Get start point (last point in egui_points, or first point of contour)
                            let start = egui_points.last().copied().unwrap_or(screen_pt);

                            let c = self.world_to_screen(ctrl.point, center);
                            let e = self.world_to_screen(end.point, center);

                            // Sample the quadratic bezier
                            for t in 1..=10 {
                                let t = t as f32 / 10.0;
                                let pt = quadratic_bezier(start, c, e, t);
                                egui_points.push(pt);
                            }

                            i += 2; // Skip ctrl, end
                        } else {
                            i += 1;
                        }
                    }
                    PointType::QuadTo => {
                        // Standalone QuadTo without preceding QuadData - treat as line
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
        use crate::handles::{ellipse_handles, rect_four_point_handle, Handle};

        match node_name {
            Some(name) => {
                if let Some(node) = state.library.root.child(name) {
                    let mut handle_set = HandleSet::new(name);
                    let mut use_four_point = false;

                    if let Some(ref proto) = node.prototype {
                        match proto.as_str() {
                            "corevector.ellipse" => {
                                // Read from "position" Point port (per corevector.ndbx)
                                let position = node
                                    .input("position")
                                    .and_then(|p| p.value.as_point().cloned())
                                    .unwrap_or(Point::ZERO);
                                let width = node
                                    .input("width")
                                    .and_then(|p| p.value.as_float())
                                    .unwrap_or(100.0);
                                let height = node
                                    .input("height")
                                    .and_then(|p| p.value.as_float())
                                    .unwrap_or(100.0);

                                for h in ellipse_handles(position.x, position.y, width, height) {
                                    handle_set.add(h);
                                }
                            }
                            "corevector.rect" => {
                                // Read from "position" Point port (per corevector.ndbx)
                                let position = node
                                    .input("position")
                                    .and_then(|p| p.value.as_point().cloned())
                                    .unwrap_or(Point::ZERO);
                                let width = node
                                    .input("width")
                                    .and_then(|p| p.value.as_float())
                                    .unwrap_or(100.0);
                                let height = node
                                    .input("height")
                                    .and_then(|p| p.value.as_float())
                                    .unwrap_or(100.0);

                                // Use FourPointHandle for rect nodes (only update if not dragging)
                                if self.four_point_handle.as_ref().map_or(true, |h| !h.is_dragging()) {
                                    self.four_point_handle = Some(rect_four_point_handle(name, position.x, position.y, width, height));
                                }
                                use_four_point = true;
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
                                // Read from "position" Point port (per corevector.ndbx)
                                let position = node
                                    .input("position")
                                    .and_then(|p| p.value.as_point().cloned())
                                    .unwrap_or(Point::ZERO);

                                handle_set.add(Handle::point("position", position));
                            }
                            _ => {}
                        }
                    }

                    // FourPointHandle and regular handles are mutually exclusive
                    if use_four_point {
                        self.handles = None;
                    } else {
                        self.four_point_handle = None;
                        if !handle_set.handles().is_empty() {
                            self.handles = Some(handle_set);
                        } else {
                            self.handles = None;
                        }
                    }
                } else {
                    self.handles = None;
                    self.four_point_handle = None;
                }
            }
            None => {
                self.handles = None;
                self.four_point_handle = None;
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

/// Evaluate a quadratic bezier curve at parameter t.
fn quadratic_bezier(p0: Pos2, p1: Pos2, p2: Pos2, t: f32) -> Pos2 {
    let t2 = t * t;
    let mt = 1.0 - t;
    let mt2 = mt * mt;

    Pos2::new(
        mt2 * p0.x + 2.0 * mt * t * p1.x + t2 * p2.x,
        mt2 * p0.y + 2.0 * mt * t * p1.y + t2 * p2.y,
    )
}
