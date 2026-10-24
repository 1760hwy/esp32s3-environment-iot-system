# 发布副本验证记录

日期：2026-10-24，Asia/Shanghai。验证对象为独立发布目录 `esp32s3-environment-iot-system`，不是对生产设备重新验收。

## 本次实际执行

| 检查 | 实际结果 | 范围 |
|---|---|---|
| ESP-IDF v6.1，`idf.py build` | 退出码 0，`Project build complete` | 当前任务版 main.c、原传感器驱动、占位 app_secrets.h |
| 固件分区检查 | BIN 935888 bytes，应用分区 0x100000，剩余 0x1b830，约 11% | 默认 DIO / 80 MHz / 2 MB Flash；非运行时内存或性能指标 |
| Android `assembleDebug testDebugUnitTest lintDebug` | `BUILD SUCCESSFUL`；JVM 单元测试 4/4；Lint 0 errors / 11 warnings | 使用空云账号配置，只验证构建、模型单元测试与静态检查 |
| `python -m unittest discover -s tests -v`，server 目录 | 7/7 通过 | 乱序字段、时间交集、缺失/空值、排序、时区、非法输入及空数据；HTTP/数据库模块为离线导入桩 |
| `docker compose config --quiet`，server 目录 | 退出码 0 | 从空配置示例生成本地 config.env；仅验证 Compose 配置结构 |

Android 实际命令：

```powershell
# Android 工程目录；在本机使用 Android Studio JBR 与已有 SDK / 缓存。
$env:JAVA_HOME = 'C:\d\android\Androidstudio\jbr'
$env:ANDROID_HOME = 'C:\Users\hwy\AppData\Local\Android\Sdk'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --offline --no-configuration-cache --console=plain
```

首次完整构建 1m52s。随后调整设备页编号为读取 BuildConfig 后，重跑同一套检查：16s，BUILD SUCCESSFUL。时间仅为本机本次构建日志，不是性能承诺。未修改原开发工程的 Gradle 缓存或上级 Git 暂存区。

固件构建用示例配置，没有执行 `flash`、串口操作、复位或上云。构建日志中的烧录命令是工具打印的提示，不是已执行操作。

## 保留的既有证据

原 Android 开发工程的 `FRONTEND_VALIDATION.md` 在 2026-10-23 记录：云端绑定后 JVM 4 项、API 37 设备测试 9 项通过，Lint 0 errors / 11 warnings；真实手机展示过云端读数、内存历史及暂停/恢复同步。

公开的 `assets/android-dashboard-real.png`、`assets/android-history-real.png` 是该次已有真机原图，未修改数值、曲线或界面。原图保留系统状态区域。它们不是 2026-10-24 发布副本重新安装、联网的证明。早期模拟界面截图未混入当前真实数据展示。

用户已明确说明整体项目完成；本次写作将其作为项目背景，未把该描述等同于新增测试记录。

## 发布改动

- 固件保留当前开发工程 main.c 的独立 FreeRTOS 采集任务与传感器驱动；网络模块采用既有公开教程的 app_secrets.h 脱敏适配。未迁移传感器逻辑或调整硬件连接。
- Android 把开发账号与设备编号移至本地 cloud.properties → BuildConfig，设备页使用相同配置编号；原页面、轮询和数据规则保留。
- NAS 四个运行文件与原 ZIP 解码后内容一致；不分发原 config.env 或压缩包。
- 新增 schema.sql 是基于 INSERT 契约编写的最小建表方案，未经目标 NAS 的真实 schema 比对；不自动迁移现有表。
- 新增中文文档、示意图、离线解析测试和许可证范围说明。
- 配图生成源码为 `tools/render_diagrams.py`；需要 Pillow 与中文字体，可用 `--font` 指定。真实截图不由该脚本生成。

## 未执行与限制

本次未对发布副本重跑 Android 设备测试、真实账户登录、ESP32 / 传感器 / OLED 联调、NAS 容器运行、PostgreSQL SQL 执行、云记录写入、分页补采、长时间稳定性或传感器精度标定。没有把静态检查或离线测试描述为数据库、云端或物理硬件通过。

Docker 镜像标签、Python 依赖版本范围仍沿用原工程，并非镜像摘要或精确版本锁定。首次联网重建的依赖解析结果可能随时间变化。APP 内存历史、云端同步状态与 NAS 默认页限制详见正文。

第三方权限存在 AHT20 明确例外，见根目录 THIRD_PARTY.md；不宣称所有文件均为 MIT。

发布前另检查：真实配置值比对、常见令牌模式、私有文件忽略规则、本地 Markdown 链接与图片。结果记录于 `publication-checks.txt`。该检查是针对已知配置和规则的发布审计，不是完整依赖漏洞审计或绝对无泄漏保证。

## 仓库排版检查与工具状态

初次导入的 `git diff --cached --check` 提示原工程和第三方源码既有的行尾空白。为保留驱动源码，本次不做全量格式重排；这些提示不计作功能测试通过。新增文档及配置另行检查。Docker Desktop Linux daemon 未运行，未启动它或执行容器部署；Compose 配置校验不依赖该 daemon，已单独通过。
