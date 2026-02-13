//! Centralized theme constants based on Tailwind's Zinc color palette.
//!
//! Design principles:
//! - Neutral gray tones with subtle blue-violet undertone from Tailwind's Zinc palette
//! - Purple/violet accent color for selections and highlights
//! - Sharp corners (0px) for a modern, precise feel
//! - Minimal borders - use background color differentiation instead
//! - Good contrast for readability
//!
//! Note: Not all tokens are used yet - they are defined for the complete design system.

#![allow(dead_code)]

use eframe::egui::{self, Color32, CornerRadius, FontId, Stroke, Style, Visuals};

// =============================================================================
// ZINC SCALE (Tailwind v4 Zinc Palette)
// =============================================================================
// Neutral gray tones with a subtle blue-violet undertone

/// Lightest - near white
pub const ZINC_50: Color32 = Color32::from_rgb(250, 250, 250);
/// Very light
pub const ZINC_100: Color32 = Color32::from_rgb(244, 244, 245);
/// Light
pub const ZINC_200: Color32 = Color32::from_rgb(228, 228, 231);
/// Light-medium
pub const ZINC_300: Color32 = Color32::from_rgb(212, 212, 216);
/// Medium - good for muted text
pub const ZINC_400: Color32 = Color32::from_rgb(159, 159, 169);
/// Medium-dark - node fills, secondary elements
pub const ZINC_500: Color32 = Color32::from_rgb(113, 113, 123);
/// Dark - node fills, interactive elements
pub const ZINC_600: Color32 = Color32::from_rgb(82, 82, 92);
/// Darker - elevated surfaces
pub const ZINC_700: Color32 = Color32::from_rgb(63, 63, 70);
/// Very dark - panel backgrounds
pub const ZINC_800: Color32 = Color32::from_rgb(39, 39, 42);
/// Near black - main backgrounds
pub const ZINC_900: Color32 = Color32::from_rgb(24, 24, 27);
/// Deepest - true dark background
pub const ZINC_950: Color32 = Color32::from_rgb(9, 9, 11);

// =============================================================================
// LEGACY GRAY ALIASES (map to Zinc for backward compatibility)
// =============================================================================

pub const GRAY_0: Color32 = Color32::from_rgb(0, 0, 0);
pub const GRAY_50: Color32 = ZINC_900;
pub const GRAY_100: Color32 = ZINC_800;
pub const GRAY_150: Color32 = ZINC_700;
pub const GRAY_200: Color32 = ZINC_600;
pub const GRAY_250: Color32 = ZINC_500;
pub const GRAY_300: Color32 = ZINC_500;
pub const GRAY_350: Color32 = ZINC_400;
pub const GRAY_400: Color32 = ZINC_400;
pub const GRAY_500: Color32 = ZINC_300;
pub const GRAY_600: Color32 = ZINC_200;
pub const GRAY_700: Color32 = ZINC_100;
pub const GRAY_800: Color32 = ZINC_50;
pub const GRAY_900: Color32 = ZINC_50;
pub const GRAY_1000: Color32 = Color32::from_rgb(255, 255, 255);

pub const GRAY_325: Color32 = ZINC_400;
pub const GRAY_550: Color32 = ZINC_300;
pub const GRAY_775: Color32 = ZINC_50;

// =============================================================================
// ACCENT COLORS (Purple/Violet - Linear-inspired)
// =============================================================================

/// Selection background (subtle violet tint)
pub const VIOLET_900: Color32 = Color32::from_rgb(45, 38, 64);
/// Lighter selection background (for better text contrast)
pub const VIOLET_800: Color32 = Color32::from_rgb(76, 58, 118);
/// Darker pressed state
pub const VIOLET_600: Color32 = Color32::from_rgb(124, 58, 237);
/// Primary accent color
pub const VIOLET_500: Color32 = Color32::from_rgb(139, 92, 246);
/// Hover/lighter accent
pub const VIOLET_400: Color32 = Color32::from_rgb(167, 139, 250);

// Legacy blue aliases (map to violet for backwards compat)
pub const BLUE_350: Color32 = VIOLET_900;
pub const BLUE_400: Color32 = VIOLET_500;
pub const BLUE_500: Color32 = VIOLET_400;

// =============================================================================
// STATUS COLORS
// =============================================================================

pub const SUCCESS_GREEN: Color32 = Color32::from_rgb(34, 197, 94);
pub const WARNING_YELLOW: Color32 = Color32::from_rgb(234, 179, 8);
pub const ERROR_RED: Color32 = Color32::from_rgb(255, 100, 103);  // #ff6467

