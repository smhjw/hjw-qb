# QBRemote 0.1.20 验证报告

## 自动化结果

- `testDebugUnitTest`：170 tests，0 failures，0 errors，0 skipped。
- `verifyPureLogicCoverage`：通过（行覆盖率门槛 80%）。
- `verifyCriticalLogicCoverage`：通过（行覆盖率门槛 90%）。
- `compileDebugAndroidTestKotlin`：通过。
- `lintRelease`：0 errors，0 warnings。
- `:benchmark:assembleBenchmark`：通过。
- `assembleRelease bundleRelease`：通过。
- Bundletool 1.18.3 `validate`：通过；可生成使用固定密钥签名的 universal APK。

## 发布包核验

- applicationId：`com.hjw.qbremote`
- versionCode / versionName：`21` / `0.1.20`
- minSdk / targetSdk / compileSdk：`26` / `36` / `36`
- R8：关闭（`isMinifyEnabled=false`）
- 资源压缩：关闭（`isShrinkResources=false`）
- 发布证书 SHA-256：`BF:FC:E8:6C:02:F4:16:79:92:20:1B:57:E1:82:10:1E:93:EB:4D:BA:BE:01:D9:AB:6C:67:37:C1:4E:B6:9A:F2`
- 与已有 `qbremote-v0.1.15-no-r8-release.apk` 的证书 SHA-256 一致。
- APK SHA-256：`85549701C1F83C1A70D727583F71D9201A5952C4BF8F97A44B89969F2D8DA7C8`
- AAB SHA-256：`E422F5BE4CE4347E9C12A4EF9D59A1F7DFF39E3D18C12E545D2D3B00F8C4B7E3`

## 当前环境限制

- `adb devices` 无已连接设备。
- 本地没有可启动的 API 26/35/36 AVD。
- 因此未执行 `connectedDebugAndroidTest`、旧版本覆盖安装、bundletool 设备安装，以及真机上的拖动残影和无边框观感验收；这些项目不能标记为已验证。
