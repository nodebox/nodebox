//! Address bar with breadcrumb navigation and stop button.

#![allow(dead_code)]

use eframe::egui::{self, Sense, Color32};
use crate::theme;

/// Time threshold in seconds before the stop button becomes prominent.
const STOP_BUTTON_HIGHLIGHT_THRESHOLD_SECS: f32 = 3.0;

/// Action returned from the address bar.
#[derive(Debug, Clone, PartialEq)]
pub enum AddressBarAction {
    /// No action taken.
    None,
    /// User clicked on a path segment; navigate to it.
    NavigateTo(String),
    /// User clicked the stop button to cancel rendering.
    StopClicked,
}

/// The address bar showing current network path.
pub struct AddressBar {
    /// Path segments (e.g., ["root", "network1"]).
    segments: Vec<String>,
    /// Hovered segment index (for highlighting).
    hovered_segment: Option<usize>,
    /// Whether rendering is currently in progress.
    is_rendering: bool,
    /// How long the current render has been running (in seconds).
    render_elapsed_secs: f32,
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
            hovered_segment: None,
            is_rendering: false,
            render_elapsed_secs: 0.0,
        }
    }

    /// Update the rendering state for the stop button.
    pub fn set_render_state(&mut self, is_rendering: bool, elapsed_secs: f32) {
        self.is_rendering = is_rendering;
        self.render_elapsed_secs = elapsed_secs;
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

    /// Get the current path as a string.
    pub fn path(&self) -> String {
        format!("/{}", self.segments.join("/"))
    }

    /// Show the address bar. Returns an action if user interacted with it.
    pub fn show(&mut self, ui: &mut egui::Ui) -> AddressBarAction {
        let mut action = AddressBarAction::None;
        self.hovered_segment = None;

        // Clean background - uses panel bg for seamless integration
        let rect = ui.available_rect_before_wrap();
        ui.painter().rect_filled(rect, 0.0, theme::PANEL_BG);

        // Use centered layout to vertically center all content
        ui.with_layout(egui::Layout::left_to_right(egui::Align::Center), |ui| {
            ui.add_space(theme::PADDING);

            // Draw path segments with separators - smaller, more subtle
            for (i, segment) in self.segments.iter().enumerate() {
                // Separator (except before first segment)
                if i > 0 {
                    ui.label(
                        egui::RichText::new("/")
                            .color(theme::TEXT_DISABLED)
                            .size(11.0),
                    );
                }

                // Segment as clickable text - subtle styling
                let is_last = i == self.segments.len() - 1;
                let text_color = if is_last {
                    theme::TEXT_DEFAULT
                } else {
                    theme::TEXT_SUBDUED
                };

                let response = ui.add(
                    egui::Label::new(
                        egui::RichText::new(segment)
                            .color(text_color)
                            .size(11.0),
                    )
                    .sense(Sense::click()),
                );

                // Subtle hover effect
                if response.hovered() {
                    self.hovered_segment = Some(i);
                    ui.ctx().set_cursor_icon(egui::CursorIcon::PointingHand);
                }

                // Handle click - navigate to this segment's path
                if response.clicked() {
                    let path = format!(
                        "/{}",
                        self.segments[..=i].join("/")
                    );
                    action = AddressBarAction::NavigateTo(path);
                }
            }

            // Right-aligned stop button
            ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                ui.add_space(theme::PADDING);

                // Stop button
                if self.draw_stop_button(ui) {
                    action = AddressBarAction::StopClicked;
                }
            });
        });

        action
    }

    /// Draw the stop button and return true if clicked.
    fn draw_stop_button(&self, ui: &mut egui::Ui) -> bool {
        // Determine button color based on rendering state
        let button_color = if !self.is_rendering {
            // Not rendering: subtle (disabled appearance)
            theme::ZINC_600
        } else if self.render_elapsed_secs < STOP_BUTTON_HIGHLIGHT_THRESHOLD_SECS {
            // Rendering but less than threshold: subtle
            theme::ZINC_600
        } else {
            // Rendering and past threshold: prominent
            theme::ZINC_200
        };

        // Draw the stop button (circle with square inside)
        let size = 16.0;
        let (rect, response) = ui.allocate_exact_size(
            egui::vec2(size, size),
            if self.is_rendering { Sense::click() } else { Sense::hover() },
        );

        if ui.is_rect_visible(rect) {
            let painter = ui.painter();
            let center = rect.center();
            let radius = size / 2.0 - 1.0;

            // Hover effect: slightly brighter when hovered and rendering
            let color = if response.hovered() && self.is_rendering {
                brighten_color(button_color, 0.2)
            } else {
                button_color
            };

            // Draw circle outline
            painter.circle_stroke(
                center,
                radius,
                egui::Stroke::new(1.5, color),
            );

            // Draw square inside (stop symbol)
            let square_size = size / 3.0;
            let square_rect = egui::Rect::from_center_size(
                center,
                egui::vec2(square_size, square_size),
            );
            painter.rect_filled(square_rect, 0.0, color);

            // Show cursor hint when clickable
            if response.hovered() && self.is_rendering {
                ui.ctx().set_cursor_icon(egui::CursorIcon::PointingHand);
            }
        }

        // Check if clicked before consuming response for tooltip
        let clicked = response.clicked() && self.is_rendering;

        // Add tooltip
        if response.hovered() {
            let tooltip_text = if self.is_rendering {
                "Stop rendering (Cmd+.)"
            } else {
                "Stop (not rendering)"
            };
            response.on_hover_text(tooltip_text);
        }

        clicked
    }
}

/// Brighten a color by a factor (0.0 = no change, 1.0 = fully white).
fn brighten_color(color: Color32, factor: f32) -> Color32 {
    let r = color.r() as f32 + (255.0 - color.r() as f32) * factor;
    let g = color.g() as f32 + (255.0 - color.g() as f32) * factor;
    let b = color.b() as f32 + (255.0 - color.b() as f32) * factor;
    Color32::from_rgb(r as u8, g as u8, b as u8)
}
