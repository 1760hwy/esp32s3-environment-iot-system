# 配置与安全边界

公开源码不包含真实账号、Wi-Fi 密码、设备密钥或数据库密码。

- 固件：复制 `firmware/main/app_secrets.example.h` 为 `app_secrets.h` 后本地填写。
- Android：复制 `android/cloud.properties.example` 为 `cloud.properties` 后本地填写，构建时转成 BuildConfig。
- 同步端：复制 `server/config.env.example` 为 `config.env`，只供 Compose 读取。
- 以上私有文件已忽略；真实构建产物仍会嵌入固件/Android 配置，因此不发布 BIN、ELF 或带账号的 APK。BuildConfig 并非密码保险箱，反编译可读取其值。
- 这是个人学习部署方式。若向公众分发 APP，需另行设计服务端鉴权与最小权限，不能把共享云账号放进安装包。
- MQTT 延续原工程 `mqtt://...:1883`，未使用 TLS；按平台支持情况升级加密与设备级权限后再用于敏感生产环境。
- PostgreSQL 使用参数化写入；仅给同步账号所需数据库权限，在受控网络连库，避免直接向公网开放数据库。
- 本仓库不会连接你的云账号、创建云设备、烧录硬件或修改 NAS。所有部署步骤由读者在自己的环境执行。

如发现敏感配置误提交，应先撤销/轮换，再处理 Git 历史；删掉当前文件不能撤销泄漏的凭据。