// Legacy alias
pub const WARNING_ORANGE: Color32 = WARNING_YELLOW;

// =============================================================================
// SEMANTIC COLORS - Panel & Background
// =============================================================================

/// Main panel background (dark)
pub const PANEL_BG: Color32 = ZINC_800;
/// Top bar / title bar background
pub const TOP_BAR_BG: Color32 = ZINC_800;
/// Tab bar background
pub const TAB_BAR_BG: Color32 = ZINC_700;
/// Bottom bar / footer background
pub const BOTTOM_BAR_BG: Color32 = ZINC_800;
/// Elevated surface (cards, dialogs, popups)
pub const SURFACE_ELEVATED: Color32 = ZINC_600;
/// Text edit / input field background
pub const TEXT_EDIT_BG: Color32 = ZINC_600;
/// Hover state background
pub const HOVER_BG: Color32 = ZINC_500;
/// Selection background (visible violet with good text contrast)
pub const SELECTION_BG: Color32 = VIOLET_800;
/// Text edit selection highlight (blue, readable with white text)
pub const TEXT_EDIT_SELECTION_BG: Color32 = Color32::from_rgb(37, 99, 175);
/// Field hover background (ZINC_700 at ~50% opacity over ZINC_600)
pub const FIELD_HOVER_BG: Color32 = Color32::from_rgb(72, 72, 81);

// =============================================================================
// SEMANTIC COLORS - Text
// =============================================================================

/// Strong/active text (brightest)
pub const TEXT_STRONG: Color32 = ZINC_50;
/// Default body text
pub const TEXT_DEFAULT: Color32 = ZINC_100;
/// Secondary/muted text
pub const TEXT_SUBDUED: Color32 = ZINC_300;
/// Disabled/non-interactive text
pub const TEXT_DISABLED: Color32 = ZINC_400;

// =============================================================================
// SEMANTIC COLORS - Widgets & Borders
// =============================================================================

/// Widget inactive background
pub const WIDGET_INACTIVE_BG: Color32 = ZINC_600;
/// Widget hovered background
pub const WIDGET_HOVERED_BG: Color32 = ZINC_500;
/// Widget active/pressed background
pub const WIDGET_ACTIVE_BG: Color32 = ZINC_300;
/// Non-interactive widget background
pub const WIDGET_NONINTERACTIVE_BG: Color32 = ZINC_700;
/// Border color (use sparingly - prefer no borders)
pub const BORDER_COLOR: Color32 = ZINC_500;
/// Secondary border color
pub const BORDER_SECONDARY: Color32 = ZINC_400;

// =============================================================================
// LAYOUT CONSTANTS - Heights
// =============================================================================

/// Top bar / title bar height
pub const TOP_BAR_HEIGHT: f32 = 28.0;
/// Standard title bar height
pub const TITLE_BAR_HEIGHT: f32 = 24.0;
/// List item height
pub const LIST_ITEM_HEIGHT: f32 = 28.0;
/// Table header height
pub const TABLE_HEADER_HEIGHT: f32 = 32.0;
/// Standard row height
pub const ROW_HEIGHT: f32 = 24.0;

// Legacy constants (for compatibility)
pub const ADDRESS_BAR_HEIGHT: f32 = TOP_BAR_HEIGHT;
pub const ANIMATION_BAR_HEIGHT: f32 = 28.0;
pub const PANE_HEADER_HEIGHT: f32 = TITLE_BAR_HEIGHT;
/// Fixed label column width - aligns with pane header separator.
/// This is the x position of the separator in headers AND the width of the labels column.
pub const LABEL_WIDTH: f32 = 112.0;

// =============================================================================
// DATA TABLE COLORS
// =============================================================================

/// Zebra stripe: even row background (matches panel bg)
pub const TABLE_ROW_EVEN: Color32 = ZINC_800;
/// Zebra stripe: odd row alternating background (between ZINC_800 and ZINC_700)
pub const TABLE_ROW_ODD: Color32 = Color32::from_rgb(51, 51, 56);
/// Table header background
pub const TABLE_HEADER_BG: Color32 = ZINC_700;
/// Table header text color
pub const TABLE_HEADER_TEXT: Color32 = ZINC_200;
/// Table cell text color
pub const TABLE_CELL_TEXT: Color32 = ZINC_100;
/// Index column text color (subdued)
pub const TABLE_INDEX_TEXT: Color32 = ZINC_300;

