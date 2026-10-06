# NodeBox in Rust

NodeBox is a node-based tool for generative design. This branch holds the Rust
rewrite. The Java application in `src/main/java` is the reference for behaviour
and is not changed.

## Running

```bash
cargo run                                        # the desktop app (egui)
cargo test --workspace --exclude nodebox-python  # all tests
cargo check --workspace --exclude nodebox-python # must report zero warnings
```

`nodebox-python` needs Python development headers, so it is left out by default.

## Crates

| Crate | Contents |
|-------|----------|
| `nodebox-core` | Geometry, node model, `.ndbx` files, node operations, evaluation, fonts, SVG export, undo history. No GUI code. Builds for `wasm32-unknown-unknown`. |
| `nodebox-vello` | Turns evaluated paths into a Vello scene and renders it with wgpu. |
| `nodebox-desktop` | The egui desktop app. |
| `nodebox-python` | Python bindings (pyo3). |

Code without GUI code in it belongs in `nodebox-core`, or in `nodebox-vello`
when it needs Vello, so that every front end can use it.

## Using the core as a library

```rust
use nodebox_core::geometry::{Color, Point};
use nodebox_core::ops;
use nodebox_core::svg::render_to_svg;

fn main() {
    let mut star = ops::star(Point::ZERO, 5, 80.0, 35.0);
    star.fill = Some(Color::hsb(0.6, 0.7, 0.9));
    let svg = render_to_svg(&[star], 200.0, 200.0);
    std::fs::write("star.svg", svg).unwrap();
}
```

`cargo run -p nodebox-core --example generate_svg` writes larger examples to
`./output/`.

## Evaluating a document

```rust
use std::sync::Arc;
use nodebox_core::eval::evaluate_network;
use nodebox_core::node::populate_default_ports;
use nodebox_core::platform::{Platform, ProjectContext, TestPlatform};

let mut library = nodebox_core::ndbx::parse_file("examples/01 Basics/01 Shape/01 Primitives/01 Primitives.ndbx")?;
populate_default_ports(&mut library.root);
let platform: Arc<dyn Platform> = Arc::new(TestPlatform::new());
let (paths, _output, errors) = evaluate_network(&library, &platform, &ProjectContext::new_unsaved());
```

A node type the evaluator does not implement is reported in `errors`.

## Porting nodes

`libraries/*/*.ndbx` and the Java functions are the source of truth. See
`AGENTS.md` for the checklist.
