param(
    [switch]$Clean
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $ProjectRoot

$JavaHome = Join-Path $ProjectRoot "tools/android-build/tools/jdk17"
$AndroidHome = Join-Path $ProjectRoot "tools/android-build/tools/android-sdk"

if (!(Test-Path $JavaHome)) {
    throw "JDK not found: $JavaHome"
}
if (!(Test-Path $AndroidHome)) {
    throw "Android SDK not found: $AndroidHome"
}

$env:JAVA_HOME = $JavaHome
$env:ANDROID_HOME = $AndroidHome
$env:ANDROID_SDK_ROOT = $AndroidHome
$env:PATH = "$JavaHome\bin;$AndroidHome\platform-tools;$env:PATH"

if ($Clean) {
    .\gradlew.bat clean
}

.\gradlew.bat bundleRelease

if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Write-Host "AAB generated: app/build/outputs/bundle/release/app-release.aab"
