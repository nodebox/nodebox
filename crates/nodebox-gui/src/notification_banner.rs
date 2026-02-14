//! Notification banner UI component.
//!
//! Renders dismissible warning/info banners below the address bar.
//! Styled with ZINC colors for an unobtrusive appearance.

use eframe::egui;
use crate::state::NotificationLevel;
use crate::theme;

/// Height of a single notification banner.
pub const BANNER_HEIGHT: f32 = 28.0;

/// Draw notification banners and return IDs of any that were dismissed.
pub fn show_notifications(
    ui: &mut egui::Ui,
    notifications: &[(u64, String, NotificationLevel)],
) -> Vec<u64> {
    let mut dismissed = Vec::new();

    for (id, message, _level) in notifications {
        let (rect, _) = ui.allocate_exact_size(
            egui::vec2(ui.available_width(), BANNER_HEIGHT),
            egui::Sense::hover(),
        );

        if !ui.is_rect_visible(rect) {
            continue;
        }

        // Zinc styling: unobtrusive, blends with the dark theme
        let bg_color = theme::ZINC_700;
        let text_color = theme::ZINC_300;
        let icon_color = theme::ZINC_400;

        // Background
        ui.painter().rect_filled(rect, 0.0, bg_color);

        // Subtle bottom separator
        ui.painter().line_segment(
            [
                egui::pos2(rect.left(), rect.bottom() - 0.5),
                egui::pos2(rect.right(), rect.bottom() - 0.5),
            ],
            egui::Stroke::new(1.0, theme::ZINC_600),
        );

        // Warning icon
        let icon_font = egui::FontId::proportional(12.0);
        let icon_galley = ui.painter().layout_no_wrap(
            "\u{26A0}".to_string(),
            icon_font,
            icon_color,
        );
        let icon_x = rect.left() + theme::PADDING;
        ui.painter().galley(
            egui::pos2(icon_x, rect.center().y - icon_galley.size().y / 2.0),
            icon_galley.clone(),
            icon_color,
        );

        // Message text
        let text_font = egui::FontId::proportional(11.0);
        let text_x = icon_x + icon_galley.size().x + theme::PADDING_SMALL;
        let max_text_width = rect.right() - text_x - 28.0; // room for dismiss button

        let galley = ui.painter().layout(
            message.clone(),
            text_font,
            text_color,
            max_text_width,
        );
        ui.painter().galley(
            egui::pos2(text_x, rect.center().y - galley.size().y / 2.0),
            galley,
            text_color,
        );

        // Dismiss button (x) on the right
        let dismiss_size = 20.0;
        let dismiss_rect = egui::Rect::from_center_size(
            egui::pos2(rect.right() - theme::PADDING - dismiss_size / 2.0, rect.center().y),
            egui::vec2(dismiss_size, dismiss_size),
        );

        let dismiss_response = ui.interact(
            dismiss_rect,
            egui::Id::new(("dismiss_notification", *id)),
            egui::Sense::click(),
        );

        let dismiss_color = if dismiss_response.hovered() {
            theme::ZINC_200
        } else {
            theme::ZINC_400
        };

        ui.painter().text(
            dismiss_rect.center(),
            egui::Align2::CENTER_CENTER,
            "\u{2715}",
            egui::FontId::proportional(12.0),
            dismiss_color,
        );

        if dismiss_response.clicked() {
            dismissed.push(*id);
        }
    }

    dismissed
}
