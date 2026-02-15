//! Node graph evaluation and template definitions for NodeBox.
//!
//! This crate contains the evaluation engine, node templates, node factory,
//! and cancellation support. It is shared between desktop and WASM targets.

pub mod cancellation;
pub mod eval;
pub mod node_factory;
pub mod node_templates;

// Re-export key types for convenience
pub use cancellation::CancellationToken;
pub use eval::{EvalOutcome, EvalResult, NodeError, NodeOutput};
pub use node_factory::{create_node_from_template, template_has_compatible_input};
pub use node_templates::{NodeTemplate, NODE_TEMPLATES};
