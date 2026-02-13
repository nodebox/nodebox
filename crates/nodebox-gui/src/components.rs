//! Shared UI components for consistent styling across panes.
//!
//! This module provides reusable building blocks for the NodeBox UI,
//! ensuring consistent appearance and behavior.

#![allow(dead_code)]

use eframe::egui::{self, Rect};
use crate::theme;

/// Draw a pane header with title and return position info for additional content.
///
/// This function draws:
/// - Header background
/// - UPPERCASE title text on the left
/// - Vertical separator line after the title
///
/// Returns the x position after the separator where additional content can be placed.
pub fn draw_pane_header_with_title(
    ui: &mut egui::Ui,
    title: &str,
) -> (Rect, f32) {
    let header_height = theme::PANE_HEADER_HEIGHT;
    let (header_rect, _) = ui.allocate_exact_size(
        egui::vec2(ui.available_width(), header_height),
        egui::Sense::hover(),
    );

    // Pixel-align the rect for crisp borders
    let aligned_rect = Rect::from_min_max(
        egui::pos2(header_rect.min.x.floor(), header_rect.min.y.floor()),
        egui::pos2(header_rect.max.x.ceil(), header_rect.max.y.floor()),
    );

    // Header background
    ui.painter().rect_filled(
        aligned_rect,
        0.0,
        theme::PANE_HEADER_BACKGROUND_COLOR,
    );

    // Top border (1px light line)
    ui.painter().line_segment(
        [
            egui::pos2(aligned_rect.left(), aligned_rect.top() + 0.5),
            egui::pos2(aligned_rect.right(), aligned_rect.top() + 0.5),
        ],
        egui::Stroke::new(1.0, theme::ZINC_600),
    );

    // Title on left (UPPERCASE)
    let title_font = egui::FontId::proportional(10.0);
    let title_galley = ui.painter().layout_no_wrap(
        title.to_uppercase(),
        title_font,
        theme::PANE_HEADER_FOREGROUND_COLOR,
    );
    let title_x = header_rect.left() + theme::PADDING;
    ui.painter().galley(
        egui::pos2(title_x, header_rect.center().y - title_galley.size().y / 2.0),
        title_galley.clone(),
        theme::PANE_HEADER_FOREGROUND_COLOR,
    );

    // Vertical separator line (1px) - at fixed position to align with labels column
    let sep_x = header_rect.left() + theme::LABEL_WIDTH;
    ui.painter().line_segment(
        [
            egui::pos2(sep_x, header_rect.top() + theme::PADDING_SMALL),
            egui::pos2(sep_x, header_rect.bottom() - theme::PADDING_SMALL),
        ],
        egui::Stroke::new(1.0, theme::TEXT_DISABLED),
    );

    // Bottom border (1px dark line) - painted on foreground layer so content
    // below (canvas, table headers) cannot paint over it.
    let fg_painter = ui.ctx().layer_painter(egui::LayerId::new(
        egui::Order::Foreground,
        ui.id().with("header_border"),
    ));
    fg_painter.line_segment(
        [
            egui::pos2(aligned_rect.left(), aligned_rect.bottom() - 0.5),
            egui::pos2(aligned_rect.right(), aligned_rect.bottom() - 0.5),
        ],
        egui::Stroke::new(1.0, theme::ZINC_900),
    );

    // Return header rect and x position after separator (8px margin)
    let content_start_x = sep_x + theme::PADDING;
    (header_rect, content_start_x)
}

/// Draw a simple text button in a header.
///
/// Returns true if clicked.
pub fn header_text_button(
    ui: &mut egui::Ui,
    header_rect: Rect,
    x: f32,
    text: &str,
    width: f32,
) -> (bool, f32) {
    let button_font = egui::FontId::proportional(10.0);
    let button_rect = Rect::from_min_size(
        egui::pos2(x, header_rect.top()),
        egui::vec2(width, header_rect.height()),
    );

    let response = ui.interact(
        button_rect,
        ui.id().with(text),
        egui::Sense::click(),
    );

    let color = if response.hovered() {
        theme::TEXT_STRONG
    } else {
        theme::PANE_HEADER_FOREGROUND_COLOR
    };

    ui.painter().text(
        button_rect.left_center(),
        egui::Align2::LEFT_CENTER,
        text,
        button_font,
        color,
    );

    (response.clicked(), x + width + theme::PADDING)
}

