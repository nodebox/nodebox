# NodeBox Design System

This document defines the visual language and design principles for the NodeBox GUI. All UI development should follow these guidelines to ensure a consistent, professional, and modern interface.

**Reference Implementation:** `crates/nodebox-gui/src/theme.rs`

---

## Design Philosophy

NodeBox follows a **Linear-inspired design philosophy** with these core principles:

### 1. Sharp & Geometric
- **Straight lines and 90° angles** for a clean, precise aesthetic
- Most UI elements have **zero corner radius** — sharp rectangles
- Subtle rounding (`4px`) only for specific highlighted elements:
  - Selected items in lists/dialogs (e.g., node selection dialog)
  - Focused input fields
- Creates a professional, tool-like appearance

### 2. No Borders, Use Backgrounds
- **Eliminate borders wherever possible** — they add visual noise
- Delineate sections through background color differentiation
- Panels are distinguished by subtle background shade changes
- Only use borders for:
  - Window/dialog outer edges (1px, very subtle)
  - Focus states on inputs
  - Critical separation needs

### 3. Deep, Rich Dark Theme
- **Near-black backgrounds** with subtle cool undertones
- Multiple gray levels create depth through layering
- Darker = further back, lighter = elevated/interactive
- Comfortable for extended use, reduces eye strain

### 4. Violet Accent
- **Purple/violet as the primary accent color** — distinctive and modern
- Used for: selections, links, active states, primary actions
- Adds personality without overwhelming
- Status colors: green (success), yellow (warning), red (error)

### 5. Generous Spacing
- **Breathable, airy layouts** — don't crowd elements
- Consistent 4px grid for all dimensions
- Larger padding creates a premium feel
- White space is a feature, not wasted space

---

## Color System

### Gray Scale (Linear-inspired Dark Theme)

Deep blacks with subtle cool undertones, creating a sophisticated dark interface.

| Token | RGB | Hex | Usage |
|-------|-----|-----|-------|
| `GRAY_0` | `(0, 0, 0)` | #000000 | Pure black |
| `GRAY_50` | `(9, 9, 11)` | #09090B | Deepest background |
| `GRAY_100` | `(17, 17, 19)` | #111113 | Main panel background |
| `GRAY_150` | `(23, 23, 26)` | #17171A | Secondary panels, sidebar |
| `GRAY_200` | `(31, 31, 35)` | #1F1F23 | Elevated surfaces, inputs |
| `GRAY_250` | `(39, 39, 43)` | #27272B | Subtle borders, hover bg |
| `GRAY_300` | `(49, 49, 54)` | #313136 | Widget backgrounds |
| `GRAY_350` | `(55, 55, 61)` | #37373D | Active/pressed states |
| `GRAY_400` | `(75, 75, 82)` | #4B4B52 | Disabled elements |
| `GRAY_500` | `(107, 107, 115)` | #6B6B73 | Muted text, icons |
| `GRAY_600` | `(139, 139, 148)` | #8B8B94 | Secondary text |
| `GRAY_700` | `(171, 171, 181)` | #ABABB5 | Body text |
| `GRAY_800` | `(219, 219, 224)` | #DBDBE0 | Primary text |
| `GRAY_900` | `(235, 235, 240)` | #EBEBF0 | Bright/emphasized text |
| `GRAY_1000` | `(255, 255, 255)` | #FFFFFF | Pure white |

### Primary Accent: Violet

Purple/violet creates a distinctive, modern identity.

| Token | RGB | Hex | Usage |
|-------|-----|-----|-------|
| `VIOLET_900` | `(45, 38, 64)` | #2D2640 | Selection background (subtle) |
| `VIOLET_600` | `(124, 58, 237)` | #7C3AED | Pressed/darker accent |
| `VIOLET_500` | `(139, 92, 246)` | #8B5CF6 | Primary accent |
| `VIOLET_400` | `(167, 139, 250)` | #A78BFA | Hover/lighter accent |

### Status Colors

Use sparingly for semantic feedback only.

