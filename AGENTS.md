# Repository Notes

## Project Layout

- Android app (Kotlin + Jetpack Compose), single `:app` module.
- `app/src/main/java/com/hjw/qbremote/data/` — backends (qBittorrent WebUI API,
  Transmission RPC), persistence (Preferences DataStore), credential storage
  (Android Keystore AES-GCM).
- `app/src/main/java/com/hjw/qbremote/ui/` — Compose UI + MainViewModel.
  Pure logic lives in `*Support.kt` files with matching JVM unit tests under
  `app/src/test/`.

## Build & Test (any machine)

- Requires JDK 17 and an Android SDK (`ANDROID_HOME`), nothing else.
- Unit tests: `./gradlew testDebugUnitTest`
- Lint: `./gradlew lintDebug`
- Debug build: `./gradlew assembleDebug`
- CI (`.github/workflows/ci.yml`) runs tests, lint, debug build and an unsigned
  release build (validates the R8 config) on every push.
- The Gradle wrapper is pinned with `distributionSha256Sum`; when upgrading
  Gradle, update both `distributionUrl` and the checksum (fetch
  `<distributionUrl>.sha256` from services.gradle.org).

## Android Release Packaging (owner's Windows machine)

- The offline toolchain (`jdk17\` + `android-sdk\`) lives OUTSIDE the repo at a
  sibling of the project root, e.g. `D:\hjw\codex\tools\android-build\tools`.
  Both build scripts auto-detect it (sibling `..\tools\android-build\tools`,
  or the `-ToolsRoot` / `QBR_TOOLS_DIR` override).
- Release command: `.\scripts\build-release-aab.ps1`
  - Produces BOTH the Play AAB and the signed release APK into `dist\`
    (gitignored), plus `mapping.txt` for Play Console crash deobfuscation.
  - Verifies the keystore SHA256 fingerprint against `RELEASE_KEY_SHA256`
    before building.
- `dist\` also gets `SHA256SUMS-vX.Y.Z.txt`; attach it (and the signed APK)
  to the GitHub Release.
- Pushing a `vX.Y.Z` tag runs `.github/workflows/release.yml`: it rebuilds
  unsigned artifacts + checksums as a cross-check and opens a draft GitHub
  Release. Signed binaries are still attached manually from `dist\`.
- Signing config comes from `keystore.properties` (see
  `keystore.properties.example`). NEVER commit the real file or the keystore.
- Do NOT replace or rotate the existing release signing config unless the user
  explicitly asks. This app must keep using the current fixed signing identity
  so Google Play updates continue to work.

## Hard Rules

- R8/minification stays ON for release. If a release build misbehaves, fix the
  specific keep rule in `app/proguard-rules.pro` — do not disable minification.
- Never commit binary artifacts (`*.apk`, `*.aab`, screenshots); distribute via
  GitHub Releases / Play Console (`.gitignore` enforces this).
- Gson-persisted models and enums under `data/` need keep rules; check
  `app/proguard-rules.pro` when adding persisted types.
- Release notes live in `docs/releases/vX.Y.Z.md` (lowercase `v`).
