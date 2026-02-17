# Build Windows Electron app (NSIS installer).
# Usage: .\scripts\build-windows-app-electron.ps1 [-SkipWasm]

param(
    [switch]$SkipWasm
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Split-Path -Parent $ScriptDir
$ElectronDir = Join-Path $RootDir "electron-app"

# Build WASM module
if (-not $SkipWasm) {
    Write-Host "==> Building WASM module..."
    Set-Location (Join-Path $RootDir "crates")
    wasm-pack build nodebox-electron --target web --out-dir (Join-Path $ElectronDir "wasm")
}

# Build renderer
Write-Host "==> Building Electron app (Vite)..."
Set-Location $ElectronDir
npm run build

# Package with electron-builder
Write-Host "==> Packaging Windows app..."
npx electron-builder --win

Write-Host ""
Write-Host "==> Done! Output in electron-app\release\"
Get-ChildItem (Join-Path $ElectronDir "release") -Filter "*.exe" -ErrorAction SilentlyContinue