| Token | RGB | Hex | Usage |
|-------|-----|-----|-------|
| `SUCCESS_GREEN` | `(34, 197, 94)` | #22C55E | Success, valid |
| `WARNING_YELLOW` | `(234, 179, 8)` | #EAB308 | Warning, caution |
| `ERROR_RED` | `(239, 68, 68)` | #EF4444 | Error, danger |

### Semantic Color Mapping

**Always use semantic tokens**, never raw values:

```rust
// Backgrounds (darkest to lightest)
PANEL_BG           // Main panels (GRAY_100)
TAB_BAR_BG         // Sidebars, secondary (GRAY_150)
SURFACE_ELEVATED   // Cards, dialogs, inputs (GRAY_200)
HOVER_BG           // Hover states (GRAY_250)
SELECTION_BG       // Selected items (VIOLET_900)

// Text (brightest to dimmest)
TEXT_STRONG        // Emphasized, active (GRAY_900)
TEXT_DEFAULT       // Body text (GRAY_700)
TEXT_SUBDUED       // Secondary info (GRAY_500)
TEXT_DISABLED      // Disabled (GRAY_400)

// Widgets
WIDGET_INACTIVE_BG   // Button default (GRAY_250)
WIDGET_HOVERED_BG    // Button hover (GRAY_300)
WIDGET_ACTIVE_BG     // Button pressed (GRAY_350)
```

---

## Typography

### Font Sizes

| Token | Size | Usage |
|-------|------|-------|
| `FONT_SIZE_SMALL` | 11px | Labels, captions |
| `FONT_SIZE_BASE` | 13px | Body text, buttons |
| `FONT_SIZE_HEADING` | 16px | Section headings |

### Text Hierarchy

Use text color (not weight) to establish hierarchy:

1. **Strong** (`TEXT_STRONG` / GRAY_900) — Active, focused, primary
2. **Default** (`TEXT_DEFAULT` / GRAY_700) — Standard body text
3. **Subdued** (`TEXT_SUBDUED` / GRAY_500) — Secondary, hints
4. **Disabled** (`TEXT_DISABLED` / GRAY_400) — Non-interactive

```rust
// Example: List item
ui.label(RichText::new("Primary text").color(TEXT_DEFAULT));
ui.label(RichText::new("Secondary").color(TEXT_SUBDUED));
```

---

## Spacing System

All spacing follows a **4px grid** for visual consistency.

### Spacing Tokens

| Token | Value | Usage |
|-------|-------|-------|
| `PADDING_SMALL` | 4px | Tight spaces, icon gaps |
| `PADDING` | 8px | Standard padding |
| `PADDING_LARGE` | 12px | Button padding, sections |
| `PADDING_XL` | 16px | Large section gaps |
| `VIEW_PADDING` | 12px | Panel content margins |
| `ITEM_SPACING` | 8px | Between list items |
| `INDENT` | 16px | Tree/hierarchy indent |

### Layout Heights

| Token | Value | Usage |
|-------|-------|-------|
| `TOP_BAR_HEIGHT` | 28px | Address bar, toolbar |
| `TITLE_BAR_HEIGHT` | 24px | Pane headers |
| `LIST_ITEM_HEIGHT` | 28px | List items (taller for touch) |
| `ROW_HEIGHT` | 24px | Parameter rows |
| `TABLE_HEADER_HEIGHT` | 32px | Table headers |

---

## Corner Radii

Sharp corners by default, with subtle rounding for interactive highlights.

| Token | Value | Usage |
|-------|-------|-------|
| `CORNER_RADIUS` | 0px | Panels, windows, dialogs — sharp edges |
| `CORNER_RADIUS_SMALL` | 4px | Selected items, focused inputs only |

**When to use `CORNER_RADIUS_SMALL` (4px):**
- Selection highlight in node selection dialog
- Hovered/selected list items
- Focused text input fields

**Everything else: sharp corners (0px)**

---

## Component Patterns

### Panels (No Borders)

Panels are distinguished by background color, not borders:

```rust
// Main content area
ui.painter().rect_filled(rect, 0.0, PANEL_BG);

// Sidebar or secondary panel
ui.painter().rect_filled(rect, 0.0, TAB_BAR_BG);

// Elevated card or section
ui.painter().rect_filled(rect, CORNER_RADIUS, SURFACE_ELEVATED);
```

