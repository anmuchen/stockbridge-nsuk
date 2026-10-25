package com.stockbridge.client.integration;

import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * 配方界面跳转入口。
 *
 * <p>原版 1.0.4 的 {@code showRecipesPreferred} 优先走 EMI，只有 EMI 未安装时才退到 JEI。
 * 本整合包实际使用的是 JEI，因此这里把优先级改为 <b>JEI 优先</b>，
 * EMI 仅作为兜底保留。
 */
public final class EmiIntegration {

    private EmiIntegration() {
    }

    /** 走 EMI 显示配方（仅在安装了 EMI 时有效）。 */
    public static void showRecipes(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !ModList.get().isLoaded("emi")) {
            return;
        }
        try {
            Class<?> emiApi = Class.forName("dev.emi.emi.api.EmiApi");
            Class<?> emiStackClass = Class.forName("dev.emi.emi.api.stack.EmiStack");
            Object emiStack = emiStackClass.getMethod("of", ItemStack.class).invoke(null, stack);
            emiApi.getMethod("displayRecipes", emiStackClass).invoke(null, emiStack);
        } catch (Throwable t) {
            com.stockbridge.StockBridge.LOGGER.warn("[StockBridge] EMI recipe lookup failed", t);
        }
    }

    /** JEI 优先，EMI 兜底。 */
    public static void showRecipesPreferred(ItemStack stack) {
        if (ModList.get().isLoaded("jei")) {
            JeiIntegration.showRecipes(stack);
        } else if (ModList.get().isLoaded("emi")) {
            showRecipes(stack);
        }
    }
}