/// Draw a toggle button in a header (subtle, Figma-like styling).
///
/// Returns true if clicked.
pub fn header_toggle_button(
    ui: &mut egui::Ui,
    header_rect: Rect,
    x: f32,
    text: &str,
    active: bool,
    tooltip: &str,
) -> (bool, f32) {
    let font = egui::FontId::proportional(10.0);

    // Calculate text width for proper sizing
    let galley = ui.painter().layout_no_wrap(
        text.to_string(),
        font.clone(),
        theme::TEXT_DEFAULT,
    );
    let text_width = galley.size().x;

    let button_rect = Rect::from_min_size(
        egui::pos2(x, header_rect.top()),
        egui::vec2(text_width, header_rect.height()),
    );

    let response = ui.interact(
        button_rect,
        ui.id().with(text),
        egui::Sense::click(),
    );

    let color = if active {
        theme::TEXT_DEFAULT
    } else {
        theme::TEXT_DISABLED
    };

    ui.painter().text(
        egui::pos2(x, header_rect.center().y),
        egui::Align2::LEFT_CENTER,
        text,
        font,
        color,
    );

    if !tooltip.is_empty() {
        response.clone().on_hover_text(tooltip);
    }

    (response.clicked(), x + text_width + theme::PADDING)
}

/// Draw a tab button in a header.
///
/// Returns true if clicked.
pub fn header_tab_button(
    ui: &mut egui::Ui,
    header_rect: Rect,
    x: f32,
    text: &str,
    active: bool,
) -> (bool, f32) {
    let font = egui::FontId::proportional(11.0);

    // Calculate text width
    let galley = ui.painter().layout_no_wrap(
        text.to_string(),
        font.clone(),
        theme::TEXT_STRONG,
    );
    let text_width = galley.size().x;

    let button_rect = Rect::from_min_size(
        egui::pos2(x, header_rect.top()),
        egui::vec2(text_width, header_rect.height()),
    );

    let response = ui.interact(
        button_rect,
        ui.id().with(format!("tab_{}", text)),
        egui::Sense::click(),
    );

    let color = if active {
        theme::TEXT_STRONG
    } else {
        theme::TEXT_SUBDUED
    };

    ui.painter().text(
        egui::pos2(x, header_rect.center().y),
        egui::Align2::LEFT_CENTER,
        text,
        font,
        color,
    );

    (response.clicked(), x + text_width + theme::PADDING_LARGE)
}

/// Draw a vertical separator line in a header.
pub fn header_separator(ui: &mut egui::Ui, header_rect: Rect, x: f32) -> f32 {
    ui.painter().line_segment(
        [
            egui::pos2(x, header_rect.top() + theme::PADDING_SMALL),
            egui::pos2(x, header_rect.bottom() - theme::PADDING_SMALL),
        ],
        egui::Stroke::new(1.0, theme::TEXT_DISABLED),
    );
    x + theme::PADDING
}