**Key principle:** Adjacent panels have different background shades. No borders needed.

### Pane Headers

Blend seamlessly with their panels:

```rust
// Header background matches panel, slightly lighter text
ui.painter().rect_filled(header_rect, 0.0, PANE_HEADER_BACKGROUND_COLOR);
ui.label(
    RichText::new("PARAMETERS")
        .size(FONT_SIZE_SMALL)
        .color(PANE_HEADER_FOREGROUND_COLOR)
);
```

- Background: `GRAY_150` (matches sidebar tone)
- Text: `GRAY_600` (subdued, not competing with content)
- No bottom border — spacing separates header from content

### Buttons

Sharp corners, no borders, background-based states:

```rust
let bg = if response.is_pointer_button_down_on() {
    WIDGET_ACTIVE_BG
} else if response.hovered() {
    WIDGET_HOVERED_BG
} else {
    WIDGET_INACTIVE_BG
};

ui.painter().rect_filled(rect, 0.0, bg);  // Sharp corners
```

- Default: `GRAY_250`
- Hover: `GRAY_300`
- Active: `GRAY_350`
- No border strokes, sharp 90° corners

### Input Fields

Elevated background, sharp corners:

```rust
let response = ui.add(
    egui::TextEdit::singleline(&mut value)
        .desired_width(100.0)
);
```

- Background: `SURFACE_ELEVATED` (GRAY_200)
- Text: `TEXT_DEFAULT`
- No border by default, sharp corners
- Focus: Subtle `VIOLET_500` border or 4px rounded highlight

### Selections (Exception: Use Subtle Rounding)

Selected items get subtle rounding to make them "pop":

```rust
let bg = if is_selected {
    SELECTION_BG  // Violet-tinted background
} else if is_hovered {
    HOVER_BG
} else {
    Color32::TRANSPARENT
};

// Use CORNER_RADIUS_SMALL (4px) for selections only
ui.painter().rect_filled(item_rect, CORNER_RADIUS_SMALL, bg);
```

- Selected: `VIOLET_900` with 4px rounding
- Hovered: `HOVER_BG` with 4px rounding
- Default: transparent, no rounding
- Text becomes `TEXT_STRONG` when selected

### Dialogs / Modals

```rust
egui::Window::new("Add Node")
    .fixed_size([500.0, 400.0])
    .frame(egui::Frame::window(&ctx.style())
        .fill(SURFACE_ELEVATED)
        .stroke(Stroke::new(1.0, GRAY_250))
        .rounding(0.0))  // Sharp corners
```

- Background: `SURFACE_ELEVATED` (elevated from underlying content)
- Border: 1px `GRAY_250` (only place we use a visible border)
- Corner radius: `0px` (sharp)
- No drop shadow

---

## Interaction States

### State Progression (No Borders)

| State | Background | Text |
|-------|------------|------|
| Default | `GRAY_250` | `TEXT_DEFAULT` |
| Hovered | `GRAY_300` | `TEXT_STRONG` |
| Active/Pressed | `GRAY_350` | `TEXT_STRONG` |
| Selected | `VIOLET_900` | `TEXT_STRONG` |
| Disabled | `GRAY_150` | `TEXT_DISABLED` |

### Hover Effects

- Background color lightens one step
- Text brightens to `TEXT_STRONG`
- No expansion, no borders, no shadows
- Cursor changes to pointer

---

## Layout Guidelines

### Panel Structure

```
┌─────────────────────────────────────┐
│ PANE HEADER (24px)                  │  ← GRAY_150
│                                     │
│  Content Area                       │  ← GRAY_100
│  (VIEW_PADDING on all sides)        │
│                                     │
└─────────────────────────────────────┘
```

No borders between header and content — spacing creates separation.

### Sidebar + Main Layout

```
┌──────────┬──────────────────────────┐
│          │                          │
│ Sidebar  │  Main Content            │
│ GRAY_150 │  GRAY_100                │
│          │                          │
└──────────┴──────────────────────────┘
```

