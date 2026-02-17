#!/bin/bash
# Build macOS Electron app with signing and notarization.
# Usage: ./scripts/build-mac-app-electron.sh [--skip-wasm]
#
# Optional .env variables for code signing + notarization:
#   APPLE_DEVELOPER_CERTIFICATE_NAME  (maps to CSC_NAME)
#   APPLE_ID                          (Apple ID email)
#   APPLE_ID_PASSWORD                 (app-specific password)
#   APPLE_TEAM_ID                     (10-char team ID)
#
# Without these, the app is built unsigned (ad-hoc).

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

# Check if a valid signing certificate is available in the keychain.
CAN_SIGN=false
CSC_RAW="${APPLE_DEVELOPER_CERTIFICATE_NAME:-}"
if [ -n "$CSC_RAW" ] && security find-identity -v -p codesigning 2>/dev/null | grep -q "Developer ID Application"; then
    # electron-builder expects just the name/team, not the "Developer ID Application:" prefix.
    export CSC_NAME="${CSC_RAW#Developer ID Application: }"
    CAN_SIGN=true
    echo "==> Code signing enabled (CSC_NAME=$CSC_NAME)"
else
    export CSC_IDENTITY_AUTO_DISCOVERY=false
    echo "==> No signing certificate — building unsigned"
fi

# Map notarization variables (electron-builder 25+ convention).
# Only notarize when the app is properly signed.
if [ "$CAN_SIGN" = true ] && [ -n "${APPLE_ID:-}" ] && [ -n "${APPLE_ID_PASSWORD:-}" ] && [ -n "${APPLE_TEAM_ID:-}" ]; then
    export APPLE_APP_SPECIFIC_PASSWORD="${APPLE_ID_PASSWORD}"
    echo "==> Notarization enabled"
else
    # Unset Apple env vars so electron-builder doesn't attempt notarization.
    unset APPLE_ID APPLE_TEAM_ID APPLE_ID_PASSWORD APPLE_APP_SPECIFIC_PASSWORD 2>/dev/null || true
    echo "==> Notarization skipped (unsigned or missing credentials)"
fi

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