/// Draw a segmented control with two options in a header.
///
/// Returns (clicked_index, new_x) where clicked_index is Some(0) or Some(1) if clicked,
/// or None if nothing was clicked.
pub fn header_segmented_control(
    ui: &mut egui::Ui,
    header_rect: Rect,
    x: f32,
    labels: [&str; 2],
    selected: usize,
    disabled_index: Option<usize>,
) -> (Option<usize>, f32) {
    let font = egui::FontId::proportional(11.0);
    let padding_h = 6.0; // Horizontal padding inside each segment
    let y_center = header_rect.center().y;

    // Calculate widths for each segment
    let galley0 = ui.painter().layout_no_wrap(
        labels[0].to_string(),
        font.clone(),
        theme::TEXT_STRONG,
    );
    let galley1 = ui.painter().layout_no_wrap(
        labels[1].to_string(),
        font.clone(),
        theme::TEXT_STRONG,
    );

    let width0 = galley0.size().x + padding_h * 2.0;
    let width1 = galley1.size().x + padding_h * 2.0;
    let total_width = width0 + width1;

    // Content area respecting 1px borders
    let content_top = header_rect.top() + 1.0;
    let content_bottom = header_rect.bottom() - 1.0;
    let content_height = content_bottom - content_top;

    // Only draw the selected segment (unselected is transparent/header bg)
    // Don't draw highlight if the selected segment is disabled
    if disabled_index != Some(selected) {
        let selected_x = if selected == 0 { x } else { x + width0 };
        let selected_width = if selected == 0 { width0 } else { width1 };
        let selected_rect = Rect::from_min_size(
            egui::pos2(selected_x, content_top),
            egui::vec2(selected_width, content_height),
        );
        ui.painter().rect_filled(
            selected_rect,
            0.0,
            theme::ZINC_600,
        );
    }

    // Create interaction rects and draw labels
    let rect0 = Rect::from_min_size(
        egui::pos2(x, header_rect.top()),
        egui::vec2(width0, header_rect.height()),
    );
    let rect1 = Rect::from_min_size(
        egui::pos2(x + width0, header_rect.top()),
        egui::vec2(width1, header_rect.height()),
    );

    let response0 = ui.interact(
        rect0,
        ui.id().with(format!("seg_{}", labels[0])),
        egui::Sense::click(),
    );
    let response1 = ui.interact(
        rect1,
        ui.id().with(format!("seg_{}", labels[1])),
        egui::Sense::click(),
    );

    // Draw text labels (disabled segments use TEXT_DISABLED)
    let color0 = if disabled_index == Some(0) {
        theme::TEXT_DISABLED
    } else if selected == 0 {
        theme::TEXT_STRONG
    } else {
        theme::TEXT_SUBDUED
    };
    let color1 = if disabled_index == Some(1) {
        theme::TEXT_DISABLED
    } else if selected == 1 {
        theme::TEXT_STRONG
    } else {
        theme::TEXT_SUBDUED
    };

    ui.painter().text(
        egui::pos2(x + width0 / 2.0, y_center),
        egui::Align2::CENTER_CENTER,
        labels[0],
        font.clone(),
        color0,
    );
    ui.painter().text(
        egui::pos2(x + width0 + width1 / 2.0, y_center),
        egui::Align2::CENTER_CENTER,
        labels[1],
        font,
        color1,
    );

    let clicked = if response0.clicked() && disabled_index != Some(0) {
        Some(0)
    } else if response1.clicked() && disabled_index != Some(1) {
        Some(1)
    } else {
        None
    };

    (clicked, x + total_width)
}

