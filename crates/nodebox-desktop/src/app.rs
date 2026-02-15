//! Main application state and update loop.

use eframe::egui::{self, Pos2, Rect};
use nodebox_core::geometry::Point;
use nodebox_core::port::{Port, ProjectContext};
use std::sync::Arc;

use crate::address_bar::{AddressBar, AddressBarAction};
use crate::animation_bar::{AnimationBar, AnimationEvent};
use crate::components;
use crate::history::{History, SelectionSnapshot};
use crate::icon_cache::IconCache;
use crate::native_menu::{MenuAction, NativeMenuHandle};
use crate::notification_banner;
use crate::recent_files::RecentFiles;
use crate::network_view::{NetworkAction, NetworkView};
use crate::node_selection_dialog::NodeSelectionDialog;
use nodebox_core::node::{Connection, PortType};
use crate::parameter_panel::ParameterPanel;
use crate::render_worker::{RenderResult, RenderState, RenderWorkerHandle};
use crate::state::AppState;
use crate::theme;
use crate::viewer_pane::{HandleResult, ViewerPane};

/// The main NodeBox application.
pub struct NodeBoxApp {
    /// Port for platform-abstracted file operations.
    port: Arc<dyn Port>,
    /// Project context for the current project (tracks save location).
    project_context: ProjectContext,
    state: AppState,
    address_bar: AddressBar,
    viewer_pane: ViewerPane,
    network_view: NetworkView,
    parameters: ParameterPanel,
    animation_bar: AnimationBar,
    node_dialog: NodeSelectionDialog,
    /// Shared icon cache for the node selection dialog.
    icon_cache: IconCache,
    history: History,
    /// Previous library state for detecting changes.
    previous_library_hash: u64,
    /// Snapshot of the library at end of last frame, used for undo (pre-mutation state).
    previous_library: Arc<nodebox_core::node::NodeLibrary>,
    /// Snapshot of the selection at end of last frame, used for undo (pre-mutation state).
    previous_selection: SelectionSnapshot,
    /// Background render worker.
    render_worker: RenderWorkerHandle,
    /// State tracking for render requests.
    render_state: RenderState,
    /// Whether a render is pending (needs to be dispatched).
    render_pending: bool,
    /// Native menu handle for macOS system menu bar.
    native_menu: Option<NativeMenuHandle>,
    /// Recent files list for "Open Recent" menu.
    recent_files: RecentFiles,
    /// Pending connection to create after the node dialog selects a node.
    /// Stores (from_node_name, output_type) from a drag-to-empty-space action.
    pending_connection: Option<(String, PortType)>,
    /// Whether any component was dragging in the previous frame (for undo group detection).
    was_dragging: bool,
    /// Vertical split ratio between Parameters (top) and Network (bottom). Default 0.35.
    right_panel_split: f32,
}

impl NodeBoxApp {
    /// Create a new NodeBox application instance with a Port for file operations.
    ///
    /// This is the primary constructor that accepts an `Arc<dyn Port>` for
    /// platform-abstracted file operations.
    pub fn new_with_port(
        cc: &eframe::CreationContext<'_>,
        port: Arc<dyn Port>,
        initial_file: Option<std::path::PathBuf>,
    ) -> Self {
        // Configure the global theme/style
        theme::configure_style(&cc.egui_ctx);

        // Initialize native menu bar (macOS)
        let native_menu = Some(NativeMenuHandle::new());

        let mut state = AppState::new();

        // Load recent files from disk
        let mut recent_files = RecentFiles::load();

        // Determine project context from initial file
        let project_context = if let Some(ref path) = initial_file {
            if let Err(e) = state.load_file(path) {
                log::error!("Failed to load initial file {:?}: {}", path, e);
                ProjectContext::new_unsaved()
            } else {
                // Add to recent files on successful load
                recent_files.add_file(path.clone());
                recent_files.save();

                // Create project context from file path
                if let (Some(parent), Some(file_name)) = (path.parent(), path.file_name()) {
                    ProjectContext::new(parent, file_name.to_string_lossy().to_string())
                } else {
                    ProjectContext::new_unsaved()
                }
            }
        } else {
            ProjectContext::new_unsaved()
        };

        // Rebuild native menu with recent files
        if let Some(ref menu) = native_menu {
            menu.rebuild_recent_menu(&recent_files.files());
        }

        let hash = Self::hash_library(&state.library);
        let prev_library = Arc::clone(&state.library);
        Self {
            port,
            project_context,
            state,
            address_bar: AddressBar::new(),
            viewer_pane: ViewerPane::new(),
            network_view: NetworkView::new(),
            parameters: ParameterPanel::new(),
            animation_bar: AnimationBar::new(),
            node_dialog: NodeSelectionDialog::new(),
            icon_cache: IconCache::new(),
            history: History::new(),
            previous_library_hash: hash,
            previous_library: prev_library,
            previous_selection: SelectionSnapshot::default(),
            render_worker: RenderWorkerHandle::spawn(),
            render_state: RenderState::new(),
            render_pending: true,
            native_menu,
            recent_files,
            pending_connection: None,
            was_dragging: false,
            right_panel_split: 0.35,
        }
    }

    /// Create a new NodeBox application instance (legacy constructor).
    ///
    /// This constructor creates a DesktopPort internally for backwards compatibility.
    /// Prefer using `new_with_port` for new code.
    #[allow(dead_code)]
    pub fn new(_cc: &eframe::CreationContext<'_>) -> Self {
        Self::new_with_file(_cc, None, None)
    }

    /// Create a new NodeBox application instance, optionally loading an initial file.
    ///
    /// This constructor creates a DesktopPort internally for backwards compatibility.
    /// Prefer using `new_with_port` for new code.
    pub fn new_with_file(
        cc: &eframe::CreationContext<'_>,
        initial_file: Option<std::path::PathBuf>,
        native_menu: Option<NativeMenuHandle>,
    ) -> Self {
        // Configure the global theme/style
        theme::configure_style(&cc.egui_ctx);

        // Create a default DesktopPort for backwards compatibility
        #[cfg(not(target_arch = "wasm32"))]
        let port: Arc<dyn Port> = Arc::new(crate::DesktopPort::new());
        #[cfg(target_arch = "wasm32")]
        compile_error!("WASM builds must use new_with_port with a custom Port implementation");

        let mut state = AppState::new();

        // Load recent files from disk
        let mut recent_files = RecentFiles::load();

        // Determine project context from initial file
        let project_context = if let Some(ref path) = initial_file {
            if let Err(e) = state.load_file(path) {
                log::error!("Failed to load initial file {:?}: {}", path, e);
                ProjectContext::new_unsaved()
            } else {
                // Add to recent files on successful load
                recent_files.add_file(path.clone());
                recent_files.save();

                // Create project context from file path
                if let (Some(parent), Some(file_name)) = (path.parent(), path.file_name()) {
                    ProjectContext::new(parent, file_name.to_string_lossy().to_string())
                } else {
                    ProjectContext::new_unsaved()
                }
            }
        } else {
            ProjectContext::new_unsaved()
        };

        // Rebuild native menu with recent files
        if let Some(ref menu) = native_menu {
            menu.rebuild_recent_menu(&recent_files.files());
        }

        let hash = Self::hash_library(&state.library);
        let prev_library = Arc::clone(&state.library);
        Self {
            port,
            project_context,
            state,
            address_bar: AddressBar::new(),
            viewer_pane: ViewerPane::new(),
            network_view: NetworkView::new(),
            parameters: ParameterPanel::new(),
            animation_bar: AnimationBar::new(),
            node_dialog: NodeSelectionDialog::new(),
            icon_cache: IconCache::new(),
            history: History::new(),
            previous_library_hash: hash,
            previous_library: prev_library,
            previous_selection: SelectionSnapshot::default(),
            render_worker: RenderWorkerHandle::spawn(),
            render_state: RenderState::new(),
            render_pending: true,
            native_menu,
            recent_files,
            pending_connection: None,
            was_dragging: false,
            right_panel_split: 0.35,
        }
    }

    /// Create a new NodeBox application instance for testing.
    ///
    /// This constructor creates an app without spawning a render worker thread,
    /// making it suitable for unit tests and integration tests.
    #[cfg(test)]
    #[allow(dead_code)]
    pub fn new_for_testing() -> Self {
        let state = AppState::new();
        let hash = Self::hash_library(&state.library);
        let prev_library = Arc::clone(&state.library);
        Self {
            port: Arc::new(crate::DesktopPort::new()),
            project_context: ProjectContext::new_unsaved(),
            state,
            address_bar: AddressBar::new(),
            viewer_pane: ViewerPane::new(),
            network_view: NetworkView::new(),
            parameters: ParameterPanel::new(),
            animation_bar: AnimationBar::new(),
            node_dialog: NodeSelectionDialog::new(),
            icon_cache: IconCache::new(),
            history: History::new(),
            previous_library_hash: hash,
            previous_library: prev_library,
            previous_selection: SelectionSnapshot::default(),
            render_worker: RenderWorkerHandle::spawn(),
            render_state: RenderState::new(),
            render_pending: false,
            native_menu: None,
            recent_files: RecentFiles::new(),
            pending_connection: None,
            was_dragging: false,
            right_panel_split: 0.35,
        }
    }

