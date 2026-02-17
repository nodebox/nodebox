#!/bin/bash
# Notarize macOS application bundle for NodeBox
#
# Required environment variables (set in .env file via ./scripts/setup-secrets.sh):
#   APPLE_ID          - Apple ID email for notarization
#   APPLE_ID_PASSWORD - App-specific password for notarization
#   APPLE_TEAM_ID     - Apple Developer Team ID
#
# Usage: ./scripts/notarize-mac-app-rust.sh [--release|--debug]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Load .env file if it exists
if [ -f "$PROJECT_ROOT/.env" ]; then
    set -a
    source "$PROJECT_ROOT/.env"
    set +a
fi

# Validate required environment variables
if [ -z "$APPLE_ID" ] || [ -z "$APPLE_ID_PASSWORD" ] || [ -z "$APPLE_TEAM_ID" ]; then
    echo "Error: Missing required environment variables"
    echo "Required: APPLE_ID, APPLE_ID_PASSWORD, APPLE_TEAM_ID"
    echo "Run ./scripts/setup-secrets.sh to create the .env file"
    exit 1
fi

# Default to release build
BUILD_TYPE="release"
if [[ "$1" == "--debug" ]]; then
    BUILD_TYPE="debug"
fi

BUNDLE_DIR="$PROJECT_ROOT/target/$BUILD_TYPE/NodeBox.app"
ZIP_PATH="$PROJECT_ROOT/target/$BUILD_TYPE/NodeBox.zip"

if [ ! -d "$BUNDLE_DIR" ]; then
    echo "Error: Bundle not found at $BUNDLE_DIR"
    echo "Run build-mac-app-rust.sh and sign-mac-app-rust.sh first"
    exit 1
fi

# Verify it's signed
echo "Verifying signature..."
if ! codesign --verify "$BUNDLE_DIR" 2>/dev/null; then
    echo "Error: Bundle is not properly signed"
    echo "Run sign-mac-app-rust.sh first"
    exit 1
fi

# Create ZIP for notarization
echo "Creating ZIP archive..."
rm -f "$ZIP_PATH"
ditto -c -k --keepParent "$BUNDLE_DIR" "$ZIP_PATH"

# Submit for notarization
echo "Submitting for notarization..."
xcrun notarytool submit "$ZIP_PATH" \
    --apple-id "$APPLE_ID" \
    --team-id "$APPLE_TEAM_ID" \
    --password "$APPLE_ID_PASSWORD" \
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
