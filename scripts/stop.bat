@echo off
setlocal

set "BACKEND_PORT=8080"
set "FRONTEND_PORT=5173"

if not "%~1"=="" set "BACKEND_PORT=%~1"
if not "%~2"=="" set "FRONTEND_PORT=%~2"

echo Stopping AgentHub...

REM Kill by PID files first
if exist ".run\pids\backend.pid" (
    for /f "delims=" %%i in (.run\pids\backend.pid) do (
        echo Stopping backend process %%i...
        taskkill /F /T /PID %%i 2>nul
    )
    del /f .run\pids\backend.pid 2>nul
)

if exist ".run\pids\frontend.pid" (
    for /f "delims=" %%i in (.run\pids\frontend.pid) do (
        echo Stopping frontend process %%i...
        taskkill /F /T /PID %%i 2>nul
    )
    del /f .run\pids\frontend.pid 2>nul
)

REM Kill by port - find PIDs listening on the configured ports
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%BACKEND_PORT%.*LISTENING" 2^>nul') do (
    echo Stopping backend on port %BACKEND_PORT%, PID %%a...
    taskkill /F /T /PID %%a 2>nul
)

for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%FRONTEND_PORT%.*LISTENING" 2^>nul') do (
    echo Stopping frontend on port %FRONTEND_PORT%, PID %%a...
    taskkill /F /T /PID %%a 2>nul
)

echo AgentHub stopped.
endlocal
