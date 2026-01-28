//! Canvas viewer for rendering geometry.

use eframe::egui::{self, Pos2, Rect, Stroke, Vec2};
use nodebox_core::geometry::{Color, Path, PointType};
use crate::state::AppState;

/// The canvas viewer widget.
pub struct CanvasViewer {
    /// Current zoom level (1.0 = 100%).
    zoom: f32,

    /// Current pan offset.
    pan: Vec2,

    /// Whether we're currently panning.
    _panning: bool,

    /// Last mouse position during pan.
    _last_pan_pos: Option<Pos2>,
}

impl Default for CanvasViewer {
    fn default() -> Self {
        Self::new()
    }
}

impl CanvasViewer {
    /// Create a new canvas viewer.
    pub fn new() -> Self {
        Self {
            zoom: 1.0,
            pan: Vec2::ZERO,
            _panning: false,
            _last_pan_pos: None,
        }
    }

    /// Get the current zoom level.
    pub fn zoom(&self) -> f32 {
        self.zoom
    }

    /// Zoom in by a step.
    pub fn zoom_in(&mut self) {
        self.zoom = (self.zoom * 1.25).min(10.0);
    }

    /// Zoom out by a step.
    pub fn zoom_out(&mut self) {
        self.zoom = (self.zoom / 1.25).max(0.1);
    }

    /// Fit the view to show all geometry.
    pub fn fit_to_window(&mut self) {
        self.zoom = 1.0;
        self.pan = Vec2::ZERO;
    }

    /// Show the canvas viewer.
    pub fn show(&mut self, ui: &mut egui::Ui, state: &AppState) {
        let (response, painter) =
            ui.allocate_painter(ui.available_size(), egui::Sense::click_and_drag());

        let rect = response.rect;

        // Handle zoom with scroll wheel
        if response.hovered() {
            let scroll = ui.input(|i| i.raw_scroll_delta.y);
            if scroll != 0.0 {
                let zoom_factor = 1.0 + scroll * 0.001;
                self.zoom = (self.zoom * zoom_factor).clamp(0.1, 10.0);
            }
        }

        // Handle panning with middle mouse button or right drag
        if response.dragged_by(egui::PointerButton::Middle)
            || response.dragged_by(egui::PointerButton::Secondary)
        {
            self.pan += response.drag_delta();
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

        // Calculate transform: screen = (world * zoom) + pan + center
        let center = rect.center().to_vec2();

        // Draw all geometry
        for path in &state.geometry {
            self.draw_path(&painter, path, center);
        }

        // Draw origin crosshair
        let origin = (Pos2::ZERO.to_vec2() * self.zoom + self.pan + center).to_pos2();
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

    /// Draw a background grid.
    fn draw_grid(&self, painter: &egui::Painter, rect: Rect) {
        let grid_size = 50.0 * self.zoom;
        let grid_color = egui::Color32::from_rgba_unmultiplied(200, 200, 200, 30);

        let center = rect.center().to_vec2();
        let origin = self.pan + center;

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
                let screen_pt = (world_pt.to_vec2() * self.zoom + self.pan + center).to_pos2();

                match pp.point_type {
                    PointType::LineTo => {
                        egui_points.push(screen_pt);
                        i += 1;
                    }
                    PointType::CurveTo => {
                        // For curves, we need to sample points
                        // Get the control points and end point
                        if i + 2 < contour.points.len() {
                            let ctrl1 = &contour.points[i];
                            let ctrl2 = &contour.points[i + 1];
                            let end = &contour.points[i + 2];

                            // Get last point as start
                            let start = if let Some(&last) = egui_points.last() {
                                last
                            } else {
                                screen_pt
                            };

                            let c1 = (Pos2::new(ctrl1.point.x as f32, ctrl1.point.y as f32).to_vec2()
                                * self.zoom
                                + self.pan
                                + center)
                                .to_pos2();
                            let c2 = (Pos2::new(ctrl2.point.x as f32, ctrl2.point.y as f32).to_vec2()
                                * self.zoom
                                + self.pan
                                + center)
                                .to_pos2();
                            let e = (Pos2::new(end.point.x as f32, end.point.y as f32).to_vec2()
                                * self.zoom
                                + self.pan
                                + center)
                                .to_pos2();

                            // Sample the cubic bezier
                            for t in 1..=10 {
                                let t = t as f32 / 10.0;
                                let pt = cubic_bezier(start, c1, c2, e, t);
                                egui_points.push(pt);
                            }

                            i += 3;
                        } else {
                            egui_points.push(screen_pt);
                            i += 1;
                        }
                    }
                    PointType::CurveData => {
                        // Skip curve data points, they're handled with CurveTo
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
                // Use convex polygon for simple shapes
                if egui_points.len() >= 3 {
                    // For complex paths, we'd need triangulation
                    // For now, just draw as a polygon
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
                    path.stroke_width as f32 * self.zoom,
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
