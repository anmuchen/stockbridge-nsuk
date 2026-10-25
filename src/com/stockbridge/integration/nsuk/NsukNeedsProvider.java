package com.stockbridge.integration.nsuk;

import com.stockbridge.colony_terminal.ColonyNeedsProvider;
import common.cn.kafei.simukraft.building.BuildingBlockData;
import common.cn.kafei.simukraft.building.BuildingStructure;
import common.cn.kafei.simukraft.building.BuildingStructureService;
import common.cn.kafei.simukraft.building.BuildingTaskData;
import common.cn.kafei.simukraft.building.BuildingTaskStatus;
import common.cn.kafei.simukraft.material.WorkMaterialPolicy;
import common.cn.kafei.simukraft.material.WorkMaterialRequest;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 让 StockBridge 的需求终端读取 New-Sim-U-Kraft 的城市建造需求。
 *
 * <p>数据来源全部通过反射访问 simukraft 的内部存储，避免在未安装 simukraft 时
 * 触发类加载失败：
 * <ul>
 *   <li>城市列表：{@code SimuSqliteStorage.cities.loadAll(dimensionId)} 返回的
 *       {@code Cities} 复合标签列表</li>
 *   <li>建造任务：{@code SimuSqliteStorage.buildingTasks.findByDimension(dimensionId)}</li>
 * </ul>
 *
 * <p>需求口径：把该城市所有未完成建造任务的剩余方块（从 {@code currentBlockIndex}
 * 起算）换算成物品数量。已完成与已中断的任务不计入。
 */
public final class NsukNeedsProvider implements ColonyNeedsProvider {

    private static final String MOD_ID = "simukraft";
    private static final String STORAGE_CLASS = "common.cn.kafei.simukraft.storage.SimuSqliteStorage";
    private static final long NEEDS_CACHE_MS = 10000L;

    /** 结构解析缓存：同一个建筑文件只解析一次。 */
    private static final Map<String, BuildingStructure> STRUCTURE_CACHE = new HashMap<>();
    /**
     * 方块列表缓存：{@code resolvePlacedBlocks} 会为每个方块构造对象并复制方块实体 NBT，
     * 一栋大建筑可达数千方块，每次打开界面都重算会造成数秒卡顿。
     * 该结果只与任务 ID、原点、旋转有关，因此可以长期缓存。
     */
    private static final Map<String, List<BuildingBlockData>> BLOCKS_CACHE = new HashMap<>();
    /** 需求结果缓存，避免 GUI 每次刷新都全量重算。 */
    private static final Map<String, CachedNeeds> NEEDS_CACHE = new HashMap<>();

    private static volatile boolean reflected;
    private static Method loadAllCities;
    private static Method findTasksByDimension;

    private record CitySnapshot(UUID id, String name, BlockPos core) {
    }

    private record CachedNeeds(long stamp, Map<ItemStack, Integer> needs) {
    }

    @Override
    public boolean isAvailable() {
        return ModList.get().isLoaded(MOD_ID);
    }

    // ---------------------------------------------------------------- 反射装配

    private static synchronized boolean ensureReflected(ServerLevel level) {
        if (reflected) {
            return loadAllCities != null;
        }
        reflected = true;
        try {
            Class<?> storageClass = Class.forName(STORAGE_CLASS);
            Field storagesField = storageClass.getDeclaredField("STORAGES");
            storagesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<Object, Object> storages = (Map<Object, Object>) storagesField.get(null);
            Object server = level.getServer();
            Object storage = storages.get(server);
            if (storage == null) {
                Method bootstrap = storageClass.getMethod("bootstrap", net.minecraft.server.MinecraftServer.class);
                bootstrap.invoke(null, server);
                storage = storages.get(server);
            }
            if (storage == null) {
                return false;
            }
            Field citiesField = storageClass.getDeclaredField("cities");
            citiesField.setAccessible(true);
            Object cities = citiesField.get(storage);
            Field tasksField = storageClass.getDeclaredField("buildingTasks");
            tasksField.setAccessible(true);
            Object tasks = tasksField.get(storage);
            if (cities == null || tasks == null) {
                return false;
            }
            loadAllCities = cities.getClass().getMethod("loadAll", String.class);
            findTasksByDimension = tasks.getClass().getMethod("findByDimension", String.class);
            // 把 repository 实例钉在 Method 上：反射调用需要接收者，这里缓存到静态字段。
            CITIES = cities;
            TASKS = tasks;
            return true;
        } catch (Throwable t) {
            loadAllCities = null;
            findTasksByDimension = null;
            return false;
        }
    }

