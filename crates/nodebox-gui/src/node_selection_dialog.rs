//! Modal node selection dialog with search and category filtering.

use eframe::egui::{self, Color32, Key, Vec2};
use nodebox_core::geometry::Point;
use nodebox_core::node::{Node, NodeLibrary};
use crate::icon_cache::IconCache;
use crate::node_library::{NodeTemplate, NODE_TEMPLATES, create_node_from_template};
use crate::theme;

/// Categories for filtering nodes.
const CATEGORIES: &[&str] = &["All", "geometry", "transform", "color"];

/// The modal node selection dialog.
pub struct NodeSelectionDialog {
    /// Whether the dialog is visible.
    pub visible: bool,
    /// Search query string.
    search_query: String,
    /// Selected category (None = All).
    selected_category: Option<String>,
    /// Filtered list of node indices.
    filtered_indices: Vec<usize>,
    /// Currently selected index in filtered list.
    selected_index: usize,
    /// Position where the node should be created.
    create_position: Point,
    /// Whether search input should be focused.
    focus_search: bool,
}

impl Default for NodeSelectionDialog {
    fn default() -> Self {
        Self::new()
    }
}

impl NodeSelectionDialog {
    /// Create a new node selection dialog.
    pub fn new() -> Self {
        let mut dialog = Self {
            visible: false,
            search_query: String::new(),
            selected_category: None,
            filtered_indices: Vec::new(),
            selected_index: 0,
            create_position: Point::ZERO,
            focus_search: false,
        };
        dialog.update_filtered_list();
        dialog
    }

    /// Open the dialog at the given position.
    pub fn open(&mut self, position: Point) {
        self.visible = true;
        self.search_query.clear();
        self.selected_category = None;
        self.selected_index = 0;
        self.create_position = position;
        self.focus_search = true;
        self.update_filtered_list();
    }

    /// Close the dialog.
    pub fn close(&mut self) {
        self.visible = false;
        self.search_query.clear();
    }

    /// Update the filtered list based on search query and category.
    fn update_filtered_list(&mut self) {
        self.filtered_indices.clear();
        let query = self.search_query.to_lowercase();

        for (i, template) in NODE_TEMPLATES.iter().enumerate() {
            // Filter by category
            if let Some(ref cat) = self.selected_category {
                if template.category != cat {
                    continue;
                }
            }

            // Filter by search query
            if !query.is_empty() {
                let matches = self.fuzzy_match(template, &query);
                if !matches {
                    continue;
                }
            }

            self.filtered_indices.push(i);
        }

        // Reset selection if out of bounds
        if self.selected_index >= self.filtered_indices.len() {
            self.selected_index = 0;
        }
    }

    /// Perform fuzzy matching on a template.
    fn fuzzy_match(&self, template: &NodeTemplate, query: &str) -> bool {
        let name = template.name.to_lowercase();
        let desc = template.description.to_lowercase();

        // Exact start match
        if name.starts_with(query) {
            return true;
        }

        // Contains match
        if name.contains(query) || desc.contains(query) {
            return true;
        }

        // First letters match (e.g., "rc" matches "rect create")
        let name_chars: Vec<char> = name.chars().collect();
        let query_chars: Vec<char> = query.chars().collect();

        if query_chars.len() <= name_chars.len() {
            let mut qi = 0;
            for &nc in &name_chars {
                if qi < query_chars.len() && nc == query_chars[qi] {
                    qi += 1;
                }
            }
            if qi == query_chars.len() {
                return true;
            }
        }

        false
    }

