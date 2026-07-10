param(
    [int]$Port = 8080,
    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$backendDir = Join-Path $repoRoot "backend"

if (-not (Test-Path (Join-Path $backendDir "pom.xml"))) {
    throw "backend/pom.xml not found"
}

$env:SERVER_PORT = "$Port"
if (-not $env:SPRING_PROFILES_ACTIVE) {
    $env:SPRING_PROFILES_ACTIVE = "dev"
}

$mvnArgs = @("spring-boot:run")

if ($SkipTests) {
    $mvnArgs += "-DskipTests"
}

Write-Host "Starting AgentHub backend"
Write-Host "Backend: http://127.0.0.1:$Port"
Write-Host "Health:  http://127.0.0.1:$Port/api/health"

Push-Location $backendDir
try {
    & mvn @mvnArgs
} finally {
    Pop-Location
}
