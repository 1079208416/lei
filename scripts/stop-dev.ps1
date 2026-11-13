$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$pidFile = Join-Path $root "work\dev-pids.json"

if (-not (Test-Path -LiteralPath $pidFile)) {
    Write-Host "No running process record found."
    exit 0
}

$pids = Get-Content -LiteralPath $pidFile -Raw | ConvertFrom-Json
foreach ($processId in @($pids.backend, $pids.frontend)) {
    if ($processId) {
        & taskkill.exe /PID $processId /T /F 2>$null | Out-Null
    }
}

Remove-Item -LiteralPath $pidFile -Force -ErrorAction SilentlyContinue
Write-Host "Local services stopped."
