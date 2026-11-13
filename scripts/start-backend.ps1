param(
    [int]$Port = 18080
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $root "backend"
$mavenHome = Join-Path $root "work\maven-home"
$mavenRepo = Join-Path $root "work\m2"

New-Item -ItemType Directory -Path $mavenHome, $mavenRepo -Force | Out-Null

if (-not $env:JAVA_HOME -or -not (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
    $jdkCandidates = @(
        "D:\develop\jdk",
        "C:\Program Files\Java\jdk-21",
        "C:\Program Files\Java\jdk-17"
    )
    foreach ($candidate in $jdkCandidates) {
        if (Test-Path (Join-Path $candidate "bin\java.exe")) {
            $env:JAVA_HOME = $candidate
            break
        }
    }
}

if (-not $env:JAVA_HOME) {
    throw "JDK 17 or 21 is required. Set JAVA_HOME before starting the backend."
}

$env:MAVEN_USER_HOME = $mavenHome
$env:MAVEN_OPTS = "-Duser.home=`"$mavenHome`" -Dmaven.repo.local=`"$mavenRepo`""

Push-Location $backend
try {
    & ".\mvnw.cmd" spring-boot:run "-Dspring-boot.run.arguments=--server.port=$Port"
}
finally {
    Pop-Location
}
