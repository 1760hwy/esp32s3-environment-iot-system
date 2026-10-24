# ESP32-S3 采集与 MQTT 固件

在已激活 ESP-IDF v6.1 的终端进入本目录。

```powershell
Copy-Item main/app_secrets.example.h main/app_secrets.h
# 编辑 app_secrets.h，填写自己的配置。
idf.py set-target esp32s3
idf.py build
# 下列命令会写入连接的硬件，请换成自己的串口：
idf.py -p COMx flash monitor
```

保留当前 sample_project 的 main.c 独立采集任务结构和传感器驱动；网络模块复用已公开教程中的 app_secrets.h 配置适配。GPIO8 SDA / GPIO9 SCL，AHT20 0x38、BH1750 0x23、SSD1306 0x3C。主循环含两个 1000ms 等待点，周期另加采集和调度开销。

组件版本见 dependencies.lock。真实配置、sdkconfig、build、managed_components 和二进制均不纳入 Git。第三方与 AHT20 许可例外见 ../THIRD_PARTY.md。
