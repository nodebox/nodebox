#!/bin/bash
# Build Debian package for NodeBox
# Usage: ./scripts/build-linux-deb.sh [--release|--debug]
#
# Prerequisites:
#   - dpkg-deb (usually pre-installed on Debian/Ubuntu)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
VERSION=$(grep '^version' "$PROJECT_ROOT/Cargo.toml" | head -1 | sed 's/.*"\(.*\)".*/\1/' | tr -d ' ')
ARCH=$(dpkg --print-architecture 2>/dev/null || echo "amd64")

# Default to release build
BUILD_TYPE="release"
CARGO_FLAGS="--release"

if [[ "$1" == "--debug" ]]; then
    BUILD_TYPE="debug"
    CARGO_FLAGS=""
fi

echo "Building NodeBox $VERSION ($BUILD_TYPE) for $ARCH..."

# Build the binary
cd "$PROJECT_ROOT"
cargo build $CARGO_FLAGS -p nodebox-gui

# Set up paths
BINARY_PATH="$PROJECT_ROOT/target/$BUILD_TYPE/nodebox-gui"
PKG_DIR="$PROJECT_ROOT/target/$BUILD_TYPE/nodebox_${VERSION}_${ARCH}"
DEB_PATH="$PROJECT_ROOT/target/$BUILD_TYPE/nodebox_${VERSION}_${ARCH}.deb"

# Clean up any existing package directory
rm -rf "$PKG_DIR"
rm -f "$DEB_PATH"

# Create package structure
mkdir -p "$PKG_DIR/DEBIAN"
mkdir -p "$PKG_DIR/usr/bin"
mkdir -p "$PKG_DIR/usr/share/applications"
mkdir -p "$PKG_DIR/usr/share/icons/hicolor/256x256/apps"
mkdir -p "$PKG_DIR/usr/share/icons/hicolor/128x128/apps"
mkdir -p "$PKG_DIR/usr/share/mime/packages"

# Copy binary
cp "$BINARY_PATH" "$PKG_DIR/usr/bin/nodebox"
chmod 755 "$PKG_DIR/usr/bin/nodebox"

# Copy libraries if they exist
if [ -d "$PROJECT_ROOT/libraries" ]; then
    mkdir -p "$PKG_DIR/usr/share/nodebox"
    cp -r "$PROJECT_ROOT/libraries" "$PKG_DIR/usr/share/nodebox/"
fi

# Create control file
cat > "$PKG_DIR/DEBIAN/control" << EOF
Package: nodebox
Version: $VERSION
Section: graphics
Priority: optional
Architecture: $ARCH
Maintainer: NodeBox Contributors <nodebox@example.com>
Description: Node-based generative design application
 NodeBox is a visual programming environment for creating
 generative graphics using a node-based workflow.
Homepage: https://github.com/nodebox/nodebox
EOF

# Create desktop file
cat > "$PKG_DIR/usr/share/applications/nodebox.desktop" << EOF
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

# Create MIME type file
cat > "$PKG_DIR/usr/share/mime/packages/nodebox.xml" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<mime-info xmlns="http://www.freedesktop.org/standards/shared-mime-info">
    <mime-type type="application/x-nodebox">
        <comment>NodeBox Document</comment>
        <glob pattern="*.ndbx"/>
        <icon name="nodebox"/>
    </mime-type>
</mime-info>
EOF

# Create postinst script for MIME type registration
cat > "$PKG_DIR/DEBIAN/postinst" << 'EOF'
#!/bin/bash
set -e
if [ "$1" = "configure" ]; then
    update-mime-database /usr/share/mime 2>/dev/null || true
    update-desktop-database /usr/share/applications 2>/dev/null || true
    gtk-update-icon-cache /usr/share/icons/hicolor 2>/dev/null || true
fi
EOF
chmod 755 "$PKG_DIR/DEBIAN/postinst"

# Create postrm script
cat > "$PKG_DIR/DEBIAN/postrm" << 'EOF'
#!/bin/bash
set -e
if [ "$1" = "remove" ]; then
    update-mime-database /usr/share/mime 2>/dev/null || true
    update-desktop-database /usr/share/applications 2>/dev/null || true
fi
EOF
chmod 755 "$PKG_DIR/DEBIAN/postrm"

# Note: You'll need to add icon files
# For now, create placeholder message
echo "Note: Add icon files to:"
echo "  - $PKG_DIR/usr/share/icons/hicolor/256x256/apps/nodebox.png"
echo "  - $PKG_DIR/usr/share/icons/hicolor/128x128/apps/nodebox.png"

# Build the package
dpkg-deb --build "$PKG_DIR"

echo ""
echo "Debian package created: $DEB_PATH"
echo ""
echo "Install with: sudo dpkg -i $DEB_PATH"
