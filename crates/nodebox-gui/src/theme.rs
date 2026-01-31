//! Centralized theme constants inspired by Rerun's refined design system.
//!
//! Design tokens are organized by:
//! - Gray scale (dark theme optimized)
//! - Semantic colors (panel backgrounds, text, etc.)
//! - Spacing and layout constants
//! - Typography settings

use eframe::egui::{self, Color32, FontId, Rounding, Stroke, Style, Visuals};

// =============================================================================
// GRAY SCALE (Dark Theme)
// =============================================================================
// 21-stop gray scale from pure black to pure white

pub const GRAY_0: Color32 = Color32::from_rgb(0, 0, 0);
pub const GRAY_100: Color32 = Color32::from_rgb(13, 16, 17);
pub const GRAY_150: Color32 = Color32::from_rgb(20, 24, 25);
pub const GRAY_200: Color32 = Color32::from_rgb(28, 33, 35);
pub const GRAY_250: Color32 = Color32::from_rgb(38, 43, 46);
pub const GRAY_300: Color32 = Color32::from_rgb(49, 56, 59);
pub const GRAY_325: Color32 = Color32::from_rgb(55, 63, 66);
pub const GRAY_400: Color32 = Color32::from_rgb(76, 86, 90);
pub const GRAY_550: Color32 = Color32::from_rgb(125, 140, 146);
pub const GRAY_700: Color32 = Color32::from_rgb(174, 194, 202);
pub const GRAY_775: Color32 = Color32::from_rgb(202, 216, 222);
pub const GRAY_800: Color32 = Color32::from_rgb(211, 222, 227);
pub const GRAY_1000: Color32 = Color32::from_rgb(255, 255, 255);

// =============================================================================
// ACCENT COLORS
// =============================================================================

// Selection blue (primary accent)
pub const BLUE_350: Color32 = Color32::from_rgb(24, 73, 187);
pub const BLUE_400: Color32 = Color32::from_rgb(51, 102, 255);
pub const BLUE_500: Color32 = Color32::from_rgb(68, 138, 255);

// Status colors
pub const SUCCESS_GREEN: Color32 = Color32::from_rgb(0, 218, 126);
pub const WARNING_ORANGE: Color32 = Color32::from_rgb(255, 122, 12);
pub const ERROR_RED: Color32 = Color32::from_rgb(171, 1, 22);

// =============================================================================
// SEMANTIC COLORS - Panel & Background
// =============================================================================

/// Main panel background (dark)
pub const PANEL_BG: Color32 = GRAY_100;
/// Top bar / title bar background
pub const TOP_BAR_BG: Color32 = GRAY_100;
/// Tab bar background
pub const TAB_BAR_BG: Color32 = GRAY_200;
/// Bottom bar / footer background
pub const BOTTOM_BAR_BG: Color32 = GRAY_150;
/// Text edit / input field background
pub const TEXT_EDIT_BG: Color32 = GRAY_200;
/// Hover state background
pub const HOVER_BG: Color32 = GRAY_325;
/// Selection background
pub const SELECTION_BG: Color32 = BLUE_350;

// =============================================================================
// SEMANTIC COLORS - Text
// =============================================================================

/// Strong/active text (brightest)
pub const TEXT_STRONG: Color32 = GRAY_1000;
/// Default body text
pub const TEXT_DEFAULT: Color32 = GRAY_775;
/// Secondary/muted text
pub const TEXT_SUBDUED: Color32 = GRAY_550;
/// Disabled/non-interactive text
pub const TEXT_DISABLED: Color32 = GRAY_400;

// =============================================================================
// SEMANTIC COLORS - Widgets & Borders
// =============================================================================

/// Widget inactive background
pub const WIDGET_INACTIVE_BG: Color32 = GRAY_300;
/// Widget hovered background
pub const WIDGET_HOVERED_BG: Color32 = GRAY_325;
/// Widget active/pressed background
pub const WIDGET_ACTIVE_BG: Color32 = GRAY_325;
/// Non-interactive widget background
pub const WIDGET_NONINTERACTIVE_BG: Color32 = GRAY_150;
/// Border color
pub const BORDER_COLOR: Color32 = GRAY_250;
/// Secondary border color
pub const BORDER_SECONDARY: Color32 = GRAY_400;

// =============================================================================
// LAYOUT CONSTANTS - Heights
// =============================================================================

/// Top bar / title bar height
pub const TOP_BAR_HEIGHT: f32 = 28.0;
/// Standard title bar height
pub const TITLE_BAR_HEIGHT: f32 = 24.0;
/// List item height
pub const LIST_ITEM_HEIGHT: f32 = 24.0;
/// Table header height
pub const TABLE_HEADER_HEIGHT: f32 = 32.0;
/// Standard row height
pub const ROW_HEIGHT: f32 = 22.0;

