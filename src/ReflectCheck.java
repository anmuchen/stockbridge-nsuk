import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 在真实 classpath 下验证：
 * 1) 反射字段名 / 方法名确实存在于 simukraft 2.2.0 中；
 * 2) 新增的 NsukNeedsProvider 能被类加载器解析（不触发静态初始化）；
 * 3) 它实现的接口方法签名与 StockBridge 1.0.4 一致。
 */
public final class ReflectCheck {

    private static int failures = 0;

    private static void ok(String what) {
        System.out.println("  OK   " + what);
    }

    private static void fail(String what, Throwable t) {
        failures++;
        System.out.println("  FAIL " + what + " -> " + t);
    }

    private static Class<?> cls(String name) throws Exception {
        return Class.forName(name, false, ReflectCheck.class.getClassLoader());
    }

    public static void main(String[] args) {
        System.out.println("== simukraft 反射锚点 ==");
        try {
            Class<?> storage = cls("common.cn.kafei.simukraft.storage.SimuSqliteStorage");
            Field storages = storage.getDeclaredField("STORAGES");
            ok("SimuSqliteStorage.STORAGES : " + storages.getType().getSimpleName());
            Field cities = storage.getDeclaredField("cities");
            ok("SimuSqliteStorage.cities : " + cities.getType().getName());
            Field tasks = storage.getDeclaredField("buildingTasks");
            ok("SimuSqliteStorage.buildingTasks : " + tasks.getType().getName());
            Method bootstrap = storage.getMethod("bootstrap", Class.forName("net.minecraft.server.MinecraftServer", false, ReflectCheck.class.getClassLoader()));
            ok("SimuSqliteStorage.bootstrap(MinecraftServer)");
        } catch (Throwable t) {
            fail("SimuSqliteStorage 反射锚点", t);
        }

        try {
            Class<?> cityRepo = cls("common.cn.kafei.simukraft.storage.CitySqliteRepository");
            Method loadAll = cityRepo.getMethod("loadAll", String.class);
            ok("CitySqliteRepository.loadAll(String) -> " + loadAll.getReturnType().getSimpleName());
        } catch (Throwable t) {
            fail("CitySqliteRepository.loadAll", t);
        }

        try {
            Class<?> taskRepo = cls("common.cn.kafei.simukraft.storage.BuildingTaskSqliteRepository");
            Method findByDim = taskRepo.getMethod("findByDimension", String.class);
            ok("BuildingTaskSqliteRepository.findByDimension(String) -> " + findByDim.getReturnType().getSimpleName());
        } catch (Throwable t) {
            fail("BuildingTaskSqliteRepository.findByDimension", t);
        }

        try {
            Class<?> svc = cls("common.cn.kafei.simukraft.building.BuildingStructureService");
            Class<?> taskData = cls("common.cn.kafei.simukraft.building.BuildingTaskData");
            Class<?> structure = cls("common.cn.kafei.simukraft.building.BuildingStructure");
            Method loadStructure = svc.getMethod("loadStructure", taskData);
            ok("BuildingStructureService.loadStructure(BuildingTaskData) -> " + loadStructure.getReturnType().getSimpleName());
            Method resolve = svc.getMethod("resolvePlacedBlocks", structure,
                    Class.forName("net.minecraft.core.BlockPos", false, ReflectCheck.class.getClassLoader()), int.class);
            ok("BuildingStructureService.resolvePlacedBlocks(BuildingStructure, BlockPos, int)");
            Method from = cls("common.cn.kafei.simukraft.building.BuildingTaskStatus").getMethod("from", String.class);
            ok("BuildingTaskStatus.from(String) -> " + from.getReturnType().getSimpleName());
        } catch (Throwable t) {
            fail("BuildingStructureService / BuildingTaskStatus", t);
        }

        System.out.println("== 新增类 ==");
        try {
            Class<?> provider = cls("com.stockbridge.integration.nsuk.NsukNeedsProvider");
            Class<?> iface = cls("com.stockbridge.colony_terminal.ColonyNeedsProvider");
            boolean assignable = iface.isAssignableFrom(provider);
            if (assignable) {
                ok("NsukNeedsProvider implements ColonyNeedsProvider");
            } else {
                fail("NsukNeedsProvider 未实现 ColonyNeedsProvider", null);
            }
            for (Method m : iface.getMethods()) {
                Method impl = provider.getMethod(m.getName(), m.getParameterTypes());
                ok("override " + impl.getName() + " -> " + impl.getReturnType().getSimpleName());
            }
            // 构造器存在性
            provider.getConstructor();
            ok("NsukNeedsProvider() 构造器可用");
        } catch (Throwable t) {
            fail("NsukNeedsProvider", t);
        }

        try {
            Class<?> integration = cls("com.stockbridge.colony_terminal.ColonyIntegration");
            Method provider = integration.getMethod("provider");
            ok("ColonyIntegration.provider() -> " + provider.getReturnType().getSimpleName());
        } catch (Throwable t) {
            fail("ColonyIntegration", t);
        }

        System.out.println();
        if (failures == 0) {
            System.out.println("RESULT: ALL CHECKS PASSED");
        } else {
            System.out.println("RESULT: " + failures + " CHECK(S) FAILED");
            System.exit(1);
        }
    }
}
