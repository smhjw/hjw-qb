# qbremote

[English](README.md) | 简体中文

`qbremote` 是一个通过 qBittorrent WebUI API 远程管理 qBittorrent 的 Android 应用。

## 项目概览

- 应用名：`qbremote`
- Application ID：`com.hjw.qbremote`
- 技术栈：Kotlin + Jetpack Compose (Material 3)
- 最低 SDK：`26`
- 目标 / 编译 SDK：`35`
- 版本：`0.1.1`（`versionCode = 2`）

## 核心功能

### 1) 连接与服务器管理

- 支持通过主机/IP 或完整 `http(s)://` URL 连接 qBittorrent
- 支持 HTTPS 开关与刷新间隔配置
- 多服务器配置：可保存、切换并快速重连
- 密码加密存储（`EncryptedSharedPreferences`），并支持旧数据迁移

### 2) 仪表盘

- 全局上传/下载速度
- 总上传/下载流量
- 上传/下载限速
- 状态统计标签：上传中、下载中、暂停、错误、校验中、排队中、总数
- 可选图表面板（多指标）

### 3) 统一种子列表

- 全局统一列表（不按 tracker/站点分组）
- 支持排序：
  - 添加时间
  - 上传/下载速度
  - 分享率
  - 总上传/总下载
  - 种子大小
  - 活动时间
  - 做种数/下载数
  - Cross-seed 数量
- 每次切换排序规则后，自动跳转到第一条种子
- 搜索支持：名称、标签、分类、哈希、保存路径
- 支持双击顶部区域快速回到列表顶部

### 4) 种子操作

- 暂停 / 继续
- 删除种子（可选同时删除文件）

### 5) 种子详情页

- 标签页：信息、Tracker、Peer、文件
- 重命名种子
- 修改保存路径
- 修改分类和标签
- 设置单种上传/下载限速
- 设置分享率
- Cross-seed 详情查看

### 6) 添加种子

- 支持磁力链接/URL（支持多行）
- 支持从 Android 文件选择器添加 `.torrent` 文件
- 添加选项：
  - 自动种子管理
  - 添加后暂停
  - 跳过哈希校验
  - 顺序下载
  - 优先首尾文件块
  - 上传/下载限速

### 7) 体验与本地化

- 深色/浅色主题切换
- 中英文本地化
- 按页面上下文自适应自动刷新

## 稳定性改进

- 仪表盘全量刷新与数据清洗
- 可疑种子记录快照修复
- `401/403` 自动重新登录
- 网络与服务端瞬时错误指数退避重试

## 构建（使用 `tools/` 本地工具链）

项目可使用以下内置工具链构建：

- `tools/android-build/tools/jdk17`
- `tools/android-build/tools/android-sdk`

PowerShell 示例：

```powershell
$env:JAVA_HOME="D:\hjw\codex\qb-remote-android\tools\android-build\tools\jdk17"
$env:ANDROID_HOME="D:\hjw\codex\qb-remote-android\tools\android-build\tools\android-sdk"
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
$env:PATH="$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:PATH"

.\gradlew.bat assembleDebug
```

APK 输出：

- `app/build/outputs/apk/debug/app-debug.apk`

如果需要自定义文件名：

```powershell
Rename-Item "app/build/outputs/apk/debug/app-debug.apk" "qbremote.apk"
```

Google Play 用的 release AAB：

1. 将 `keystore.properties.example` 复制为 `keystore.properties`，并填入你的签名参数
2. 执行打包：

```powershell
.\gradlew.bat bundleRelease
```

输出：

- `app/build/outputs/bundle/release/app-release.aab`

快捷脚本：

```powershell
.\scripts\build-release-aab.ps1
```

## qBittorrent WebUI 设置

1. 打开 `Tools -> Options -> Web UI`
2. 勾选 `Web User Interface (Remote control)`
3. 设置 WebUI 端口（默认 `8080`）
4. 设置用户名和密码
5. 确保局域网/广域网防火墙允许 Android 设备访问

## 路线图

- 多选批量操作
- 更多 Tracker/Peer/文件高级控制
- 更完整的发布自动化（CI + 签名构建）
- 更高覆盖率的测试（ViewModel 与 Repository 层）

## Google Play 文档

- [Google Play 上架清单](docs/google-play/PLAY_RELEASE_CHECKLIST.zh-CN.md)
- [Data Safety 填写建议](docs/google-play/DATA_SAFETY_GUIDE.zh-CN.md)
- [隐私政策（英文）](docs/google-play/PRIVACY_POLICY.md)
- [隐私政策（简体中文）](docs/google-play/PRIVACY_POLICY.zh-CN.md)
