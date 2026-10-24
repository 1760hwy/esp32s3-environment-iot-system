# CSDN 发布操作

- 推荐标题：开源实战：基于 ESP32-S3 的多参数环境监测与物联网云平台设计
- 摘要：使用 ESP32-S3、AHT20、BH1750 和 SSD1306 完成温湿度与光照采集，通过 MQTT 接入新大陆云，用 Java/XML 原生 Android 展示真实数据，并在 NAS 上通过 Python 将云端记录归档到 PostgreSQL。文章附源码、接线、配置、复现步骤及数据边界说明。
- 建议标签：ESP32-S3、ESP-IDF、物联网、MQTT、Android、PostgreSQL。
- 正文：CSDN_ESP32-S3多参数环境监测与物联网云平台设计.md。
- 仓库：https://github.com/1760hwy/esp32s3-environment-iot-system。

在 CSDN Markdown 编辑器导入正文后，将 assets/ 中 5 张 PNG 上传到编辑器，替换相应本地图片引用，再检查预览。不要把本机绝对路径直接粘贴成图片网址。正文已经在开头和结尾引用开源仓库。

assets/ 图像清单：architecture.png、wiring.png、data-boundaries.png 为源码示意；android-dashboard-real.png、android-history-real.png 为 2026-10-23 已有真机原图。没有伪造硬件照片、云平台截图或 NAS 写入截图。

预览时检查代码围栏、表格、中文图片清晰度及 GitHub 链接。按实际发布界面填写分类与标签；本文没有自动提交到 CSDN，不声称已有文章 URL。GitHub 版本中的相对图片引用可直接阅读，也可从仓库原图取图。

许可：自有代码与文档 MIT，第三方及 AHT20 例外须保留。避免使用“全部 MIT”“绝不丢数”“下载即用”“设备在线已确认”等超出当前实现的宣传。
