//! macOS file-open handler via Objective-C runtime method injection.
//!
//! When a user double-clicks a `.ndbx` file in Finder, macOS sends an
//! `application:openFiles:` message to the app delegate. winit 0.30.12's
//! delegate doesn't implement this method, so macOS shows an error dialog.
//!
//! This module injects `application:openFiles:` into winit's delegate class
//! at runtime using `class_addMethod`. The injected method stores received
//! file paths in a global `Mutex<Vec<PathBuf>>`, which the app's `update()`
//! loop polls each frame.
//!
//! Must be called after `NSApplication` and its delegate exist (i.e., inside
//! the eframe creation callback), but before the event loop dispatches Apple
//! Events — which is the case because the creation callback runs during
//! `applicationDidFinishLaunching:`, before queued events are processed.

#![allow(non_snake_case)]

use std::ffi::{c_char, c_void, CStr};
use std::path::PathBuf;
use std::sync::Mutex;

// Raw FFI to the Objective-C runtime (libobjc.dylib, always available on macOS).
#[link(name = "objc", kind = "dylib")]
extern "C" {
    fn objc_getClass(name: *const c_char) -> *mut c_void;
    fn sel_registerName(name: *const c_char) -> *mut c_void;
    fn object_getClass(obj: *mut c_void) -> *mut c_void;
    fn class_addMethod(
        cls: *mut c_void,
        name: *mut c_void,
        imp: unsafe extern "C" fn(*mut c_void, *mut c_void, *mut c_void, *mut c_void),
        types: *const c_char,
    ) -> bool;
}

// objc_msgSend uses the standard C calling convention on arm64/x86_64 for
// pointer-sized returns. We declare typed wrappers to avoid variadic FFI issues.
extern "C" {
    #[link_name = "objc_msgSend"]
    fn msg_send_ptr(obj: *mut c_void, sel: *mut c_void) -> *mut c_void;
    #[link_name = "objc_msgSend"]
    fn msg_send_usize(obj: *mut c_void, sel: *mut c_void) -> usize;
    #[link_name = "objc_msgSend"]
    fn msg_send_ptr_usize(obj: *mut c_void, sel: *mut c_void, idx: usize) -> *mut c_void;
}

/// File paths received via `application:openFiles:`.
static PENDING_FILES: Mutex<Vec<PathBuf>> = Mutex::new(Vec::new());

/// The C-ABI function injected as `application:openFiles:`.
///
/// Called by the Objective-C runtime when macOS delivers a file-open event.
/// Arguments: `self`, `_cmd`, `NSApplication*`, `NSArray<NSString*>*`.
unsafe extern "C" fn handle_open_files(
    _this: *mut c_void,
    _sel: *mut c_void,
    _app: *mut c_void,
    filenames: *mut c_void,
) {
    if filenames.is_null() {
        return;
    }

    let count_sel = sel_registerName(c"count".as_ptr());
    let count = msg_send_usize(filenames, count_sel);

    let object_at_sel = sel_registerName(c"objectAtIndex:".as_ptr());
    let utf8_sel = sel_registerName(c"UTF8String".as_ptr());

    let mut files = Vec::new();
    for i in 0..count {
        let ns_str = msg_send_ptr_usize(filenames, object_at_sel, i);
        if ns_str.is_null() {
            continue;
        }
        let c_str = msg_send_ptr(ns_str, utf8_sel) as *const c_char;
        if c_str.is_null() {
            continue;
        }
        let path_str = CStr::from_ptr(c_str).to_string_lossy();
        let path = PathBuf::from(path_str.as_ref());
        if path.extension().is_some_and(|ext| ext == "ndbx") {
            files.push(path);
        }
    }

    if let Ok(mut pending) = PENDING_FILES.lock() {
        pending.extend(files);
    }
}

/// Inject `application:openFiles:` into the current `NSApp` delegate's class.
///
/// Call this after `NSApplication` has been initialized (e.g., in the eframe
/// creation callback). If the delegate already implements `application:openFiles:`,
/// `class_addMethod` returns false and the existing implementation is kept.
pub fn inject_file_open_handler() {
    unsafe {
        let ns_app_class = objc_getClass(c"NSApplication".as_ptr());
        if ns_app_class.is_null() {
            return;
        }

        let shared_sel = sel_registerName(c"sharedApplication".as_ptr());
        let app = msg_send_ptr(ns_app_class, shared_sel);
        if app.is_null() {
            return;
        }

        let delegate_sel = sel_registerName(c"delegate".as_ptr());
        let delegate = msg_send_ptr(app, delegate_sel);
        if delegate.is_null() {
            return;
        }

        let cls = object_getClass(delegate);
        if cls.is_null() {
            return;
        }

        let open_sel = sel_registerName(c"application:openFiles:".as_ptr());

        // "v@:@@" = void return, self, _cmd, id (NSApplication), id (NSArray)
        class_addMethod(cls, open_sel, handle_open_files, c"v@:@@".as_ptr());
    }
}

/// Drain and return any file paths received via `application:openFiles:`.
pub fn take_pending_files() -> Vec<PathBuf> {
    PENDING_FILES
        .lock()
        .map(|mut f| f.drain(..).collect())
        .unwrap_or_default()
}
