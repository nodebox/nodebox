//! Address bar with breadcrumb navigation.

use eframe::egui::{self, Sense};
use crate::theme;

/// The address bar showing current network path.
pub struct AddressBar {
    /// Path segments (e.g., ["root", "network1"]).
    segments: Vec<String>,
    /// Status message displayed on the right.
    message: String,
    /// Hovered segment index (for highlighting).
    hovered_segment: Option<usize>,
}

impl Default for AddressBar {
    fn default() -> Self {
        Self::new()
    }
}

impl AddressBar {
    /// Create a new address bar.
    pub fn new() -> Self {
        Self {
            segments: vec!["root".to_string()],
            message: String::new(),
            hovered_segment: None,
        }
    }

    /// Set the current path from a path string (e.g., "/root/network1").
    pub fn set_path(&mut self, path: &str) {
        self.segments = path
            .trim_matches('/')
            .split('/')
            .filter(|s| !s.is_empty())
            .map(String::from)
            .collect();
        if self.segments.is_empty() {
            self.segments.push("root".to_string());
        }
    }

    /// Set the status message.
    pub fn set_message(&mut self, message: impl Into<String>) {
        self.message = message.into();
    }

    /// Clear the status message.
    pub fn clear_message(&mut self) {
        self.message.clear();
    }

    /// Get the current path as a string.
    pub fn path(&self) -> String {
        format!("/{}", self.segments.join("/"))
    }

    /// Show the address bar. Returns the clicked path if a segment was clicked.
    pub fn show(&mut self, ui: &mut egui::Ui) -> Option<String> {
        let mut clicked_path = None;
        self.hovered_segment = None;

        // Background
        let rect = ui.available_rect_before_wrap();
        ui.painter().rect_filled(rect, 0.0, theme::ADDRESS_BAR_BACKGROUND);

        ui.horizontal(|ui| {
            ui.add_space(8.0);

            // Draw path segments with separators
            for (i, segment) in self.segments.iter().enumerate() {
                // Separator (except before first segment)
                if i > 0 {
                    ui.label(
                        egui::RichText::new(" > ")
                            .color(theme::ADDRESS_SEPARATOR_COLOR)
                            .size(12.0),
                    );
                }

                // Segment as clickable text
                let response = ui.add(
                    egui::Label::new(
                        egui::RichText::new(segment)
                            .color(theme::TEXT_NORMAL)
                            .size(12.0),
                    )
                    .sense(Sense::click()),
                );

                // Hover effect
                if response.hovered() {
                    self.hovered_segment = Some(i);
                    // Draw hover background
                    ui.painter().rect_filled(
                        response.rect.expand(2.0),
                        2.0,
                        theme::ADDRESS_SEGMENT_HOVER,
                    );
                    // Redraw the text on top
                    ui.painter().text(
                        response.rect.center(),
                        egui::Align2::CENTER_CENTER,
                        segment,
                        egui::FontId::proportional(12.0),
                        theme::TEXT_BRIGHT,
                    );
                }

                // Handle click - navigate to this segment's path
                if response.clicked() {
                    let path = format!(
                        "/{}",
                        self.segments[..=i].join("/")
                    );
                    clicked_path = Some(path);
                }
            }

            // Right-aligned status message
            ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                ui.add_space(8.0);
                if !self.message.is_empty() {
                    ui.label(
                        egui::RichText::new(&self.message)
                            .color(theme::TEXT_DISABLED)
                            .size(11.0),
                    );
                }
            });
        });

        clicked_path
    }
}