    private static Object CITIES;
    private static Object TASKS;

    private static String dimensionId(ServerLevel level) {
        return level.dimension().location().toString();
    }

    // ---------------------------------------------------------------- 城市读取

    private static List<CitySnapshot> loadCities(ServerLevel level) {
        List<CitySnapshot> result = new ArrayList<>();
        if (!ensureReflected(level)) {
            return result;
        }
        try {
            Object tag = loadAllCities.invoke(CITIES, dimensionId(level));
            if (!(tag instanceof CompoundTag compound)) {
                return result;
            }
            ListTag list = compound.getList("Cities", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag cityTag = list.getCompound(i);
                if (!cityTag.hasUUID("CityId")) {
                    continue;
                }
                UUID id = cityTag.getUUID("CityId");
                String name = cityTag.getString("CityName");
                BlockPos core = new BlockPos(
                        cityTag.getInt("CoreX"),
                        cityTag.getInt("CoreY"),
                        cityTag.getInt("CoreZ"));
                result.add(new CitySnapshot(id, name == null || name.isBlank() ? shortId(id) : name, core));
            }
        } catch (Throwable ignored) {
            // 存储降级或表结构变化时保持静默，终端显示为空即可。
        }
        return result;
    }

    private static String shortId(UUID id) {
        return "City-" + id.toString().substring(0, 8);
    }

    /**
     * 城市 UUID 与界面 int 句柄之间的稳定映射。城市数量很少，哈希碰撞可以忽略。
     */
    private static int handleOf(UUID id) {
        int h = id.hashCode();
        return h == Integer.MIN_VALUE ? Integer.MAX_VALUE : Math.abs(h);
    }

    // ---------------------------------------------------------------- 接口实现

    @Override
    public List<ColonyOption> getColonyOptions(ServerLevel level, BlockPos pos) {
        List<CitySnapshot> cities = loadCities(level);
        if (cities.isEmpty()) {
            return List.of();
        }
        if (pos != null) {
            cities.sort(Comparator.comparingDouble(c -> c.core().distSqr(pos)));
        }
        List<ColonyOption> options = new ArrayList<>(cities.size());
        for (CitySnapshot city : cities) {
            options.add(new ColonyOption(handleOf(city.id()), city.name()));
        }
        return options;
    }

    @Override
    public int resolveNearestColonyId(ServerLevel level, BlockPos pos) {
        List<CitySnapshot> cities = loadCities(level);
        if (cities.isEmpty()) {
            return -1;
        }
        CitySnapshot best = null;
        double bestDist = Double.MAX_VALUE;
        for (CitySnapshot city : cities) {
            double d = pos == null ? 0.0D : city.core().distSqr(pos);
            if (best == null || d < bestDist) {
                best = city;
                bestDist = d;
            }
        }
        return best == null ? -1 : handleOf(best.id());
    }

    @Override
    public String getColonyName(ServerLevel level, int colonyId) {
        for (CitySnapshot city : loadCities(level)) {
            if (handleOf(city.id()) == colonyId) {
                return city.name();
            }
        }
        return "";
    }

    @Override
    public Map<ItemStack, Integer> getAggregatedNeeds(ServerLevel level, int colonyId, BlockPos pos) {
        if (colonyId < 0) {
            return Map.of();
        }
        UUID cityId = null;
        for (CitySnapshot city : loadCities(level)) {
            if (handleOf(city.id()) == colonyId) {
                cityId = city.id();
                break;
            }
        }
        if (cityId == null) {
            return Map.of();
        }

        String cacheKey = dimensionId(level) + "|" + colonyId;
        long now = System.currentTimeMillis();
        CachedNeeds cached = NEEDS_CACHE.get(cacheKey);
        if (cached != null && now - cached.stamp() < NEEDS_CACHE_MS) {
            return cached.needs();
        }

        Map<Item, Integer> totals = new LinkedHashMap<>();
        for (BuildingTaskData task : loadTasks(level)) {
            if (task == null || !cityId.equals(task.cityId())) {
                continue;
            }
            if (isFinished(task)) {
                continue;
            }
            try {
                accumulateTask(task, totals);
            } catch (Throwable ignored) {
                // 单个任务解析失败不应影响其余建筑的需求统计。
            }
        }

        Map<ItemStack, Integer> needs = new LinkedHashMap<>();
        for (Map.Entry<Item, Integer> entry : totals.entrySet()) {
            if (entry.getValue() > 0) {
                needs.put(new ItemStack(entry.getKey()), entry.getValue());
            }
        }
        NEEDS_CACHE.put(cacheKey, new CachedNeeds(now, needs));
        return needs;
    }

