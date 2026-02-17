#!/bin/bash
# Render all example .ndbx files to SVG using the Java BatchRenderer.
# Run from the project root directory.
set -e

echo "=== Compiling Java code ==="
ant compile-dev

echo ""
echo "=== Rendering all examples to golden-master/java/ ==="
ant batch-render

echo ""
echo "=== Done ==="
echo "Output in golden-master/java/"
find golden-master/java -name "*.svg" | wc -l | xargs echo "Total SVGs:"