    /// Create a new NodeBox application instance for testing with an empty library.
    ///
    /// This is useful for tests that need to set up their own node configuration.
    #[cfg(test)]
    #[allow(dead_code)]
    pub fn new_for_testing_empty() -> Self {
        let mut state = AppState::new();
        state.library = Arc::new(nodebox_core::node::NodeLibrary::new("test"));
        state.geometry.clear();
        let hash = Self::hash_library(&state.library);
        let prev_library = Arc::clone(&state.library);
        Self {
            port: Arc::new(crate::DesktopPort::new()),
            project_context: ProjectContext::new_unsaved(),
            state,
            address_bar: AddressBar::new(),
            viewer_pane: ViewerPane::new(),
            network_view: NetworkView::new(),
            parameters: ParameterPanel::new(),
            animation_bar: AnimationBar::new(),
            node_dialog: NodeSelectionDialog::new(),
            icon_cache: IconCache::new(),
            history: History::new(),
            previous_library_hash: hash,
            previous_library: prev_library,
            previous_selection: SelectionSnapshot::default(),
            render_worker: RenderWorkerHandle::spawn(),
            render_state: RenderState::new(),
            render_pending: false,
            native_menu: None,
            recent_files: RecentFiles::new(),
            pending_connection: None,
            was_dragging: false,
            right_panel_split: 0.35,
        }
    }

    /// Get a reference to the application state.
    #[allow(dead_code)]
    pub fn state(&self) -> &AppState {
        &self.state
    }

    /// Get a mutable reference to the application state.
    #[allow(dead_code)]
    pub fn state_mut(&mut self) -> &mut AppState {
        &mut self.state
    }

    /// Get a reference to the history manager.
    #[allow(dead_code)]
    pub fn history(&self) -> &History {
        &self.history
    }

    /// Get a mutable reference to the history manager.
    #[allow(dead_code)]
    pub fn history_mut(&mut self) -> &mut History {
        &mut self.history
    }

    /// Get a reference to the Port for file operations.
    #[allow(dead_code)]
    pub fn port(&self) -> &Arc<dyn Port> {
        &self.port
    }

    /// Get a reference to the project context.
    #[allow(dead_code)]
    pub fn project_context(&self) -> &ProjectContext {
        &self.project_context
    }

    /// Synchronously evaluate the network for testing.
    ///
    /// Unlike the normal async flow, this directly evaluates and updates geometry.
    #[cfg(test)]
    #[allow(dead_code)]
    pub fn evaluate_for_testing(&mut self) {
        let (geometry, output, errors) = crate::eval::evaluate_network(
            &self.state.library,
            &self.port,
            &self.project_context,
        );
        if errors.is_empty() {
            self.state.geometry = geometry;
            self.state.node_output = output;
            self.state.node_errors.clear();
        } else {
            self.state.node_errors = errors
                .into_iter()
                .map(|e| (e.node_name, e.message))
                .collect();
        }
    }

    /// Simulate a frame update for testing purposes.
    ///
    /// This checks for changes and updates history, similar to what happens
    /// during a normal frame update, but without the async render worker.
    #[cfg(test)]
    #[allow(dead_code)]
    pub fn update_for_testing(&mut self) {
        // Check for changes and save to history (using previous_library for correct undo)
        let current_hash = Self::hash_library(&self.state.library);
        if current_hash != self.previous_library_hash {
            self.history.save_state(&self.previous_library, &self.previous_selection);
            self.previous_library_hash = current_hash;
            if !self.history.is_in_group() {
                self.previous_library = Arc::clone(&self.state.library);
                self.previous_selection = self.current_selection();
            }
            self.state.dirty = true;
        }
        // Synchronously evaluate using the app's port
        self.evaluate_for_testing();
    }

    /// Compute a simple hash of the library for change detection.
    fn hash_library(library: &nodebox_core::node::NodeLibrary) -> u64 {
        use std::hash::{Hash, Hasher};
        use std::collections::hash_map::DefaultHasher;
        let mut hasher = DefaultHasher::new();

        // Hash the number of children and their names/positions
        library.root.children.len().hash(&mut hasher);
        for child in &library.root.children {
            child.name.hash(&mut hasher);
            (child.position.x as i64).hash(&mut hasher);
            (child.position.y as i64).hash(&mut hasher);
            child.inputs.len().hash(&mut hasher);

            // Hash port values
            for port in &child.inputs {
                port.name.hash(&mut hasher);
                // Hash the value - convert to string representation for simplicity
                format!("{:?}", port.value).hash(&mut hasher);
            }
        }

        // Hash connections
        library.root.connections.len().hash(&mut hasher);
        for conn in &library.root.connections {
            conn.output_node.hash(&mut hasher);
            conn.input_node.hash(&mut hasher);
            conn.input_port.hash(&mut hasher);
        }

        // Hash rendered child
        library.root.rendered_child.hash(&mut hasher);

        hasher.finish()
    }

    /// Poll for render results and dispatch pending renders.
    fn poll_render_results(&mut self) {
        // Check for completed renders
        while let Some(result) = self.render_worker.try_recv_result() {
            match result {
                RenderResult::Success { id, geometry, output, errors } => {
                    if self.render_state.is_current(id) {
                        if errors.is_empty() {
                            // Success with no errors: update geometry and clear errors
                            self.state.geometry = geometry;
                            self.state.node_output = output;
                            self.state.node_errors.clear();
                        } else {
                            // Success with errors: keep last geometry, populate errors
                            self.state.node_errors = errors
                                .into_iter()
                                .map(|e| (e.node_name, e.message))
                                .collect();
                        }
                        self.render_state.complete();
                    }
                }
                RenderResult::Cancelled { id } => {
                    if self.render_state.is_current(id) {
                        // Keep previous geometry visible
                        self.render_state.complete();
                    }
                }
                RenderResult::Error { id, message } => {
                    if self.render_state.is_current(id) {
                        log::error!("Render error: {}", message);
                        // Keep last geometry on complete failure
                        self.render_state.complete();
                    }
                }
            }
        }

        // Dispatch pending render if not already rendering
        if self.render_pending && !self.render_state.is_rendering {
            let (id, cancel_token) = self.render_state.dispatch_new();
            // Update the frame number from the animation bar before rendering
            self.project_context.frame = self.animation_bar.frame();
            self.render_worker.request_render(
                id,
                Arc::clone(&self.state.library),
                cancel_token,
                self.port.clone(),
                self.project_context.clone(),
            );
            self.render_pending = false;
        }
    }

    /// Cancel the current render operation.
    fn cancel_render(&mut self) {
        if self.render_state.is_rendering {
            self.render_state.cancel();
        }
    }

    /// Build a snapshot of the current selection state.
    fn current_selection(&self) -> SelectionSnapshot {
        SelectionSnapshot {
            selected_nodes: self.network_view.selected_nodes().clone(),
            selected_node: self.state.selected_node.clone(),
        }
    }

    /// Apply a restored selection snapshot, validating that referenced nodes
    /// still exist in the current library (prunes stale names).
    fn apply_selection(&mut self, snapshot: SelectionSnapshot) {
        let valid_selected: std::collections::HashSet<String> = snapshot
            .selected_nodes
            .into_iter()
            .filter(|name| self.state.library.root.child(name).is_some())
            .collect();
        let valid_selected_node = snapshot
            .selected_node
            .filter(|name| self.state.library.root.child(name).is_some());
        self.network_view.set_selected(valid_selected);
        self.state.selected_node = valid_selected_node;
    }

    /// Check for changes and save to history, queue render if needed.
    ///
    /// Saves the *previous* library state (from before this frame's mutations)
    /// so that undo restores to the pre-mutation state.
    fn check_for_changes(&mut self) {
        let current_hash = Self::hash_library(&self.state.library);
        if current_hash != self.previous_library_hash {
            self.history.save_state(&self.previous_library, &self.previous_selection);
            self.previous_library_hash = current_hash;
            // Only update previous_library/selection when not in an undo group.
            // During a group, the group_start_state tracks the pre-drag snapshot
            // and previous state must stay frozen so that the next non-grouped
            // change still records the correct pre-mutation state.
            if !self.history.is_in_group() {
                self.previous_library = Arc::clone(&self.state.library);
                self.previous_selection = self.current_selection();
            }
            self.state.dirty = true;
            self.render_pending = true; // Queue async render
        }
    }

