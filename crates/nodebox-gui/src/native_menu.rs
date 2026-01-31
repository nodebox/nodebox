//! Native menu bar support for macOS.

#[cfg(target_os = "macos")]
use muda::{Menu, MenuItem, PredefinedMenuItem, Submenu, accelerator::Accelerator, MenuEvent};

/// Menu item identifiers for handling menu events.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum MenuAction {
    New,
    Open,
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
    _menu: Menu,
    new_id: muda::MenuId,
    open_id: muda::MenuId,
    save_id: muda::MenuId,
    save_as_id: muda::MenuId,
    export_png_id: muda::MenuId,
    export_svg_id: muda::MenuId,
    undo_id: muda::MenuId,
    redo_id: muda::MenuId,
    zoom_in_id: muda::MenuId,
    zoom_out_id: muda::MenuId,
    zoom_reset_id: muda::MenuId,
    about_id: muda::MenuId,
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

        // Initialize for macOS
        menu.init_for_nsapp();

        Self {
            _menu: menu,
            new_id,
            open_id,
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

    /// Poll for menu events and return any action.
    pub fn poll_event(&self) -> Option<MenuAction> {
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
}

impl Default for NativeMenuHandle {
    fn default() -> Self {
        Self::new()
    }
}