// =============================================================================
// PANE HEADER COLORS
// =============================================================================

/// Pane header background color (same as panel for seamless look)
pub const PANE_HEADER_BACKGROUND_COLOR: Color32 = ZINC_700;
/// Pane header foreground/text color
pub const PANE_HEADER_FOREGROUND_COLOR: Color32 = ZINC_200;
pub const PARAMETER_PANEL_WIDTH: f32 = 280.0;
pub const PARAMETER_ROW_HEIGHT: f32 = ROW_HEIGHT;

// =============================================================================
// LAYOUT CONSTANTS - Spacing (4px grid)
// =============================================================================

/// Standard padding
pub const PADDING: f32 = 8.0;
/// Small padding
pub const PADDING_SMALL: f32 = 4.0;
/// Large padding
pub const PADDING_LARGE: f32 = 12.0;
/// Extra large padding
pub const PADDING_XL: f32 = 16.0;
/// View/panel padding
pub const VIEW_PADDING: f32 = 12.0;
/// Item spacing
pub const ITEM_SPACING: f32 = 8.0;
/// Menu item spacing
pub const MENU_SPACING: f32 = 4.0;
/// Indent for hierarchical items
pub const INDENT: f32 = 16.0;
/// Icon to text padding
pub const ICON_TEXT_PADDING: f32 = 8.0;

// =============================================================================
// LAYOUT CONSTANTS - Sizing
// =============================================================================

/// Standard corner radius (sharp/square for most UI)
pub const CORNER_RADIUS: f32 = 0.0;
/// Small corner radius (subtle rounding for selections, highlighted items)
pub const CORNER_RADIUS_SMALL: f32 = 4.0;
/// Large button size
pub const BUTTON_SIZE_LARGE: f32 = 24.0;
/// Button icon size
pub const BUTTON_ICON_SIZE: f32 = 16.0;
/// Small icon size
pub const ICON_SIZE_SMALL: f32 = 16.0;
/// Scroll bar width
pub const SCROLL_BAR_WIDTH: f32 = 8.0;

// =============================================================================
// TYPOGRAPHY
// =============================================================================

/// Base font size
pub const FONT_SIZE_BASE: f32 = 13.0;
/// Small font size
pub const FONT_SIZE_SMALL: f32 = 11.0;
/// Large/heading font size
pub const FONT_SIZE_HEADING: f32 = 16.0;
/// Line height ratio
pub const LINE_HEIGHT_RATIO: f32 = 1.4;

// =============================================================================
// LEGACY CONSTANTS (for backward compatibility)
// =============================================================================

// Parameter panel value colors (now violet)
pub const VALUE_TEXT: Color32 = VIOLET_400;
pub const VALUE_TEXT_HOVER: Color32 = VIOLET_500;

// Background colors
pub const BACKGROUND_COLOR: Color32 = ZINC_700;
pub const HEADER_BACKGROUND: Color32 = ZINC_700;
pub const DARK_BACKGROUND: Color32 = ZINC_800;

// Text colors
pub const TEXT_NORMAL: Color32 = TEXT_DEFAULT;
pub const TEXT_BRIGHT: Color32 = TEXT_STRONG;

// Port/parameter colors (labels on left are darker)
pub const PORT_LABEL_BACKGROUND: Color32 = ZINC_700;
pub const PORT_VALUE_BACKGROUND: Color32 = ZINC_600;

// Tab colors
pub const SELECTED_TAB_BACKGROUND: Color32 = ZINC_600;
pub const UNSELECTED_TAB_BACKGROUND: Color32 = ZINC_700;

// Address bar colors
pub const ADDRESS_BAR_BACKGROUND: Color32 = ZINC_700;
pub const ADDRESS_SEGMENT_HOVER: Color32 = ZINC_500;
pub const ADDRESS_SEPARATOR_COLOR: Color32 = ZINC_400;

// Animation bar colors
pub const ANIMATION_BAR_BACKGROUND: Color32 = ZINC_800;

// Network view colors
pub const NETWORK_BACKGROUND: Color32 = ZINC_800;
/// Grid lines - subtle contrast against zinc-800 background
pub const NETWORK_GRID: Color32 = ZINC_700;

// Network View - Tooltips
pub const TOOLTIP_BG: Color32 = SURFACE_ELEVATED;
pub const TOOLTIP_TEXT: Color32 = TEXT_STRONG;

