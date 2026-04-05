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
        return effectiveState(stack, gameTime) == state;
    }

    public static float currentFreshnessScore(ItemStack stack, Level level) {
        return currentFreshnessScore(stack, level.getGameTime());
    }

    public static float currentFreshnessScore(ItemStack stack, long gameTime) {
        IngredientStateData data = getOrCreateData(stack, gameTime);
        if (data == null) {
            return 0.0F;
        }

        long decayTicks = decayTicks(stack);
        if (decayTicks <= 0L || decayTicks >= Long.MAX_VALUE / 4L) {
            return 1.0F;
        }

        long age = Math.max(0L, gameTime - data.createdTick());
        float ratio = age / (float) decayTicks;
        return Mth.clamp(1.0F - ratio, 0.0F, 1.0F);
    }

    public static FreshnessBand freshnessBand(ItemStack stack, Level level) {
        return freshnessBand(stack, level.getGameTime());
    }

    public static FreshnessBand freshnessBand(ItemStack stack, long gameTime) {
        return FreshnessBand.fromScore(currentFreshnessScore(stack, gameTime));
    }

    public static IngredientState effectiveState(ItemStack stack, long gameTime) {
        IngredientStateData data = getOrCreateData(stack, gameTime);
        if (data == null) {
            return IngredientState.PANTRY_READY;
        }

        return switch (freshnessBand(stack, gameTime)) {
            case SPOILED -> IngredientState.SPOILED;
            case MOLDY -> IngredientState.MOLDY;
            default -> data.state();
        };
    }

    public static String freshnessLabel(ItemStack stack, Level level) {
        return freshnessBand(stack, level).getSerializedName();
    }

    public static long decayTicks(ItemStack stack) {
        if (stack.getItem() instanceof KitchenIngredientItem ingredientItem) {
            return ingredientItem.decayTicks();
        }
        return Long.MAX_VALUE / 4L;
    }
}
