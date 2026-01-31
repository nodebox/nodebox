#!/bin/bash
# Build macOS application bundle for NodeBox
# Usage: ./scripts/build-mac-bundle.sh [--release]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
VERSION=$(grep '^version' "$PROJECT_ROOT/Cargo.toml" | head -1 | sed 's/.*"\(.*\)".*/\1/' | tr -d ' ')

# Default to release build
BUILD_TYPE="release"
CARGO_FLAGS="--release"

if [[ "$1" == "--debug" ]]; then
    BUILD_TYPE="debug"
    CARGO_FLAGS=""
fi

echo "Building NodeBox $VERSION ($BUILD_TYPE)..."

# Build the binary
cd "$PROJECT_ROOT"
cargo build $CARGO_FLAGS -p nodebox-gui

# Set up paths
BINARY_PATH="$PROJECT_ROOT/target/$BUILD_TYPE/nodebox-gui"
BUNDLE_DIR="$PROJECT_ROOT/target/$BUILD_TYPE/NodeBox.app"
CONTENTS_DIR="$BUNDLE_DIR/Contents"
MACOS_DIR="$CONTENTS_DIR/MacOS"
RESOURCES_DIR="$CONTENTS_DIR/Resources"

# Clean up any existing bundle
rm -rf "$BUNDLE_DIR"

# Create bundle structure
mkdir -p "$MACOS_DIR"
mkdir -p "$RESOURCES_DIR"

# Copy binary
cp "$BINARY_PATH" "$MACOS_DIR/NodeBox"

# Copy icons
cp "$PROJECT_ROOT/platform/mac/res/Resources/nodebox.icns" "$RESOURCES_DIR/"
cp "$PROJECT_ROOT/platform/mac/res/Resources/nodeboxfile.icns" "$RESOURCES_DIR/"

# Copy libraries (for .ndbx files)
if [ -d "$PROJECT_ROOT/libraries" ]; then
    cp -r "$PROJECT_ROOT/libraries" "$RESOURCES_DIR/"
fi

# Create Info.plist for native Rust binary
cat > "$CONTENTS_DIR/Info.plist" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleDevelopmentRegion</key>
    <string>en</string>
    <key>CFBundleDocumentTypes</key>
    <array>
        <dict>
            <key>CFBundleTypeExtensions</key>
            <array>
                <string>ndbx</string>
            </array>
            <key>CFBundleTypeIconFile</key>
            <string>nodeboxfile</string>
            <key>CFBundleTypeName</key>
            <string>NodeBox Document</string>
            <key>CFBundleTypeRole</key>
            <string>Editor</string>
            <key>LSHandlerRank</key>
            <string>Owner</string>
        </dict>
    </array>
    <key>CFBundleExecutable</key>
    <string>NodeBox</string>
    <key>CFBundleIconFile</key>
    <string>nodebox</string>
    <key>CFBundleIconName</key>
    <string>nodebox</string>
    <key>CFBundleIdentifier</key>
    <string>be.emrg.nodebox</string>
    <key>CFBundleInfoDictionaryVersion</key>
    <string>6.0</string>
    <key>CFBundleName</key>
    <string>NodeBox</string>
    <key>CFBundleDisplayName</key>
    <string>NodeBox</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleShortVersionString</key>
    <string>$VERSION</string>
    <key>CFBundleVersion</key>
    <string>$VERSION</string>
    <key>LSMinimumSystemVersion</key>
    <string>10.15</string>
    <key>NSHighResolutionCapable</key>
    <true/>
    <key>NSSupportsAutomaticGraphicsSwitching</key>
    <true/>
</dict>
</plist>
EOF

# Create PkgInfo
echo -n "APPL????" > "$CONTENTS_DIR/PkgInfo"

echo "Bundle created at: $BUNDLE_DIR"
echo ""
echo "To run: open '$BUNDLE_DIR'"
echo "To sign: ./scripts/sign-mac-bundle.sh"
