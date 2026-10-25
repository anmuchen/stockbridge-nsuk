package com.stockbridge.client.integration;

import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.List;

/**
 * JEI 配方跳转。
 *
 * <p>原版 1.0.4 用 {@code getMethod("show", focus.getClass())} 查找显示方法，
 * 但 JEI 的入口是 {@code IRecipesGui.show(IFocus)} 与 {@code show(List<IFocus<?>>)}，
 * 不存在以具体实现类为参数的签名，反射必然失败。
 *
 * <p>这里依次尝试两条路径，并在全部失败时以 warn 级别输出异常，
 * 便于在 latest.log 中直接定位是哪一环不匹配。
 */
public final class JeiIntegration {

    private JeiIntegration() {
    }

    public static void showRecipes(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !ModList.get().isLoaded("jei")) {
            return;
        }
        try {
            Class<?> runtimeClass = Class.forName("mezz.jei.library.runtime.JeiRuntime");
            Object runtime = runtimeClass.getMethod("getInstance").invoke(null);
            if (runtime == null) {
                warn("JeiRuntime.getInstance() returned null", null);
                return;
            }

            Object helpers = runtimeClass.getMethod("getJeiHelpers").invoke(runtime);
            Object focusFactory = helpers.getClass().getMethod("getFocusFactory").invoke(helpers);

            Class<?> roleClass = Class.forName("mezz.jei.api.recipe.RecipeIngredientRole");
            Object outputRole = roleClass.getField("OUTPUT").get(null);

            Class<?> ingredientTypeClass = Class.forName("mezz.jei.api.ingredients.IIngredientType");
            Object itemStackType = Class.forName("mezz.jei.api.constants.VanillaTypes")
                    .getField("ITEM_STACK")
                    .get(null);

            Method createFocus = focusFactory.getClass()
                    .getMethod("createFocus", roleClass, ingredientTypeClass, Object.class);
            Object focus = createFocus.invoke(focusFactory, outputRole, itemStackType, stack);
            if (focus == null) {
                warn("createFocus returned null", null);
                return;
            }

            Object recipesGui = runtimeClass.getMethod("getRecipesGui").invoke(runtime);

            // 首选 show(IFocus) 默认方法，失败再退到 show(List)
            try {
                Method showSingle = recipesGui.getClass()
                        .getMethod("show", Class.forName("mezz.jei.api.recipe.IFocus"));
                showSingle.invoke(recipesGui, focus);
                return;
            } catch (Throwable first) {
                Method showList = recipesGui.getClass().getMethod("show", List.class);
                showList.invoke(recipesGui, List.of(focus));
            }
        } catch (Throwable t) {
            warn("JEI recipe lookup failed", t);
        }
    }

    private static void warn(String message, Throwable t) {
        if (t == null) {
            com.stockbridge.StockBridge.LOGGER.warn("[StockBridge] " + message);
        } else {
            com.stockbridge.StockBridge.LOGGER.warn("[StockBridge] " + message, t);
        }
    }
}