    /// Show the dialog. Returns the selected template if one was chosen.
    pub fn show(&mut self, ctx: &egui::Context, library: &NodeLibrary, icon_cache: &mut IconCache) -> Option<Node> {
        if !self.visible {
            return None;
        }

        let mut result = None;
        let mut should_close = false;

        // Handle keyboard input first
        ctx.input(|i| {
            if i.key_pressed(Key::Escape) {
                should_close = true;
            }
            if i.key_pressed(Key::ArrowDown) {
                if !self.filtered_indices.is_empty() {
                    self.selected_index = (self.selected_index + 1) % self.filtered_indices.len();
                }
            }
            if i.key_pressed(Key::ArrowUp) {
                if !self.filtered_indices.is_empty() {
                    if self.selected_index == 0 {
                        self.selected_index = self.filtered_indices.len() - 1;
                    } else {
                        self.selected_index -= 1;
                    }
                }
            }
        });

        // Modal window - clean Figma-like styling
        let dialog_frame = egui::Frame::NONE
            .fill(theme::PANEL_BG)
            .stroke(egui::Stroke::new(1.0, theme::BORDER_COLOR))
            .corner_radius(egui::CornerRadius::same(theme::CORNER_RADIUS as u8))
            .inner_margin(egui::Margin::same(0));

        egui::Window::new("Add Node")
            .collapsible(false)
            .resizable(false)
            .title_bar(false) // Custom title bar
            .fixed_size([500.0, 400.0])
            .anchor(egui::Align2::CENTER_CENTER, [0.0, 0.0])
            .frame(dialog_frame)
            .show(ctx, |ui| {
                // Custom header
                ui.horizontal(|ui| {
                    ui.add_space(theme::PADDING_LARGE);
                    ui.label(
                        egui::RichText::new("Add Node")
                            .color(theme::TEXT_DEFAULT)
                            .size(12.0),
                    );
                    ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                        ui.add_space(theme::PADDING);
                        if ui.small_button("×").clicked() {
                            should_close = true;
                        }
                    });
                });

                ui.add_space(theme::PADDING_SMALL);

                // Search input - full width, clean styling
                ui.horizontal(|ui| {
                    ui.add_space(theme::PADDING_LARGE);
                    let response = ui.add(
                        egui::TextEdit::singleline(&mut self.search_query)
                            .desired_width(ui.available_width() - theme::PADDING_LARGE * 2.0)
                            .hint_text("Search nodes..."),
                    );
                    ui.add_space(theme::PADDING_LARGE);

                    // Focus search on open
                    if self.focus_search {
                        response.request_focus();
                        self.focus_search = false;
                    }

                    // Update filter when search changes
                    if response.changed() {
                        self.update_filtered_list();
                    }

                    // Handle Enter key on search input
                    if response.lost_focus() && ui.input(|i| i.key_pressed(Key::Enter)) {
                        if let Some(&idx) = self.filtered_indices.get(self.selected_index) {
                            let template = &NODE_TEMPLATES[idx];
                            result = Some(create_node_from_template(template, library, self.create_position));
                            should_close = true;
                        }
                    }
                });

                ui.add_space(theme::PADDING);

                // Category filters - subtle pill buttons
                ui.horizontal(|ui| {
                    ui.add_space(theme::PADDING_LARGE);
                    for &cat in CATEGORIES {
                        let is_selected = if cat == "All" {
                            self.selected_category.is_none()
                        } else {
                            self.selected_category.as_deref() == Some(cat)
                        };

                        let text_color = if is_selected {
                            theme::TEXT_STRONG
                        } else {
                            theme::TEXT_SUBDUED
                        };

                        let response = ui.add(
                            egui::Label::new(
                                egui::RichText::new(cat).color(text_color).size(10.0),
                            ).sense(egui::Sense::click())
                        );

                        if response.clicked() {
                            if cat == "All" {
                                self.selected_category = None;
                            } else {
                                self.selected_category = Some(cat.to_string());
                            }
                            self.update_filtered_list();
                        }

                        ui.add_space(theme::PADDING);
                    }
                });

                ui.add_space(theme::PADDING_SMALL);

                // Subtle separator
                let sep_rect = ui.available_rect_before_wrap();
                ui.painter().line_segment(
                    [
                        egui::pos2(sep_rect.min.x, sep_rect.min.y),
                        egui::pos2(sep_rect.max.x, sep_rect.min.y),
                    ],
                    egui::Stroke::new(1.0, theme::BORDER_COLOR),
                );
                ui.add_space(1.0);

                // Node list - clean, minimal styling
                egui::ScrollArea::vertical()
                    .auto_shrink([false, false])
                    .show(ui, |ui| {
                        for (list_idx, &template_idx) in self.filtered_indices.iter().enumerate() {
                            let template = &NODE_TEMPLATES[template_idx];
                            let is_selected = list_idx == self.selected_index;

                            // Create a frame for the item
                            let response = ui.allocate_response(
                                Vec2::new(ui.available_width(), 28.0),
                                egui::Sense::click(),
                            );

                            // Background - subtle
                            let bg_color = if is_selected {
                                theme::SELECTION_BG
                            } else if response.hovered() {
                                theme::HOVER_BG
                            } else {
                                Color32::TRANSPARENT
                            };

                            if bg_color != Color32::TRANSPARENT {
                                ui.painter().rect_filled(
                                    response.rect.shrink2(Vec2::new(theme::PADDING, 0.0)),
                                    theme::CORNER_RADIUS_SMALL,
                                    bg_color,
                                );
                            }

                            // Icon (loaded from libraries or fallback)
                            let icon_pos = response.rect.min + Vec2::new(theme::PADDING_LARGE, 4.0);
                            let function = format!("corevector/{}", template.name);
                            icon_cache.draw_icon(
                                ctx,
                                ui.painter(),
                                icon_pos,
                                20.0,
                                Some(&function),
                                template.category,
                            );

                            // Name
                            ui.painter().text(
                                response.rect.min + Vec2::new(theme::PADDING_LARGE + 24.0, 7.0),
                                egui::Align2::LEFT_TOP,
                                template.name,
                                egui::FontId::proportional(11.0),
                                if is_selected { theme::TEXT_STRONG } else { theme::TEXT_DEFAULT },
                            );

                            // Description - on the right, smaller
                            ui.painter().text(
                                egui::pos2(response.rect.max.x - theme::PADDING_LARGE, response.rect.min.y + 7.0),
                                egui::Align2::RIGHT_TOP,
                                template.description,
                                egui::FontId::proportional(10.0),
                                theme::TEXT_DISABLED,
                            );

                            // Handle click
                            if response.clicked() {
                                self.selected_index = list_idx;
                            }

                            // Handle double-click
                            if response.double_clicked() {
                                result = Some(create_node_from_template(template, library, self.create_position));
                                should_close = true;
                            }
                        }

                        if self.filtered_indices.is_empty() {
                            ui.vertical_centered(|ui| {
                                ui.add_space(50.0);
                                ui.label(
                                    egui::RichText::new("No matching nodes")
                                        .color(theme::TEXT_DISABLED)
                                        .size(11.0),
                                );
                            });
                        }
                    });
            });

        if should_close {
            self.close();
        }

        result
    }
}
