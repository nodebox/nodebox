#!/usr/bin/env bash
# Build WASM module and start the Electron dev server.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "==> Building WASM module..."
cd "$ROOT_DIR/crates"
wasm-pack build nodebox-electron --target web --out-dir "$ROOT_DIR/electron-app/wasm"

echo "==> Starting Electron dev server..."
cd "$ROOT_DIR/electron-app"
npm run dev
