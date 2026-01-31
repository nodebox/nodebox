# Build Windows installer for NodeBox
# Usage: .\scripts\build-windows-installer.ps1 [-Debug]
#
# Prerequisites:
#   - Install WiX Toolset v3: https://wixtoolset.org/
#   - Install cargo-wix: cargo install cargo-wix

param(
    [switch]$Debug
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir

# Get version from Cargo.toml
$CargoContent = Get-Content "$ProjectRoot\Cargo.toml" -Raw
if ($CargoContent -match 'version\s*=\s*"([^"]+)"') {
    $Version = $Matches[1]
} else {
    $Version = "0.1.0"
}

Write-Host "Building NodeBox $Version for Windows..."

# Build type
$BuildType = if ($Debug) { "debug" } else { "release" }
$CargoFlags = if ($Debug) { @() } else { @("--release") }

# Build the binary
Set-Location $ProjectRoot
cargo build @CargoFlags -p nodebox-gui

$BinaryPath = "$ProjectRoot\target\$BuildType\nodebox-gui.exe"
$OutputDir = "$ProjectRoot\target\$BuildType\installer"

# Create output directory
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

# Copy binary
Copy-Item $BinaryPath "$OutputDir\NodeBox.exe"

# Copy libraries if they exist
if (Test-Path "$ProjectRoot\libraries") {
    Copy-Item -Recurse "$ProjectRoot\libraries" "$OutputDir\"
}

# Copy icon
$IconSrc = "$ProjectRoot\platform\windows\installer\nodebox.ico"
if (Test-Path $IconSrc) {
    Copy-Item $IconSrc "$OutputDir\"
}

Write-Host ""
Write-Host "Build complete! Files in: $OutputDir"
Write-Host ""
Write-Host "To create an MSI installer:"
Write-Host "  1. Install WiX Toolset: https://wixtoolset.org/"
Write-Host "  2. Install cargo-wix: cargo install cargo-wix"
Write-Host "  3. Run: cargo wix -p nodebox-gui"
Write-Host ""
Write-Host "Or use the portable executable directly: $OutputDir\NodeBox.exe"
