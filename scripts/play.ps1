$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not $env:JAVA_HOME -or -not (Test-Path $env:JAVA_HOME)) {
    $candidates = @(
        "C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot",
        "C:\Program Files\Eclipse Adoptium\jdk-21.0.7.6-hotspot",
        "C:\Program Files\Java\jdk-21"
    )
    foreach ($candidate in $candidates) {
        if (Test-Path $candidate) {
            $env:JAVA_HOME = $candidate
            break
        }
    }
}

$jar = Get-ChildItem -Path "build\libs" -Filter "ruby-*.jar" -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notlike "*-sources.jar" } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

$newestSource = Get-ChildItem -Path "src" -Recurse -Include *.java,*.json |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

$needsBuild = -not $jar
if (-not $needsBuild -and $newestSource) {
    $needsBuild = $newestSource.LastWriteTime -gt $jar.LastWriteTime
}

if ($needsBuild) {
    Write-Host "Building Ruby..."
} else {
    Write-Host "Jar up to date, skipping compile..."
}

& .\gradlew.bat play