    /// Handle a menu action from the native menu bar.
    fn handle_menu_action(&mut self, action: MenuAction, ctx: &egui::Context) {
        match action {
            MenuAction::New => self.state.new_document(),
            MenuAction::Open => self.open_file(),
            MenuAction::OpenRecent(path) => self.open_recent_file(&path),
            MenuAction::ClearRecent => self.clear_recent_files(),
            MenuAction::Save => self.save_file(),
            MenuAction::SaveAs => self.save_file_as(),
            MenuAction::ExportPng => self.export_png(),
            MenuAction::ExportSvg => self.export_svg(),
            MenuAction::Undo => {
                let sel = self.current_selection();
                if let Some((previous, prev_sel)) = self.history.undo(&self.state.library, &sel) {
                    self.state.library = previous;
                    self.previous_library_hash = Self::hash_library(&self.state.library);
                    self.previous_library = Arc::clone(&self.state.library);
                    self.apply_selection(prev_sel);
                    self.previous_selection = self.current_selection();
                    self.render_pending = true;
                }
            }
            MenuAction::Redo => {
                let sel = self.current_selection();
                if let Some((next, next_sel)) = self.history.redo(&self.state.library, &sel) {
                    self.state.library = next;
                    self.previous_library_hash = Self::hash_library(&self.state.library);
                    self.previous_library = Arc::clone(&self.state.library);
                    self.apply_selection(next_sel);
                    self.previous_selection = self.current_selection();
                    self.render_pending = true;
                }
            }
            MenuAction::ZoomIn => self.viewer_pane.zoom_in(),
            MenuAction::ZoomOut => self.viewer_pane.zoom_out(),
            MenuAction::ZoomReset => self.viewer_pane.reset_zoom(),
            MenuAction::About => self.state.show_about = true,
            // Clipboard actions handled by system
            MenuAction::Cut | MenuAction::Copy | MenuAction::Paste |
            MenuAction::Delete | MenuAction::SelectAll => {}
        }
        ctx.request_repaint();
    }

    /// Show the menu bar.
    #[cfg(not(target_os = "macos"))]
    fn show_menu_bar(&mut self, ui: &mut egui::Ui, ctx: &egui::Context) {
        // Collect recent files to avoid borrow issues
        let recent_files_list = self.recent_files.files();

        egui::MenuBar::new().ui(ui, |ui| {
            ui.menu_button("File", |ui| {
                if ui.button("New").clicked() {
                    self.state.new_document();
                    ui.close();
                }
                if ui.button("Open...").clicked() {
                    self.open_file();
                    ui.close();
                }
                ui.menu_button("Open Recent", |ui| {
                    if recent_files_list.is_empty() {
                        ui.label("No recent files");
                    } else {
                        // Store the path to open (if any) to avoid borrow issues
                        let mut path_to_open = None;
                        for path in &recent_files_list {
                            let display_name = path
                                .file_name()
                                .and_then(|n| n.to_str())
                                .unwrap_or("Unknown");
                            if ui.button(display_name).clicked() {
                                path_to_open = Some(path.clone());
                                ui.close();
                            }
                        }
                        if let Some(path) = path_to_open {
                            self.open_recent_file(&path);
                        }
                        ui.separator();
                    }
                    if ui.add_enabled(!recent_files_list.is_empty(), egui::Button::new("Clear Recent")).clicked() {
                        self.clear_recent_files();
                        ui.close();
                    }
                });
                if ui.button("Save").clicked() {
                    self.save_file();
                    ui.close();
                }
                if ui.button("Save As...").clicked() {
                    self.save_file_as();
                    ui.close();
                }
                ui.separator();
                if ui.button("Export SVG...").clicked() {
                    self.export_svg();
                    ui.close();
                }
                if ui.button("Export PNG...").clicked() {
                    self.export_png();
                    ui.close();
                }
                ui.separator();
                if ui.button("Quit").clicked() {
                    ctx.send_viewport_cmd(egui::ViewportCommand::Close);
                }
            });

            ui.menu_button("Edit", |ui| {
                let undo_text = if self.history.can_undo() {
                    format!("Undo ({})", self.history.undo_count())
                } else {
                    "Undo".to_string()
                };
                if ui.add_enabled(self.history.can_undo(), egui::Button::new(undo_text)).clicked() {
                    let sel = self.current_selection();
                    if let Some((previous, prev_sel)) = self.history.undo(&self.state.library, &sel) {
                        self.state.library = previous;
                        self.previous_library_hash = Self::hash_library(&self.state.library);
                        self.previous_library = Arc::clone(&self.state.library);
                        self.apply_selection(prev_sel);
                        self.previous_selection = self.current_selection();
                        self.render_pending = true;
                    }
                    ui.close();
                }
                let redo_text = if self.history.can_redo() {
                    format!("Redo ({})", self.history.redo_count())
                } else {
                    "Redo".to_string()
                };
                if ui.add_enabled(self.history.can_redo(), egui::Button::new(redo_text)).clicked() {
                    let sel = self.current_selection();
                    if let Some((next, next_sel)) = self.history.redo(&self.state.library, &sel) {
                        self.state.library = next;
                        self.previous_library_hash = Self::hash_library(&self.state.library);
                        self.previous_library = Arc::clone(&self.state.library);
                        self.apply_selection(next_sel);
                        self.previous_selection = self.current_selection();
                        self.render_pending = true;
                    }
                    ui.close();
                }
                ui.separator();
                if ui.button("Delete Selected").clicked() {
                    ui.close();
                }
            });

            ui.menu_button("View", |ui| {
                if ui.button("Zoom In").clicked() {
                    self.viewer_pane.zoom_in();
                    ui.close();
                }
                if ui.button("Zoom Out").clicked() {
                    self.viewer_pane.zoom_out();
                    ui.close();
                }
                if ui.button("Fit to Window").clicked() {
                    self.viewer_pane.fit_to_window();
                    ui.close();
                }
                ui.separator();
                ui.checkbox(&mut self.viewer_pane.show_handles, "Show Handles");
                ui.checkbox(&mut self.viewer_pane.show_points, "Show Points");
                ui.checkbox(&mut self.viewer_pane.show_origin, "Show Origin");
                ui.checkbox(&mut self.viewer_pane.show_canvas_border, "Show Canvas");
            });

            ui.menu_button("Help", |ui| {
                if ui.button("About NodeBox").clicked() {
                    self.state.show_about = true;
                    ui.close();
                }
            });
        });
    }
}