Different background colors distinguish areas. No vertical border needed.

### Network View Grid

```
GRID_CELL_SIZE = 48px
NODE_MARGIN = 8px
NODE_WIDTH = 128px (48×3 - 8×2)
NODE_HEIGHT = 32px (48×1 - 8×2)
NODE_PADDING = 4px
NODE_ICON_SIZE = 24px
```

---

## Do's and Don'ts

### Do

- ✓ Use **sharp 90° corners** for most UI elements
- ✓ Use background color to distinguish panels and sections
- ✓ Apply subtle 4px rounding **only** for selected/hovered items
- ✓ Use the violet accent for selections and links
- ✓ Keep interfaces spacious with generous padding
- ✓ Use semantic color tokens from `theme.rs`

### Don't

- ✗ Round corners on panels, dialogs, or buttons — keep them sharp
- ✗ Add borders to separate panels — use background colors instead
- ✗ Use borders on buttons or list items
- ✗ Add drop shadows (they add visual noise)
- ✗ Hardcode color values — use tokens
- ✗ Use expansion effects on hover — just change color

---

## Implementation Checklist

When creating new UI components:

1. [ ] Import tokens from `crate::theme`
2. [ ] Use semantic colors, not raw values
3. [ ] Use **sharp corners (0px)** by default
4. [ ] Only use 4px rounding for selections/highlights
5. [ ] **NO BORDERS** — use background differentiation
6. [ ] Implement hover/active states via background color
7. [ ] Use violet (`SELECTION_BG`) for selections
8. [ ] Align to 4px grid

---

## Quick Reference

```rust
use crate::theme::{
    // Backgrounds
    PANEL_BG, TAB_BAR_BG, SURFACE_ELEVATED, HOVER_BG, SELECTION_BG,

    // Text
    TEXT_STRONG, TEXT_DEFAULT, TEXT_SUBDUED, TEXT_DISABLED,

    // Widgets
    WIDGET_INACTIVE_BG, WIDGET_HOVERED_BG, WIDGET_ACTIVE_BG,

    // Accents
    VIOLET_400, VIOLET_500, VIOLET_900,
    SUCCESS_GREEN, WARNING_YELLOW, ERROR_RED,

    // Layout
    CORNER_RADIUS, CORNER_RADIUS_SMALL,
    PADDING, PADDING_SMALL, PADDING_LARGE, PADDING_XL,
    ROW_HEIGHT, LIST_ITEM_HEIGHT,

    // Typography
    FONT_SIZE_BASE, FONT_SIZE_SMALL, FONT_SIZE_HEADING,
};
```

---

## egui Styling System

NodeBox uses egui for its GUI. Understanding how to properly configure egui's styling is essential for maintaining visual consistency.

### Global Style Configuration

All global styles are configured in `theme.rs` in the `configure_style()` function. This function is called once at startup.

```rust
pub fn configure_style(ctx: &egui::Context) {
    let mut style = Style::default();
    let mut visuals = Visuals::dark();

    // ... configure style and visuals ...

    style.visuals = visuals;
    ctx.set_style(style);
}
```

### Widget Visuals: Set ALL Properties

**Critical:** egui widgets have multiple background properties. If you only set `bg_fill`, egui will use its default brownish-gray for other properties. Always set ALL of these:

```rust
// For each widget state (noninteractive, inactive, hovered, active, open):
visuals.widgets.inactive.bg_fill = SLATE_700;       // Primary background
visuals.widgets.inactive.weak_bg_fill = SLATE_700;  // IMPORTANT: Button frames use this!
visuals.widgets.inactive.fg_stroke = Stroke::new(1.0, SLATE_200);  // Text/icon color
visuals.widgets.inactive.bg_stroke = Stroke::NONE;  // Border (usually none)
visuals.widgets.inactive.corner_radius = CornerRadius::ZERO;
```

The `weak_bg_fill` property is particularly important - it's used for:
- Button backgrounds
- ComboBox button backgrounds
- Frame backgrounds for "weak" (unfocused) widgets

If you see brownish-gray colors appearing, you likely forgot to set `weak_bg_fill`.

