package com.stockbridge.client.gui.colony_terminal;

import com.stockbridge.client.gui.VanillaGuiPanel;
import com.stockbridge.colony_terminal.ColonyNeedEntry;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 需求终端列表行渲染（修复版）。
 *
 * <p>原版 1.0.4 在 {@code renderRows} 里调用了
 * {@code graphics.enableScissor(x, y, x + width, y + rows * rowHeight)}，
 * 传入的是 GUI 局部坐标；而 {@code GuiGraphics.enableScissor} 直接用窗口绝对坐标
 * 构造裁剪矩形，不考虑当前 PoseStack 的平移。由于该方法在 {@code renderLabels}
 * 中执行（此时坐标已平移到屏幕中央），裁剪区被错误地留在窗口左上角，
 * 整块列表行因此被裁掉 —— 表现为表头正常、行不可见、但悬停提示仍然生效。
 *
 * <p>行数本身已由 {@code visibleRows} 限制（终端为 4 行），不存在溢出问题，
 * 因此这里直接省略裁剪，避免坐标语义错配。
 */
public final class ColonyDemandRowRenderer {

    private ColonyDemandRowRenderer() {
    }

    public static void renderRows(GuiGraphics graphics, Font font, List<ColonyNeedEntry> entries,
                                  int scrollOffset, int visibleRows, int x, int y, int width, int rowHeight) {
        for (int i = 0; i < visibleRows; i++) {
            int index = scrollOffset + i;
            if (index >= entries.size()) {
                break;
            }
            renderRow(graphics, font, entries.get(index), x, y + i * rowHeight, width, rowHeight);
        }
    }

    private static void renderRow(GuiGraphics graphics, Font font, ColonyNeedEntry entry,
                                  int x, int y, int width, int rowHeight) {
        boolean hasDeficit = entry.deficit() > 0;
        VanillaGuiPanel.renderListRow(graphics, x, y, width, rowHeight, hasDeficit);

        graphics.renderItem(entry.stack(), x + 1, y + 1);
        graphics.drawString(font, trimName(entry.stack().getHoverName().getString(), 9),
                x + 26, y + 5, VanillaGuiPanel.TEXT, false);
        graphics.drawString(font, Integer.toString(entry.needed()),
                x + 100, y + 5, VanillaGuiPanel.TEXT, false);
        graphics.drawString(font, Integer.toString(entry.inRs()),
                x + 132, y + 5, VanillaGuiPanel.TEXT, false);

        if (hasDeficit) {
            graphics.drawString(font, Integer.toString(entry.deficit()),
                    x + 164, y + 5, 0xFFB71C1C, false);
        } else {
            graphics.drawString(font, "-", x + 168, y + 5, 0xFF2E7D32, false);
        }
    }

    public static Component tooltipFor(ColonyNeedEntry entry) {
        if (entry.deficit() > 0) {
            return Component.translatable("gui.stockbridge.colony_demand.tooltip_deficit_actions",
                    entry.deficit(), entry.stack().getHoverName());
        }
        return Component.translatable("gui.stockbridge.colony_demand.tooltip_ok",
                entry.stack().getHoverName());
    }

    private static String trimName(String name, int max) {
        if (name.length() <= max) {
            return name;
        }
        return name.substring(0, max - 2) + "\u2026";
    }
}
