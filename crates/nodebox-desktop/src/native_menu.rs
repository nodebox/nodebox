//! Native menu bar support for macOS.
//!
//! Note: Menu polling is not yet integrated into the main event loop.

#![allow(dead_code)]

#[cfg(target_os = "macos")]
use muda::{Menu, MenuId, MenuItem, PredefinedMenuItem, Submenu, accelerator::Accelerator, MenuEvent};
#[cfg(target_os = "macos")]
use std::cell::{Cell, RefCell};
use std::path::PathBuf;

/// Menu item identifiers for handling menu events.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum MenuAction {
    New,
    Open,
    OpenRecent(PathBuf),
    ClearRecent,
    Save,
    SaveAs,
    ExportPng,
    ExportSvg,
    Undo,
    Redo,
    Cut,
    Copy,
    Paste,
    Delete,
    SelectAll,
    ZoomIn,
    ZoomOut,
    ZoomReset,
    About,
}

/// Handle to the native menu, with item IDs for event handling.
#[cfg(target_os = "macos")]
pub struct NativeMenuHandle {
    menu: Menu,
    initialized: Cell<bool>,
    new_id: MenuId,
    open_id: MenuId,
    recent_submenu: Submenu,
    clear_recent_id: MenuId,
    /// Map from menu IDs to file paths for recent files
    recent_file_ids: RefCell<Vec<(MenuId, PathBuf)>>,
    save_id: MenuId,
    save_as_id: MenuId,
    export_png_id: MenuId,
    export_svg_id: MenuId,
    undo_id: MenuId,
    redo_id: MenuId,
    zoom_in_id: MenuId,
    zoom_out_id: MenuId,
    zoom_reset_id: MenuId,
    about_id: MenuId,
}

#[cfg(not(target_os = "macos"))]
pub struct NativeMenuHandle;

