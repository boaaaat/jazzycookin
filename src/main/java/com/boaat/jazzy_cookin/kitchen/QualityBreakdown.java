package com.boaat.jazzy_cookin.kitchen;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

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
    public MutableComponent summary() {
        return Component.translatable(
                "tooltip.jazzycookin.grade_summary",
                this.grade.displayName(),
                Math.round(this.finalScore * 100.0F)
        );
    }

    public float seasoningScore() {
        return this.combineScore;
    }

    public float textureScore() {
        return this.finishingScore;
    }

    public float presentationScore() {
        return this.platingScore;
    }
}
