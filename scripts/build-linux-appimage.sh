#!/bin/bash
# Build AppImage for NodeBox
# Usage: ./scripts/build-linux-appimage.sh [--release|--debug]
#
# Prerequisites:
#   - appimagetool: https://github.com/AppImage/AppImageKit/releases

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
VERSION=$(grep '^version' "$PROJECT_ROOT/Cargo.toml" | head -1 | sed 's/.*"\(.*\)".*/\1/' | tr -d ' ')
ARCH=$(uname -m)

# Default to release build
BUILD_TYPE="release"
CARGO_FLAGS="--release"

if [[ "$1" == "--debug" ]]; then
    BUILD_TYPE="debug"
    CARGO_FLAGS=""
fi

echo "Building NodeBox $VERSION AppImage ($BUILD_TYPE) for $ARCH..."

# Check for appimagetool
if ! command -v appimagetool &> /dev/null; then
    echo "Error: appimagetool not found"
    echo ""
    echo "Install it from: https://github.com/AppImage/AppImageKit/releases"
    echo "Or run:"
    echo "  wget https://github.com/AppImage/AppImageKit/releases/download/continuous/appimagetool-x86_64.AppImage"
    echo "  chmod +x appimagetool-x86_64.AppImage"
    echo "  sudo mv appimagetool-x86_64.AppImage /usr/local/bin/appimagetool"
    exit 1
fi

# Build the binary
cd "$PROJECT_ROOT"
cargo build $CARGO_FLAGS -p nodebox-gui

# Set up paths
BINARY_PATH="$PROJECT_ROOT/target/$BUILD_TYPE/nodebox-gui"
APPDIR="$PROJECT_ROOT/target/$BUILD_TYPE/NodeBox.AppDir"
APPIMAGE_PATH="$PROJECT_ROOT/target/$BUILD_TYPE/NodeBox-$VERSION-$ARCH.AppImage"

# Clean up
rm -rf "$APPDIR"
rm -f "$APPIMAGE_PATH"

# Create AppDir structure
mkdir -p "$APPDIR/usr/bin"
mkdir -p "$APPDIR/usr/share/applications"
mkdir -p "$APPDIR/usr/share/icons/hicolor/256x256/apps"

# Copy binary
cp "$BINARY_PATH" "$APPDIR/usr/bin/nodebox"
chmod 755 "$APPDIR/usr/bin/nodebox"

# Copy libraries if they exist
if [ -d "$PROJECT_ROOT/libraries" ]; then
    mkdir -p "$APPDIR/usr/share/nodebox"
    cp -r "$PROJECT_ROOT/libraries" "$APPDIR/usr/share/nodebox/"
fi

# Create desktop file
cat > "$APPDIR/nodebox.desktop" << EOF
[Desktop Entry]
Name=NodeBox
Comment=Node-based generative design
Exec=nodebox %F
Icon=nodebox
Terminal=false
Type=Application
Categories=Graphics;2DGraphics;VectorGraphics;
MimeType=application/x-nodebox;
StartupNotify=true
EOF

# Copy desktop file to standard location too
cp "$APPDIR/nodebox.desktop" "$APPDIR/usr/share/applications/"

# Create AppRun script
cat > "$APPDIR/AppRun" << 'EOF'
#!/bin/bash
SELF=$(readlink -f "$0")
HERE=${SELF%/*}
export PATH="${HERE}/usr/bin:${PATH}"
export LD_LIBRARY_PATH="${HERE}/usr/lib:${LD_LIBRARY_PATH}"
exec "${HERE}/usr/bin/nodebox" "$@"
EOF
chmod 755 "$APPDIR/AppRun"

# Note about icons
echo "Note: Add icon file to: $APPDIR/nodebox.png (256x256 recommended)"
echo "      and: $APPDIR/usr/share/icons/hicolor/256x256/apps/nodebox.png"

# Create placeholder icon if none exists
if [ ! -f "$APPDIR/nodebox.png" ]; then
    # Create a simple placeholder (1x1 transparent PNG)
    echo "Creating placeholder icon..."
    printf '\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x08\x06\x00\x00\x00\x1f\x15\xc4\x89\x00\x00\x00\nIDATx\x9cc\x00\x01\x00\x00\x05\x00\x01\r\n-\xb4\x00\x00\x00\x00IEND\xaeB`\x82' > "$APPDIR/nodebox.png"
    cp "$APPDIR/nodebox.png" "$APPDIR/usr/share/icons/hicolor/256x256/apps/"
fi

# Build AppImage
ARCH=$ARCH appimagetool "$APPDIR" "$APPIMAGE_PATH"

echo ""
echo "AppImage created: $APPIMAGE_PATH"
echo ""
echo "Run with: chmod +x $APPIMAGE_PATH && $APPIMAGE_PATH"
