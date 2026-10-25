package com.stockbridge.jade;

import com.stockbridge.colony_terminal.ColonyDemandTerminalBlock;
import com.stockbridge.colony_terminal.ColonyDemandTerminalBlockEntity;
import com.stockbridge.colony_terminal.ColonyNeedEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Comparator;
import java.util.List;

/**
 * 需求终端的 Jade tooltip（修复版）。
 *
 * <p>相对原版 1.0.4 的改动：原实现用 {@code terminal.isMinecoloniesPresent()}
 * 判断「是否已安装城市类模组」，该方法只检查 MineColonies 是否存在。
 * 本改造的数据源是 New-Sim-U-Kraft，因此在装有 NSK、未装 MineColonies 的整合包里，
 * tooltip 会误报「需要 New-Sim-U-Kraft」并提前 return，导致城市名与需求条目都不显示。
 *
 * <p>现在只要 MineColonies 或 simukraft 任一存在，就继续输出完整信息。
 */
public class ColonyDemandJadeProvider implements IBlockComponentProvider {

    public static final ColonyDemandJadeProvider INSTANCE = new ColonyDemandJadeProvider();
    public static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath("stockbridge", "colony_demand_terminal");

    public ColonyDemandJadeProvider() {
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        BlockEntity blockEntity = accessor.getBlockEntity();
        if (!(blockEntity instanceof ColonyDemandTerminalBlockEntity terminal)) {
            return;
        }

        boolean powered = accessor.getBlockState().getValue(ColonyDemandTerminalBlock.POWERED);
        tooltip.add(Component.translatable(powered
                ? "jade.stockbridge.network_active"
                : "jade.stockbridge.network_inactive"));

        boolean hasCitySource = terminal.isMinecoloniesPresent() || ModList.get().isLoaded("simukraft");
        if (!hasCitySource) {
            tooltip.add(Component.translatable("jade.stockbridge.colony_requires_minecolonies"));
            return;
        }

        String colonyName = terminal.getCachedColonyName();
        if (!colonyName.isEmpty()) {
            tooltip.add(Component.translatable("jade.stockbridge.colony_name", colonyName));
        }

        List<ColonyNeedEntry> entries = terminal.getCachedEntries();
        tooltip.add(Component.translatable("jade.stockbridge.colony_open_requests", entries.size()));

        entries.stream()
                .filter(entry -> entry.deficit() > 0)
                .sorted(Comparator.comparingInt(ColonyNeedEntry::deficit).reversed())
                .limit(5)
                .forEach(entry -> tooltip.add(Component.translatable(
                        "jade.stockbridge.colony_deficit_line",
                        entry.deficit(),
                        entry.stack().getHoverName())));
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
