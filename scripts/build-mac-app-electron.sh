#!/bin/bash
# Build macOS Electron app with signing and notarization.
# Usage: ./scripts/build-mac-app-electron.sh [--skip-wasm]
#
# Requires .env with:
#   APPLE_DEVELOPER_CERTIFICATE_NAME  (maps to CSC_NAME)
#   APPLE_ID                          (Apple ID email)
#   APPLE_ID_PASSWORD                 (app-specific password, maps to APPLE_PASSWORD)
#   APPLE_TEAM_ID                     (10-char team ID)

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

# Load .env if present
if [ -f "$ROOT_DIR/.env" ]; then
    echo "==> Loading .env..."
    set -a
    source "$ROOT_DIR/.env"
    set +a
fi

# Map signing variables to electron-builder conventions
export CSC_NAME="${APPLE_DEVELOPER_CERTIFICATE_NAME:-}"
export APPLE_PASSWORD="${APPLE_ID_PASSWORD:-}"

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
echo "==> Packaging macOS app..."
npx electron-builder --mac

echo ""
echo "==> Done! Output in electron-app/release/"
ls -la "$ELECTRON_DIR/release/"*.dmg 2>/dev/null || true