### Widget States

egui has five widget states, each needs full configuration:

| State | When Used |
|-------|-----------|
| `noninteractive` | Labels, static text, disabled widgets |
| `inactive` | Buttons/widgets not being interacted with |
| `hovered` | Mouse hovering over widget |
| `active` | Widget being clicked/pressed |
| `open` | ComboBox/menu when popup is open |

### Menu and Popup Styling

For menus and popups (like ComboBox dropdowns):

```rust
// Sharp corners on menu popups
visuals.menu_corner_radius = CornerRadius::ZERO;

// No shadow on popups
visuals.popup_shadow = egui::Shadow::NONE;

// Tight menu margins
style.spacing.menu_margin = egui::Margin::same(2);
```

### Selection Styling

For selected items in lists and menus:

```rust
visuals.selection.bg_fill = SELECTION_BG;  // Background color (violet)
visuals.selection.stroke = Stroke::new(1.0, TEXT_STRONG);  // Text visibility
```

**Note:** `selection.stroke` helps ensure text remains visible on the selection background.

### Local Style Overrides

For widget-specific styling, modify `ui.style_mut()` before rendering:

```rust
// Example: Smaller font for a specific widget
let style = ui.style_mut();
style.override_font_id = Some(egui::FontId::proportional(FONT_SIZE_SMALL));

// Then render your widget
egui::ComboBox::from_id_salt(id)
    .selected_text(label)
    .show_ui(ui, |ui| { ... });
```

### Common Styling Properties

| Property | Location | Purpose |
|----------|----------|---------|
| `style.spacing.button_padding` | Spacing | Internal button padding |
| `style.spacing.menu_margin` | Spacing | Menu interior margin |
| `style.spacing.item_spacing` | Spacing | Gap between items |
| `visuals.window_corner_radius` | Visuals | Window/dialog corners |
| `visuals.menu_corner_radius` | Visuals | Menu popup corners |
| `visuals.popup_shadow` | Visuals | Shadow on popups |
| `visuals.window_shadow` | Visuals | Shadow on windows |
| `visuals.selection.bg_fill` | Visuals | Selection highlight color |
| `visuals.widgets.*.bg_fill` | Visuals | Widget background |
| `visuals.widgets.*.weak_bg_fill` | Visuals | Widget frame/button background |
| `visuals.widgets.*.fg_stroke` | Visuals | Widget text/icon color |

### Debugging Style Issues

If a widget has wrong colors:

1. **Brownish-gray background?** → Set `weak_bg_fill` for all widget states
2. **Wrong text color?** → Check `fg_stroke` for the relevant state
3. **Rounded corners appearing?** → Set `corner_radius = CornerRadius::ZERO`
4. **Selection not visible?** → Check `selection.bg_fill` and `selection.stroke`
5. **Menu has shadows?** → Set `popup_shadow = Shadow::NONE`

### Example: Full Widget State Configuration

```rust
// Inactive state (not hovered, not clicked)
visuals.widgets.inactive.bg_fill = SLATE_700;
visuals.widgets.inactive.weak_bg_fill = SLATE_700;
visuals.widgets.inactive.fg_stroke = Stroke::new(1.0, SLATE_200);
visuals.widgets.inactive.corner_radius = CornerRadius::ZERO;
visuals.widgets.inactive.bg_stroke = Stroke::NONE;

// Hovered state
visuals.widgets.hovered.bg_fill = SLATE_600;
visuals.widgets.hovered.weak_bg_fill = SLATE_600;
visuals.widgets.hovered.fg_stroke = Stroke::new(1.0, SLATE_100);
visuals.widgets.hovered.corner_radius = CornerRadius::ZERO;
visuals.widgets.hovered.expansion = 0.0;  // No size change on hover
visuals.widgets.hovered.bg_stroke = Stroke::NONE;
```

---

## Evolution

This design system is living documentation. When adding new patterns:

1. Check if existing tokens cover your use case
2. Add new tokens to `theme.rs` with semantic names
3. Update this document with the new pattern
4. **Remember: sharp corners, no borders, background differentiation**