#[cfg(target_os = "macos")]
impl NativeMenuHandle {
    /// Create and initialize the native menu bar.
    pub fn new() -> Self {
        let menu = Menu::new();

        // App menu (NodeBox)
        let app_menu = Submenu::new("NodeBox", true);
        let about = MenuItem::new("About NodeBox", true, None);
        let about_id = about.id().clone();
        app_menu.append(&about).unwrap();
        app_menu.append(&PredefinedMenuItem::separator()).unwrap();
        app_menu.append(&PredefinedMenuItem::services(None)).unwrap();
        app_menu.append(&PredefinedMenuItem::separator()).unwrap();
        app_menu.append(&PredefinedMenuItem::hide(None)).unwrap();
        app_menu.append(&PredefinedMenuItem::hide_others(None)).unwrap();
        app_menu.append(&PredefinedMenuItem::show_all(None)).unwrap();
        app_menu.append(&PredefinedMenuItem::separator()).unwrap();
        app_menu.append(&PredefinedMenuItem::quit(None)).unwrap();

        // File menu
        let file_menu = Submenu::new("File", true);
        let new_item = MenuItem::new("New", true, Some(Accelerator::new(Some(muda::accelerator::Modifiers::META), muda::accelerator::Code::KeyN)));
        let new_id = new_item.id().clone();
        let open_item = MenuItem::new("Open...", true, Some(Accelerator::new(Some(muda::accelerator::Modifiers::META), muda::accelerator::Code::KeyO)));
        let open_id = open_item.id().clone();

        // Open Recent submenu
        let recent_submenu = Submenu::new("Open Recent", true);
        let clear_recent = MenuItem::new("Clear Recent", true, None);
        let clear_recent_id = clear_recent.id().clone();
        // Start with just "Clear Recent" (will be rebuilt with files later)
        recent_submenu.append(&clear_recent).unwrap();

        let save_item = MenuItem::new("Save", true, Some(Accelerator::new(Some(muda::accelerator::Modifiers::META), muda::accelerator::Code::KeyS)));
        let save_id = save_item.id().clone();
        let save_as_item = MenuItem::new("Save As...", true, Some(Accelerator::new(Some(muda::accelerator::Modifiers::META | muda::accelerator::Modifiers::SHIFT), muda::accelerator::Code::KeyS)));
        let save_as_id = save_as_item.id().clone();

        let export_submenu = Submenu::new("Export", true);
        let export_png = MenuItem::new("PNG Image...", true, None);
        let export_png_id = export_png.id().clone();
        let export_svg = MenuItem::new("SVG Vector...", true, None);
        let export_svg_id = export_svg.id().clone();
        export_submenu.append(&export_png).unwrap();
        export_submenu.append(&export_svg).unwrap();

        file_menu.append(&new_item).unwrap();
        file_menu.append(&open_item).unwrap();
        file_menu.append(&recent_submenu).unwrap();
        file_menu.append(&PredefinedMenuItem::separator()).unwrap();
        file_menu.append(&save_item).unwrap();
        file_menu.append(&save_as_item).unwrap();
        file_menu.append(&PredefinedMenuItem::separator()).unwrap();
        file_menu.append(&export_submenu).unwrap();

        // Edit menu
        let edit_menu = Submenu::new("Edit", true);
        let undo_item = MenuItem::new("Undo", true, Some(Accelerator::new(Some(muda::accelerator::Modifiers::META), muda::accelerator::Code::KeyZ)));
        let undo_id = undo_item.id().clone();
        let redo_item = MenuItem::new("Redo", true, Some(Accelerator::new(Some(muda::accelerator::Modifiers::META | muda::accelerator::Modifiers::SHIFT), muda::accelerator::Code::KeyZ)));
        let redo_id = redo_item.id().clone();

        edit_menu.append(&undo_item).unwrap();
        edit_menu.append(&redo_item).unwrap();
        edit_menu.append(&PredefinedMenuItem::separator()).unwrap();
        edit_menu.append(&PredefinedMenuItem::cut(None)).unwrap();
        edit_menu.append(&PredefinedMenuItem::copy(None)).unwrap();
        edit_menu.append(&PredefinedMenuItem::paste(None)).unwrap();
        edit_menu.append(&PredefinedMenuItem::select_all(None)).unwrap();

        // View menu
        let view_menu = Submenu::new("View", true);
        let zoom_in = MenuItem::new("Zoom In", true, Some(Accelerator::new(Some(muda::accelerator::Modifiers::META), muda::accelerator::Code::Equal)));
        let zoom_in_id = zoom_in.id().clone();
        let zoom_out = MenuItem::new("Zoom Out", true, Some(Accelerator::new(Some(muda::accelerator::Modifiers::META), muda::accelerator::Code::Minus)));
        let zoom_out_id = zoom_out.id().clone();
        let zoom_reset = MenuItem::new("Actual Size", true, Some(Accelerator::new(Some(muda::accelerator::Modifiers::META), muda::accelerator::Code::Digit0)));
        let zoom_reset_id = zoom_reset.id().clone();

        view_menu.append(&zoom_in).unwrap();
        view_menu.append(&zoom_out).unwrap();
        view_menu.append(&zoom_reset).unwrap();
        view_menu.append(&PredefinedMenuItem::separator()).unwrap();
        view_menu.append(&PredefinedMenuItem::fullscreen(None)).unwrap();

        // Window menu
        let window_menu = Submenu::new("Window", true);
        window_menu.append(&PredefinedMenuItem::minimize(None)).unwrap();
        window_menu.append(&PredefinedMenuItem::maximize(None)).unwrap();
        window_menu.append(&PredefinedMenuItem::separator()).unwrap();
        window_menu.append(&PredefinedMenuItem::close_window(None)).unwrap();

        // Help menu
        let help_menu = Submenu::new("Help", true);

        // Build the menu bar
        menu.append(&app_menu).unwrap();
        menu.append(&file_menu).unwrap();
        menu.append(&edit_menu).unwrap();
        menu.append(&view_menu).unwrap();
        menu.append(&window_menu).unwrap();
        menu.append(&help_menu).unwrap();

        // Note: init_for_nsapp() is called lazily in poll_event()
        // because NSApplication must exist first (created by eframe)

        Self {
            menu,
            initialized: Cell::new(false),
            new_id,
            open_id,
            recent_submenu,
            clear_recent_id,
            recent_file_ids: RefCell::new(Vec::new()),
            save_id,
            save_as_id,
            export_png_id,
            export_svg_id,
            undo_id,
            redo_id,
            zoom_in_id,
            zoom_out_id,
            zoom_reset_id,
            about_id,
        }
    }

