package com.boaat.jazzy_cookin.kitchen.sim;

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
        return new FoodMatterData(
                gameTime,
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
