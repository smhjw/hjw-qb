# qbremote

`qbremote` is an Android app for remotely managing qBittorrent through the WebUI API.

## Project Snapshot

- App name: `qbremote`
- Application ID: `com.hjw.qbremote`
- Kotlin + Jetpack Compose (Material 3)
- Min SDK: `26`
- Target / Compile SDK: `35`
- Version: `0.1.1` (`versionCode = 2`)

## Core Features

### 1) Connection and Server Management

- Connect to qBittorrent using host/IP or full `http(s)://` URL
- HTTPS toggle and configurable refresh interval
- Multi-server profiles: save, switch, and reconnect quickly
- Encrypted password storage (`EncryptedSharedPreferences`) with legacy migration

### 2) Dashboard

- Global upload/download speed
- Total uploaded/downloaded traffic
- Upload/download rate limits
- Status pills: uploading, downloading, paused, error, checking, queued, total
- Optional chart panel with multiple metrics

### 3) Unified Torrent List

- Unified global list (not grouped by tracker/site)
- Sorting options:
  - Added time
  - Upload / Download speed
  - Share ratio
  - Total uploaded / downloaded
  - Torrent size
  - Activity time
  - Seeders / Leechers
  - Cross-seed count
- Every sort change automatically jumps to the first torrent
- Search supports: name, tags, category, hash, and save path
- Double-tap top area to quickly jump back to top

### 4) Torrent Operations

- Pause / Resume
- Delete torrent (with optional file deletion)

### 5) Torrent Detail Page

- Tabs: Info, Trackers, Peers, Files
- Rename torrent
- Change save path
- Change category and tags
- Set per-torrent upload/download limits
- Set share ratio
- Cross-seed details view

### 6) Add Torrent

- Add via Magnet/URL (multi-line supported)
- Add `.torrent` files from Android file picker
- Add options:
  - Auto torrent management
  - Pause after add
  - Skip hash checking
  - Sequential download
  - Prioritize first/last pieces
  - Upload/download limits

### 7) UX and Localization

- Dark / Light theme switch
- Chinese and English localization
- Adaptive auto-refresh by page context

## Reliability Improvements

- Full dashboard refresh with data sanitation
- Snapshot repair for suspicious torrent records
- Automatic re-login on `401/403`
- Retry with exponential backoff for network and server-side transient failures

## Build (Using Local Toolchain in `tools/`)

This project can be built with the bundled toolchain under:

- `tools/android-build/tools/jdk17`
- `tools/android-build/tools/android-sdk`

PowerShell example:

```powershell
$env:JAVA_HOME="D:\hjw\codex\qb-remote-android\tools\android-build\tools\jdk17"
$env:ANDROID_HOME="D:\hjw\codex\qb-remote-android\tools\android-build\tools\android-sdk"
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
$env:PATH="$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:PATH"

.\gradlew.bat assembleDebug
```

APK output:

- `app/build/outputs/apk/debug/app-debug.apk`

If you want a custom filename:

```powershell
Rename-Item "app/build/outputs/apk/debug/app-debug.apk" "qbremote.apk"
```

## qBittorrent WebUI Setup

In desktop qBittorrent:

1. Open `Tools -> Options -> Web UI`
2. Enable `Web User Interface (Remote control)`
3. Set the WebUI port (default `8080`)
4. Set username/password
5. Ensure LAN/WAN firewall rules allow access from your Android device

## Roadmap

- Batch operations for multi-selection
- More tracker/peer/file advanced controls
- Stronger release automation (CI + signed builds)
- More test coverage (ViewModel and repository layers)
