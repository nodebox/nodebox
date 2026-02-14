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

        // Warning icon: draw a small triangle with "!" using lines
        let icon_x = rect.left() + theme::PADDING;
        let icon_cx = icon_x + 6.0;
        let icon_cy = rect.center().y;
        let icon_stroke = egui::Stroke::new(1.5, icon_color);
        // Triangle outline
        let tri_top = egui::pos2(icon_cx, icon_cy - 5.0);
        let tri_bl = egui::pos2(icon_cx - 6.0, icon_cy + 5.0);
        let tri_br = egui::pos2(icon_cx + 6.0, icon_cy + 5.0);
        ui.painter().line_segment([tri_top, tri_bl], icon_stroke);
        ui.painter().line_segment([tri_bl, tri_br], icon_stroke);
        ui.painter().line_segment([tri_br, tri_top], icon_stroke);
        // Exclamation mark inside
        ui.painter().line_segment(
            [egui::pos2(icon_cx, icon_cy - 2.5), egui::pos2(icon_cx, icon_cy + 1.5)],
            icon_stroke,
        );
        ui.painter().circle_filled(egui::pos2(icon_cx, icon_cy + 3.5), 0.8, icon_color);
        let icon_width = 12.0 + theme::PADDING_SMALL;

        // Message text
        let text_font = egui::FontId::proportional(11.0);
        let text_x = icon_x + icon_width;
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

        // Draw X with two line segments (avoids missing Unicode glyph issues)
        let cx = dismiss_rect.center();
        let half = 4.0;
        let stroke = egui::Stroke::new(1.5, dismiss_color);
        ui.painter().line_segment(
            [egui::pos2(cx.x - half, cx.y - half), egui::pos2(cx.x + half, cx.y + half)],
            stroke,
        );
        ui.painter().line_segment(
            [egui::pos2(cx.x + half, cx.y - half), egui::pos2(cx.x - half, cx.y + half)],
            stroke,
        );

        if dismiss_response.clicked() {
            dismissed.push(*id);
        }
    }

    dismissed
}