impl eframe::App for NodeBoxApp {
    #[allow(unused_variables)]
    fn update(&mut self, ctx: &egui::Context, frame: &mut eframe::Frame) {
        // Poll for native menu events (macOS system menu bar)
        if let Some(ref native_menu) = self.native_menu {
            if let Some(action) = native_menu.poll_event() {
                self.handle_menu_action(action, ctx);
            }
        }

        // Poll for background render results
        self.poll_render_results();

        // Request repaint while rendering is in progress
        if self.render_state.is_rendering || self.render_pending {
            ctx.request_repaint();
        }

        // 1. Menu bar (top-most) - only show in-window menu on non-macOS platforms
        #[cfg(not(target_os = "macos"))]
        egui::TopBottomPanel::top("menu_bar")
            .frame(egui::Frame::NONE.fill(theme::PANEL_BG))
            .show(ctx, |ui| {
                self.show_menu_bar(ui, ctx);
            });

        // 2. Address bar (below menu) - frameless, handles its own styling
        egui::TopBottomPanel::top("address_bar")
            .exact_height(theme::ADDRESS_BAR_HEIGHT)
            .frame(egui::Frame::NONE)
            .show(ctx, |ui| {
                // Update render state for stop button
                let elapsed_secs = self.render_state.elapsed()
                    .map(|d| d.as_secs_f32())
                    .unwrap_or(0.0);
                self.address_bar.set_render_state(
                    self.render_state.is_rendering,
                    elapsed_secs,
                );

                match self.address_bar.show(ui) {
                    AddressBarAction::NavigateTo(_path) => {
                        // Future: navigate to sub-network
                    }
                    AddressBarAction::StopClicked => {
                        self.cancel_render();
                    }
                    AddressBarAction::None => {}
                }
            });

        // 2b. Notification banners (below address bar, only shown when notifications exist)
        if !self.state.notifications.is_empty() {
            let banner_height = notification_banner::BANNER_HEIGHT
                * self.state.notifications.len() as f32;
            egui::TopBottomPanel::top("notification_banners")
                .exact_height(banner_height)
                .frame(egui::Frame::NONE)
                .show(ctx, |ui| {
                    let notifs: Vec<_> = self.state.notifications
                        .iter()
                        .map(|n| (n.id, n.message.clone(), n.level.clone()))
                        .collect();

                    let dismissed = notification_banner::show_notifications(ui, &notifs);

                    for id in dismissed {
                        self.state.dismiss_notification(id);
                    }
                });
        }

        // 3. Animation bar (bottom) - frameless, handles its own styling
        let anim_response = egui::TopBottomPanel::bottom("animation_bar")
            .exact_height(theme::ANIMATION_BAR_HEIGHT)
            .frame(egui::Frame::NONE)
            .show(ctx, |ui| {
                self.animation_bar.show(ui)
            });
        match anim_response.inner {
            AnimationEvent::FrameChanged(_)
            | AnimationEvent::Rewind
            | AnimationEvent::Stop => {
                self.render_pending = true;
            }
            _ => {}
        }

        // Update animation playback
        if self.animation_bar.is_playing() {
            if self.animation_bar.update() {
                self.render_pending = true;
            }
            ctx.request_repaint();
        }

        // Capture pre-frame library state and selection for undo group detection.
        // These are cheap clones (Arc pointer + small HashSet).
        let pre_frame_library = Arc::clone(&self.state.library);
        let pre_frame_selection = self.current_selection();

        // 4. Right side panel containing Parameters (top) and Network (bottom)
        //
        // Style the built-in separator: egui uses noninteractive.bg_stroke (normal),
        // hovered.fg_stroke (hover), and active.fg_stroke (dragging).
        ctx.style_mut(|style| {
            style.visuals.widgets.noninteractive.bg_stroke = egui::Stroke::new(2.0, theme::ZINC_900);
            style.visuals.widgets.hovered.fg_stroke = egui::Stroke::new(2.0, theme::ZINC_700);
            style.visuals.widgets.active.fg_stroke = egui::Stroke::new(2.0, theme::ZINC_300);
        });

        egui::SidePanel::right("right_panel")
            .default_width(450.0)
            .min_width(300.0)
            .resizable(true)
            .frame(egui::Frame::NONE.fill(theme::PANEL_BG))
            .show(ctx, |ui| {
                // Remove default spacing to have tighter control
                ui.spacing_mut().item_spacing = egui::vec2(0.0, 0.0);

                let available = ui.available_rect_before_wrap();

                // Enforce minimum heights: each panel gets at least 80px
                let min_panel_height = 80.0_f32;
                let min_ratio = min_panel_height / available.height();
                let max_ratio = 1.0 - min_ratio;
                self.right_panel_split = self.right_panel_split.clamp(min_ratio, max_ratio);

                let split_y = available.min.y + available.height() * self.right_panel_split;

                // Top: Parameters pane
                let params_rect = Rect::from_min_max(
                    available.min,
                    Pos2::new(available.max.x, split_y),
                );

                ui.scope_builder(egui::UiBuilder::new().max_rect(params_rect), |ui| {
                    ui.set_clip_rect(params_rect);
                    self.parameters
                        .show(ui, &mut self.state, self.port.as_ref(), &self.project_context);
                });

                // Horizontal splitter between Parameters and Network.
                // The interaction zone overlaps both panels so there is no visible gap.
                let half = theme::SPLITTER_AFFORDANCE / 2.0;
                let splitter_rect = Rect::from_min_max(
                    Pos2::new(available.min.x, split_y - half),
                    Pos2::new(available.max.x, split_y + half),
                );

                let splitter_id = ui.id().with("params_network_splitter");
                let splitter_response = ui.interact(splitter_rect, splitter_id, egui::Sense::drag());

                let is_active = splitter_response.dragged();
                let is_hovered = splitter_response.hovered();

                // Draw splitter line at the boundary
                let stroke_color = if is_active {
                    theme::ZINC_300
                } else if is_hovered {
                    theme::ZINC_400
                } else {
                    theme::ZINC_600
                };
                ui.painter().line_segment(
                    [
                        Pos2::new(available.min.x, split_y),
                        Pos2::new(available.max.x, split_y),
                    ],
                    egui::Stroke::new(theme::SPLITTER_THICKNESS, stroke_color),
                );

                if is_hovered || is_active {
                    ui.ctx().set_cursor_icon(egui::CursorIcon::ResizeVertical);
                }

                if splitter_response.dragged() {
                    if let Some(pointer_pos) = ui.ctx().pointer_latest_pos() {
                        let new_ratio = (pointer_pos.y - available.min.y) / available.height();
                        self.right_panel_split = new_ratio.clamp(min_ratio, max_ratio);
                    }
                }

                // Bottom: Network pane
                let network_rect = Rect::from_min_max(
                    Pos2::new(available.min.x, split_y),
                    available.max,
                );

                ui.scope_builder(egui::UiBuilder::new().max_rect(network_rect), |ui| {
                    ui.set_clip_rect(network_rect);

                    // Network header with "+ New Node" button
                    let (header_rect, x) = components::draw_pane_header_with_title(ui, "Network");

                    // "+ New Node" button after the separator
                    let (clicked, _) = components::header_text_button(
                        ui,
                        header_rect,
                        x,
                        "+ New Node",
                        70.0,
                    );

                    if clicked {
                        self.node_dialog.open(Point::new(0.0, 0.0));
                    }

                    // Network view
                    let action = self.network_view.show(ui, &mut self.state.library, &self.state.node_errors);

                    // Handle network actions
                    match action {
                        NetworkAction::OpenNodeDialog(pos) => {
                            self.node_dialog.open(pos);
                        }
                        NetworkAction::OpenNodeDialogForConnection { position, from_node, output_type } => {
                            self.node_dialog.open_for_connection(position, output_type.clone());
                            self.pending_connection = Some((from_node, output_type));
                        }
                        NetworkAction::None => {}
                    }

                    // Update selected node from network view
                    let selected = self.network_view.selected_nodes();
                    if selected.len() == 1 {
                        self.state.selected_node = selected.iter().next().cloned();
                    } else if selected.is_empty() {
                        self.state.selected_node = None;
                    } else if !self.state.selected_node.as_ref().is_some_and(|n| selected.contains(n)) {
                        // Multiple selected but current isn't among them (e.g., after alt-drag copy)
                        self.state.selected_node = selected.iter().next().cloned();
                    }
                });
            });

        // Restore widget strokes for the rest of the UI
        ctx.style_mut(|style| {
            style.visuals.widgets.noninteractive.bg_stroke = egui::Stroke::NONE;
            style.visuals.widgets.hovered.fg_stroke = egui::Stroke::new(1.0, theme::ZINC_50);
            style.visuals.widgets.active.fg_stroke = egui::Stroke::new(1.0, theme::ZINC_50);
        });

        // 5. Central panel: Viewer (left side, takes remaining space) - clean frame
        egui::CentralPanel::default()
            .frame(egui::Frame::NONE.fill(theme::PANEL_BG))
            .show(ctx, |ui| {
                // Update handles for selected node
                self.viewer_pane.update_handles_for_node(
                    self.state.selected_node.as_deref(),
                    &self.state,
                );

                // Show viewer and handle interactions
                // Get wgpu render state for GPU-accelerated rendering (when available)
                #[cfg(feature = "gpu-rendering")]
                let render_state = frame.wgpu_render_state();
                #[cfg(not(feature = "gpu-rendering"))]
                let render_state: Option<&crate::viewer_pane::RenderState> = None;

                let result = self.viewer_pane.show(ui, &self.state, render_state);
                match result {
                    HandleResult::PointChange { param, value } => {
                        self.handle_parameter_change(&param, value);
                        self.render_pending = true;
                    }
                    HandleResult::FourPointChange { x, y, width, height } => {
                        self.handle_four_point_change(x, y, width, height);
                        self.render_pending = true;
                    }
                    HandleResult::StringChange { param, value } => {
                        self.handle_string_change(&param, &value);
                        self.render_pending = true;
                    }
                    HandleResult::None => {}
                }
            });

        // 6. Node selection dialog
        if self.node_dialog.visible {
            if let Some(new_node) = self.node_dialog.show(ctx, &self.state.library, &mut self.icon_cache) {
                let node_name = new_node.name.clone();

                // Find the first compatible input port for any pending connection
                let connection_to_create = self.pending_connection.take().and_then(|(from_node, output_type)| {
                    new_node.inputs.iter()
                        .find(|p| PortType::is_compatible(&output_type, &p.port_type))
                        .map(|p| (from_node, p.name.clone()))
                });

                Arc::make_mut(&mut self.state.library).root.children.push(new_node);
                self.state.selected_node = Some(node_name.clone());

                // Create the auto-connection if drag-to-create was used
                if let Some((from_node, port_name)) = connection_to_create {
                    Arc::make_mut(&mut self.state.library)
                        .root
                        .connect(Connection::new(from_node, node_name, port_name));
                }
            }
        }

        // Clear pending connection if dialog was dismissed
        if !self.node_dialog.visible {
            self.pending_connection = None;
        }

        // 7. About dialog
        if self.state.show_about {
            egui::Window::new("About NodeBox")
                .collapsible(false)
                .resizable(false)
                .anchor(egui::Align2::CENTER_CENTER, [0.0, 0.0])
                .show(ctx, |ui| {
                    ui.vertical_centered(|ui| {
                        ui.heading("NodeBox");
                        ui.label("Version 4.0 (Rust)");
                        ui.add_space(10.0);
                        ui.label("A node-based generative design tool");
                        ui.add_space(10.0);
                        ui.hyperlink_to("Visit website", "https://www.nodebox.net");
                        ui.add_space(10.0);
                        if ui.button("Close").clicked() {
                            self.state.show_about = false;
                        }
                    });
                });
        }

        // Handle keyboard shortcuts
        let (do_undo, do_redo, do_cancel) = ctx.input(|i| {
            let undo = i.modifiers.command && i.key_pressed(egui::Key::Z) && !i.modifiers.shift;
            let redo = (i.modifiers.command && i.modifiers.shift && i.key_pressed(egui::Key::Z))
                || (i.modifiers.command && i.key_pressed(egui::Key::Y));
            // Cmd/Ctrl + . to cancel rendering
            let cancel = i.modifiers.command && i.key_pressed(egui::Key::Period);
            (undo, redo, cancel)
        });

        if do_cancel {
            self.cancel_render();
        }

        if do_undo {
            let sel = self.current_selection();
            if let Some((previous, prev_sel)) = self.history.undo(&self.state.library, &sel) {
                self.state.library = previous;
                self.previous_library_hash = Self::hash_library(&self.state.library);
                self.previous_library = Arc::clone(&self.state.library);
                self.apply_selection(prev_sel);
                self.previous_selection = self.current_selection();
                self.render_pending = true;
            }
        }
        if do_redo {
            let sel = self.current_selection();
            if let Some((next, next_sel)) = self.history.redo(&self.state.library, &sel) {
                self.state.library = next;
                self.previous_library_hash = Self::hash_library(&self.state.library);
                self.previous_library = Arc::clone(&self.state.library);
                self.apply_selection(next_sel);
                self.previous_selection = self.current_selection();
                self.render_pending = true;
            }
        }

        // Detect drag transitions for undo grouping.
        // When a drag starts, begin an undo group so all intermediate changes
        // are collapsed into a single undo entry. When the drag ends, close the group.
        let is_dragging = self.viewer_pane.is_dragging()
            || self.network_view.is_dragging_nodes()
            || self.parameters.is_dragging();

        if is_dragging && !self.was_dragging {
            // Drag just started — begin undo group with the pre-frame state
            // (captured before any panel mutations this frame).
            self.history.begin_undo_group(&pre_frame_library, &pre_frame_selection);
        }
        if !is_dragging && self.was_dragging {
            // Drag just ended — close the group (creates a single undo entry).
            self.history.end_undo_group(&self.state.library);
            // Update hash and snapshot so check_for_changes doesn't create a duplicate entry.
            self.previous_library_hash = Self::hash_library(&self.state.library);
            self.previous_library = Arc::clone(&self.state.library);
            self.previous_selection = self.current_selection();
        }
        self.was_dragging = is_dragging;

        // Check for state changes and save to history
        self.check_for_changes();

        // Request repaint if a change was detected (ensures next frame runs to dispatch render)
        if self.render_pending {
            ctx.request_repaint();
        }
    }
}

