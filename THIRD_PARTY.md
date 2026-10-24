# 第三方来源与许可范围

本项目自有代码及文档采用 MIT，详见 LICENSE；不替代第三方许可。

| 路径 / 依赖 | 来源 | 许可与处理 |
|---|---|---|
| `firmware/main/BSP/ss1306/` | Eric Gionet / K0I05 SSD1306 组件 | MIT，保留源文件声明，全文见 `LICENSES/SSD1306-MIT.txt` |
| `firmware/main/BSP/bh1750/` | aZholtikov/zh_bh1750 的项目适配版本 | Apache-2.0，见 `LICENSES/BH1750-Apache-2.0.txt`；本项目修改为共享 BSP I²C 总线适配 |
| `firmware/main/BSP/ath20/` | 项目原有驱动，参考本地 ESP-IDF-AHT20 材料并迁移到新 I²C API | **许可未确认，不在本项目 MIT 授权范围内。** 作者无法确认旧参考代码的授权，不向使用者额外授予这部分的再许可或再分发权。用于再分发、商用前须自行取得权利确认或替换该驱动。 |
| `android/gradle/wrapper/gradle-wrapper.jar`、Gradle 启动脚本 | Gradle 官方 Wrapper | Apache-2.0，脚本头部保留原声明；许可全文见 `LICENSES/BH1750-Apache-2.0.txt` |
| ESP-IDF、MQTT、esp_type_utils、AndroidX、Material、requests、psycopg | 构建工具从各自上游获取 | 遵循依赖各自许可；本仓库不打包整个 SDK、依赖缓存或第三方运行库 |

上游入口：[SSD1306](https://github.com/K0I05/ESP32-S3_ESP-IDF_COMPONENTS/tree/main/components/peripherals/i2c/esp_ssd1306)、[BH1750](https://github.com/aZholtikov/zh_bh1750)、[Gradle](https://github.com/gradle/gradle)。

因此本仓库是公开学习源码及自有部分 MIT 发布，**不能宣称全部源码均为 MIT 或全部第三方授权已完成核验**。未包含无关参考库、厂商 PDF、原始备份、数据库数据和未脱敏截图。
