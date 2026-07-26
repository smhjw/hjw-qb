[CmdletBinding()]
param(
    [switch]$Clean,
    # Root containing jdk17/ and android-sdk/. Overrides auto-detection.
    [string]$ToolsRoot = ""
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $ProjectRoot

$ToolsCandidates = @()
if ($ToolsRoot) { $ToolsCandidates += $ToolsRoot }
if ($env:QBR_TOOLS_DIR) { $ToolsCandidates += $env:QBR_TOOLS_DIR }
$ToolsCandidates += @(
    (Join-Path (Split-Path -Parent $ProjectRoot) "tools/android-build/tools"),
    (Join-Path (Split-Path -Parent $ProjectRoot) "tools")
)

$ResolvedToolsRoot = $null
foreach ($candidate in $ToolsCandidates) {
    if (-not $candidate) { continue }
    if ((Test-Path (Join-Path $candidate "jdk17")) -and (Test-Path (Join-Path $candidate "android-sdk"))) {
        $ResolvedToolsRoot = $candidate
        break
    }
}
if (-not $ResolvedToolsRoot) {
    throw ("Android build toolchain not found (need jdk17\ and android-sdk\). " +
        "Pass -ToolsRoot or set QBR_TOOLS_DIR. Tried: " + ($ToolsCandidates -join "; "))
}

$JavaHome = Join-Path $ResolvedToolsRoot "jdk17"
$AndroidHome = Join-Path $ResolvedToolsRoot "android-sdk"

$env:JAVA_HOME = $JavaHome
$env:ANDROID_HOME = $AndroidHome
$env:ANDROID_SDK_ROOT = $AndroidHome
$env:PATH = "$JavaHome\bin;$AndroidHome\platform-tools;$env:PATH"

$GradleArgs = @()
if ($Clean) {
    $GradleArgs += "clean"
}
$GradleArgs += "assembleDebug"

& .\gradlew.bat @GradleArgs

if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Write-Host "APK generated: app/build/outputs/apk/debug/app-debug.apk"
