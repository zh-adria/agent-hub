param(
    [int]$BackendPort = 8080,
    [int]$FrontendPort = 5173
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$backendDir = Join-Path $repoRoot "backend"
$frontendDir = Join-Path $repoRoot "frontend"
$runDir = Join-Path $repoRoot ".run"
$logDir = Join-Path $runDir "logs"
$pidDir = Join-Path $runDir "pids"

New-Item -ItemType Directory -Force -Path $logDir, $pidDir | Out-Null

function Assert-FreePort($Port, $Name) {
    $conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($conn) {
        throw "$Name port $Port is already in use by process $($conn.OwningProcess). Run scripts\stop.bat first."
    }
}

function Wait-Http($Url, $Name) {
    for ($i = 0; $i -lt 90; $i++) {
        Start-Sleep -Seconds 1
        try {
            $body = & curl.exe -s -m 2 $Url
            if ($LASTEXITCODE -eq 0 -and $body) {
    Write-Output "$Name ready: $Url"
                return
            }
        } catch {}
    }
    throw "$Name did not become ready: $Url"
}

function Stop-StartedProcess($Process, $PidFile) {
    if ($Process -and -not $Process.HasExited) {
        Stop-Process -Id $Process.Id -Force -ErrorAction SilentlyContinue
    }
    if (Test-Path $PidFile) {
        Remove-Item -LiteralPath $PidFile -Force -ErrorAction SilentlyContinue
    }
}

if (-not (Test-Path (Join-Path $backendDir "pom.xml"))) {
    throw "backend/pom.xml not found"
}

if (-not (Test-Path (Join-Path $frontendDir "package.json"))) {
    throw "frontend/package.json not found"
}

Assert-FreePort $BackendPort "Backend"
Assert-FreePort $FrontendPort "Frontend"

if (-not (Test-Path (Join-Path $frontendDir "node_modules"))) {
    Write-Output "Installing frontend dependencies..."
    Push-Location $frontendDir
    try {
        & npm.cmd install
        if ($LASTEXITCODE -ne 0) {
            throw "npm install failed"
        }
    } finally {
        Pop-Location
    }
}

$env:SERVER_PORT = "$BackendPort"
if (-not $env:SPRING_PROFILES_ACTIVE) {
    $env:SPRING_PROFILES_ACTIVE = "dev"
}
$env:VITE_BACKEND_URL = "http://127.0.0.1:$BackendPort"
$backendOut = Join-Path $logDir "backend.out.log"
$backendErr = Join-Path $logDir "backend.err.log"
$backendPidFile = Join-Path $pidDir "backend.pid"
$backendProcess = Start-Process -FilePath "mvn.cmd" -ArgumentList @("-q", "spring-boot:run") -WorkingDirectory $backendDir -WindowStyle Hidden -PassThru -RedirectStandardOutput $backendOut -RedirectStandardError $backendErr
Set-Content -Path $backendPidFile -Value $backendProcess.Id

$frontendOut = Join-Path $logDir "frontend.out.log"
$frontendErr = Join-Path $logDir "frontend.err.log"
$frontendPidFile = Join-Path $pidDir "frontend.pid"
$frontendProcess = Start-Process -FilePath "npm.cmd" -ArgumentList @("run", "dev", "--", "--port", "$FrontendPort", "--host", "127.0.0.1") -WorkingDirectory $frontendDir -WindowStyle Hidden -PassThru -RedirectStandardOutput $frontendOut -RedirectStandardError $frontendErr
Set-Content -Path $frontendPidFile -Value $frontendProcess.Id

try {
    Wait-Http "http://127.0.0.1:$BackendPort/api/health" "Backend"
    Wait-Http "http://127.0.0.1:$FrontendPort" "Frontend"
} catch {
    Stop-StartedProcess $backendProcess $backendPidFile
    Stop-StartedProcess $frontendProcess $frontendPidFile
    Write-Output $_.Exception.Message
    Write-Output "Backend log: $backendOut"
    Write-Output "Frontend log: $frontendOut"
    throw
}

Write-Output "AgentHub started"
Write-Output "Backend:  http://127.0.0.1:$BackendPort"
Write-Output "Frontend: http://127.0.0.1:$FrontendPort"
