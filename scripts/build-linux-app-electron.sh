#!/bin/bash
# Build Linux Electron app (AppImage + deb).
# Usage: ./scripts/build-linux-app-electron.sh [--skip-wasm]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
ELECTRON_DIR="$ROOT_DIR/electron-app"

SKIP_WASM=false
for arg in "$@"; do
    case "$arg" in
        --skip-wasm) SKIP_WASM=true ;;
    esac
done

# Build WASM module
if [ "$SKIP_WASM" = false ]; then
    echo "==> Building WASM module..."
    cd "$ROOT_DIR/crates"
    wasm-pack build nodebox-electron --target web --out-dir "$ELECTRON_DIR/wasm"
fi

# Build renderer
echo "==> Building Electron app (Vite)..."
cd "$ELECTRON_DIR"
npm run build

# Package with electron-builder
echo "==> Packaging Linux app..."
npx electron-builder --linux

echo ""
echo "==> Done! Output in electron-app/release/"
ls -la "$ELECTRON_DIR/release/"*.AppImage "$ELECTRON_DIR/release/"*.deb 2>/dev/null || true
