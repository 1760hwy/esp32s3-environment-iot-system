# Android Java/XML 客户端

用 Android Studio 打开本目录。复制 cloud.properties.example 为 cloud.properties 并填写自己的账户、密码和设备编号；本地文件会转成 BuildConfig，APK 仍会包含它们，勿发布带真实账户的 APK。

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --no-configuration-cache --console=plain
```

使用 compileSdk / targetSdk 37、minSdk 24，保留原 Gradle/AGP 版本与测试。Gradle daemon 工具链要求 Java 25，Java 源码兼容级别为 11。

运行链路：NleCloudSource → NleCloudApi → NleSensorMapper → MonitorViewModel → 三个页面。前台完成一次请求后延时 5/10/30 秒再请求；失败保留真实旧数据；退到后台停止后续轮询。历史仅本次运行内存，不读取 PostgreSQL，无设备在线状态判断。模型中的 demo 方法只用于预览和测试。

构建检查与真机历史证据见 ../docs/VALIDATION.md，完整说明见主教程。界面编辑入口见 界面修改指南.md。
