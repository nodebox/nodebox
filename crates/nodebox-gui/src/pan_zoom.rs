//! Shared pan and zoom utilities for canvas-like views.

use eframe::egui::{self, Pos2, Rect, Vec2};

/// Pan and zoom state for a canvas view.
#[derive(Clone, Debug)]
pub struct PanZoom {
    /// Current zoom level.
    pub zoom: f32,
    /// Current pan offset.
    pub pan: Vec2,
    /// Minimum zoom level.
    pub min_zoom: f32,
    /// Maximum zoom level.
    pub max_zoom: f32,
}

impl Default for PanZoom {
    fn default() -> Self {
        Self::new()
    }
}

impl PanZoom {
    /// Create a new pan/zoom state with default values.
    pub fn new() -> Self {
        Self {
            zoom: 1.0,
            pan: Vec2::ZERO,
            min_zoom: 0.1,
            max_zoom: 10.0,
        }
    }

    /// Create with custom zoom limits.
    pub fn with_zoom_limits(min_zoom: f32, max_zoom: f32) -> Self {
        Self {
            zoom: 1.0,
            pan: Vec2::ZERO,
            min_zoom,
            max_zoom,
        }
    }

    /// Handle scroll wheel zoom centered on mouse position.
    ///
    /// - `rect`: The view rectangle in screen coordinates
    /// - `ui`: The egui UI context
    /// - `origin`: Additional offset for the coordinate system origin (e.g., rect center for centered views)
    ///
    /// Returns true if zoom changed.
    pub fn handle_scroll_zoom(&mut self, rect: Rect, ui: &egui::Ui, origin: Vec2) -> bool {
        if let Some(mouse_pos) = ui.input(|i| i.pointer.hover_pos()) {
            if rect.contains(mouse_pos) {
                let scroll = ui.input(|i| i.raw_scroll_delta.y);
                if scroll != 0.0 {
                    let zoom_factor = 1.0 + scroll * 0.001;
                    let new_zoom = (self.zoom * zoom_factor).clamp(self.min_zoom, self.max_zoom);

                    if new_zoom != self.zoom {
                        // Calculate the world position under the mouse before zoom
                        let offset = origin + self.pan;
                        let world_pos = (mouse_pos.to_vec2() - offset) / self.zoom;

                        // Adjust pan so the same world position stays under the mouse after zoom
                        let new_offset = mouse_pos.to_vec2() - world_pos * new_zoom;
                        self.pan = new_offset - origin;

                        self.zoom = new_zoom;
                        return true;
                    }
                }
            }
        }
        false
    }

    /// Handle panning via drag.
    ///
    /// Returns true if pan changed.
    pub fn handle_drag_pan(&mut self, response: &egui::Response, button: egui::PointerButton) -> bool {
        if response.dragged_by(button) {
            self.pan += response.drag_delta();
            return true;
        }
        false
    }

    /// Convert a world position to screen position.
    pub fn world_to_screen(&self, world: Pos2, origin: Vec2) -> Pos2 {
        (world.to_vec2() * self.zoom + self.pan + origin).to_pos2()
    }

    /// Convert a screen position to world position.
    pub fn screen_to_world(&self, screen: Pos2, origin: Vec2) -> Pos2 {
        ((screen.to_vec2() - self.pan - origin) / self.zoom).to_pos2()
    }

    /// Zoom in by a fixed step.
    pub fn zoom_in(&mut self) {
        self.zoom = (self.zoom * 1.25).min(self.max_zoom);
    }

    /// Zoom out by a fixed step.
    pub fn zoom_out(&mut self) {
        self.zoom = (self.zoom / 1.25).max(self.min_zoom);
    }

    /// Reset to default zoom and pan.
    pub fn reset(&mut self) {
        self.zoom = 1.0;
        self.pan = Vec2::ZERO;
    }
}