impl NodeBoxApp {
    /// Handle FourPointHandle change (rect x, y, width, height).
    fn handle_four_point_change(&mut self, x: f64, y: f64, width: f64, height: f64) {
        if let Some(ref node_name) = self.state.selected_node {
            if let Some(node) = Arc::make_mut(&mut self.state.library).root.child_mut(node_name) {
                // Write to "position" Point port (per corevector.ndbx)
                if let Some(port) = node.input_mut("position") {
                    port.value = nodebox_core::Value::Point(Point::new(x, y));
                }
                if let Some(port) = node.input_mut("width") {
                    port.value = nodebox_core::Value::Float(width);
                }
                if let Some(port) = node.input_mut("height") {
                    port.value = nodebox_core::Value::Float(height);
                }
            }
        }
    }

    /// Handle string parameter change from freehand handle.
    fn handle_string_change(&mut self, param_name: &str, value: &str) {
        if let Some(ref node_name) = self.state.selected_node {
            if let Some(node) = Arc::make_mut(&mut self.state.library).root.child_mut(node_name) {
                if let Some(port) = node.input_mut(param_name) {
                    port.value = nodebox_core::Value::String(value.to_string());
                }
            }
        }
    }

    /// Handle parameter change from viewer handles.
    fn handle_parameter_change(&mut self, param_name: &str, new_position: Point) {
        if let Some(ref node_name) = self.state.selected_node {
            if let Some(node) = Arc::make_mut(&mut self.state.library).root.child_mut(node_name) {
                match param_name {
                    "position" => {
                        // Write to "position" Point port (per corevector.ndbx)
                        if let Some(port) = node.input_mut("position") {
                            port.value = nodebox_core::Value::Point(new_position);
                        }
                    }
                    "width" => {
                        // Get center from position port
                        let center_x = node.input("position")
                            .and_then(|p| p.value.as_point().cloned())
                            .map(|p| p.x)
                            .unwrap_or(0.0);
                        let new_width = (new_position.x - center_x) * 2.0;
                        if let Some(width_port) = node.input_mut("width") {
                            width_port.value = nodebox_core::Value::Float(new_width.abs());
                        }
                    }
                    "height" => {
                        // Get center from position port
                        let center_y = node.input("position")
                            .and_then(|p| p.value.as_point().cloned())
                            .map(|p| p.y)
                            .unwrap_or(0.0);
                        let new_height = (new_position.y - center_y) * 2.0;
                        if let Some(height_port) = node.input_mut("height") {
                            height_port.value = nodebox_core::Value::Float(new_height.abs());
                        }
                    }
                    "size" => {
                        // Get center from position port
                        let center = node.input("position")
                            .and_then(|p| p.value.as_point().cloned())
                            .unwrap_or(Point::ZERO);
                        if let Some(width_port) = node.input_mut("width") {
                            width_port.value = nodebox_core::Value::Float((new_position.x - center.x).abs());
                        }
                        if let Some(height_port) = node.input_mut("height") {
                            height_port.value = nodebox_core::Value::Float((new_position.y - center.y).abs());
                        }
                    }
                    "point1" | "point2" => {
                        if let Some(port) = node.input_mut(param_name) {
                            port.value = nodebox_core::Value::Point(new_position);
                        }
                    }
                    _ => {}
                }
            }
        }
    }

    fn open_file(&mut self) {
        use nodebox_core::port::FileFilter;

        match self.port.show_open_project_dialog(&[FileFilter::nodebox()]) {
            Ok(Some(path)) => {
                if let Err(e) = self.state.load_file(&path) {
                    log::error!("Failed to load file: {}", e);
                } else {
                    // Update project context with new file location
                    if let (Some(parent), Some(file_name)) = (path.parent(), path.file_name()) {
                        self.project_context =
                            ProjectContext::new(parent, file_name.to_string_lossy().to_string());
                    }
                    self.add_to_recent_files(path);
                }
            }
            Ok(None) => {} // User cancelled
            Err(e) => log::error!("Failed to open file dialog: {}", e),
        }
    }

    /// Open a file from the recent files list.
    fn open_recent_file(&mut self, path: &std::path::Path) {
        if let Err(e) = self.state.load_file(path) {
            log::error!("Failed to load recent file: {}", e);
        } else {
            // Update project context with new file location
            if let (Some(parent), Some(file_name)) = (path.parent(), path.file_name()) {
                self.project_context =
                    ProjectContext::new(parent, file_name.to_string_lossy().to_string());
            }
            self.add_to_recent_files(path.to_path_buf());
        }
    }

    /// Add a file to the recent files list and update the menu.
    fn add_to_recent_files(&mut self, path: std::path::PathBuf) {
        self.recent_files.add_file(path);
        self.recent_files.save();
        if let Some(ref menu) = self.native_menu {
            menu.rebuild_recent_menu(&self.recent_files.files());
        }
    }

    /// Clear all recent files.
    fn clear_recent_files(&mut self) {
        self.recent_files.clear();
        self.recent_files.save();
        if let Some(ref menu) = self.native_menu {
            menu.rebuild_recent_menu(&self.recent_files.files());
        }
    }

    fn save_file(&mut self) {
        if let Some(ref path) = self.state.current_file.clone() {
            if let Err(e) = self.state.save_file(path) {
                log::error!("Failed to save file: {}", e);
            }
        } else {
            self.save_file_as();
        }
    }

    fn save_file_as(&mut self) {
        use nodebox_core::port::FileFilter;

        match self.port.show_save_project_dialog(&[FileFilter::nodebox()], Some("untitled.ndbx")) {
            Ok(Some(path)) => {
                if let Err(e) = self.state.save_file(&path) {
                    log::error!("Failed to save file: {}", e);
                } else {
                    // Update project context with new file location
                    if let (Some(parent), Some(file_name)) = (path.parent(), path.file_name()) {
                        self.project_context =
                            ProjectContext::new(parent, file_name.to_string_lossy().to_string());
                    }
                    self.add_to_recent_files(path);
                }
            }
            Ok(None) => {} // User cancelled
            Err(e) => log::error!("Failed to open save dialog: {}", e),
        }
    }

