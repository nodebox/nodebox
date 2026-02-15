// Design tokens mapped from crates/nodebox-desktop/src/theme.rs
// Linear-inspired dark theme with Tailwind Zinc palette and Violet accents.

// =============================================================================
// ZINC SCALE (Tailwind v4 Zinc Palette)
// =============================================================================

export const ZINC_50 = '#fafafa';
export const ZINC_100 = '#f4f4f5';
export const ZINC_200 = '#e4e4e7';
export const ZINC_300 = '#d4d4d8';
export const ZINC_400 = '#9f9fa9';
export const ZINC_500 = '#71717b';
export const ZINC_600 = '#52525c';
export const ZINC_700 = '#3f3f46';
export const ZINC_800 = '#27272a';
export const ZINC_900 = '#18181b';
export const ZINC_950 = '#09090b';

// =============================================================================
// ACCENT COLORS (Purple/Violet)
// =============================================================================

export const VIOLET_400 = '#a78bfa';
export const VIOLET_500 = '#8b5cf6';
export const VIOLET_600 = '#7c3aed';
export const VIOLET_800 = '#4c3a76';
export const VIOLET_900 = '#2d2640';

// =============================================================================
// STATUS COLORS
// =============================================================================

export const SUCCESS_GREEN = '#22c55e';
export const WARNING_YELLOW = '#eab308';
export const ERROR_RED = '#ff6467';

// =============================================================================
// SEMANTIC COLORS - Panel & Background
// =============================================================================

export const PANEL_BG = ZINC_800;
export const TOP_BAR_BG = ZINC_800;
export const TAB_BAR_BG = ZINC_700;
export const BOTTOM_BAR_BG = ZINC_800;
export const SURFACE_ELEVATED = ZINC_600;
export const TEXT_EDIT_BG = ZINC_600;
export const HOVER_BG = ZINC_500;
export const SELECTION_BG = VIOLET_800;
export const TEXT_EDIT_SELECTION_BG = '#2563af';
export const FIELD_HOVER_BG = '#484851';

// =============================================================================
// SEMANTIC COLORS - Text
// =============================================================================

export const TEXT_STRONG = ZINC_50;
export const TEXT_DEFAULT = ZINC_100;
export const TEXT_SUBDUED = ZINC_300;
export const TEXT_DISABLED = ZINC_400;

// =============================================================================
// SEMANTIC COLORS - Widgets & Borders
// =============================================================================

export const WIDGET_INACTIVE_BG = ZINC_600;
export const WIDGET_HOVERED_BG = ZINC_500;
export const WIDGET_ACTIVE_BG = ZINC_300;
export const WIDGET_NONINTERACTIVE_BG = ZINC_700;
export const BORDER_COLOR = ZINC_500;
export const BORDER_SECONDARY = ZINC_400;

// =============================================================================
// LAYOUT CONSTANTS - Heights
// =============================================================================

export const TOP_BAR_HEIGHT = 28;
export const TITLE_BAR_HEIGHT = 24;
export const LIST_ITEM_HEIGHT = 28;
export const TABLE_HEADER_HEIGHT = 32;
export const ROW_HEIGHT = 24;
export const ADDRESS_BAR_HEIGHT = TOP_BAR_HEIGHT;
export const ANIMATION_BAR_HEIGHT = 28;
export const PANE_HEADER_HEIGHT = TITLE_BAR_HEIGHT;
export const LABEL_WIDTH = 112;
export const PARAMETER_PANEL_WIDTH = 280;
export const PARAMETER_ROW_HEIGHT = ROW_HEIGHT;

// =============================================================================
// DATA TABLE COLORS
// =============================================================================

export const TABLE_ROW_EVEN = ZINC_800;
export const TABLE_ROW_ODD = '#333338';
export const TABLE_HEADER_BG = ZINC_700;
export const TABLE_HEADER_TEXT = ZINC_200;
export const TABLE_CELL_TEXT = ZINC_100;
export const TABLE_INDEX_TEXT = ZINC_300;

// =============================================================================
// PANE HEADER COLORS
// =============================================================================

export const PANE_HEADER_BACKGROUND_COLOR = ZINC_700;
export const PANE_HEADER_FOREGROUND_COLOR = ZINC_200;

// =============================================================================
// LAYOUT CONSTANTS - Spacing (4px grid)
// =============================================================================

export const PADDING = 8;
export const PADDING_SMALL = 4;
export const PADDING_LARGE = 12;
export const PADDING_XL = 16;
export const VIEW_PADDING = 12;
export const ITEM_SPACING = 8;
export const MENU_SPACING = 4;
export const INDENT = 16;
export const ICON_TEXT_PADDING = 8;

// =============================================================================
// LAYOUT CONSTANTS - Sizing
// =============================================================================

