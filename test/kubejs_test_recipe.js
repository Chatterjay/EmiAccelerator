// EMI Accelerator — 配方变更测试脚本
// 用法: 将此文件放入 kubejs/server_scripts/ 目录, 然后执行:
//   /kubejs reload     (重载脚本)
//   /emiacc reload --force  (触发 EMI 全量重载, 包含延迟搜索)
//
// 观察: 重载耗时应在 ~29s 而非 ~39s,
//       提示消息应包含 "(搜索后台构建…)"
//       约 10s 后搜索自动可用

ServerEvents.recipes(event => {
    // 临时配方: 8 个原木 → 1 个钻石
    // 让测试者可以在 EMI 中搜索 "diamond" 看到这个配方
    event.shaped(
        Item.of('minecraft:diamond', 1),
        [
            'LLL',
            'L L',
            'LLL'
        ],
        {
            L: '#minecraft:logs'
        }
    ).id('emi_accelerator:test_recipe');

    console.info('[EMI加速测试] 测试配方已注册: 8 原木 → 1 钻石');
});

// 清理: 在脚本重载时移除旧版本测试配方
ServerEvents.loaded(event => {
    console.info('[EMI加速测试] 脚本已加载, 执行 /emiacc reload --force 开始测试');
});
