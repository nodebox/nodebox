//! Application shell: owns the open documents and app-level state.
//!
//! Each [`Document`] is hosted in its own OS window. The first document
//! lives in the root viewport; the others are shown in immediate child
//! viewports. Documents communicate app-level intents (open, new, quit,
//! close-me) back to the shell through [`AppRequest`]s drained every frame.

use eframe::egui;
use std::path::{Path, PathBuf};
use std::sync::mpsc::Receiver;
use std::sync::Arc;

use crate::document::{AppRequest, Document, DocumentEnv};
use crate::native_menu::NativeMenuHandle;
use crate::recent_files::RecentFiles;
use crate::theme;
use nodebox_core::platform::Platform;

/// The NodeBox application shell: documents, native menu, recent files.
pub struct NodeBoxApp {
    /// Platform for platform-abstracted file operations.
    port: Arc<dyn Platform>,
    /// Open documents. The first one is hosted in the root viewport.
    documents: Vec<Document>,
    /// Native menu handle for the macOS system menu bar.
    native_menu: Option<NativeMenuHandle>,
    /// Recent files list for "Open Recent" menus.
    recent_files: RecentFiles,
    /// Receives file paths the OS asks us to open (e.g. Finder double-click).
    open_file_rx: Option<Receiver<PathBuf>>,
    /// Counter for stable per-document viewport identifiers.
    next_document_id: u64,
    /// Index of the most recently focused document (for menu routing).
    focused_document: usize,
}

impl NodeBoxApp {
    /// Create the application with a Platform for file operations,
    /// opening one window per initial file (or one empty window).
    pub fn new_with_port(
        cc: &eframe::CreationContext<'_>,
        port: Arc<dyn Platform>,
        initial_files: Vec<PathBuf>,
    ) -> Self {
        // Configure the global theme/style
        theme::configure_style(&cc.egui_ctx);

        let native_menu = Some(NativeMenuHandle::new());
        let recent_files = RecentFiles::load();
        if let Some(ref menu) = native_menu {
            menu.rebuild_recent_menu(&recent_files.files());
        }

        let mut app = Self {
            port,
            documents: Vec::new(),
            native_menu,
            recent_files,
            open_file_rx: crate::platform_integration::take_open_file_receiver(),
            next_document_id: 0,
            focused_document: 0,
        };
        for path in &initial_files {
            app.open_document(path);
        }
        if app.documents.is_empty() {
            app.new_document();
        }
        app
    }

    fn next_id(&mut self) -> u64 {
        self.next_document_id += 1;
        self.next_document_id
    }

    /// Create a new empty document window.
    fn new_document(&mut self) {
        let id = self.next_id();
        self.documents.push(Document::new(id, Arc::clone(&self.port)));
    }

    /// Open `path`: reuse the window if the file is already open, fill a
    /// pristine (untitled, unmodified) window if one exists, otherwise
    /// create a new window.
    fn open_document(&mut self, path: &Path) {
        if self
            .documents
            .iter()
            .any(|d| d.file_path() == Some(path))
        {
            // Already open; don't create a second divergent copy.
            return;
        }
        if let Some(doc) = self.documents.iter_mut().find(|d| d.is_pristine()) {
            doc.load_path(path);
        } else {
            let id = self.next_id();
            self.documents
                .push(Document::open(id, Arc::clone(&self.port), path));
        }
    }

    /// Add a file to the recent files list and update the native menu.
    fn add_recent_file(&mut self, path: PathBuf) {
        self.recent_files.add_file(path);
        self.recent_files.save();
        self.rebuild_recent_menu();
    }

    /// Clear all recent files.
    fn clear_recent_files(&mut self) {
        self.recent_files.clear();
        self.recent_files.save();
        self.rebuild_recent_menu();
    }

    fn rebuild_recent_menu(&self) {
        if let Some(ref menu) = self.native_menu {
            menu.rebuild_recent_menu(&self.recent_files.files());
        }
    }

    /// Ask every dirty document for confirmation before quitting.
    /// Returns true if the app may quit.
    fn confirm_quit(&mut self) -> bool {
        let mut saved_files = Vec::new();
        let mut confirmed = true;
        for doc in self.documents.iter_mut() {
            if doc.is_dirty() && !doc.confirm_discard_changes() {
                confirmed = false;
                break;
            }
            // A "Save" choice in the prompt may have produced recent-file
            // requests; collect them before the documents are dropped.
            for req in doc.requests.drain(..) {
                if let AppRequest::AddRecentFile(path) = req {
                    saved_files.push(path);
                }
            }
        }
        for path in saved_files {
            self.add_recent_file(path);
        }
        confirmed
    }
}

