@echo off
setlocal

set "BACKEND_PORT=8080"
set "FRONTEND_PORT=5173"

if not "%~1"=="" set "BACKEND_PORT=%~1"
if not "%~2"=="" set "FRONTEND_PORT=%~2"

echo Starting AgentHub (local dev mode)...
echo Backend port: %BACKEND_PORT%
echo Frontend port: %FRONTEND_PORT%
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-all.ps1" -BackendPort %BACKEND_PORT% -FrontendPort %FRONTEND_PORT%

if errorlevel 1 (
  echo AgentHub startup failed. Check .run\logs for details.
  if not "%AGENTHUB_NO_PAUSE%"=="1" pause
  exit /b 1
)

echo.
echo AgentHub started.
echo Backend:  http://127.0.0.1:%BACKEND_PORT%
echo Frontend: http://127.0.0.1:%FRONTEND_PORT%

if not "%AGENTHUB_NO_PAUSE%"=="1" pause

endlocal
