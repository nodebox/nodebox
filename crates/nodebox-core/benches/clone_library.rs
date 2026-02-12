//! Benchmark: NodeLibrary clone cost at various document sizes.
//!
//! Measures deep clone (`NodeLibrary::clone`) vs. `Arc::clone` to quantify
//! the potential benefit of a copy-on-write approach.

use std::hint::black_box;
use std::sync::Arc;
use std::time::{Duration, Instant};

use nodebox_core::node::{Connection, Node, NodeLibrary, Port};
use nodebox_core::geometry::Point;
use nodebox_core::Color;

/// Build a realistic node with typical ports.
fn make_shape_node(name: &str, proto: &str) -> Node {
    Node::new(name)
        .with_prototype(proto)
        .with_position(1.0, 2.0)
        .with_input(Port::point("position", Point::ZERO))
        .with_input(Port::float("width", 100.0))
        .with_input(Port::float("height", 100.0))
        .with_input(Port::color("fill", Color::rgba(0.8, 0.2, 0.1, 1.0)))
        .with_input(Port::color("stroke", Color::rgba(0.0, 0.0, 0.0, 1.0)))
        .with_input(Port::float("strokeWidth", 1.0))
}

/// Build a NodeLibrary with `n` nodes and ~n connections between them.
fn build_library(node_count: usize) -> NodeLibrary {
    let mut library = NodeLibrary::new("bench_document");
    library.set_width(1000.0);
    library.set_height(1000.0);

    let protos = [
        "corevector.ellipse",
        "corevector.rect",
        "corevector.polygon",
        "corevector.star",
        "corevector.colorize",
        "corevector.translate",
        "corevector.rotate",
        "corevector.scale",
    ];

    let mut root = Node::network("root");

    // Create nodes
    for i in 0..node_count {
        let proto = protos[i % protos.len()];
        let name = format!("node{}", i);
        root.children.push(make_shape_node(&name, proto));
    }

    // Create connections: each node (except the first) connects to the previous one
    for i in 1..node_count {
        root.connections.push(Connection::new(
            format!("node{}", i - 1),
            format!("node{}", i),
            "shape",
        ));
    }

    if node_count > 0 {
        root.rendered_child = Some(format!("node{}", node_count - 1));
    }

    library.root = root;
    library
}

/// Time a closure over `iterations` runs, return (total, per-iteration average).
fn bench<F: FnMut()>(mut f: F, iterations: u32) -> (Duration, Duration) {
    // Warmup
    for _ in 0..iterations.min(100) {
        f();
    }

    let start = Instant::now();
    for _ in 0..iterations {
        f();
    }
    let total = start.elapsed();
    (total, total / iterations)
}

fn main() {
    let sizes = [5, 20, 50, 100, 200];
    let iterations = 10_000;

    println!("NodeLibrary Clone Benchmark");
    println!("===========================");
    println!("Iterations per measurement: {iterations}\n");

    println!(
        "{:>6} | {:>12} | {:>12} | {:>12} | {:>10}",
        "Nodes", "Deep Clone", "Arc::clone", "Arc+make_mut", "Speedup"
    );
    println!("{}", "-".repeat(72));

    for &n in &sizes {
        let lib = build_library(n);
        let arc_lib = Arc::new(build_library(n));

        // 1. Deep clone (current behavior: what happens every render dispatch)
        let (_, deep_avg) = bench(
            || {
                black_box(lib.clone());
            },
            iterations,
        );

        // 2. Arc::clone (proposed: what render dispatch would cost with Arc)
        let (_, arc_avg) = bench(
            || {
                black_box(Arc::clone(&arc_lib));
            },
            iterations,
        );

        // 3. Arc::clone + Arc::make_mut (proposed: worst case mutation while render holds ref)
        // Simulates: render holds an Arc, UI needs to mutate → COW clone triggered
        let (_, cow_avg) = bench(
            || {
                let render_ref = Arc::clone(&arc_lib); // render worker holds this
                let mut ui_ref = Arc::clone(&arc_lib); // UI holds this
                Arc::make_mut(&mut ui_ref); // triggers COW clone
                black_box(render_ref);
                black_box(ui_ref);
            },
            iterations,
        );

        let speedup = deep_avg.as_nanos() as f64 / arc_avg.as_nanos().max(1) as f64;

        println!(
            "{:>6} | {:>9.1?} | {:>9.1?} | {:>9.1?} | {:>8.0}x",
            n, deep_avg, arc_avg, cow_avg, speedup
        );
    }

    println!();
    println!("Legend:");
    println!("  Deep Clone    = full NodeLibrary::clone() [current, every render]");
    println!("  Arc::clone    = Arc reference count bump [proposed, every render]");
    println!("  Arc+make_mut  = COW clone [proposed, only when mutating during active render]");

    // Also measure memory: approximate size of the library by serializing
    println!();
    println!("Approximate struct sizes (std::mem::size_of):");
    println!("  NodeLibrary: {} bytes", std::mem::size_of::<NodeLibrary>());
    println!("  Node:        {} bytes", std::mem::size_of::<Node>());
    println!("  Port:        {} bytes", std::mem::size_of::<Port>());
    println!("  Connection:  {} bytes", std::mem::size_of::<Connection>());

    // Rough heap size estimate for largest library
    let large_lib = build_library(200);
    let node_count = large_lib.root.children.len();
    let conn_count = large_lib.root.connections.len();
    let port_count: usize = large_lib.root.children.iter().map(|n| n.inputs.len()).sum();
    println!();
    println!("200-node library stats:");
    println!("  {node_count} nodes, {conn_count} connections, {port_count} total ports");
}
