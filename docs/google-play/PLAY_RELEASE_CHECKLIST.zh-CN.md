# Google Play 上架清单（qbremote）

## 1. 代码与包体准备

- [ ] 将 [keystore.properties.example](../../../keystore.properties.example) 复制为 `keystore.properties` 并填写真实签名信息
- [ ] 使用 release 签名生成 AAB（不是 debug APK）
- [ ] AAB 路径：`app/build/outputs/bundle/release/app-release.aab`
- [ ] 确认 `versionCode` 比线上版本大（当前在 [app/build.gradle.kts](../../../app/build.gradle.kts)）

PowerShell（在项目根目录执行）：

```powershell
$env:JAVA_HOME="D:\hjw\codex\qb-remote-android\tools\android-build\tools\jdk17"
$env:ANDROID_HOME="D:\hjw\codex\qb-remote-android\tools\android-build\tools\android-sdk"
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
$env:PATH="$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:PATH"

.\gradlew.bat bundleRelease
```

## 2. Play Console 应用设置

- [ ] 包名确认：`com.hjw.qbremote`
- [ ] 设置默认语言、应用名称、简短说明、完整说明
- [ ] 上传应用图标、功能图、手机截图（可选平板截图）
- [ ] 填写分类、联系邮箱、目标国家/地区

## 3. 合规与政策

- [ ] 提供隐私政策 URL（建议使用本仓库隐私政策页面）
- [ ] 完成 Data safety 表单（见 [DATA_SAFETY_GUIDE.zh-CN.md](./DATA_SAFETY_GUIDE.zh-CN.md)）
- [ ] 完成内容分级问卷
- [ ] 填写目标受众和广告声明（本应用通常选择“无广告”）
- [ ] 如果审核需要登录，提供可用的测试服务器与账号（App Access）

## 4. 测试轨道与发布

- [ ] 先发到内部测试轨道进行基础验证
- [ ] 再发到封闭测试轨道收集真实反馈
- [ ] 通过封闭测试后，再申请生产发布

注意：
- 若你是 2023-11-13 之后创建的个人开发者账号，正式发布前通常需要满足封闭测试要求（常见为 12 名测试者持续 14 天）。

## 5. 发版后维护

- [ ] 保留上传密钥与 Play App Signing 配置备份
- [ ] 每次发布递增 `versionCode`
- [ ] 维护更新日志（建议与 GitHub Release 同步）
- [ ] 定期复查 target API 要求和政策变化

## 建议的仓库公开链接

- 隐私政策（中文）：[PRIVACY_POLICY.zh-CN.md](./PRIVACY_POLICY.zh-CN.md)
- 隐私政策（英文）：[PRIVACY_POLICY.md](./PRIVACY_POLICY.md)
