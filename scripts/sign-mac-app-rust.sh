#!/bin/bash
# Sign macOS application bundle for NodeBox
#
# Required environment variables (can be set in .env file):
#   APPLE_DEVELOPER_CERTIFICATE_NAME - Developer ID Application certificate name
#                              e.g., "Developer ID Application: Your Name (TEAMID)"
#
# Optional environment variables:
#   KEYCHAIN_PROFILE - Notarization keychain profile name (default: "nodebox-notarize")
#
# Usage: ./scripts/sign-mac-app-rust.sh [--release|--debug]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Load .env file if it exists
if [ -f "$PROJECT_ROOT/.env" ]; then
    set -a
    source "$PROJECT_ROOT/.env"
    set +a
fi

# Check required environment variables
if [ -z "$APPLE_DEVELOPER_CERTIFICATE_NAME" ]; then
    echo "Error: APPLE_DEVELOPER_CERTIFICATE_NAME environment variable not set"
    echo ""
    echo "Set it in .env file or export it:"
    echo "  export APPLE_DEVELOPER_CERTIFICATE_NAME=\"Developer ID Application: Your Name (TEAMID)\""
    echo ""
    echo "To find your certificate name, run:"
    echo "  security find-identity -v -p codesigning"
    exit 1
fi

# Default to release build
BUILD_TYPE="release"
if [[ "$1" == "--debug" ]]; then
    BUILD_TYPE="debug"
fi

BUNDLE_DIR="$PROJECT_ROOT/target/$BUILD_TYPE/NodeBox.app"
ENTITLEMENTS="$PROJECT_ROOT/platform/mac/NodeBox.entitlements"

if [ ! -d "$BUNDLE_DIR" ]; then
    echo "Error: Bundle not found at $BUNDLE_DIR"
    echo "Run build-mac-app-rust.sh first"
    exit 1
fi

echo "Signing NodeBox.app with: $APPLE_DEVELOPER_CERTIFICATE_NAME"

# Sign all frameworks and dylibs first (if any)
find "$BUNDLE_DIR" -type f \( -name "*.dylib" -o -name "*.framework" \) -print0 2>/dev/null | while IFS= read -r -d '' file; do
    echo "Signing: $file"
    codesign --force --options runtime --timestamp \
        --sign "$APPLE_DEVELOPER_CERTIFICATE_NAME" \
        --entitlements "$ENTITLEMENTS" \
        "$file"
done

# Sign the main executable
echo "Signing main executable..."
codesign --force --options runtime --timestamp \
    --sign "$APPLE_DEVELOPER_CERTIFICATE_NAME" \
    --entitlements "$ENTITLEMENTS" \
    "$BUNDLE_DIR/Contents/MacOS/NodeBox"

# Sign the entire bundle
echo "Signing bundle..."
codesign --force --options runtime --timestamp \
    --sign "$APPLE_DEVELOPER_CERTIFICATE_NAME" \
    --entitlements "$ENTITLEMENTS" \
    "$BUNDLE_DIR"

# Verify signature
echo ""
echo "Verifying signature..."
codesign --verify --verbose=2 "$BUNDLE_DIR"
spctl --assess --verbose=2 "$BUNDLE_DIR" 2>&1 || true

echo ""
echo "Bundle signed successfully!"
echo "To notarize: ./scripts/notarize-mac-app-rust.sh"
