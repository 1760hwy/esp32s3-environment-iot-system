# 基于 ESP32-S3 的多参数环境监测与物联网云平台设计

ESP32-S3 + AHT20 + BH1750 + SSD1306 + 新大陆云 + Android Java/XML + PostgreSQL。

这是完整系统的独立发布副本，包含固件、原生 Android APP、NAS 云数据同步程序及中文复现教程。原始开发工程保持原位。

[完整中文教程 / CSDN 稿](docs/CSDN_ESP32-S3多参数环境监测与物联网云平台设计.md) · [验证记录](docs/VALIDATION.md) · [安全配置](SECURITY.md) · [许可与第三方例外](THIRD_PARTY.md)

![系统架构](docs/assets/architecture.png)

## 功能

- AHT20 采集温湿度，BH1750 采集光照，SSD1306 本地显示。
- ESP-IDF v6.1，新 I²C master API，共享 GPIO8 / GPIO9 总线；FreeRTOS 独立采集任务。
- Wi-Fi / MQTT 上传到 NLECloud，三个标识统一为 `Temperature`、`Humidity`、`Light`。
- Android 使用 Java/XML，真实云数据、趋势图、指标筛选、主题、手动及前台定时同步。
- Python / Docker 同步云端历史默认页到现有 PostgreSQL，按设备与采样时间去重。

## 目录

```text
firmware/       ESP-IDF 工程，当前开发版采集任务结构
android/        Android Studio Java/XML 工程和已有测试
server/         NAS 同步源码、配置示例与最小建表 SQL
docs/           中文文章、真实 APP 截图、架构与接线图、验证记录
LICENSES/       第三方许可证
```

## 开始复现

```powershell
git clone https://github.com/1760hwy/esp32s3-environment-iot-system.git
cd esp32s3-environment-iot-system
Copy-Item firmware/main/app_secrets.example.h firmware/main/app_secrets.h
Copy-Item android/cloud.properties.example android/cloud.properties
Copy-Item server/config.env.example server/config.env
```

先按完整教程创建自己的云项目、设备及三个传感器，填写上述私有配置，再分别构建/部署。配置示例故意不含可直接使用的账号。完整步骤见[教程](docs/CSDN_ESP32-S3多参数环境监测与物联网云平台设计.md)。

## 当前边界

APP 历史只来自本次运行内存，未查询 PostgreSQL 或云端历史。APP 的云端同步状态不代表 ESP32 在线状态。NAS 仅同步云历史默认页，断网或高采样率时可能漏采；不具备分页补采保证。`server/schema.sql` 是依据同步代码新增的最小复现建表脚本，不是原 NAS 数据库导出。仓库不包含 PostgreSQL 实例、账号创建、数据库数据或成品 APK。

开发者已报告整体完成；已有 2026-10-23 Android 真机证据。本次发布副本的检查结果与未复验项目见验证记录，不把编译通过写成硬件或 NAS 联调通过。

## 许可

自有代码与文档采用 MIT；SSD1306、BH1750、Gradle Wrapper 等保持上游许可。**AHT20 原参考代码许可未确认，不纳入 MIT 授权，使用或再分发前请阅读 [THIRD_PARTY.md](THIRD_PARTY.md)。整个仓库不能视为全部 MIT。**

固件入门旧教程：[esp32s3-nlecloud-environment-tutorial](https://github.com/1760hwy/esp32s3-nlecloud-environment-tutorial)。本仓库扩展到 Android 与数据库归档，并保留当前固件任务结构。