// Legacy constants (for compatibility)
pub const ADDRESS_BAR_HEIGHT: f32 = TOP_BAR_HEIGHT;
pub const ANIMATION_BAR_HEIGHT: f32 = 27.0;
pub const PANE_HEADER_HEIGHT: f32 = TITLE_BAR_HEIGHT;
pub const LABEL_WIDTH: f32 = 100.0;
pub const PARAMETER_PANEL_WIDTH: f32 = 280.0;
pub const PARAMETER_ROW_HEIGHT: f32 = ROW_HEIGHT;

// =============================================================================
// LAYOUT CONSTANTS - Spacing (8px grid)
// =============================================================================

/// Standard padding
pub const PADDING: f32 = 8.0;
/// Small padding
pub const PADDING_SMALL: f32 = 4.0;
/// Large padding
pub const PADDING_LARGE: f32 = 12.0;
/// View/panel padding
pub const VIEW_PADDING: f32 = 12.0;
/// Item spacing
pub const ITEM_SPACING: f32 = 8.0;
/// Menu item spacing
pub const MENU_SPACING: f32 = 1.0;
/// Indent for hierarchical items
pub const INDENT: f32 = 14.0;
/// Icon to text padding
pub const ICON_TEXT_PADDING: f32 = 4.0;

// =============================================================================
// LAYOUT CONSTANTS - Sizing
// =============================================================================

/// Standard corner radius
pub const CORNER_RADIUS: f32 = 6.0;
/// Small corner radius (widgets)
pub const CORNER_RADIUS_SMALL: f32 = 4.0;
/// Large button size
pub const BUTTON_SIZE_LARGE: f32 = 22.0;
/// Button icon size
pub const BUTTON_ICON_SIZE: f32 = 12.0;
/// Small icon size
pub const ICON_SIZE_SMALL: f32 = 14.0;
/// Scroll bar width
pub const SCROLL_BAR_WIDTH: f32 = 6.0;

// =============================================================================
// TYPOGRAPHY
// =============================================================================

/// Base font size (12px is industry standard for dense UIs)
pub const FONT_SIZE_BASE: f32 = 12.0;
/// Small font size
pub const FONT_SIZE_SMALL: f32 = 11.0;
/// Large/heading font size
pub const FONT_SIZE_HEADING: f32 = 16.0;
/// Line height ratio
pub const LINE_HEIGHT_RATIO: f32 = 1.333;

// =============================================================================
// LEGACY CONSTANTS (for backward compatibility)
// =============================================================================

// Parameter panel value colors
pub const VALUE_TEXT: Color32 = BLUE_500;
pub const VALUE_TEXT_HOVER: Color32 = Color32::from_rgb(168, 200, 255);

// Background colors
pub const BACKGROUND_COLOR: Color32 = GRAY_200;
pub const HEADER_BACKGROUND: Color32 = GRAY_250;
pub const DARK_BACKGROUND: Color32 = GRAY_150;

// Text colors
pub const TEXT_NORMAL: Color32 = TEXT_DEFAULT;
pub const TEXT_BRIGHT: Color32 = TEXT_STRONG;

// Port/parameter colors
pub const PORT_LABEL_BACKGROUND: Color32 = GRAY_300;
pub const PORT_VALUE_BACKGROUND: Color32 = GRAY_200;

// Tab colors
pub const SELECTED_TAB_BACKGROUND: Color32 = GRAY_150;
pub const UNSELECTED_TAB_BACKGROUND: Color32 = GRAY_250;

// Address bar colors
pub const ADDRESS_BAR_BACKGROUND: Color32 = GRAY_200;
pub const ADDRESS_SEGMENT_HOVER: Color32 = GRAY_325;
pub const ADDRESS_SEPARATOR_COLOR: Color32 = GRAY_400;

// Animation bar colors
pub const ANIMATION_BAR_BACKGROUND: Color32 = GRAY_150;

// Network view colors
pub const NETWORK_BACKGROUND: Color32 = GRAY_200;
pub const NETWORK_GRID: Color32 = GRAY_250;

// Port type colors (semantic colors for data types)
pub const PORT_COLOR_INT: Color32 = Color32::from_rgb(116, 119, 121);
pub const PORT_COLOR_FLOAT: Color32 = Color32::from_rgb(116, 119, 121);
pub const PORT_COLOR_STRING: Color32 = Color32::from_rgb(92, 90, 91);
pub const PORT_COLOR_BOOLEAN: Color32 = Color32::from_rgb(92, 90, 91);
pub const PORT_COLOR_POINT: Color32 = Color32::from_rgb(119, 154, 173);
pub const PORT_COLOR_COLOR: Color32 = Color32::from_rgb(94, 85, 112);
pub const PORT_COLOR_GEOMETRY: Color32 = Color32::from_rgb(20, 20, 20);
pub const PORT_COLOR_LIST: Color32 = Color32::from_rgb(76, 137, 174);
pub const PORT_COLOR_DATA: Color32 = Color32::from_rgb(52, 85, 129);