    /// Rebuild the "Open Recent" submenu with the given list of files.
    pub fn rebuild_recent_menu(&self, files: &[PathBuf]) {
        // Clear all items from the submenu by removing each one based on its kind
        let items = self.recent_submenu.items();
        for item in items {
            match item {
                muda::MenuItemKind::MenuItem(m) => { let _ = self.recent_submenu.remove(&m); }
                muda::MenuItemKind::Submenu(s) => { let _ = self.recent_submenu.remove(&s); }
                muda::MenuItemKind::Predefined(p) => { let _ = self.recent_submenu.remove(&p); }
                muda::MenuItemKind::Check(c) => { let _ = self.recent_submenu.remove(&c); }
                muda::MenuItemKind::Icon(i) => { let _ = self.recent_submenu.remove(&i); }
            }
        }

        // Clear the recent file IDs mapping
        self.recent_file_ids.borrow_mut().clear();

        // Add file items
        for path in files {
            // Use filename for display, full path for tooltip would be nice but muda doesn't support it
            let display_name = path
                .file_name()
                .and_then(|n| n.to_str())
                .unwrap_or("Unknown");
            let item = MenuItem::new(display_name, true, None);
            let id = item.id().clone();
            self.recent_submenu.append(&item).unwrap();
            self.recent_file_ids.borrow_mut().push((id, path.clone()));
        }

        // Add separator and Clear Recent if there are files
        if !files.is_empty() {
            self.recent_submenu.append(&PredefinedMenuItem::separator()).unwrap();
        }

        // Add Clear Recent item
        let clear_item = MenuItem::new("Clear Recent", !files.is_empty(), None);
        self.recent_submenu.append(&clear_item).unwrap();
    }

    /// Ensure the menu is initialized for NSApp.
    /// Must be called after NSApplication exists (i.e., after eframe starts).
    fn ensure_initialized(&self) {
        if !self.initialized.get() {
            self.menu.init_for_nsapp();
            self.initialized.set(true);
        }
    }

    /// Poll for menu events and return any action.
    pub fn poll_event(&self) -> Option<MenuAction> {
        // Lazily initialize the menu for NSApp on first poll
        self.ensure_initialized();

        if let Ok(event) = MenuEvent::receiver().try_recv() {
            if event.id == self.new_id {
                return Some(MenuAction::New);
            } else if event.id == self.open_id {
                return Some(MenuAction::Open);
            } else if event.id == self.save_id {
                return Some(MenuAction::Save);
            } else if event.id == self.save_as_id {
                return Some(MenuAction::SaveAs);
            } else if event.id == self.export_png_id {
                return Some(MenuAction::ExportPng);
            } else if event.id == self.export_svg_id {
                return Some(MenuAction::ExportSvg);
            } else if event.id == self.undo_id {
                return Some(MenuAction::Undo);
            } else if event.id == self.redo_id {
                return Some(MenuAction::Redo);
            } else if event.id == self.zoom_in_id {
                return Some(MenuAction::ZoomIn);
            } else if event.id == self.zoom_out_id {
                return Some(MenuAction::ZoomOut);
            } else if event.id == self.zoom_reset_id {
                return Some(MenuAction::ZoomReset);
            } else if event.id == self.about_id {
                return Some(MenuAction::About);
            }

            // Check recent file IDs
            let recent_ids = self.recent_file_ids.borrow();
            for (id, path) in recent_ids.iter() {
                if event.id == *id {
                    return Some(MenuAction::OpenRecent(path.clone()));
                }
            }
            drop(recent_ids);

            // Check for Clear Recent - look through submenu items
            for item in self.recent_submenu.items() {
                if let muda::MenuItemKind::MenuItem(menu_item) = item {
                    if menu_item.id() == &event.id && menu_item.text() == "Clear Recent" {
                        return Some(MenuAction::ClearRecent);
                    }
                }
            }
        }
        None
    }
}

#[cfg(not(target_os = "macos"))]
impl NativeMenuHandle {
    pub fn new() -> Self {
        Self
    }

    pub fn poll_event(&self) -> Option<MenuAction> {
        None
    }

    pub fn rebuild_recent_menu(&self, _files: &[PathBuf]) {
        // No-op on non-macOS platforms
    }
}

impl Default for NativeMenuHandle {
    fn default() -> Self {
        Self::new()
    }
}
