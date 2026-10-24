# NLECloud → PostgreSQL 同步端

本目录由开发用 ZIP 中的 sync.py、requirements.txt、Dockerfile、compose.yaml 提取，原 config.env 不进入仓库。同步代码行为保持原样。

先准备自己的 PostgreSQL、iot_db 和专用账号，再连接数据库执行 schema.sql；该 SQL 是按 INSERT 契约补充的最小建表结构，不是原 NAS 导出或数据库迁移。

```bash
cp config.env.example config.env
# 编辑自己的账号、设备与数据库连接信息。
docker compose build
docker compose up -d
docker compose logs --tail=50 -f
```

程序容器主动连接云端和数据库，不开放应用端口。DB_HOST 需从容器内可达；DB_PORT 显式按实际数据库端口设置，模板 5432 不是原 NAS 端口承诺。

只同步历史默认页，只接受三个指标 RecordTime 完全匹配的行。device_id 列写的是 DEVICE_NAME。无分页补采保证；账户 token 仅内存缓存。已连接数据库的权限、版本和原 schema 未在本次发布时重新核验。

详细步骤与验收 SQL 见[主教程](../docs/CSDN_ESP32-S3多参数环境监测与物联网云平台设计.md)。

离线解析检查：`python -m unittest discover -s tests -v`。该测试使用 HTTP/数据库导入桩，不证明容器、网络或真实数据库可用。如最小系统报 `ZoneInfoNotFoundError: Asia/Shanghai`，需先为 Python 环境补充 IANA 时区数据库（系统 tzdata 或 Python tzdata 包），再启动同步；本次未构建 Docker 镜像核实该基础镜像的时区数据。