// Node selection dialog colors
pub const DIALOG_BACKGROUND: Color32 = GRAY_200;
pub const DIALOG_BORDER: Color32 = GRAY_100;
pub const SELECTED_ITEM: Color32 = SELECTION_BG;
pub const HOVERED_ITEM: Color32 = GRAY_250;

// Button colors
pub const BUTTON_NORMAL: Color32 = WIDGET_INACTIVE_BG;
pub const BUTTON_HOVER: Color32 = WIDGET_HOVERED_BG;
pub const BUTTON_ACTIVE: Color32 = WIDGET_ACTIVE_BG;

// =============================================================================
// STYLE CONFIGURATION
// =============================================================================

/// Configure egui's global style and visuals for NodeBox's dark theme.
pub fn configure_style(ctx: &egui::Context) {
    let mut style = Style::default();
    let mut visuals = Visuals::dark();

    // Text styles
    style.text_styles.insert(
        egui::TextStyle::Body,
        FontId::proportional(FONT_SIZE_BASE),
    );
    style.text_styles.insert(
        egui::TextStyle::Small,
        FontId::proportional(FONT_SIZE_SMALL),
    );
    style.text_styles.insert(
        egui::TextStyle::Heading,
        FontId::proportional(FONT_SIZE_HEADING),
    );
    style.text_styles.insert(
        egui::TextStyle::Button,
        FontId::proportional(FONT_SIZE_BASE),
    );
    style.text_styles.insert(
        egui::TextStyle::Monospace,
        FontId::monospace(FONT_SIZE_BASE),
    );

    // Spacing
    style.spacing.item_spacing = egui::vec2(ITEM_SPACING, ITEM_SPACING);
    style.spacing.button_padding = egui::vec2(PADDING_LARGE, PADDING);
    style.spacing.menu_margin = egui::Margin::same(MENU_SPACING);
    style.spacing.indent = INDENT;
    style.spacing.scroll = egui::style::ScrollStyle {
        bar_width: SCROLL_BAR_WIDTH,
        bar_inner_margin: 2.0,
        bar_outer_margin: 2.0,
        ..Default::default()
    };

    // Visuals - Window
    visuals.window_fill = PANEL_BG;
    visuals.window_stroke = Stroke::new(1.0, BORDER_COLOR);
    visuals.window_rounding = Rounding::same(CORNER_RADIUS);

    // Visuals - Panel
    visuals.panel_fill = PANEL_BG;
    visuals.faint_bg_color = GRAY_150;
    visuals.extreme_bg_color = GRAY_100;

    // Visuals - Widgets
    visuals.widgets.noninteractive.bg_fill = WIDGET_NONINTERACTIVE_BG;
    visuals.widgets.noninteractive.fg_stroke = Stroke::new(1.0, TEXT_SUBDUED);
    visuals.widgets.noninteractive.rounding = Rounding::same(CORNER_RADIUS_SMALL);

    visuals.widgets.inactive.bg_fill = WIDGET_INACTIVE_BG;
    visuals.widgets.inactive.fg_stroke = Stroke::new(1.0, TEXT_DEFAULT);
    visuals.widgets.inactive.rounding = Rounding::same(CORNER_RADIUS_SMALL);

    visuals.widgets.hovered.bg_fill = WIDGET_HOVERED_BG;
    visuals.widgets.hovered.fg_stroke = Stroke::new(1.0, TEXT_STRONG);
    visuals.widgets.hovered.rounding = Rounding::same(CORNER_RADIUS_SMALL);
    visuals.widgets.hovered.expansion = 2.0;

    visuals.widgets.active.bg_fill = WIDGET_ACTIVE_BG;
    visuals.widgets.active.fg_stroke = Stroke::new(1.0, TEXT_STRONG);
    visuals.widgets.active.rounding = Rounding::same(CORNER_RADIUS_SMALL);
    visuals.widgets.active.expansion = 2.0;

    visuals.widgets.open.bg_fill = WIDGET_ACTIVE_BG;
    visuals.widgets.open.fg_stroke = Stroke::new(1.0, TEXT_STRONG);
    visuals.widgets.open.rounding = Rounding::same(CORNER_RADIUS_SMALL);

    // Selection
    visuals.selection.bg_fill = SELECTION_BG;
    visuals.selection.stroke = Stroke::new(2.0, BLUE_400);

    // Separators
    visuals.widgets.noninteractive.bg_stroke = Stroke::new(1.0, BORDER_COLOR);

    // Hyperlinks
    visuals.hyperlink_color = BLUE_500;

    // Apply styles
    style.visuals = visuals;
    ctx.set_style(style);
}
