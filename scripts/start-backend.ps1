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

function Get-JavaMajor($JavaHome) {
    if (-not $JavaHome) {
        return 0
    }
    $javaExe = Join-Path $JavaHome "bin\java.exe"
    if (-not (Test-Path $javaExe)) {
        return 0
    }
    $versionText = & $javaExe -version 2>&1 | Out-String
    if ($versionText -match 'version "(\d+)') {
        return [int]$Matches[1]
    }
    return 0
}

function Use-Jdk21 {
    $candidates = @()
    if ($env:JAVA_HOME) {
        $candidates += $env:JAVA_HOME
    }
    $roots = @(
        (Join-Path $env:ProgramFiles "Java"),
        ([Environment]::GetEnvironmentVariable("ProgramFiles(x86)")),
        "D:\Program Files",
        "D:\Program Files (x86)"
    ) | Where-Object { $_ -and (Test-Path $_) }
    foreach ($root in $roots) {
        $candidates += Get-ChildItem -LiteralPath $root -Directory -ErrorAction SilentlyContinue |
                Where-Object { $_.Name -like "jdk*" -or $_.Name -like "*jdk*" } |
                Select-Object -ExpandProperty FullName
    }
    foreach ($candidate in ($candidates | Select-Object -Unique)) {
        if ((Get-JavaMajor $candidate) -ge 21) {
            $env:JAVA_HOME = $candidate
            $env:Path = (Join-Path $candidate "bin") + [IO.Path]::PathSeparator + $env:Path
            Write-Host "Using JDK: $candidate"
            return
        }
    }
    throw "JDK 21 not found. Install JDK 21 or set JAVA_HOME to a JDK 21 directory."
}

Use-Jdk21

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