// Network View - Connections
pub const CONNECTION_HOVER: Color32 = ERROR_RED; // Red indicates deletable
pub const PORT_HOVER: Color32 = VIOLET_400; // Accent for interactive

// Node body fill colors - muted tints based on output type
// Base: ZINC_500 (113, 113, 123) - all variants stay dark and professional
pub const NODE_BODY_GEOMETRY: Color32 = ZINC_500;                          // Standard zinc
pub const NODE_BODY_INT: Color32 = Color32::from_rgb(105, 110, 135);       // Subtle blue tint
pub const NODE_BODY_FLOAT: Color32 = Color32::from_rgb(105, 110, 135);     // Subtle blue tint
pub const NODE_BODY_STRING: Color32 = Color32::from_rgb(102, 122, 112);    // Subtle green tint
pub const NODE_BODY_BOOLEAN: Color32 = Color32::from_rgb(125, 115, 102);   // Subtle amber tint
pub const NODE_BODY_POINT: Color32 = Color32::from_rgb(100, 118, 128);     // Subtle cyan tint
pub const NODE_BODY_COLOR: Color32 = Color32::from_rgb(120, 106, 125);     // Subtle pink tint
pub const NODE_BODY_LIST: Color32 = Color32::from_rgb(100, 122, 120);      // Subtle teal tint
pub const NODE_BODY_DATA: Color32 = Color32::from_rgb(128, 114, 102);      // Subtle orange tint
pub const NODE_BODY_DEFAULT: Color32 = ZINC_500;                           // Fallback

// Node Category Colors (for node icons/identity)
pub const CATEGORY_GEOMETRY: Color32 = Color32::from_rgb(80, 120, 200);
pub const CATEGORY_TRANSFORM: Color32 = Color32::from_rgb(200, 120, 80);
pub const CATEGORY_COLOR: Color32 = Color32::from_rgb(200, 80, 120);
pub const CATEGORY_MATH: Color32 = Color32::from_rgb(120, 200, 80);
pub const CATEGORY_LIST: Color32 = Color32::from_rgb(200, 200, 80);
pub const CATEGORY_STRING: Color32 = Color32::from_rgb(180, 80, 200);
pub const CATEGORY_DATA: Color32 = Color32::from_rgb(80, 200, 200);
pub const CATEGORY_DEFAULT: Color32 = ZINC_400;

// Handle Colors (violet-based to match accent)
pub const HANDLE_PRIMARY: Color32 = VIOLET_500;

// Canvas/Viewer grid (uses alpha, so defined as function)
pub fn viewer_grid() -> Color32 {
    Color32::from_rgba_unmultiplied(212, 212, 216, 40) // zinc-300 with alpha
}
pub const VIEWER_CROSSHAIR: Color32 = ZINC_300;

// Point Type Visualization
pub const POINT_LINE_TO: Color32 = Color32::from_rgb(100, 200, 100);
pub const POINT_CURVE_TO: Color32 = Color32::from_rgb(200, 100, 100);
pub const POINT_CURVE_DATA: Color32 = Color32::from_rgb(100, 100, 200);

// Timeline
pub const TIMELINE_BG: Color32 = ZINC_700;
pub const TIMELINE_MARKER: Color32 = ZINC_500;
pub const TIMELINE_PLAYHEAD: Color32 = ERROR_RED;

// Port type colors (semantic colors for data types)
pub const PORT_COLOR_INT: Color32 = Color32::from_rgb(99, 102, 241);    // Indigo
pub const PORT_COLOR_FLOAT: Color32 = Color32::from_rgb(99, 102, 241);  // Indigo
pub const PORT_COLOR_STRING: Color32 = Color32::from_rgb(34, 197, 94); // Green
pub const PORT_COLOR_BOOLEAN: Color32 = Color32::from_rgb(234, 179, 8); // Yellow
pub const PORT_COLOR_POINT: Color32 = Color32::from_rgb(56, 189, 248);  // Sky blue
pub const PORT_COLOR_COLOR: Color32 = Color32::from_rgb(236, 72, 153);  // Pink
pub const PORT_COLOR_GEOMETRY: Color32 = ZINC_500; // Same as node body
pub const PORT_COLOR_LIST: Color32 = Color32::from_rgb(20, 184, 166);   // Teal
pub const PORT_COLOR_DATA: Color32 = Color32::from_rgb(249, 115, 22);   // Orange

// Node selection dialog colors
pub const DIALOG_BACKGROUND: Color32 = ZINC_700;
pub const DIALOG_BORDER: Color32 = ZINC_500;
pub const SELECTED_ITEM: Color32 = SELECTION_BG;
pub const HOVERED_ITEM: Color32 = ZINC_600;

