//! Application state management.

use std::path::{Path, PathBuf};
use nodebox_core::geometry::{Path as GeoPath, Color, Point};
use nodebox_ops;
use nodebox_svg::render_to_svg;

/// The main application state.
pub struct AppState {
    /// Current file path (if saved).
    pub current_file: Option<PathBuf>,

    /// Whether the document has unsaved changes.
    pub dirty: bool,

    /// Whether to show the about dialog.
    pub show_about: bool,

    /// The current geometry to render.
    pub geometry: Vec<GeoPath>,

    /// Currently selected node (if any).
    pub selected_node: Option<String>,

    /// Canvas background color.
    pub background_color: Color,
}

impl Default for AppState {
    fn default() -> Self {
        Self::new()
    }
}

impl AppState {
    /// Create a new application state with demo content.
    pub fn new() -> Self {
        // Create some demo geometry
        let mut geometry = Vec::new();

        // Add some demo shapes
        let mut circle = nodebox_ops::ellipse(Point::new(200.0, 200.0), 100.0, 100.0);
        circle.fill = Some(Color::rgb(0.9, 0.2, 0.2));
        geometry.push(circle);

        let mut rect = nodebox_ops::rect(Point::new(350.0, 200.0), 80.0, 80.0, Point::ZERO);
        rect.fill = Some(Color::rgb(0.2, 0.8, 0.3));
        geometry.push(rect);

        let mut star = nodebox_ops::star(Point::new(500.0, 200.0), 5, 50.0, 25.0);
        star.fill = Some(Color::rgb(0.2, 0.4, 0.9));
        geometry.push(star);

        let mut hex = nodebox_ops::polygon(Point::new(200.0, 350.0), 45.0, 6, true);
        hex.fill = Some(Color::rgb(0.8, 0.5, 0.2));
        geometry.push(hex);

        // Add a spiral of circles
        for i in 0..12 {
            let angle = i as f64 * 30.0 * std::f64::consts::PI / 180.0;
            let radius = 80.0 + i as f64 * 10.0;
            let x = 400.0 + radius * angle.cos();
            let y = 400.0 + radius * angle.sin();

            let hue = i as f64 / 12.0;
            let color = hsb_to_rgb(hue, 0.8, 0.9);

            let mut dot = GeoPath::ellipse(x, y, 15.0, 15.0);
            dot.fill = Some(color);
            geometry.push(dot);
        }

        Self {
            current_file: None,
            dirty: false,
            show_about: false,
            geometry,
            selected_node: None,
            background_color: Color::WHITE,
        }
    }

    /// Create a new empty document.
    pub fn new_document(&mut self) {
        self.current_file = None;
        self.dirty = false;
        self.geometry.clear();
        self.selected_node = None;
    }

    /// Load a file.
    pub fn load_file(&mut self, path: &Path) -> Result<(), String> {
        // For now, just update the path
        // TODO: Actually parse the .ndbx file and load nodes
        self.current_file = Some(path.to_path_buf());
        self.dirty = false;
        Ok(())
    }

    /// Save the current document.
    pub fn save_file(&mut self, path: &Path) -> Result<(), String> {
        // TODO: Implement proper .ndbx saving
        self.current_file = Some(path.to_path_buf());
        self.dirty = false;
        Ok(())
    }

    /// Export to SVG.
    pub fn export_svg(&self, path: &Path) -> Result<(), String> {
        // Calculate bounds
        let mut min_x = f64::MAX;
        let mut min_y = f64::MAX;
        let mut max_x = f64::MIN;
        let mut max_y = f64::MIN;

        for geo in &self.geometry {
            if let Some(bounds) = geo.bounds() {
                min_x = min_x.min(bounds.x);
                min_y = min_y.min(bounds.y);
                max_x = max_x.max(bounds.x + bounds.width);
                max_y = max_y.max(bounds.y + bounds.height);
            }
        }

        let width = (max_x - min_x + 40.0).max(100.0);
        let height = (max_y - min_y + 40.0).max(100.0);

        let svg = render_to_svg(&self.geometry, width, height);
        std::fs::write(path, svg).map_err(|e| e.to_string())
    }
}

/// Convert HSB to RGB color.
fn hsb_to_rgb(h: f64, s: f64, b: f64) -> Color {
    let h = h * 6.0;
    let i = h.floor() as i32;
    let f = h - i as f64;
    let p = b * (1.0 - s);
    let q = b * (1.0 - s * f);
    let t = b * (1.0 - s * (1.0 - f));

    let (r, g, b) = match i % 6 {
        0 => (b, t, p),
        1 => (q, b, p),
        2 => (p, b, t),
        3 => (p, q, b),
        4 => (t, p, b),
        _ => (b, p, q),
    };

    Color::rgb(r, g, b)
}
