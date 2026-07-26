# TorrentRemote (qbitremote)

[![CI](https://github.com/smhjw/qbitremote/actions/workflows/ci.yml/badge.svg)](https://github.com/smhjw/qbitremote/actions/workflows/ci.yml)

`TorrentRemote` 是一款用 Kotlin + Jetpack Compose 编写的 Android 应用，可在一台手机上远程管理多台 qBittorrent 与 Transmission 服务器。

`TorrentRemote` is an Android app built with Kotlin + Jetpack Compose for managing multiple qBittorrent and Transmission servers from one phone.

[中文](#zh-cn) | [English](#en)

<a id="zh-cn"></a>

## 中文

### 项目简介

`TorrentRemote` 聚焦多服务器远程管理体验：首页汇总所有服务器的实时速度与状态，钱包式卡片一目了然；每台服务器有独立的图表仪表盘；种子列表与详情页覆盖日常管理的全部操作。qBittorrent 与 Transmission 两套后端在同一界面内无缝混用。

### 功能总览

**多服务器管理**

- 同时支持 qBittorrent WebUI API（4.x / 5.x，自动适配新旧暂停/继续端点）与 Transmission RPC（多路径探测、会话握手、代理兼容加固）
- 多服务器配置保存、无感切换，每台服务器独立缓存快照，离线时展示最后一次数据
- 故障服务器指数退避重连，避免高频重试触发服务端封禁；应用退后台自动暂停轮询省电省流量

**首页与仪表盘**

- 首页全服务器实时上下行速度曲线与钱包式服务器卡片堆叠，长按拖动排序
- 每台服务器独立图表仪表盘：今日国家 Peer 分布（世界热力图）、分类占比、标签上传量、Tracker 站点占比、种子状态占比、分享率分布、体积分布、今日上传量
- 仪表盘卡片可显隐、可拖拽排序，布局按服务器分别记忆

**种子管理**

- 种子列表按状态、分类、标签三行独立过滤，搜索 + 11 种排序，跨页返回位置恢复、快速回顶
- 种子详情四页签：信息、Tracker、用户（Peer）、文件；支持重命名、修改保存路径、导出种子（qB）、重新汇报、重新校验
- Tracker 复制/编辑/删除与 passkey 隐藏，辅种（跨站同种）识别与明细
- 单种限速与分享率管理；删除时可选同时删除文件
- 添加种子支持磁力链接、URL 与 .torrent 文件；可直接从系统分享菜单把链接分享进应用

**全局限速**

- 读取/写入服务器全局限速与备用限速，支持调度计划（时段 + 星期预设）
- 多服务器在线时，限速弹窗顶部可直接切换目标服务器

**体验与个性化**

- 深色 / 浅色 / 自定义背景图主题（自动识别图片明暗适配配色），快速切换稳定持久化
- 简体中文与英文双语言
- 种子完成系统通知（后台由 WorkManager 定期检查），锁屏仅显示标题保护隐私

**安全**

- 服务器凭据使用 Android Keystore AES-GCM 加密存储，永不进入云备份（备份已全局关闭）
- 通过未加密 HTTP 连接公网地址时给出明确风险提示（局域网地址不受打扰）
- 发布版本经 R8 混淆与资源压缩，日志在发布包中剥离

### 系统要求

- Android 8.0（API 26）及以上，目标平台 Android 16（API 36）
- qBittorrent 4.x / 5.x（启用 WebUI），或 Transmission 2.80+（启用 RPC）

### 下载与校验

预编译安装包永远不会提交到本仓库。请从以下渠道获取正式版本：

- [GitHub Releases](https://github.com/smhjw/qbitremote/releases)（签名 APK + `SHA256SUMS-vX.Y.Z.txt` 校验和清单）
- Google Play

下载后请对照每次发布附带的校验和清单校验文件完整性。

### 从源码构建

```bash
./gradlew assembleDebug
```

仅需 JDK 17 与 Android SDK（`ANDROID_HOME`）。CI 会在每次推送时运行单元测试、Lint、调试构建与未签名发布构建；签名发布流程见 `scripts/build-release-aab.ps1`（需要本地密钥，详见 [AGENTS.md](AGENTS.md)）。

### 应用截图

<img width="1440" height="2954" alt="首页总览" src="https://github.com/user-attachments/assets/f34713cc-6aff-4f5b-8558-b88ee3846f5f" />
<img width="1440" height="2954" alt="服务器仪表盘" src="https://github.com/user-attachments/assets/c96e7356-ebd3-4d79-abe2-85921a2bf6df" />
<img width="1440" height="2954" alt="图表" src="https://github.com/user-attachments/assets/73fedb29-8718-4205-b535-84f1b8a02e1a" />
<img width="1440" height="2954" alt="种子列表" src="https://github.com/user-attachments/assets/475f3886-a69d-4249-ba14-69e0db7e605c" />
<img width="1440" height="2954" alt="种子详情" src="https://github.com/user-attachments/assets/2cf7ad26-b5af-46dc-82bd-324efa372c00" />
<img width="1440" height="2954" alt="设置" src="https://github.com/user-attachments/assets/6ad4c09e-a6c0-487e-9def-b1efe9f078ea" />

### 许可证

详见 [LICENSE](LICENSE)。

<a id="en"></a>

## English

### Overview

`TorrentRemote` is built for multi-server torrent management on the go: the home page aggregates realtime speeds and status across every server in a wallet-style card stack, each server gets its own chart dashboard, and the torrent list and detail pages cover day-to-day management end to end. qBittorrent and Transmission backends mix seamlessly in one UI.

### Features

**Multi-server management**

- Supports qBittorrent WebUI API (4.x / 5.x, with automatic pause/resume endpoint fallback) and Transmission RPC (multi-path probing, session handshake, proxy-hardened transport) side by side
- Save multiple server profiles with seamless switching; each server keeps an independent cached snapshot shown while offline
- Exponential backoff for unreachable servers prevents reconnect storms from triggering server-side bans; polling pauses automatically in the background to save battery and data

**Home & dashboards**

- Realtime aggregate upload/download speed curves plus a wallet-style server card stack with long-press drag reorder
- Per-server chart dashboard: today's per-country peer distribution (world heat map), category share, per-tag upload volume, tracker site share, torrent state share, share-ratio distribution, size distribution, and today's upload total
- Dashboard cards can be hidden and drag-reordered, with layout remembered per server

**Torrent management**

- Torrent list with independent three-row filtering (state / category / tags), search, 11 sort options, cross-page position restore, and quick scroll-to-top
- Detail page with four tabs — Info, Trackers, Peers, Files — plus rename, move save path, export .torrent (qB), re-announce, and recheck
- Tracker copy/edit/delete with passkey masking; cross-seed detection and details
- Per-torrent speed limits and share-ratio management; optional file deletion on remove
- Add torrents via magnet link, URL, or .torrent file — or share a link straight into the app from any other app

**Global speed limits**

- Read and write server-wide normal and alternative speed limits with scheduler support (time window + day presets)
- With multiple servers online, the speed limit dialog can switch the target server from a dropdown

**Experience & personalization**

- Dark / light / custom background image themes (image tone detected automatically), with fast, reliably persisted switching
- Simplified Chinese and English
- Torrent completion notifications (periodic background checks via WorkManager) with lock-screen privacy — only the title is shown

**Security**

- Server credentials are encrypted with Android Keystore AES-GCM and never leave the device (backups are fully disabled)
- A clear warning is shown when connecting to a public address over unencrypted HTTP (LAN addresses are left alone)
- Release builds are R8-minified with resource shrinking, and logging is stripped from release packages

### Requirements

- Android 8.0 (API 26) or newer; targets Android 16 (API 36)
- qBittorrent 4.x / 5.x with WebUI enabled, or Transmission 2.80+ with RPC enabled

### Downloads

Prebuilt binaries are never committed to this repository. Get releases from:

- [GitHub Releases](https://github.com/smhjw/qbitremote/releases) (signed APK + `SHA256SUMS-vX.Y.Z.txt` checksum manifest)
- Google Play

Verify downloads against the checksum manifest attached to each release.

### Building from source

```bash
./gradlew assembleDebug
```

Only JDK 17 and an Android SDK (`ANDROID_HOME`) are required. CI runs unit tests, lint, a debug build, and an unsigned release build on every push; the signed release pipeline lives in `scripts/build-release-aab.ps1` (local keys required, see [AGENTS.md](AGENTS.md)).

### Screenshots

<img width="1440" height="2954" alt="Home overview" src="https://github.com/user-attachments/assets/f34713cc-6aff-4f5b-8558-b88ee3846f5f" />
<img width="1440" height="2954" alt="Server dashboard" src="https://github.com/user-attachments/assets/c96e7356-ebd3-4d79-abe2-85921a2bf6df" />
<img width="1440" height="2954" alt="Charts" src="https://github.com/user-attachments/assets/73fedb29-8718-4205-b535-84f1b8a02e1a" />
<img width="1440" height="2954" alt="Torrent list" src="https://github.com/user-attachments/assets/475f3886-a69d-4249-ba14-69e0db7e605c" />
<img width="1440" height="2954" alt="Torrent details" src="https://github.com/user-attachments/assets/2cf7ad26-b5af-46dc-82bd-324efa372c00" />
<img width="1440" height="2954" alt="Settings" src="https://github.com/user-attachments/assets/6ad4c09e-a6c0-487e-9def-b1efe9f078ea" />

### License

See [LICENSE](LICENSE).
