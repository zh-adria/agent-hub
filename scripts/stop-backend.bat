@echo off
setlocal

set "PORT=8080"
if not "%~1"=="" set "PORT=%~1"

echo Stopping AgentHub backend on port %PORT%

powershell -NoProfile -ExecutionPolicy Bypass -Command "$conn = Get-NetTCPConnection -LocalPort %PORT% -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1; if ($conn) { Stop-Process -Id $conn.OwningProcess -Force; Write-Host ('Stopped process ' + $conn.OwningProcess) } else { Write-Host 'No listening process found.' }"

endlocal