    fn export_svg(&mut self) {
        use nodebox_core::port::FileFilter;

        // Export dialogs use save_project_dialog since exports are not sandboxed
        match self.port.show_save_project_dialog(&[FileFilter::svg()], Some("export.svg")) {
            Ok(Some(path)) => {
                // Use document dimensions for export
                let width = self.state.library.width();
                let height = self.state.library.height();
                if let Err(e) = self.state.export_svg(&path, width, height) {
                    log::error!("Failed to export SVG: {}", e);
                }
            }
            Ok(None) => {} // User cancelled
            Err(e) => log::error!("Failed to open export dialog: {}", e),
        }
    }

    fn export_png(&mut self) {
        use nodebox_core::port::FileFilter;

        // Export dialogs use save_project_dialog since exports are not sandboxed
        match self.port.show_save_project_dialog(&[FileFilter::png()], Some("export.png")) {
            Ok(Some(path)) => {
                // Use document dimensions for export
                let width = self.state.library.width() as u32;
                let height = self.state.library.height() as u32;

                if let Err(e) = crate::export::export_png(
                    &self.state.geometry,
                    &path,
                    width,
                    height,
                    self.state.background_color,
                ) {
                    log::error!("Failed to export PNG: {}", e);
                }
            }
            Ok(None) => {} // User cancelled
            Err(e) => log::error!("Failed to open export dialog: {}", e),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use nodebox_core::node::{Node, Port};

    /// Test that simulates what happens when handle_result_point_change is processed.
    /// This mimics the behavior in the UI loop when HandleResult::PointChange is received.
    #[test]
    fn test_handle_point_change_triggers_render() {
        let mut app = NodeBoxApp::new_for_testing();

        // Set up a node with a position parameter
        Arc::make_mut(&mut app.state.library).root.children.push(
            Node::new("ellipse1")
                .with_prototype("corevector.ellipse")
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::float("width", 100.0))
                .with_input(Port::float("height", 100.0)),
        );
        Arc::make_mut(&mut app.state.library).root.rendered_child = Some("ellipse1".to_string());
        app.state.selected_node = Some("ellipse1".to_string());

        // Reset render_pending to false
        app.render_pending = false;

        // Simulate what happens in the UI loop when a handle is dragged.
        // This mimics the code in the `match result` block for HandleResult::PointChange.
        let result = HandleResult::PointChange {
            param: "position".to_string(),
            value: Point::new(50.0, 50.0),
        };

        match result {
            HandleResult::PointChange { param, value } => {
                app.handle_parameter_change(&param, value);
                app.render_pending = true; // This is the fix!
            }
            HandleResult::FourPointChange { x, y, width, height } => {
                app.handle_four_point_change(x, y, width, height);
                app.render_pending = true;
            }
            HandleResult::StringChange { param, value } => {
                app.handle_string_change(&param, &value);
                app.render_pending = true;
            }
            HandleResult::None => {}
        }

        // After the fix, render_pending should be true immediately
        assert!(
            app.render_pending,
            "Handle change should trigger render immediately"
        );
    }

    /// Test that simulates what happens when HandleResult::FourPointChange is processed.
    /// This mimics the behavior in the UI loop when a rectangle handle is dragged.
    #[test]
    fn test_handle_four_point_change_triggers_render() {
        let mut app = NodeBoxApp::new_for_testing();

        // Set up a node with position and size parameters
        Arc::make_mut(&mut app.state.library).root.children.push(
            Node::new("rect1")
                .with_prototype("corevector.rect")
                .with_input(Port::float("x", 0.0))
                .with_input(Port::float("y", 0.0))
                .with_input(Port::float("width", 100.0))
                .with_input(Port::float("height", 100.0)),
        );
        Arc::make_mut(&mut app.state.library).root.rendered_child = Some("rect1".to_string());
        app.state.selected_node = Some("rect1".to_string());

        // Reset render_pending to false
        app.render_pending = false;

        // Simulate what happens in the UI loop when a four-point handle is dragged.
        let result = HandleResult::FourPointChange {
            x: 50.0,
            y: 50.0,
            width: 200.0,
            height: 200.0,
        };

        match result {
            HandleResult::PointChange { param, value } => {
                app.handle_parameter_change(&param, value);
                app.render_pending = true;
            }
            HandleResult::FourPointChange { x, y, width, height } => {
                app.handle_four_point_change(x, y, width, height);
                app.render_pending = true; // This is the fix!
            }
            HandleResult::StringChange { param, value } => {
                app.handle_string_change(&param, &value);
                app.render_pending = true;
            }
            HandleResult::None => {}
        }

        // After the fix, render_pending should be true immediately
        assert!(
            app.render_pending,
            "Four-point handle change should trigger render immediately"
        );
    }

    /// Test that parameter changes via check_for_changes() properly set render_pending.
    /// This verifies the core fix: when a parameter is modified and check_for_changes()
    /// detects the hash mismatch, render_pending should be set to true.
    #[test]
    fn test_parameter_change_sets_render_pending_via_check_for_changes() {
        let mut app = NodeBoxApp::new_for_testing();

        // Set up a node with a width parameter
        Arc::make_mut(&mut app.state.library).root.children.push(
            Node::new("rect1")
                .with_prototype("corevector.rect")
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::float("width", 100.0))
                .with_input(Port::float("height", 100.0)),
        );
        Arc::make_mut(&mut app.state.library).root.rendered_child = Some("rect1".to_string());

        // Update the hash to match current state
        app.previous_library_hash = NodeBoxApp::hash_library(&app.state.library);
        app.previous_library = Arc::clone(&app.state.library);
        app.render_pending = false;

        // Modify the width parameter (simulates what happens when user changes value in panel)
        if let Some(node) = Arc::make_mut(&mut app.state.library).root.child_mut("rect1") {
            if let Some(port) = node.input_mut("width") {
                port.value = nodebox_core::Value::Float(200.0);
            }
        }

        // Call check_for_changes (this is what the update() loop calls at the end)
        app.check_for_changes();

        // After check_for_changes detects the hash mismatch, render_pending should be true
        assert!(
            app.render_pending,
            "check_for_changes() should set render_pending = true when parameter changes"
        );

        // Also verify the hash was updated
        let new_hash = NodeBoxApp::hash_library(&app.state.library);
        assert_eq!(
            app.previous_library_hash, new_hash,
            "previous_library_hash should be updated after check_for_changes()"
        );
    }

    /// Test the full flow: parameter change → check_for_changes → evaluate → geometry update.
    #[test]
    fn test_parameter_change_triggers_render_and_updates_geometry() {
        let mut app = NodeBoxApp::new_for_testing();

        // Set up a rect node
        Arc::make_mut(&mut app.state.library).root.children.push(
            Node::new("rect1")
                .with_prototype("corevector.rect")
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::float("width", 100.0))
                .with_input(Port::float("height", 100.0)),
        );
        Arc::make_mut(&mut app.state.library).root.rendered_child = Some("rect1".to_string());

        // Initial evaluation
        app.update_for_testing();
        let initial_geometry = app.state.geometry.clone();

        // Change width parameter
        if let Some(node) = Arc::make_mut(&mut app.state.library).root.child_mut("rect1") {
            if let Some(port) = node.input_mut("width") {
                port.value = nodebox_core::Value::Float(200.0);
            }
        }

        // Simulate frame update (should detect change and re-evaluate)
        app.update_for_testing();

