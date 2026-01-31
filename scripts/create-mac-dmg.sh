#!/bin/bash
# Create a DMG installer for NodeBox
#
# Usage: ./scripts/create-mac-dmg.sh [--release|--debug]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
VERSION=$(grep '^version' "$PROJECT_ROOT/Cargo.toml" | head -1 | sed 's/.*"\(.*\)".*/\1/' | tr -d ' ')

# Default to release build
BUILD_TYPE="release"
if [[ "$1" == "--debug" ]]; then
    BUILD_TYPE="debug"
fi

BUNDLE_DIR="$PROJECT_ROOT/target/$BUILD_TYPE/NodeBox.app"
DMG_PATH="$PROJECT_ROOT/target/$BUILD_TYPE/NodeBox-$VERSION.dmg"
DMG_TEMP="$PROJECT_ROOT/target/$BUILD_TYPE/dmg-temp"

if [ ! -d "$BUNDLE_DIR" ]; then
    echo "Error: Bundle not found at $BUNDLE_DIR"
    echo "Run build-mac-bundle.sh first"
    exit 1
fi

echo "Creating DMG for NodeBox $VERSION..."

# Clean up any existing temp directory
rm -rf "$DMG_TEMP"
rm -f "$DMG_PATH"

# Create temp directory with bundle and Applications symlink
mkdir -p "$DMG_TEMP"
cp -R "$BUNDLE_DIR" "$DMG_TEMP/"
ln -s /Applications "$DMG_TEMP/Applications"

# Create DMG
hdiutil create -volname "NodeBox $VERSION" \
    -srcfolder "$DMG_TEMP" \
    -ov -format UDZO \
    "$DMG_PATH"

# Clean up
rm -rf "$DMG_TEMP"

echo ""
echo "DMG created: $DMG_PATH"
echo ""
echo "To sign the DMG (optional):"
echo "  codesign --sign \"\$DEVELOPER_ID_APPLICATION\" \"$DMG_PATH\""
