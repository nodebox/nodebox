//! macOS open-file event integration.
//!
//! When the user double-clicks an .ndbx file in Finder (or drags it onto the
//! dock icon), macOS does not pass the path via argv. Instead it sends an
//! open-documents Apple event, which NSApplication routes to the
//! `application:openURLs:` method of its delegate. winit's application
//! delegate does not implement that selector, so the event is silently
//! dropped.
//!
//! This module adds an `application:openURLs:` implementation to the
//! delegate's class at runtime and forwards the received paths through a
//! channel that the app drains every frame.
//!
//! Timing matters: AppKit may deliver the launch document right after
//! `applicationWillFinishLaunching:` — before eframe's creation context
//! runs. The method is therefore injected from an
//! `NSApplicationWillFinishLaunchingNotification` observer registered before
//! the event loop starts: at that point winit's delegate already exists
//! (created in `EventLoop::new`), and open events are always delivered
//! after the notification fires.

use std::path::PathBuf;
use std::ptr::NonNull;
use std::sync::mpsc::{channel, Receiver, Sender};
use std::sync::OnceLock;

use block2::RcBlock;
use objc2::ffi::class_addMethod;
use objc2::runtime::{AnyClass, AnyObject, Sel};
use objc2::{msg_send, sel};
use objc2_app_kit::{NSApplication, NSApplicationWillFinishLaunchingNotification};
use objc2_foundation::{
    MainThreadMarker, NSArray, NSNotification, NSNotificationCenter, NSURL,
};

/// Channel sender shared with the Objective-C callback.
static OPEN_FILE_TX: OnceLock<Sender<PathBuf>> = OnceLock::new();

/// The `application:openURLs:` implementation added to the delegate class.
///
/// Signature per NSApplicationDelegate:
/// `- (void)application:(NSApplication *)application openURLs:(NSArray<NSURL *> *)urls`
unsafe extern "C" fn application_open_urls(
    _this: *mut AnyObject,
    _cmd: Sel,
    _application: *mut AnyObject,
    urls: *mut AnyObject,
) {
    let Some(tx) = OPEN_FILE_TX.get() else {
        return;
    };
    let urls: &NSArray<NSURL> = &*(urls as *const NSArray<NSURL>);
    for url in urls {
        if let Some(path) = url.path() {
            let _ = tx.send(PathBuf::from(path.to_string()));
        }
    }
}

/// Add `application:openURLs:` to the NSApplication delegate's class.
/// Must run on the main thread with the delegate installed.
fn add_open_urls_method() {
    let Some(mtm) = MainThreadMarker::new() else {
        log::warn!("open-file handler not installed: not on the main thread");
        return;
    };
    let app = NSApplication::sharedApplication(mtm);
    // Safety: called on the main thread; the delegate is only used to look
    // up its class, not retained beyond this scope.
    let Some(delegate) = (unsafe { app.delegate() }) else {
        log::warn!("open-file handler not installed: NSApplication has no delegate");
        return;
    };

    unsafe {
        let class: &AnyClass = msg_send![&*delegate, class];
        // Type encoding: void return, receiver, selector, two object args.
        let types = c"v@:@@";
        let imp: objc2::ffi::IMP = Some(std::mem::transmute::<
            unsafe extern "C" fn(*mut AnyObject, Sel, *mut AnyObject, *mut AnyObject),
            unsafe extern "C" fn(),
        >(application_open_urls));
        let added = class_addMethod(
            class as *const AnyClass as *mut _,
            sel!(application:openURLs:).as_ptr(),
            imp,
            types.as_ptr(),
        );
        if !added {
            // The delegate already implements the selector (e.g. a future
            // winit version); our handler is not needed in that case.
            log::warn!(
                "open-file handler not installed: {} already implements application:openURLs:",
                class.name()
            );
        }
    }
}

/// Set up the open-file handler. Call before the event loop starts.
///
/// Returns the receiving end of the channel; opened file paths arrive there.
pub fn init_open_file_handler() -> Receiver<PathBuf> {
    let (tx, rx) = channel();
    let _ = OPEN_FILE_TX.set(tx);

    // Inject the delegate method as soon as the application finishes
    // launching — after winit installs its delegate, before any open
    // events are delivered.
    let block = RcBlock::new(|_notification: NonNull<NSNotification>| {
        add_open_urls_method();
    });
    unsafe {
        let center = NSNotificationCenter::defaultCenter();
        let token = center.addObserverForName_object_queue_usingBlock(
            Some(NSApplicationWillFinishLaunchingNotification),
            None,
            None,
            &block,
        );
        // Keep the observer alive for the lifetime of the process.
        std::mem::forget(token);
    }

    rx
}
