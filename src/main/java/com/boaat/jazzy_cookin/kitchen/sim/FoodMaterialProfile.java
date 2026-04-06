package com.boaat.jazzy_cookin.kitchen.sim;

import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.IngredientStateData;

public record FoodMaterialProfile(
        float water,
        float fat,
        float protein,
        float seasoningLoad,
        float cheeseLoad,
        float onionLoad,
        float herbLoad,
        float pepperLoad,
        float aeration
) {
    public FoodMatterData create(long gameTime) {
        IngredientStateData summaryHint = new IngredientStateData(
                IngredientState.PANTRY_READY,
                gameTime,
                0.72F,
                0.72F,
                0.45F,
                0.42F,
                0.36F,
                this.water,
                0.70F,
                this.aeration,
                0,
                Math.max(1, Math.round(this.protein * 8.0F + this.fat * 4.0F)),
                1
        );
        return new FoodMatterData(
                gameTime,
                summaryHint,
                22.0F,
                22.0F,
                this.water,
                this.fat,
                this.protein,
                this.aeration,
                0.08F,
                0.12F,
                0.0F,
                0.0F,
                0.0F,
                this.seasoningLoad,
                this.cheeseLoad,
                this.onionLoad,
                this.herbLoad,
                this.pepperLoad,
                0.0F,
                0,
                0,
                0,
                0,
                false
        );
    }
}
