[CmdletBinding()]
param(
    [switch]$Clean,
    # Skip the APK if you only need the Play bundle.
    [switch]$SkipApk,
    # Root containing jdk17/ and android-sdk/. Overrides auto-detection.
    [string]$ToolsRoot = "",
    [string]$ExpectedSigningSha256 = ""
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $ProjectRoot

# ---------------------------------------------------------------------------
# Toolchain resolution.
# The offline toolchain lives outside the repo (e.g. D:\hjw\codex\tools). A
# candidate is valid when it contains jdk17\ and android-sdk\.
# ---------------------------------------------------------------------------
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
    $jdk = Join-Path $candidate "jdk17"
    $sdk = Join-Path $candidate "android-sdk"
    if ((Test-Path $jdk) -and (Test-Path $sdk)) {
        $ResolvedToolsRoot = $candidate
        break
    }
}
if (-not $ResolvedToolsRoot) {
    throw ("Android build toolchain not found. Looked for jdk17\ and android-sdk\ under: " +
        ($ToolsCandidates -join "; ") +
        ". Pass -ToolsRoot <dir> or set QBR_TOOLS_DIR.")
}

$JavaHome = Join-Path $ResolvedToolsRoot "jdk17"
$AndroidHome = Join-Path $ResolvedToolsRoot "android-sdk"
$AppModuleDir = Join-Path $ProjectRoot "app"
$KeystorePropertiesPath = Join-Path $ProjectRoot "keystore.properties"

if (!(Test-Path $KeystorePropertiesPath)) {
    throw "keystore.properties not found: $KeystorePropertiesPath (copy keystore.properties.example)"
}

$env:JAVA_HOME = $JavaHome
$env:ANDROID_HOME = $AndroidHome
$env:ANDROID_SDK_ROOT = $AndroidHome
$env:PATH = "$JavaHome\bin;$AndroidHome\platform-tools;$env:PATH"

Write-Host "Toolchain: $ResolvedToolsRoot"

# ---------------------------------------------------------------------------
# Signing configuration + fixed-identity guard.
# ---------------------------------------------------------------------------
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

# Pass the store password via a transient file instead of the command line so it
# never shows up in the process list.
$StorePassFile = New-TemporaryFile
try {
    Set-Content -Path $StorePassFile -Value $ReleaseStorePassword -NoNewline -Encoding ascii
    $KeytoolOutput = & $Keytool `
        -list -v `
        -keystore $ResolvedStoreFile `
        -alias $ReleaseKeyAlias `
        -storepass:file $StorePassFile.FullName 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to read signing certificate from keystore: $ResolvedStoreFile`n$($KeytoolOutput -join [Environment]::NewLine)"
    }
} finally {
    Remove-Item -Path $StorePassFile -Force -ErrorAction SilentlyContinue
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

# ---------------------------------------------------------------------------
# Build: AAB for Google Play upload + (optionally) the signed release APK.
# ---------------------------------------------------------------------------
if ($Clean) {
    .\gradlew.bat clean
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

$GradleTasks = @("bundleRelease")
if (-not $SkipApk) { $GradleTasks += "assembleRelease" }

.\gradlew.bat @GradleTasks
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

# ---------------------------------------------------------------------------
# Collect outputs into dist\ with versioned names.
# ---------------------------------------------------------------------------
$VersionName = (Select-String -Path (Join-Path $AppModuleDir "build.gradle.kts") `
    -Pattern 'versionName\s*=\s*"([^"]+)"' | Select-Object -First 1).Matches[0].Groups[1].Value
if ([string]::IsNullOrWhiteSpace($VersionName)) {
    throw "Failed to parse versionName from app/build.gradle.kts"
}

$DistDir = Join-Path $ProjectRoot "dist"
New-Item -ItemType Directory -Path $DistDir -Force | Out-Null

$AabSource = Join-Path $AppModuleDir "build/outputs/bundle/release/app-release.aab"
$AabTarget = Join-Path $DistDir "qbitremote-v$VersionName.aab"
Copy-Item -Path $AabSource -Destination $AabTarget -Force
Write-Host "AAB : $AabTarget"

if (-not $SkipApk) {
    $ApkSource = Join-Path $AppModuleDir "build/outputs/apk/release/app-release.apk"
    $ApkTarget = Join-Path $DistDir "qbitremote-v$VersionName.apk"
    Copy-Item -Path $ApkSource -Destination $ApkTarget -Force
    Write-Host "APK : $ApkTarget"
}

$MappingSource = Join-Path $AppModuleDir "build/outputs/mapping/release/mapping.txt"
$MappingTarget = Join-Path $DistDir "mapping-v$VersionName.txt"
if (Test-Path $MappingSource) {
    Copy-Item -Path $MappingSource -Destination $MappingTarget -Force
    Write-Host "MAP : $MappingTarget (upload to Play Console for crash deobfuscation)"
}

# SHA256 manifest for all dist outputs of this version.
$ChecksumTargets = @($AabTarget)
if (-not $SkipApk) { $ChecksumTargets += $ApkTarget }
if (Test-Path $MappingSource) { $ChecksumTargets += $MappingTarget }
$ManifestPath = Join-Path $DistDir "SHA256SUMS-v$VersionName.txt"
$ChecksumTargets | ForEach-Object {
    $h = (Get-FileHash -Path $_ -Algorithm SHA256).Hash.ToLowerInvariant()
    "{0}  {1}" -f $h, (Split-Path -Leaf $_)
} | Set-Content -Path $ManifestPath -Encoding ascii
Write-Host "SUM : $ManifestPath"

Write-Host "Done. dist\ outputs are gitignored - distribute via GitHub Releases / Play Console."