// Button colors
pub const BUTTON_NORMAL: Color32 = ZINC_500;
pub const BUTTON_HOVER: Color32 = ZINC_400;
pub const BUTTON_ACTIVE: Color32 = ZINC_300;

// =============================================================================
// STYLE CONFIGURATION
// =============================================================================

/// Configure egui's global style and visuals for NodeBox's Linear-inspired dark theme.
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

    // Spacing - compact for a minimal feel
    style.spacing.item_spacing = egui::vec2(ITEM_SPACING, ITEM_SPACING);
    style.spacing.button_padding = egui::vec2(8.0, 4.0); // Compact button padding
    style.spacing.menu_margin = egui::Margin::same(2); // Tight menu margins
    style.spacing.indent = INDENT;
    style.spacing.scroll = egui::style::ScrollStyle {
        bar_width: SCROLL_BAR_WIDTH,
        bar_inner_margin: 4.0,
        bar_outer_margin: 4.0,
        ..Default::default()
    };

    // Visuals - Window (sharp corners, subtle border)
    visuals.window_fill = SURFACE_ELEVATED;
    visuals.window_stroke = Stroke::new(1.0, ZINC_500); // Very subtle border
    visuals.window_corner_radius = CornerRadius::ZERO; // Sharp 90° corners
    visuals.window_shadow = egui::Shadow::NONE;

    // Visuals - Menu/popup (sharp corners, no shadow)
    visuals.menu_corner_radius = CornerRadius::ZERO;
    visuals.popup_shadow = egui::Shadow::NONE;

    // Visuals - Panel (no borders, use background differentiation)
    visuals.panel_fill = PANEL_BG;
    visuals.faint_bg_color = ZINC_700;
    visuals.extreme_bg_color = ZINC_900;

    // Visuals - Widgets (sharp corners, minimal borders)
    visuals.widgets.noninteractive.bg_fill = ZINC_700;
    visuals.widgets.noninteractive.weak_bg_fill = ZINC_700;
    visuals.widgets.noninteractive.fg_stroke = Stroke::new(1.0, TEXT_SUBDUED);
    visuals.widgets.noninteractive.corner_radius = CornerRadius::ZERO;
    visuals.widgets.noninteractive.bg_stroke = Stroke::NONE;

    visuals.widgets.inactive.bg_fill = ZINC_600;
    visuals.widgets.inactive.weak_bg_fill = ZINC_600;
    visuals.widgets.inactive.fg_stroke = Stroke::new(1.0, ZINC_100);
    visuals.widgets.inactive.corner_radius = CornerRadius::ZERO;
    visuals.widgets.inactive.bg_stroke = Stroke::NONE;

    visuals.widgets.hovered.bg_fill = ZINC_500;
    visuals.widgets.hovered.weak_bg_fill = ZINC_500;
    visuals.widgets.hovered.fg_stroke = Stroke::new(1.0, ZINC_50);
    visuals.widgets.hovered.corner_radius = CornerRadius::ZERO;
    visuals.widgets.hovered.expansion = 0.0; // No expansion, just color change
    visuals.widgets.hovered.bg_stroke = Stroke::NONE;

    visuals.widgets.active.bg_fill = ZINC_500;
    visuals.widgets.active.weak_bg_fill = ZINC_500;
    visuals.widgets.active.fg_stroke = Stroke::new(1.0, ZINC_50);
    visuals.widgets.active.corner_radius = CornerRadius::ZERO;
    visuals.widgets.active.expansion = 0.0;
    visuals.widgets.active.bg_stroke = Stroke::NONE;

    visuals.widgets.open.bg_fill = ZINC_500;
    visuals.widgets.open.weak_bg_fill = ZINC_500;
    visuals.widgets.open.fg_stroke = Stroke::new(1.0, ZINC_100);
    visuals.widgets.open.corner_radius = CornerRadius::ZERO;

    // Selection (violet tint with visible text)
    visuals.selection.bg_fill = SELECTION_BG;
    visuals.selection.stroke = Stroke::new(1.0, TEXT_STRONG);

    // Separators - almost invisible
    visuals.widgets.noninteractive.bg_stroke = Stroke::NONE;

    // Hyperlinks (violet accent)
    visuals.hyperlink_color = VIOLET_400;

    // Apply styles
    style.visuals = visuals;
    ctx.set_style(style);
}
