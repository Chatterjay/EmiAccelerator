# 更新日志

## 1.1.1 (2026-05-17)

- **功能**：聊天信息隐藏开关，`/emiacc chat` 指令切换（对应 `chat.hide_messages` 配置项）
- **功能**：调试模式开关，`/emiacc debug` 指令切换
- **功能**：加速总开关，`/emiacc enable` 指令切换
- **功能**：本地化支持（`en_us` / `zh_cn`）
- **重构**：配置存储改为线程安全的 `ConcurrentHashMap` + `.txt` key=value 格式
- **重构**：聊天提示逻辑统一为 `ChatHelper.sendIfNotHidden()`
- **重构**：所有配置字段访问改为方法调用 API

## 1.1.0 (2026-05-09)

- **变更**：许可协议从 GNU LGPL 3.0 切换为 GNU AGPL 3.0

## 1.0.1 (2026-05-07)

- **修复**：缓存命中时 EMI 合成记录/使用记录丢失的问题。现在缓存命中时工作线程会完整执行所有必要阶段（配方烘焙、BoM 重载、持久数据加载等），仅跳过堆栈列表重新加载（节省约 5.5 秒）。搜索索引仍在后台异步构建。
- **变更**：缓存命中重载时间从 ~130ms 增加到 ~18s（取决于模组数量），但所有 EMI 功能正常工作。

## 1.0.0 (2026-05-07)

- **功能**：首次重载时将 `EmiStackList.stacks` 序列化为 JSON 缓存
- **功能**：延迟搜索索引构建至后台线程，消除界面阻塞
- **功能**：模组列表变化时自动清除缓存（SHA-256 哈希校验）
- **功能**：诊断计时记录至 `config/emi-accelerator/reload-timings.json`
- **功能**：`/emiacc status|clear|reload|reload --force` 命令
