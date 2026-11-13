param(
    [int]$BackendPort = 18080,
    [int]$FrontendPort = 5173
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$frontend = Join-Path $root "frontend"

Push-Location $frontend
try {
    if (-not (Test-Path ".\node_modules")) {
        & npm.cmd install
    }
    $env:VITE_API_PROXY_TARGET = "http://localhost:$BackendPort"
    & npm.cmd run dev -- --host 127.0.0.1 --port $FrontendPort
}
finally {
    Pop-Location
}
