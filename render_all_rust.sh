#!/bin/bash
# Render all example .ndbx files to SVG using the Rust CLI renderer.
# Run from the project root directory.
set -e

echo "=== Building Rust CLI ==="
cargo build --release -p nodebox-cli

echo ""
echo "=== Rendering all examples to golden-master/rust/ ==="
target/release/nodebox-render --all examples golden-master/rust

echo ""
echo "=== Done ==="
echo "Output in golden-master/rust/"
find golden-master/rust -name "*.svg" | wc -l | xargs echo "Total SVGs:"
