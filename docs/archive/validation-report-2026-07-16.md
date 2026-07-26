# QBRemote 全维度优化验证报告

日期：2026-07-16

## 结论

代码编译、静态质量、单元/合同测试、覆盖率、AndroidTest 编译、Benchmark APK 编译和固定签名发布产物均已通过对应门禁。API 26/API 35 设备测试、真实双后端端到端测试及 Macrobenchmark 运行未通过验收：本机无已安装 system image，SDK Manager 多次安装均在远程包访问阶段超时且没有数据落盘。另外，协调器和 feature 文件虽已建立，`MainViewModel`、主壳与存储门面仍偏大，尚不能客观认定易维护性达到 10/10。因此当前不把六个维度虚标为满分。

## 工作区保护

- 开发基线：用户当前未提交工作区；未 reset、未 stash、未覆盖无关删除或 `.claude` 内容。
- Git 备份引用：`refs/codex/backups/pre-full-optimization-20260715-225722`
- Git 备份提交：`c617999b9a9297b684976d06026fce7f8b31f759`
- 未跟踪文件备份：`C:\Users\ADMIN\AppData\Local\Temp\qbremote-pre-optimization-20260715-225722\untracked-files.tar.gz`

## 已完成范围

- 完整删除小组件 provider、布局、provider XML、背景和文案；工作树源码/资源引用扫描无 widget 残留。
- 分享导入支持 `ACTION_SEND text/plain` 与 `ACTION_VIEW magnet`，采用一次性事件，冷/热启动均走同一入口并做合并去重。
- 导航改为可保存的参数化返回栈，统一顶部返回、系统返回和恢复语义。
- 添加种子与服务器表单使用可保存草稿；修正字段顺序、窄屏布局、IME/滚动目标、HTTP 提示和清除已保存密码操作。
- 添加文件先复制到受控缓存并校验类型、大小和 metainfo 边界；单文件 64 MiB、总缓存 256 MiB；qB 使用流式 multipart，Transmission 使用分块 Base64 请求体；结果清理和部分失败重试已接入。
- 每服务器复用连接会话；qB 认证恢复只重登一次；Transmission 409 每端点只重试一次；Transmission 批量添加保留逐项成功/失败。
- 国家统计统一为单协调路径：4 并发、15 秒 hash 最短采样间隔、30 秒持久化节流、轮转公平、部分失败保留基线、应用停止时刷新。
- 下载完成通知默认关闭；仅用户开启时申请权限；WorkManager 使用联网约束；状态按 `profileId + torrentHash` 持久化；前后台共享状态；通知包含服务器/种子并深链详情。
- 默认资源目录改为英文；`values`、`values-en`、`values-zh-rCN` 均为 296 个同构 key。
- 自定义背景改为 IO 线程限量复制、格式/尺寸验证、临时文件和原子替换；失败保留旧文件；背景遮罩采用分区渐变。
- 引入统一间距、圆角、触控和玻璃透明度 token；移除 Emoji 功能图标；开关/主题选择/关闭操作补齐 role、selected 和 48dp 触控目标。
- 仪表盘组件已从主壳拆入独立 feature 文件；连接模型与存储实现分离；通知、种子列表页面状态、种子操作、详情、服务器刷新和国家统计均已有独立协调器/状态持有器。
- 发布脚本同时生成 APK/AAB，构建前后核对固定证书，并输出文件 SHA-256。

## 已通过门禁

| 门禁 | 结果 |
|---|---|
| 单元与 MockWebServer 合同测试 | 150/150 通过，0 失败，0 跳过 |
| Debug Lint | 0 error，0 warning |
| Release Lint | 0 error，0 warning |
| AndroidTest 编译 | 通过；4 个设备测试方法已编译 |
| 纯 Kotlin 业务逻辑行覆盖率 | 85.68%，门槛 80% |
| 关键解析/验证/协调逻辑行覆盖率 | 94.77%，门槛 90% |
| Benchmark 测试 APK 编译 | `assembleBenchmarkRelease` 通过；2 个 benchmark 各 10 次迭代 |
| Release APK/AAB | `assembleRelease` 与 `bundleRelease` 通过；因首版签名包出现启动闪退，已恢复为既有的非混淆、非资源收缩发布配置，等待设备回归后再评估 R8 |
| 固定签名 | 构建前 keystore、构建后 APK/AAB 三方证书 SHA-256 一致 |
| 资源一致性 | 3 套资源各 296 key，差异 0 |
| 小组件/Emoji/密钥文本扫描 | 工作树无小组件残留、无 Emoji 功能文本、无密钥材料命中 |

## 发布产物

- APK：`release-artifacts/app-release.apk`，10,095,589 bytes
- APK SHA-256：`3DE8C09D6E0B951CED7AFB007E75FC8A766EA8B3DD375E2B9743B36544FF21E9`
- AAB：`release-artifacts/app-release.aab`，9,576,682 bytes
- AAB SHA-256：`5CCFEE0FE247D55B81777F7654134481CB6FE6A77FC960CC3FD401227BC05A65`
- 签名证书 SHA-256：`BF:FC:E8:6C:02:F4:16:79:92:20:1B:57:E1:82:10:1E:93:EB:4D:BA:BE:01:D9:AB:6C:67:37:C1:4E:B6:9A:F2`
- `applicationId`、versionCode、versionName、keystore、alias 与证书身份均未修改。

## 未通过/未执行门禁

- API 26/API 35 Emulator system image 未能安装。Emulator 本体已安装，但 SDK Manager 访问 system-image 远程包时分别在 60 秒、180 秒、300 秒超时，目录中无镜像或临时下载数据。
- `connectedAndroidTest` 无设备可运行；仅完成编译，不能据此声称中英文、字体缩放、旋转、TalkBack 和触控行为已在设备验证。
- `connectedNonMinifiedReleaseAndroidTest` 明确失败：`No connected devices!`。
- qBittorrent/Transmission 合同由 MockWebServer 验证；真实双后端连接、增删改、断网重连、文件上传与通知仍需设备和本地后端运行。
- Macrobenchmark 测试包已构建，但无设备，因此未取得冷启动中位数和滚动 p95，也无法与改造前基线比较。
- 结构已明显拆分，但当前 `MainViewModel.kt` 仍为 2,644 行、`MainScreen.kt` 主壳为 1,683 行、`ConnectionStore.kt` 为 1,086 行；存储门面仍承载多类缓存与迁移编排。继续拆分必须在设备回归条件恢复后分批进行，当前不能据此宣称易维护性满分。

## 满分判定

按既定规则，运行时门禁缺失或结构性维护问题未关闭都不能标记 10/10。恢复可下载 system image 的网络，或连接一台 API 26 与一台 API 35 设备后，应先运行设备测试、真实双后端 E2E 和 10 次 Macrobenchmark，再依据设备回归把剩余主类/存储门面分批拆小；全部门禁通过后才能完成最终满分验收。