/// Draw a zoom percentage control in a header (styled like animation_bar.rs DragValue).
///
/// Returns (new_zoom, new_x) where new_zoom is the updated zoom value (0.1 to 10.0),
/// or None if not changed.
pub fn header_zoom_control(
    ui: &mut egui::Ui,
    header_rect: Rect,
    x: f32,
    zoom: f32,
) -> (Option<f32>, f32) {
    let width = 48.0;

    // Convert zoom to percentage for display (e.g., 1.0 -> 100)
    let mut zoom_percent = (zoom * 100.0).round() as i32;

    // Override visuals for styled DragValue (matching animation_bar.rs)
    let old_visuals = ui.visuals().clone();
    let old_spacing = ui.spacing().clone();

    // All states: no borders, sharp corners, appropriate fill
    ui.visuals_mut().widgets.inactive.bg_fill = theme::ZINC_700;
    ui.visuals_mut().widgets.inactive.weak_bg_fill = theme::ZINC_700;
    ui.visuals_mut().widgets.inactive.bg_stroke = egui::Stroke::NONE;
    ui.visuals_mut().widgets.inactive.fg_stroke = egui::Stroke::new(1.0, theme::TEXT_DEFAULT);
    ui.visuals_mut().widgets.inactive.corner_radius = egui::CornerRadius::ZERO;
    ui.visuals_mut().widgets.inactive.expansion = 0.0;

    ui.visuals_mut().widgets.hovered.bg_fill = theme::ZINC_600;
    ui.visuals_mut().widgets.hovered.weak_bg_fill = theme::ZINC_600;
    ui.visuals_mut().widgets.hovered.bg_stroke = egui::Stroke::NONE;
    ui.visuals_mut().widgets.hovered.fg_stroke = egui::Stroke::new(1.0, theme::TEXT_STRONG);
    ui.visuals_mut().widgets.hovered.corner_radius = egui::CornerRadius::ZERO;
    ui.visuals_mut().widgets.hovered.expansion = 0.0;

    ui.visuals_mut().widgets.active.bg_fill = theme::ZINC_600;
    ui.visuals_mut().widgets.active.weak_bg_fill = theme::ZINC_600;
    ui.visuals_mut().widgets.active.bg_stroke = egui::Stroke::NONE;
    ui.visuals_mut().widgets.active.fg_stroke = egui::Stroke::new(1.0, theme::TEXT_STRONG);
    ui.visuals_mut().widgets.active.corner_radius = egui::CornerRadius::ZERO;
    ui.visuals_mut().widgets.active.expansion = 0.0;

    ui.visuals_mut().widgets.noninteractive.bg_fill = theme::ZINC_700;
    ui.visuals_mut().widgets.noninteractive.weak_bg_fill = theme::ZINC_700;
    ui.visuals_mut().widgets.noninteractive.bg_stroke = egui::Stroke::NONE;
    ui.visuals_mut().widgets.noninteractive.corner_radius = egui::CornerRadius::ZERO;
    ui.visuals_mut().widgets.noninteractive.expansion = 0.0;

    // Use consistent padding
    ui.spacing_mut().button_padding = egui::vec2(4.0, 2.0);

    // Allocate exact size rect within header, respecting 1px top/bottom borders
    let content_top = header_rect.top() + 1.0;
    let content_bottom = header_rect.bottom() - 1.0;
    let content_height = content_bottom - content_top;
    let rect = Rect::from_min_size(
        egui::pos2(x, content_top),
        egui::vec2(width, content_height),
    );

    let old_zoom_percent = zoom_percent;
    let response = ui.put(
        rect,
        egui::DragValue::new(&mut zoom_percent)
            .range(10..=1000)
            .speed(1.0)
            .suffix("%"),
    );

    // Restore visuals and spacing
    *ui.visuals_mut() = old_visuals;
    *ui.spacing_mut() = old_spacing;

    // Check if value changed
    let new_zoom = if zoom_percent != old_zoom_percent || response.lost_focus() {
        Some(zoom_percent as f32 / 100.0)
    } else {
        None
    };

    (new_zoom, x + width)
}

/// Draw a small icon button in a header (for zoom +/- buttons).
///
/// Returns true if clicked.
pub fn header_icon_button(
    ui: &mut egui::Ui,
    header_rect: Rect,
    x: f32,
    icon: &str,
) -> (bool, f32) {
    let width = 24.0;
    let button_rect = Rect::from_min_size(
        egui::pos2(x, header_rect.top()),
        egui::vec2(width, header_rect.height()),
    );

    let response = ui.interact(
        button_rect,
        ui.id().with(format!("icon_{}", icon)),
        egui::Sense::click(),
    );

    // Draw hover highlight
    if response.hovered() {
        ui.painter().rect_filled(
            button_rect,
            0.0,
            theme::ZINC_600,
        );
    }

    // Draw icon text centered
    let font = egui::FontId::proportional(14.0);
    let color = if response.hovered() {
        theme::TEXT_STRONG
    } else {
        theme::TEXT_DEFAULT
    };

    ui.painter().text(
        button_rect.center(),
        egui::Align2::CENTER_CENTER,
        icon,
        font,
        color,
    );

    (response.clicked(), x + width)
}
