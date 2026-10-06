//! Vello rendering of NodeBox geometry.
//!
//! This crate turns nodebox-core paths into a Vello scene and renders it with
//! wgpu. It has no GUI toolkit dependency, so the desktop app and a
//! WebAssembly viewer share it.

pub mod convert;
pub mod renderer;

pub use convert::{convert_paths, VelloPath};
pub use renderer::{build_scene, VelloConfig, VelloError, VelloRenderer, ViewTransform};
