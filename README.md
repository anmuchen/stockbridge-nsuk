# StockBridge — New-Sim-U-Kraft 适配改造

把 [StockBridge](https://modrinth.com/mod/stockbridge) 1.0.4 中原本面向 MineColonies 的「殖民地需求终端」，改造为面向《新：模拟大都市》(New-Sim-U-Kraft) 的「模拟城市需求终端」。

## 运行环境

| 项目 | 版本 |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.216 及以上 |
| Refined Storage | 2.0.9 |
| New-Sim-U-Kraft | 2.2.0 |

## 改动清单

| 文件 | 类型 | 说明 |
|---|---|---|
| `NsukNeedsProvider.java` | 新增 | 实现 `ColonyNeedsProvider`，反射读取 NSK 的 SQLite 存储，取城市列表与建造任务；材料换算交由 `WorkMaterialPolicy` 处理，自动遵循服务端配置的基础材料、通类匹配组与黑名单 |
| `ColonyIntegration.java` | 替换 | 数据源优先级改为 `simukraft` > `minecolonies` > 空实现，同一 jar 可在两类整合包通用 |
| `ColonyDemandRowRenderer.java` | 修复 | 移除错误的 `enableScissor` 调用（把 GUI 局部坐标当窗口绝对坐标使用），并纠正 `renderRows` 的参数顺序 |
| `JeiIntegration.java` | 修复 | 原实现用 `getMethod("show", focus.getClass())` 查找可变参数方法，必然抛 `NoSuchMethodException`；改为调用 `show(List)` 重载，失败时以 warn 级别记录 |
| `EmiIntegration.java` | 修改 | `showRecipesPreferred` 改为 JEI 优先、EMI 兜底 |

### 关键实现说明

需求统计没有自行解析 NSK 的配置文件，而是调用其公开 API：

```java
WorkMaterialPolicy.requiresMaterial(state)   // 该方块是否需要材料
WorkMaterialPolicy.requestForBlock(state)    // 它实际需要什么材料
```

这两个方法的判定链会依次检查创造模式、专家模式、`basicMaterials` 与 `materialCategoryGroups`。因此玩家修改 `config/simukraft-server.toml` 后，需求表会随之变化，无需改动本仓库代码。

注意：NSK 的**服务端**配置以 `simukraft-server.toml` 为准，`simukraft-common.toml` 中的同名项不生效。

## 构建

本项目不使用 Gradle，直接以 `javac` 针对本机 neoform 缓存的 Minecraft / NeoForge 编译产物编译：

```bash
python build.py
```

`build.py` 顶部硬编码了若干本机绝对路径（JDK、neoform 缓存、brigadier、slf4j），换机器需要自行调整。

流程分三步：

1. `javac` 编译 `src/` 下的改动类
2. `pack.py` 复制原版 jar、替换改动类、写入语言覆盖
3. `verify.py` 解析常量池，校验新类的所有类引用均可解析

辅助脚本 `check_materials.py` 读取真实的 `simukraft-server.toml`，离线复刻 `requiresMaterial` 的判定，用于核对需求口径。

## 许可与署名

原模组 StockBridge 1.0.4 由 **silvermist** 以 MIT 许可发布。

本仓库只包含为适配 New-Sim-U-Kraft 所改写的文件，**不包含原模组的完整 jar**。使用前请自行获取原版 StockBridge 1.0.4。

若需二次分发，请同时保留原作者的署名与 MIT 许可声明。

## 免责声明

本改造未经原模组作者审核，与原模组作者无关。使用前请备份存档。因使用本改造导致的存档损坏、崩溃或其他损失，作者不承担责任。
