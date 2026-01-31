#!/bin/bash
# Notarize macOS application bundle for NodeBox
#
# Prerequisites:
# 1. Store credentials in keychain (one-time setup):
#    xcrun notarytool store-credentials "nodebox-notarize" \
#        --apple-id "your@email.com" \
#        --team-id "TEAMID" \
#        --password "app-specific-password"
#
# Required environment variables (can be set in .env file):
#   KEYCHAIN_PROFILE - Notarization keychain profile name (default: "nodebox-notarize")
#
# Usage: ./scripts/notarize-mac-bundle.sh [--release|--debug]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Load .env file if it exists
if [ -f "$PROJECT_ROOT/.env" ]; then
    set -a
    source "$PROJECT_ROOT/.env"
    set +a
fi

# Default keychain profile
KEYCHAIN_PROFILE="${KEYCHAIN_PROFILE:-nodebox-notarize}"

# Default to release build
BUILD_TYPE="release"
if [[ "$1" == "--debug" ]]; then
    BUILD_TYPE="debug"
fi

BUNDLE_DIR="$PROJECT_ROOT/target/$BUILD_TYPE/NodeBox.app"
ZIP_PATH="$PROJECT_ROOT/target/$BUILD_TYPE/NodeBox.zip"

if [ ! -d "$BUNDLE_DIR" ]; then
    echo "Error: Bundle not found at $BUNDLE_DIR"
    echo "Run build-mac-bundle.sh and sign-mac-bundle.sh first"
    exit 1
fi

# Verify it's signed
echo "Verifying signature..."
if ! codesign --verify "$BUNDLE_DIR" 2>/dev/null; then
    echo "Error: Bundle is not properly signed"
    echo "Run sign-mac-bundle.sh first"
    exit 1
fi

# Create ZIP for notarization
echo "Creating ZIP archive..."
rm -f "$ZIP_PATH"
ditto -c -k --keepParent "$BUNDLE_DIR" "$ZIP_PATH"

# Submit for notarization
echo "Submitting for notarization..."
echo "Using keychain profile: $KEYCHAIN_PROFILE"
xcrun notarytool submit "$ZIP_PATH" \
    --keychain-profile "$KEYCHAIN_PROFILE" \
    --wait

# Staple the notarization ticket
echo "Stapling notarization ticket..."
xcrun stapler staple "$BUNDLE_DIR"

# Verify notarization
echo ""
echo "Verifying notarization..."
spctl --assess --verbose=2 "$BUNDLE_DIR"

# Recreate the ZIP with stapled bundle
echo "Creating final ZIP with stapled notarization..."
rm -f "$ZIP_PATH"
ditto -c -k --keepParent "$BUNDLE_DIR" "$ZIP_PATH"

echo ""
echo "Notarization complete!"
echo "Distribution ZIP: $ZIP_PATH"
