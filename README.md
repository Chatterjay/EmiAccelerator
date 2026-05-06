# EMI Accelerator

将 EMI 的堆栈列表缓存到磁盘，并将搜索索引构建延迟到后台线程，将 EMI 重载时间从约 40 秒缩短至约 14 秒。

## 基本信息

| 字段       | 值                                              |
|------------|-------------------------------------------------|
| **版本**   | 1.0.0                                           |
| **Minecraft** | 1.21.1                                        |
| **加载器** | NeoForge 21.1.228 (`[21,)`)                     |
| **运行侧** | 客户端专用（无需安装至服务端）                   |
| **协议**   | GNU LGPL 3.0                                    |
| **作者**   | Chatterjay                                      |

## 依赖项

| 模组 | 版本     | 必需 |
|------|----------|------|
| [EMI](https://modrinth.com/mod/emi) | 1.1.22+  | 是   |

## 功能

- **堆栈缓存**：首次重载时将 `EmiStackList.stacks` 序列化为 JSON，后续进入游戏跳过全量重载（约 130ms 对比约 40s）
- **延迟搜索**：`EmiSearch.bake()` 在后台线程运行，消除约 10s 的界面阻塞。搜索在约 7s 后自动可用，界面不卡死
- **自动刷新**：后台搜索完成后自动更新 EMI 搜索结果，无需手动重新输入
- **缓存失效**：模组列表变化时（SHA-256 哈希校验）自动清除缓存
- **诊断**：重载各阶段耗时记录至 `config/emi-accelerator/reload-timings.json`
- **配置**：`config/emi-accelerator/emi-accelerator.properties`

## 指令

| 指令                          | 说明                          |
|-------------------------------|-------------------------------|
| `/emiacc status`              | 查看缓存状态、大小、命中情况  |
| `/emiacc clear`               | 删除缓存（下次加载时重建）    |
| `/emiacc reload`              | 触发 EMI 重载                 |
| `/emiacc reload --force`      | 清除缓存并强制全量重载        |

## 构建

```bash
./gradlew build
# 产物: build/libs/emi_accelerator-<版本号>.jar
```

## 发布

推送版本标签以触发 GitHub Actions 自动构建：

```bash
git tag v1.0.0
git push origin v1.0.0
```

工作流会自动构建 jar 并发布到 GitHub Releases。
