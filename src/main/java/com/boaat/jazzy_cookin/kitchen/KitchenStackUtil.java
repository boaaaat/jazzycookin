package com.boaat.jazzy_cookin.kitchen;

import com.boaat.jazzy_cookin.item.KitchenIngredientItem;
import com.boaat.jazzy_cookin.registry.JazzyDataComponents;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class KitchenStackUtil {
    private KitchenStackUtil() {
    }

    public static IngredientStateData getData(ItemStack stack) {
        return stack.get(JazzyDataComponents.INGREDIENT_STATE.get());
    }

    public static IngredientStateData getOrCreateData(ItemStack stack, long gameTime) {
        IngredientStateData existing = getData(stack);
        if (existing != null) {
            return existing;
        }

        if (stack.getItem() instanceof KitchenIngredientItem ingredientItem) {
            IngredientStateData created = ingredientItem.defaultData(gameTime);
            stack.set(JazzyDataComponents.INGREDIENT_STATE.get(), created);
            return created;
        }

        return null;
    }

    public static void setData(ItemStack stack, IngredientStateData data) {
        stack.set(JazzyDataComponents.INGREDIENT_STATE.get(), data);
    }

    public static boolean matchesState(ItemStack stack, IngredientState state, long gameTime) {
        IngredientStateData data = getOrCreateData(stack, gameTime);
        return data != null && data.state() == state;
    }

    public static float currentFreshnessScore(ItemStack stack, Level level) {
        IngredientStateData data = getOrCreateData(stack, level.getGameTime());
        if (data == null) {
            return 0.0F;
        }

        long decayTicks = decayTicks(stack);
        if (decayTicks <= 0L || decayTicks >= Long.MAX_VALUE / 4L) {
            return 1.0F;
        }

        long age = Math.max(0L, level.getGameTime() - data.createdTick());
        float ratio = age / (float) decayTicks;
        return Mth.clamp(1.0F - ratio, 0.0F, 1.0F);
    }

    public static String freshnessLabel(ItemStack stack, Level level) {
        float score = currentFreshnessScore(stack, level);
        if (score >= 0.75F) {
            return "fresh";
        }
        if (score >= 0.45F) {
            return "aging";
        }
        if (score >= 0.2F) {
            return "stale";
        }
        if (score > 0.0F) {
            return "spoiled";
        }
        return "moldy";
    }

    public static long decayTicks(ItemStack stack) {
        if (stack.getItem() instanceof KitchenIngredientItem ingredientItem) {
            return ingredientItem.decayTicks();
        }
        return Long.MAX_VALUE / 4L;
    }
}
