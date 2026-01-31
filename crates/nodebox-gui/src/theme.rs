//! Centralized theme constants matching Java NodeBox Theme.java.

use eframe::egui::Color32;

// Layout constants
pub const ADDRESS_BAR_HEIGHT: f32 = 25.0;
pub const ANIMATION_BAR_HEIGHT: f32 = 27.0;
pub const PANE_HEADER_HEIGHT: f32 = 25.0;
pub const LABEL_WIDTH: f32 = 100.0;
pub const PARAMETER_PANEL_WIDTH: f32 = 260.0;
pub const PARAMETER_ROW_HEIGHT: f32 = 22.0;

// Parameter panel value colors
pub const VALUE_TEXT: Color32 = Color32::from_rgb(138, 180, 248);
pub const VALUE_TEXT_HOVER: Color32 = Color32::from_rgb(168, 200, 255);

// Background colors
pub const BACKGROUND_COLOR: Color32 = Color32::from_rgb(69, 69, 69);
pub const HEADER_BACKGROUND: Color32 = Color32::from_rgb(85, 85, 85);
pub const DARK_BACKGROUND: Color32 = Color32::from_rgb(50, 50, 50);

// Text colors
pub const TEXT_NORMAL: Color32 = Color32::from_rgb(200, 200, 200);
pub const TEXT_DISABLED: Color32 = Color32::from_rgb(128, 128, 128);
pub const TEXT_BRIGHT: Color32 = Color32::from_rgb(240, 240, 240);

// Port/parameter colors
pub const PORT_LABEL_BACKGROUND: Color32 = Color32::from_rgb(96, 96, 96);
pub const PORT_VALUE_BACKGROUND: Color32 = Color32::from_rgb(64, 64, 64);

// Tab colors
pub const SELECTED_TAB_BACKGROUND: Color32 = Color32::from_rgb(50, 50, 50);
pub const UNSELECTED_TAB_BACKGROUND: Color32 = Color32::from_rgb(70, 70, 70);

// Address bar colors
pub const ADDRESS_BAR_BACKGROUND: Color32 = Color32::from_rgb(60, 60, 60);
pub const ADDRESS_SEGMENT_HOVER: Color32 = Color32::from_rgb(100, 100, 100);
pub const ADDRESS_SEPARATOR_COLOR: Color32 = Color32::from_rgb(120, 120, 120);

// Animation bar colors
pub const ANIMATION_BAR_BACKGROUND: Color32 = Color32::from_rgb(55, 55, 55);

// Network view colors (matching Java Theme)
pub const NETWORK_BACKGROUND: Color32 = Color32::from_rgb(69, 69, 69);
pub const NETWORK_GRID: Color32 = Color32::from_rgb(85, 85, 85);

// Port type colors (matching Java Theme)
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
pub const DIALOG_BACKGROUND: Color32 = Color32::from_rgb(60, 60, 60);
pub const DIALOG_BORDER: Color32 = Color32::from_rgb(40, 40, 40);
pub const SELECTED_ITEM: Color32 = Color32::from_rgb(80, 100, 130);
pub const HOVERED_ITEM: Color32 = Color32::from_rgb(75, 75, 75);

// Button colors
pub const BUTTON_NORMAL: Color32 = Color32::from_rgb(80, 80, 80);
pub const BUTTON_HOVER: Color32 = Color32::from_rgb(100, 100, 100);
pub const BUTTON_ACTIVE: Color32 = Color32::from_rgb(70, 70, 70);
