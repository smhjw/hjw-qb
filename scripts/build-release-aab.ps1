[CmdletBinding()]
param(
    [switch]$Clean,
    [string]$ExpectedSigningSha256 = ""
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $ProjectRoot

$JavaHome = Join-Path $ProjectRoot "tools/android-build/tools/jdk17"
$AndroidHome = Join-Path $ProjectRoot "tools/android-build/tools/android-sdk"
$AppModuleDir = Join-Path $ProjectRoot "app"
$KeystorePropertiesPath = Join-Path $ProjectRoot "keystore.properties"

if (!(Test-Path $JavaHome)) {
    throw "JDK not found: $JavaHome"
}
if (!(Test-Path $AndroidHome)) {
    throw "Android SDK not found: $AndroidHome"
}
if (!(Test-Path $KeystorePropertiesPath)) {
    throw "keystore.properties not found: $KeystorePropertiesPath"
}

$env:JAVA_HOME = $JavaHome
$env:ANDROID_HOME = $AndroidHome
$env:ANDROID_SDK_ROOT = $AndroidHome
$env:PATH = "$JavaHome\bin;$AndroidHome\platform-tools;$env:PATH"

$SigningProps = @{}
Get-Content -Path $KeystorePropertiesPath | ForEach-Object {
    $line = $_.Trim()
    if ([string]::IsNullOrWhiteSpace($line)) { return }
    if ($line.StartsWith("#")) { return }
    $index = $line.IndexOf("=")
    if ($index -lt 1) { return }
    $key = $line.Substring(0, $index).Trim()
    $value = $line.Substring($index + 1).Trim()
    if ($key) {
        $SigningProps[$key] = $value
    }
}

function Get-RequiredSigningProp {
    param([string]$Name)

    if (-not $SigningProps.ContainsKey($Name) -or [string]::IsNullOrWhiteSpace($SigningProps[$Name])) {
        throw "Missing required '$Name' in keystore.properties"
    }
    return $SigningProps[$Name]
}

$ReleaseStoreFile = Get-RequiredSigningProp -Name "RELEASE_STORE_FILE"
$ReleaseStorePassword = Get-RequiredSigningProp -Name "RELEASE_STORE_PASSWORD"
$ReleaseKeyAlias = Get-RequiredSigningProp -Name "RELEASE_KEY_ALIAS"
$DeclaredSigningSha256 = $SigningProps["RELEASE_KEY_SHA256"]

if ([System.IO.Path]::IsPathRooted($ReleaseStoreFile)) {
    $ResolvedStoreFile = $ReleaseStoreFile
} else {
    # Keep this aligned with app/build.gradle.kts where storeFile is resolved from module dir.
    $ResolvedStoreFile = Join-Path $AppModuleDir $ReleaseStoreFile
}
$ResolvedStoreFile = [System.IO.Path]::GetFullPath($ResolvedStoreFile)

if (!(Test-Path $ResolvedStoreFile)) {
    throw "Signing keystore not found: $ResolvedStoreFile"
}

$Keytool = Join-Path $JavaHome "bin/keytool.exe"
if (!(Test-Path $Keytool)) {
    throw "keytool not found: $Keytool"
}

$KeytoolOutput = & $Keytool `
    -list -v `
    -keystore $ResolvedStoreFile `
    -alias $ReleaseKeyAlias `
    -storepass $ReleaseStorePassword 2>&1
if ($LASTEXITCODE -ne 0) {
    throw "Failed to read signing certificate from keystore: $ResolvedStoreFile`n$($KeytoolOutput -join [Environment]::NewLine)"
}

$Sha256Line = ($KeytoolOutput | Select-String -Pattern "SHA256:\s*(.+)" | Select-Object -First 1)
if (-not $Sha256Line) {
    throw "Unable to parse SHA256 fingerprint from keystore output."
}

$ActualSigningSha256 = ($Sha256Line.Matches[0].Groups[1].Value).Trim()
$NormalizedActualSha256 = ($ActualSigningSha256 -replace "\s", "").ToUpperInvariant()

$ExpectedSha256Value = $ExpectedSigningSha256
if ([string]::IsNullOrWhiteSpace($ExpectedSha256Value)) {
    $ExpectedSha256Value = $DeclaredSigningSha256
}

if ([string]::IsNullOrWhiteSpace($ExpectedSha256Value)) {
    Write-Warning "RELEASE_KEY_SHA256 is not set. Current signing SHA256: $ActualSigningSha256"
    Write-Warning "Set RELEASE_KEY_SHA256 in keystore.properties to block accidental key changes."
} else {
    $NormalizedExpectedSha256 = ($ExpectedSha256Value -replace "\s", "").ToUpperInvariant()
    if ($NormalizedExpectedSha256 -ne $NormalizedActualSha256) {
        throw "Signing key SHA256 mismatch. Expected: $ExpectedSha256Value ; Actual: $ActualSigningSha256"
    }
    Write-Host "Signing key SHA256 verified: $ActualSigningSha256"
}

if ($Clean) {
    .\gradlew.bat clean
}

.\gradlew.bat bundleRelease

if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Write-Host "AAB generated: app/build/outputs/bundle/release/app-release.aab"
