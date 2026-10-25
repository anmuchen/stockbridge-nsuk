package com.stockbridge.colony_terminal;

import com.stockbridge.integration.minecolonies.MineColoniesReflectionBridge;
import com.stockbridge.integration.nsuk.NsukNeedsProvider;
import net.neoforged.fml.ModList;

/**
 * 需求数据源选择器。
 *
 * <p>优先级：New-Sim-U-Kraft（simukraft）&gt; MineColonies（minecolonies）&gt; 空实现。
 * 两者都通过可选依赖判定，未安装时不会触发对应提供者的类加载。
 */
public final class ColonyIntegration {

    private static ColonyNeedsProvider provider;

    private ColonyIntegration() {
    }

    public static ColonyNeedsProvider provider() {
        if (provider == null) {
            provider = createProvider();
        }
        return provider;
    }

    private static ColonyNeedsProvider createProvider() {
        if (ModList.get().isLoaded("simukraft")) {
            return new NsukNeedsProvider();
        }
        if (ModList.get().isLoaded("minecolonies")) {
            return new MineColoniesReflectionBridge();
        }
        return ColonyNeedsProvider.NONE;
    }
}