    // ---------------------------------------------------------------- 任务与材料

    private static List<BuildingTaskData> loadTasks(ServerLevel level) {
        if (!ensureReflected(level)) {
            return List.of();
        }
        try {
            Object raw = findTasksByDimension.invoke(TASKS, dimensionId(level));
            if (raw instanceof List<?> list) {
                List<BuildingTaskData> tasks = new ArrayList<>(list.size());
                for (Object element : list) {
                    if (element instanceof BuildingTaskData data) {
                        tasks.add(data);
                    }
                }
                return tasks;
            }
        } catch (Throwable ignored) {
            // 同上：查询失败时按无任务处理。
        }
        return List.of();
    }

    private static boolean isFinished(BuildingTaskData task) {
        String raw = task.status();
        if (raw == null) {
            return false;
        }
        BuildingTaskStatus status = BuildingTaskStatus.from(raw);
        return status == BuildingTaskStatus.COMPLETED || status == BuildingTaskStatus.INTERRUPTED;
    }

    private static void accumulateTask(BuildingTaskData task, Map<Item, Integer> totals) {
        if (task.origin() == null) {
            return;
        }
        List<BuildingBlockData> blocks = resolveBlocks(task);
        if (blocks.isEmpty()) {
            return;
        }
        int start = Math.max(0, Math.min(task.currentBlockIndex(), blocks.size()));
        for (int i = start; i < blocks.size(); i++) {
            BlockState state = blocks.get(i).state();
            if (state == null) {
                continue;
            }
            // 交给 NSK 自己的材料策略换算：它会应用"建造模式/专家模式"、
            // 基础材料列表、通类匹配组与黑名单规则，返回该方块真实需要的材料。
            if (!WorkMaterialPolicy.requiresMaterial(state)) {
                continue;
            }
            WorkMaterialRequest request = WorkMaterialPolicy.requestForBlock(state);
            if (request == null || request.isEmpty()) {
                continue;
            }
            ItemStack display = request.displayStack();
            if (display == null || display.isEmpty()) {
                continue;
            }
            Item item = display.getItem();
            if (item == Items.AIR) {
                continue;
            }
            totals.merge(item, 1, Integer::sum);
        }
    }

    private static List<BuildingBlockData> resolveBlocks(BuildingTaskData task) {
        String blocksKey = task.taskId() + "@" + task.origin().asLong() + "#" + task.rotationDegrees();
        List<BuildingBlockData> cachedBlocks = BLOCKS_CACHE.get(blocksKey);
        if (cachedBlocks != null) {
            return cachedBlocks;
        }

        String key = task.structureFileName() + "|" + task.buildingFileName();
        BuildingStructure structure = STRUCTURE_CACHE.get(key);
        if (structure == null) {
            Optional<BuildingStructure> loaded = BuildingStructureService.loadStructure(task);
            if (loaded.isEmpty()) {
                return List.of();
            }
            structure = loaded.get();
            if (STRUCTURE_CACHE.size() > 128) {
                STRUCTURE_CACHE.clear();
            }
            STRUCTURE_CACHE.put(key, structure);
        }

        List<BuildingBlockData> blocks = BuildingStructureService.resolvePlacedBlocks(
                structure, task.origin(), task.rotationDegrees());
        List<BuildingBlockData> result = blocks == null ? List.of() : blocks;
        if (BLOCKS_CACHE.size() > 256) {
            BLOCKS_CACHE.clear();
        }
        BLOCKS_CACHE.put(blocksKey, result);
        return result;
    }
}
