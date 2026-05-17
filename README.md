# EMI Accelerator

将 EMI 物品堆栈列表缓存到磁盘，并将搜索索引构建延迟到后台线程，大幅缩短 EMI 重载时间。

## 概述

EMI 在每次进入世界或资源重载时，都会重建完整的物品堆栈列表和搜索索引。对于大量模组的整合包，这个过程可能耗费数十秒，且搜索索引构建期间游戏界面完全卡死。

EMI Accelerator 通过以下方式解决此问题：

- **堆栈缓存**：首次加载时将 `EmiStackList.stacks` 序列化为 JSON 缓存到磁盘。后续进入游戏跳过全量重载（约 130ms 对比约 40s）。
- **延迟搜索**：将 `EmiSearch.bake()` 推迟到后台线程执行，消除约 10s 的界面阻塞。
- **自动刷新**：后台搜索完成后自动更新 EMI 搜索结果，无需手动重新输入。


## 用法

| 指令 | 说明 |
|------|------|
| `/emiacc status` | 查看缓存状态、配置状态 |
| `/emiacc enable [true\|false]` | 启用/禁用加速功能 |
| `/emiacc chat [true\|false]` | 显示/隐藏聊天提示信息 |
| `/emiacc debug [true\|false]` | 启用/禁用调试日志 |
| `/emiacc clear` | 删除缓存（下次加载时重建） |
| `/emiacc reload` | 触发 EMI 重载 |
| `/emiacc reload --force` | 清除缓存并强制全量重载 |

## 配置

配置文件位于 `config/emi-accelerator/emi-accelerator.txt`：

| 选项 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `acceleration.enabled` | boolean | `true` | 是否启用加速功能 |
| `cache.enabled` | boolean | `true` | 是否启用堆栈缓存 |
| `cache.auto_clear_on_mod_change` | boolean | `true` | 模组列表变化时自动清除缓存 |
| `cache.max_file_size_mb` | int | `50` | 缓存文件大小上限（MB） |
| `diagnostics.enabled` | boolean | `true` | 是否记录各阶段耗时到 `reload-timings.json` |
| `search.deferred` | boolean | `true` | 是否启用延迟搜索 |
| `chat.hide_messages` | boolean | `false` | 是否隐藏聊天提示信息 |
| `debug.enabled` | boolean | `false` | 是否启用调试日志 |

## 工作原理

1. EMI 标签同步触发 → `EmiReloadManager.reload()` → `ReloadWorker.run()` 启动
2. 检查缓存是否存在且有效（SHA-256 模组列表哈希校验）
3. 缓存命中 → 直接反序列化堆栈列表 → 跳过全量重载 → 搜索后台构建 → 完成
4. 缓存未命中 → 正常 EMI 重载 → 完成后异步写入缓存
5. 搜索构建完成后自动刷新 EMI 界面，显示「[EMI加速] 搜索已就绪」

## 依赖

| 模组 | 版本 | 必需 |
|------|------|------|
| [EMI](https://modrinth.com/mod/emi) | 1.1.22+ | 是 |

- 加载器：NeoForge 21.1.228+
- 运行侧：客户端专用
- 环境：Minecraft 1.21.1

## 构建

```bash
./gradlew build
```

## 协议

[GNU AGPL 3.0](LICENSE)
