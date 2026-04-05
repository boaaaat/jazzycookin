package com.boaat.jazzy_cookin.kitchen;

import net.minecraft.network.chat.Component;

public record QualityBreakdown(
        DishGrade grade,
        float finalScore,
        float ingredientQuality,
        float freshness,
        float prepScore,
        float combineScore,
        float cookingScore,
        float finishingScore,
        float platingScore,
        float recipeAccuracy,
        int nourishment,
        int enjoyment
) {
    public Component summary() {
        return Component.translatable(
                "tooltip.jazzycookin.grade_summary",
                this.grade.displayName(),
                Math.round(this.finalScore * 100.0F)
        );
    }
}
