@echo off
setlocal

set "PORT=8080"

if not "%~1"=="" set "PORT=%~1"

echo Starting AgentHub backend on port %PORT%

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-backend.ps1" -Port %PORT% -SkipTests

endlocal