        // Geometry should have changed
        assert_ne!(
            app.state.geometry, initial_geometry,
            "Geometry should update after parameter change"
        );
    }

    /// Test that a simulated drag gesture creates only a single undo entry.
    /// This simulates the full drag lifecycle: begin group, mutate across multiple
    /// frames (calling check_for_changes each time), end group.
    #[test]
    fn test_drag_creates_single_undo_entry() {
        let mut app = NodeBoxApp::new_for_testing();

        // Set up a node with a width parameter
        Arc::make_mut(&mut app.state.library).root.children.push(
            Node::new("rect1")
                .with_prototype("corevector.rect")
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::float("width", 100.0))
                .with_input(Port::float("height", 100.0)),
        );
        Arc::make_mut(&mut app.state.library).root.rendered_child = Some("rect1".to_string());
        app.previous_library_hash = NodeBoxApp::hash_library(&app.state.library);
        app.previous_library = Arc::clone(&app.state.library);

        assert_eq!(app.history.undo_count(), 0);

        // Simulate drag start: capture pre-drag state
        let pre_drag_library = Arc::clone(&app.state.library);
        let sel = SelectionSnapshot::default();
        app.history.begin_undo_group(&pre_drag_library, &sel);

        // Simulate 10 frames of dragging (each mutates the library)
        for i in 1..=10 {
            let new_width = 100.0 + (i as f64 * 10.0);
            if let Some(node) = Arc::make_mut(&mut app.state.library).root.child_mut("rect1") {
                if let Some(port) = node.input_mut("width") {
                    port.value = nodebox_core::Value::Float(new_width);
                }
            }
            // check_for_changes should NOT create undo entries during group
            app.check_for_changes();
        }

        // During drag: no undo entries should have been created
        assert_eq!(app.history.undo_count(), 0, "No undo entries during active group");

        // Simulate drag end
        app.history.end_undo_group(&app.state.library);
        app.previous_library_hash = NodeBoxApp::hash_library(&app.state.library);
        app.previous_library = Arc::clone(&app.state.library);

        // Exactly one undo entry should exist (the group)
        assert_eq!(app.history.undo_count(), 1, "Drag should create exactly one undo entry");
    }

    /// Test that undoing a drag restores the pre-drag state.
    #[test]
    fn test_drag_undo_restores_pre_drag_state() {
        let mut app = NodeBoxApp::new_for_testing();

        // Set up a node with width=100
        Arc::make_mut(&mut app.state.library).root.children.push(
            Node::new("rect1")
                .with_prototype("corevector.rect")
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::float("width", 100.0))
                .with_input(Port::float("height", 100.0)),
        );
        Arc::make_mut(&mut app.state.library).root.rendered_child = Some("rect1".to_string());
        app.previous_library_hash = NodeBoxApp::hash_library(&app.state.library);
        app.previous_library = Arc::clone(&app.state.library);

        // Simulate drag: width goes from 100 → 200
        let pre_drag = Arc::clone(&app.state.library);
        let sel = SelectionSnapshot::default();
        app.history.begin_undo_group(&pre_drag, &sel);

        for i in 1..=10 {
            let new_width = 100.0 + (i as f64 * 10.0);
            if let Some(node) = Arc::make_mut(&mut app.state.library).root.child_mut("rect1") {
                if let Some(port) = node.input_mut("width") {
                    port.value = nodebox_core::Value::Float(new_width);
                }
            }
            app.check_for_changes();
        }

        app.history.end_undo_group(&app.state.library);

        // Current width should be 200
        let current_width = app.state.library.root.child("rect1").unwrap()
            .input("width").unwrap().value.as_float().unwrap();
        assert!((current_width - 200.0).abs() < 0.001);

        // Undo should restore to width=100
        let sel = SelectionSnapshot::default();
        if let Some((restored, _)) = app.history.undo(&app.state.library, &sel) {
            app.state.library = restored;
        }
        let restored_width = app.state.library.root.child("rect1").unwrap()
            .input("width").unwrap().value.as_float().unwrap();
        assert!((restored_width - 100.0).abs() < 0.001,
            "Expected width=100 after undo, got {}", restored_width);
    }

    /// Test that non-drag changes still create individual undo entries.
    #[test]
    fn test_non_drag_changes_still_create_undo_entries() {
        let mut app = NodeBoxApp::new_for_testing();

        Arc::make_mut(&mut app.state.library).root.children.push(
            Node::new("rect1")
                .with_prototype("corevector.rect")
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::float("width", 100.0))
                .with_input(Port::float("height", 100.0)),
        );
        Arc::make_mut(&mut app.state.library).root.rendered_child = Some("rect1".to_string());
        app.previous_library_hash = NodeBoxApp::hash_library(&app.state.library);
        app.previous_library = Arc::clone(&app.state.library);

        // Make two separate changes (not grouped)
        if let Some(node) = Arc::make_mut(&mut app.state.library).root.child_mut("rect1") {
            if let Some(port) = node.input_mut("width") {
                port.value = nodebox_core::Value::Float(150.0);
            }
        }
        app.check_for_changes();

        if let Some(node) = Arc::make_mut(&mut app.state.library).root.child_mut("rect1") {
            if let Some(port) = node.input_mut("width") {
                port.value = nodebox_core::Value::Float(200.0);
            }
        }
        app.check_for_changes();

        // Should have 2 separate undo entries
        assert_eq!(app.history.undo_count(), 2,
            "Non-drag changes should create separate undo entries");
    }

    /// Test that a drag followed by a normal change creates 2 undo entries.
    #[test]
    fn test_drag_then_normal_change() {
        let mut app = NodeBoxApp::new_for_testing();

        Arc::make_mut(&mut app.state.library).root.children.push(
            Node::new("rect1")
                .with_prototype("corevector.rect")
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::float("width", 100.0))
                .with_input(Port::float("height", 100.0)),
        );
        Arc::make_mut(&mut app.state.library).root.rendered_child = Some("rect1".to_string());
        app.previous_library_hash = NodeBoxApp::hash_library(&app.state.library);
        app.previous_library = Arc::clone(&app.state.library);

        // Drag operation
        let pre_drag = Arc::clone(&app.state.library);
        let sel = SelectionSnapshot::default();
        app.history.begin_undo_group(&pre_drag, &sel);
        if let Some(node) = Arc::make_mut(&mut app.state.library).root.child_mut("rect1") {
            if let Some(port) = node.input_mut("width") {
                port.value = nodebox_core::Value::Float(200.0);
            }
        }
        app.check_for_changes();
        app.history.end_undo_group(&app.state.library);
        app.previous_library_hash = NodeBoxApp::hash_library(&app.state.library);
        app.previous_library = Arc::clone(&app.state.library);

        // Normal change after drag
        if let Some(node) = Arc::make_mut(&mut app.state.library).root.child_mut("rect1") {
            if let Some(port) = node.input_mut("height") {
                port.value = nodebox_core::Value::Float(200.0);
            }
        }
        app.check_for_changes();

        // Should have 2 entries: one from drag group + one from normal change
        assert_eq!(app.history.undo_count(), 2);
    }

    /// Test that a single change can be undone (not just counted).
    /// This is the core bug: check_for_changes saved the post-mutation state,
    /// so undoing returned the same state.
    #[test]
    fn test_single_change_undo_actually_restores_previous_state() {
        let mut app = NodeBoxApp::new_for_testing_empty();

        // Set up a rect with width=100
        Arc::make_mut(&mut app.state.library).root.children.push(
            Node::new("rect1")
                .with_prototype("corevector.rect")
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::float("width", 100.0))
                .with_input(Port::float("height", 100.0)),
        );
        Arc::make_mut(&mut app.state.library).root.rendered_child = Some("rect1".to_string());
        app.previous_library_hash = NodeBoxApp::hash_library(&app.state.library);
        app.previous_library = Arc::clone(&app.state.library);

        // Change width to 200
        if let Some(node) = Arc::make_mut(&mut app.state.library).root.child_mut("rect1") {
            if let Some(port) = node.input_mut("width") {
                port.value = nodebox_core::Value::Float(200.0);
            }
        }
        app.check_for_changes();

        // Verify the change took effect
        let width = app.state.library.root.child("rect1").unwrap()
            .input("width").unwrap().value.as_float().unwrap();
        assert!((width - 200.0).abs() < 0.001);

        // Undo should restore width=100
        {
            let sel = SelectionSnapshot::default();
            if let Some((restored, _)) = app.history.undo(&app.state.library, &sel) {
                app.state.library = restored;
            }
        }
        let restored_width = app.state.library.root.child("rect1").unwrap()
            .input("width").unwrap().value.as_float().unwrap();
        assert!((restored_width - 100.0).abs() < 0.001,
            "Expected width=100 after undo, got {}", restored_width);
    }

    /// Test that creating a node can be undone.
    #[test]
    fn test_undo_node_creation() {
        let mut app = NodeBoxApp::new_for_testing_empty();

        let initial_count = app.state.library.root.children.len();

        // Create a new node (simulates what the node dialog does)
        Arc::make_mut(&mut app.state.library).root.children.push(
            Node::new("rect1")
                .with_prototype("corevector.rect")
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::float("width", 100.0))
                .with_input(Port::float("height", 100.0)),
        );
        app.check_for_changes();

        assert_eq!(app.state.library.root.children.len(), initial_count + 1,
            "Node should have been added");

        // Undo should remove the node
        {
            let sel = SelectionSnapshot::default();
            if let Some((restored, _)) = app.history.undo(&app.state.library, &sel) {
                app.state.library = restored;
            }
        }
        assert_eq!(app.state.library.root.children.len(), initial_count,
            "Undo should remove the created node");
    }

    /// Test that connecting nodes can be undone.
    #[test]
    fn test_undo_connection() {
        let mut app = NodeBoxApp::new_for_testing_empty();

        // Set up two nodes
        {
            let lib = Arc::make_mut(&mut app.state.library);
            lib.root.children.push(
                Node::new("ellipse1")
                    .with_prototype("corevector.ellipse")
                    .with_input(Port::point("position", Point::ZERO))
                    .with_input(Port::float("width", 100.0))
                    .with_input(Port::float("height", 100.0)),
            );
            lib.root.children.push(
                Node::new("colorize1")
                    .with_prototype("corevector.colorize")
                    .with_input(Port::geometry("shape")),
            );
        }
        app.previous_library_hash = NodeBoxApp::hash_library(&app.state.library);
        app.previous_library = Arc::clone(&app.state.library);

        assert_eq!(app.state.library.root.connections.len(), 0);

        // Connect them
        Arc::make_mut(&mut app.state.library).root.connect(
            nodebox_core::node::Connection::new("ellipse1", "colorize1", "shape")
        );
        app.check_for_changes();

        assert_eq!(app.state.library.root.connections.len(), 1,
            "Connection should exist");

        // Undo should remove the connection
        {
            let sel = SelectionSnapshot::default();
            if let Some((restored, _)) = app.history.undo(&app.state.library, &sel) {
                app.state.library = restored;
            }
        }
        assert_eq!(app.state.library.root.connections.len(), 0,
            "Undo should remove the connection");
    }

    /// Test that setting the rendered node can be undone.
    #[test]
    fn test_undo_set_rendered_node() {
        let mut app = NodeBoxApp::new_for_testing_empty();

        // Set up two nodes with ellipse1 as rendered
        {
            let lib = Arc::make_mut(&mut app.state.library);
            lib.root.children.push(
                Node::new("ellipse1").with_prototype("corevector.ellipse"),
            );
            lib.root.children.push(
                Node::new("rect1").with_prototype("corevector.rect"),
            );
            lib.root.rendered_child = Some("ellipse1".to_string());
        }
        app.previous_library_hash = NodeBoxApp::hash_library(&app.state.library);
        app.previous_library = Arc::clone(&app.state.library);

        // Change rendered to rect1
        Arc::make_mut(&mut app.state.library).root.rendered_child = Some("rect1".to_string());
        app.check_for_changes();

        assert_eq!(app.state.library.root.rendered_child.as_deref(), Some("rect1"));

        // Undo should restore rendered to ellipse1
        {
            let sel = SelectionSnapshot::default();
            if let Some((restored, _)) = app.history.undo(&app.state.library, &sel) {
                app.state.library = restored;
            }
        }
        assert_eq!(app.state.library.root.rendered_child.as_deref(), Some("ellipse1"),
            "Undo should restore the rendered node to ellipse1");
    }

    /// Test the exact user-reported scenario: set rendered node, drag node, then undo.
    /// Should take exactly 2 undos (not 3 with an empty one).
    #[test]
    fn test_set_rendered_then_drag_undo_no_empty_steps() {
        let mut app = NodeBoxApp::new_for_testing_empty();

        // Set up two nodes with ellipse1 as rendered
        {
            let lib = Arc::make_mut(&mut app.state.library);
            lib.root.children.push(
                Node::new("ellipse1").with_prototype("corevector.ellipse")
                    .with_input(Port::float("width", 100.0)),
            );
            lib.root.children.push(
                Node::new("rect1").with_prototype("corevector.rect")
                    .with_input(Port::float("width", 50.0)),
            );
            lib.root.rendered_child = Some("ellipse1".to_string());
        }
        app.previous_library_hash = NodeBoxApp::hash_library(&app.state.library);
        app.previous_library = Arc::clone(&app.state.library);

        // Operation 1: Set rendered to rect1
        Arc::make_mut(&mut app.state.library).root.rendered_child = Some("rect1".to_string());
        app.check_for_changes();

        // Operation 2: Drag (change width from 50 to 200)
        let pre_drag = Arc::clone(&app.state.library);
        let sel = SelectionSnapshot::default();
        app.history.begin_undo_group(&pre_drag, &sel);
        for i in 1..=5 {
            if let Some(node) = Arc::make_mut(&mut app.state.library).root.child_mut("rect1") {
                if let Some(port) = node.input_mut("width") {
                    port.value = nodebox_core::Value::Float(50.0 + i as f64 * 30.0);
                }
            }
            app.check_for_changes();
        }
        app.history.end_undo_group(&app.state.library);
        app.previous_library_hash = NodeBoxApp::hash_library(&app.state.library);
        app.previous_library = Arc::clone(&app.state.library);

        // Should have exactly 2 undo entries (set rendered + drag)
        assert_eq!(app.history.undo_count(), 2,
            "Should have exactly 2 undo entries, not more");

        // Undo #1: Should undo the drag (width back to 50)
        {
            let sel = SelectionSnapshot::default();
            if let Some((restored, _)) = app.history.undo(&app.state.library, &sel) {
                app.state.library = restored;
            }
        }
        let width = app.state.library.root.child("rect1").unwrap()
            .input("width").unwrap().value.as_float().unwrap();
        assert!((width - 50.0).abs() < 0.001,
            "First undo should restore width to 50, got {}", width);
        assert_eq!(app.state.library.root.rendered_child.as_deref(), Some("rect1"),
            "First undo should keep rendered=rect1");

        // Undo #2: Should undo the rendered node change (back to ellipse1)
        {
            let sel = SelectionSnapshot::default();
            if let Some((restored, _)) = app.history.undo(&app.state.library, &sel) {
                app.state.library = restored;
            }
        }
        assert_eq!(app.state.library.root.rendered_child.as_deref(), Some("ellipse1"),
            "Second undo should restore rendered to ellipse1");
    }

    /// Test that undoing node creation clears the selection.
    /// When a node is created and selected, undoing should clear the selection
    /// because the node no longer exists.
    #[test]
    fn test_undo_node_creation_clears_selection() {
        let mut app = NodeBoxApp::new_for_testing_empty();

        // Create a new node and select it
        Arc::make_mut(&mut app.state.library).root.children.push(
            Node::new("ellipse1")
                .with_prototype("corevector.ellipse")
                .with_input(Port::float("width", 100.0)),
        );
        app.state.selected_node = Some("ellipse1".to_string());
        app.network_view.set_selected(["ellipse1".to_string()].into_iter().collect());
        app.check_for_changes();

        assert_eq!(app.state.selected_node.as_deref(), Some("ellipse1"));
        assert!(app.network_view.selected_nodes().contains("ellipse1"));

        // Undo: node is removed, selection should be cleared
        let sel = app.current_selection();
        if let Some((restored, restored_sel)) = app.history.undo(&app.state.library, &sel) {
            app.state.library = restored;
            app.apply_selection(restored_sel);
        }

        assert_eq!(app.state.selected_node, None,
            "Selection should be cleared after undoing node creation");
        assert!(app.network_view.selected_nodes().is_empty(),
            "Network view selection should be empty after undoing node creation");
    }

    /// Test that undoing restores the previous selection.
    /// If node A was selected, then node B was created and selected,
    /// undoing should restore node A as selected.
    #[test]
    fn test_undo_restores_previous_selection() {
        let mut app = NodeBoxApp::new_for_testing_empty();

        // Start with node A
        Arc::make_mut(&mut app.state.library).root.children.push(
            Node::new("rect1")
                .with_prototype("corevector.rect")
                .with_input(Port::float("width", 100.0)),
        );
        app.state.selected_node = Some("rect1".to_string());
        app.network_view.set_selected(["rect1".to_string()].into_iter().collect());
        app.previous_library_hash = NodeBoxApp::hash_library(&app.state.library);
        app.previous_library = Arc::clone(&app.state.library);
        app.previous_selection = app.current_selection();

        // Create node B and select it
        Arc::make_mut(&mut app.state.library).root.children.push(
            Node::new("ellipse1")
                .with_prototype("corevector.ellipse")
                .with_input(Port::float("width", 100.0)),
        );
        app.state.selected_node = Some("ellipse1".to_string());
        app.network_view.set_selected(["ellipse1".to_string()].into_iter().collect());
        app.check_for_changes();

        assert_eq!(app.state.selected_node.as_deref(), Some("ellipse1"));

        // Undo: node B removed, selection should restore to rect1
        let sel = app.current_selection();
        if let Some((restored, restored_sel)) = app.history.undo(&app.state.library, &sel) {
            app.state.library = restored;
            app.apply_selection(restored_sel);
        }

        assert_eq!(app.state.selected_node.as_deref(), Some("rect1"),
            "Undo should restore previous selection to rect1");
        assert!(app.network_view.selected_nodes().contains("rect1"),
            "Network view should show rect1 selected after undo");
    }

    /// Test that redo restores the selection from before the undo.
    #[test]
    fn test_redo_restores_selection() {
        let mut app = NodeBoxApp::new_for_testing_empty();

        // Create node and select it
        Arc::make_mut(&mut app.state.library).root.children.push(
            Node::new("ellipse1")
                .with_prototype("corevector.ellipse")
                .with_input(Port::float("width", 100.0)),
        );
        app.state.selected_node = Some("ellipse1".to_string());
        app.network_view.set_selected(["ellipse1".to_string()].into_iter().collect());
        app.check_for_changes();

        // Undo: removes node, clears selection
        let sel = app.current_selection();
        if let Some((restored, restored_sel)) = app.history.undo(&app.state.library, &sel) {
            app.state.library = restored;
            app.apply_selection(restored_sel);
        }

        assert_eq!(app.state.selected_node, None);

        // Redo: restores node and selection
        let sel = app.current_selection();
        if let Some((restored, restored_sel)) = app.history.redo(&app.state.library, &sel) {
            app.state.library = restored;
            app.apply_selection(restored_sel);
        }

        assert_eq!(app.state.selected_node.as_deref(), Some("ellipse1"),
            "Redo should restore selection to ellipse1");
        assert!(app.network_view.selected_nodes().contains("ellipse1"),
            "Network view should show ellipse1 selected after redo");
    }
}
