# Build WASM module and start the Electron dev server.
# Usage: .\scripts\dev.ps1

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir

Write-Host "==> Building WASM module..."
Set-Location "$ProjectRoot\crates"
wasm-pack build nodebox-electron --target web --out-dir "$ProjectRoot\electron-app\wasm"

Write-Host "==> Starting Electron dev server..."
Set-Location "$ProjectRoot\electron-app"
npm run dev