export const CORNER_RADIUS = 0;
export const CORNER_RADIUS_SMALL = 4;
export const BUTTON_SIZE_LARGE = 24;
export const BUTTON_ICON_SIZE = 16;
export const ICON_SIZE_SMALL = 16;
export const SCROLL_BAR_WIDTH = 8;
export const SPLITTER_THICKNESS = 2;
export const SPLITTER_AFFORDANCE = 8;

// =============================================================================
// TYPOGRAPHY
// =============================================================================

export const FONT_SIZE_BASE = 13;
export const FONT_SIZE_SMALL = 11;
export const FONT_SIZE_HEADING = 16;
export const LINE_HEIGHT_RATIO = 1.4;

// Aliases used in the task spec
export const FONT_SIZE_BODY = FONT_SIZE_BASE;

// =============================================================================
// LEGACY / SEMANTIC ALIASES
// =============================================================================

export const VALUE_TEXT = VIOLET_400;
export const VALUE_TEXT_HOVER = VIOLET_500;
export const BACKGROUND_COLOR = ZINC_700;
export const HEADER_BACKGROUND = ZINC_700;
export const DARK_BACKGROUND = ZINC_800;
export const TEXT_NORMAL = TEXT_DEFAULT;
export const TEXT_BRIGHT = TEXT_STRONG;
export const PORT_LABEL_BACKGROUND = ZINC_800;
export const PORT_VALUE_BACKGROUND = ZINC_700;
export const SELECTED_TAB_BACKGROUND = ZINC_600;
export const UNSELECTED_TAB_BACKGROUND = ZINC_700;

// Address bar
export const ADDRESS_BAR_BACKGROUND = ZINC_800;
export const ADDRESS_SEGMENT_HOVER = ZINC_500;
export const ADDRESS_SEPARATOR_COLOR = ZINC_400;

// Animation bar
export const ANIMATION_BAR_BACKGROUND = ZINC_800;

// Network view
export const NETWORK_BACKGROUND = ZINC_800;
export const NETWORK_GRID = ZINC_700;
export const GRID_LINE_COLOR = ZINC_700;

// Tooltips
export const TOOLTIP_BG = SURFACE_ELEVATED;
export const TOOLTIP_TEXT = TEXT_STRONG;

// Connections
export const CONNECTION_HOVER = ERROR_RED;
export const PORT_HOVER = VIOLET_400;

// Node body fill colors by output type
export const NODE_BODY_GEOMETRY = ZINC_500;
export const NODE_BODY_INT = '#696e87';
export const NODE_BODY_FLOAT = '#696e87';
export const NODE_BODY_STRING = '#667a70';
export const NODE_BODY_BOOLEAN = '#7d7366';
export const NODE_BODY_POINT = '#647680';
export const NODE_BODY_COLOR = '#786a7d';
export const NODE_BODY_LIST = '#647a78';
export const NODE_BODY_DATA = '#807266';
export const NODE_BODY_DEFAULT = ZINC_500;

// Node category colors
export const CATEGORY_GEOMETRY = '#5078c8';
export const CATEGORY_TRANSFORM = '#c87850';
export const CATEGORY_COLOR = '#c85078';
export const CATEGORY_MATH = '#78c850';
export const CATEGORY_LIST = '#c8c850';
export const CATEGORY_STRING = '#b450c8';
export const CATEGORY_DATA = '#50c8c8';
export const CATEGORY_DEFAULT = ZINC_400;

// Handle colors
export const HANDLE_PRIMARY = VIOLET_500;

// Canvas / Viewer
export const VIEWER_GRID = 'rgba(212, 212, 216, 0.16)';
export const VIEWER_CROSSHAIR = ZINC_300;

// Point type visualization
export const POINT_LINE_TO = '#64c864';
export const POINT_CURVE_TO = '#c86464';
export const POINT_CURVE_DATA = '#6464c8';

// Timeline
export const TIMELINE_BG = ZINC_700;
export const TIMELINE_MARKER = ZINC_500;
export const TIMELINE_PLAYHEAD = ERROR_RED;

// Port type colors
export const PORT_COLOR_INT = '#6366f1';
export const PORT_COLOR_FLOAT = '#6366f1';
export const PORT_COLOR_STRING = '#22c55e';
export const PORT_COLOR_BOOLEAN = '#eab308';
export const PORT_COLOR_POINT = '#38bdf8';
export const PORT_COLOR_COLOR = '#ec4899';
export const PORT_COLOR_GEOMETRY = ZINC_500;
export const PORT_COLOR_LIST = '#14b8a6';
export const PORT_COLOR_DATA = '#f97316';

// Dialog
export const DIALOG_BACKGROUND = ZINC_700;
export const DIALOG_BORDER = ZINC_500;
export const SELECTED_ITEM = SELECTION_BG;
export const HOVERED_ITEM = ZINC_600;

// Buttons
export const BUTTON_NORMAL = ZINC_500;
export const BUTTON_HOVER = ZINC_400;
export const BUTTON_ACTIVE = ZINC_300;
