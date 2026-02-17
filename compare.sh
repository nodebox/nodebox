#!/bin/bash
# Compare Java and Rust SVG outputs.
# Compares path counts, viewBox, and key differences.

JAVA_DIR="golden-master/java"
RUST_DIR="golden-master/rust"

echo "=== Golden Master Comparison ==="
echo ""
printf "%-50s %8s %8s %s\n" "Example" "Java" "Rust" "Status"
printf "%-50s %8s %8s %s\n" "-------" "----" "----" "------"

total=0
match=0
mismatch=0
java_only=0
rust_only=0

find "$JAVA_DIR" -name "*.svg" | sort | while read java_file; do
    rel="${java_file#$JAVA_DIR/}"
    rust_file="$RUST_DIR/$rel"
    name=$(basename "$rel" .svg)

    java_paths=$(grep -c '<path ' "$java_file" 2>/dev/null || echo 0)

    if [ ! -f "$rust_file" ]; then
        printf "%-50s %8s %8s %s\n" "$name" "$java_paths" "-" "RUST_MISSING"
        continue
    fi

    rust_paths=$(grep -c '<path ' "$rust_file" 2>/dev/null || echo 0)

    if [ "$java_paths" = "$rust_paths" ]; then
        printf "%-50s %8s %8s %s\n" "$name" "$java_paths" "$rust_paths" "PATHS_MATCH"
    else
        printf "%-50s %8s %8s %s\n" "$name" "$java_paths" "$rust_paths" "PATHS_DIFFER"
    fi
done

# Check for Rust-only files
find "$RUST_DIR" -name "*.svg" | sort | while read rust_file; do
    rel="${rust_file#$RUST_DIR/}"
    java_file="$JAVA_DIR/$rel"
    if [ ! -f "$java_file" ]; then
        name=$(basename "$rel" .svg)
        rust_paths=$(grep -c '<path ' "$rust_file" 2>/dev/null || echo 0)
        printf "%-50s %8s %8s %s\n" "$name" "-" "$rust_paths" "JAVA_MISSING"
    fi
done