impl eframe::App for NodeBoxApp {
    fn update(&mut self, ctx: &egui::Context, frame: &mut eframe::Frame) {
        // 1. Native menu events (macOS system menu bar) go to the focused
        //    document; app-level actions come back as requests.
        if let Some(action) = self.native_menu.as_ref().and_then(|m| m.poll_event()) {
            let idx = self
                .focused_document
                .min(self.documents.len().saturating_sub(1));
            if let Some(doc) = self.documents.get_mut(idx) {
                doc.handle_menu_action(action, ctx);
            }
        }

        // 2. Files delivered by the OS (Finder double-click, dock drop).
        let os_paths: Vec<PathBuf> = self
            .open_file_rx
            .as_ref()
            .map(|rx| rx.try_iter().collect())
            .unwrap_or_default();
        for path in os_paths {
            self.open_document(&path);
        }

        // 3. Show the first document in the root viewport, the rest in
        //    immediate child viewports (real OS windows).
        let recent = self.recent_files.files();
        let other_documents_exist = self.documents.len() > 1;
        if let Some(doc) = self.documents.first_mut() {
            let env = DocumentEnv {
                is_root: true,
                other_documents_exist,
                recent_files: recent.clone(),
            };
            doc.show(ctx, &env, frame);
        }
        let child_env = DocumentEnv {
            is_root: false,
            other_documents_exist: true,
            recent_files: recent,
        };
        for doc in self.documents.iter_mut().skip(1) {
            let viewport_id = egui::ViewportId::from_hash_of(("nodebox-document", doc.id));
            let builder = egui::ViewportBuilder::default()
                .with_title(doc.window_title())
                .with_inner_size([1280.0, 800.0]);
            ctx.show_viewport_immediate(viewport_id, builder, |ctx, _class| {
                doc.show(ctx, &child_env, &mut *frame);
            });
        }

        // 4. Track which document is focused for menu routing (sticky:
        //    keep the last focused one when e.g. a menu steals focus).
        for (i, doc) in self.documents.iter().enumerate() {
            if doc.focused {
                self.focused_document = i;
            }
        }

        // 5. Drain document requests.
        let mut requests: Vec<(usize, AppRequest)> = Vec::new();
        for (i, doc) in self.documents.iter_mut().enumerate() {
            requests.extend(doc.requests.drain(..).map(|r| (i, r)));
        }
        let mut to_close: Vec<usize> = Vec::new();
        let mut quit = false;
        for (origin, request) in requests {
            match request {
                AppRequest::NewDocument => self.new_document(),
                AppRequest::OpenDocument(path) => self.open_document(&path),
                AppRequest::AddRecentFile(path) => self.add_recent_file(path),
                AppRequest::ClearRecentFiles => self.clear_recent_files(),
                AppRequest::CloseDocument => to_close.push(origin),
                AppRequest::Quit => quit = true,
            }
        }

        // 6. Remove closed documents (highest index first so indices stay
        //    valid). Removing index 0 promotes the next document into the
        //    root viewport; its child window disappears and its content
        //    continues in the root window.
        to_close.sort_unstable();
        to_close.dedup();
        for idx in to_close.into_iter().rev() {
            if idx < self.documents.len() {
                self.documents.remove(idx);
            }
        }
        self.focused_document = self
            .focused_document
            .min(self.documents.len().saturating_sub(1));

        // 7. Quit when the last document closed, or when a quit request
        //    survived the per-document prompts.
        if self.documents.is_empty() || (quit && self.confirm_quit()) {
            self.documents.clear();
            ctx.send_viewport_cmd(egui::ViewportCommand::Close);
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn test_app() -> NodeBoxApp {
        NodeBoxApp {
            port: Arc::new(crate::DesktopPlatform::new()),
            documents: Vec::new(),
            native_menu: None,
            recent_files: RecentFiles::new(),
            open_file_rx: None,
            next_document_id: 0,
            focused_document: 0,
        }
    }

    /// Write a minimal valid .ndbx file for opening in tests.
    fn temp_ndbx(dir: &tempfile::TempDir, name: &str) -> PathBuf {
        let path = dir.path().join(name);
        let mut state = crate::state::AppState::new();
        state.save_file(&path).unwrap();
        path
    }

    #[test]
    fn test_open_document_reuses_pristine_window() {
        let dir = tempfile::tempdir().unwrap();
        let path = temp_ndbx(&dir, "a.ndbx");
        let mut app = test_app();
        app.new_document();
        assert_eq!(app.documents.len(), 1);
        assert!(app.documents[0].is_pristine());

        app.open_document(&path);
        assert_eq!(app.documents.len(), 1, "pristine window should be reused");
        assert_eq!(app.documents[0].file_path(), Some(path.as_path()));
    }

    #[test]
    fn test_open_document_new_window_and_no_duplicates() {
        let dir = tempfile::tempdir().unwrap();
        let a = temp_ndbx(&dir, "a.ndbx");
        let b = temp_ndbx(&dir, "b.ndbx");
        let mut app = test_app();
        app.open_document(&a);
        assert_eq!(app.documents.len(), 1);

        app.open_document(&a);
        assert_eq!(app.documents.len(), 1, "same file must not open twice");

        app.open_document(&b);
        assert_eq!(app.documents.len(), 2, "second file opens a new window");
        assert_eq!(app.documents[1].file_path(), Some(b.as_path()));
    }
}
