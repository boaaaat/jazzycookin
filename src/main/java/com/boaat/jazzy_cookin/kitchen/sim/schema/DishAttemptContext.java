package com.boaat.jazzy_cookin.kitchen.sim.schema;

import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.KitchenStackUtil;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;

import net.minecraft.world.item.ItemStack;

public record DishAttemptContext(FoodMatterData matter, IngredientState state, ItemStack stack, DishAttemptData attempt) {
    public static DishAttemptContext fromMatter(FoodMatterData matter) {
        return new DishAttemptContext(
                matter,
                matter != null ? KitchenStackUtil.inferStateFromMatter(matter) : IngredientState.PANTRY_READY,
                ItemStack.EMPTY,
                DishAttemptData.EMPTY
        );
    }

    public static DishAttemptContext fromStack(ItemStack stack, long gameTime) {
        FoodMatterData matter = KitchenStackUtil.getOrCreateFoodMatter(stack, gameTime);
        return new DishAttemptContext(
                matter,
                KitchenStackUtil.effectiveState(stack, gameTime),
                stack,
                KitchenStackUtil.dishAttempt(stack)
        );
    }
}
