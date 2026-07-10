param(
    [int]$BackendPort = 8080,
    [int]$FrontendPort = 5173
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$pidDir = Join-Path $repoRoot ".run\pids"

function Stop-ByPort($Port, $Name) {
    $connections = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    foreach ($conn in $connections) {
        try {
            Stop-Process -Id $conn.OwningProcess -Force -ErrorAction Stop
            Write-Host "Stopped $Name process $($conn.OwningProcess) on port $Port"
        } catch {
            Write-Host "Failed to stop $Name process $($conn.OwningProcess): $($_.Exception.Message)"
        }
    }
}

function Stop-ByPidFile($FileName, $Name) {
    $path = Join-Path $pidDir $FileName
    if (Test-Path $path) {
        $pidValue = (Get-Content -Path $path -Raw).Trim()
        if ($pidValue) {
            try {
                Stop-Process -Id ([int]$pidValue) -Force -ErrorAction Stop
                Write-Host "Stopped $Name launcher process $pidValue"
            } catch {
                Write-Host "$Name launcher process $pidValue already stopped"
            }
        }
        Remove-Item -LiteralPath $path -Force -ErrorAction SilentlyContinue
    }
}

Stop-ByPort $FrontendPort "frontend"
Stop-ByPort $BackendPort "backend"
Stop-ByPidFile "frontend.pid" "frontend"
Stop-ByPidFile "backend.pid" "backend"

Write-Host "AgentHub stopped"
exit 0
