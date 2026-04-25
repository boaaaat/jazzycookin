package com.boaat.jazzy_cookin.kitchen.sim.schema;

import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.KitchenStackUtil;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;

public record DishAttemptContext(FoodMatterData matter, IngredientState state) {
    public static DishAttemptContext fromMatter(FoodMatterData matter) {
        return new DishAttemptContext(matter, matter != null ? KitchenStackUtil.inferStateFromMatter(matter) : IngredientState.PANTRY_READY);
    }
}
