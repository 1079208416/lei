param(
    [int]$BackendPort = 18080,
    [int]$FrontendPort = 5173
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$work = Join-Path $root "work"
New-Item -ItemType Directory -Path $work -Force | Out-Null

$backendOut = Join-Path $work "backend-dev.out.log"
$backendErr = Join-Path $work "backend-dev.err.log"
$frontendOut = Join-Path $work "frontend-dev.out.log"
$frontendErr = Join-Path $work "frontend-dev.err.log"

$backendArgs = @(
    "-NoProfile",
    "-ExecutionPolicy", "Bypass",
    "-File", (Join-Path $PSScriptRoot "start-backend.ps1"),
    "-Port", $BackendPort
)
$frontendArgs = @(
    "-NoProfile",
    "-ExecutionPolicy", "Bypass",
    "-File", (Join-Path $PSScriptRoot "start-frontend.ps1"),
    "-BackendPort", $BackendPort,
    "-FrontendPort", $FrontendPort
)

$backendProcess = Start-Process powershell.exe `
    -ArgumentList $backendArgs `
    -WorkingDirectory $root `
    -RedirectStandardOutput $backendOut `
    -RedirectStandardError $backendErr `
    -WindowStyle Hidden `
    -PassThru

$frontendProcess = Start-Process powershell.exe `
    -ArgumentList $frontendArgs `
    -WorkingDirectory $root `
    -RedirectStandardOutput $frontendOut `
    -RedirectStandardError $frontendErr `
    -WindowStyle Hidden `
    -PassThru

@{
    backend = $backendProcess.Id
    frontend = $frontendProcess.Id
} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $work "dev-pids.json") -Encoding UTF8

Write-Host "Backend starting: http://localhost:$BackendPort"
Write-Host "Frontend starting: http://localhost:$FrontendPort"
Write-Host "Logs: $work"
