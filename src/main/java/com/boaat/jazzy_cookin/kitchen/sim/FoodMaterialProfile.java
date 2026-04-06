package com.boaat.jazzy_cookin.kitchen.sim;

import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.IngredientStateData;

import net.minecraft.util.Mth;

public record FoodMaterialProfile(
        float water,
        float fat,
        float protein,
        float starch,
        float sugar,
        float fiber,
        float acidity,
        float seasoningLoad,
        float cheeseLoad,
        float onionLoad,
        float herbLoad,
        float pepperLoad,
        float aeration,
        long traitMask
) {
    public boolean hasTrait(FoodTrait trait) {
        return FoodTrait.has(this.traitMask, trait);
    }

    public FoodMatterData create(IngredientStateData summaryHint, boolean finalizedServing) {
        IngredientStateData baseSummary = summaryHint != null ? summaryHint : defaultSummary(0L);
        float baseFragmentation = Mth.clamp(0.06F + this.fiber * 0.18F, 0.04F, 0.40F);
        float baseCohesiveness = Mth.clamp(baseSummary.structure() * 0.55F + this.starch * 0.20F + this.fat * 0.10F, 0.08F, 0.80F);
        return new FoodMatterData(
                baseSummary.createdTick(),
                baseSummary,
                this.traitMask,
                22.0F,
                22.0F,
                this.water,
                this.fat,
                this.protein,
                Mth.clamp(Math.max(this.aeration, baseSummary.aeration() * 0.75F), 0.0F, 1.0F),
                baseSummary.processDepth() > 0 ? Math.max(0.24F, baseFragmentation) : baseFragmentation,
                baseSummary.processDepth() > 0 ? Math.max(0.24F, baseCohesiveness) : baseCohesiveness,
                baseSummary.state().isPlatedState() || finalizedServing ? 0.78F : 0.0F,
                0.0F,
                0.0F,
                this.seasoningLoad,
                this.cheeseLoad,
                this.onionLoad,
                this.herbLoad,
                this.pepperLoad,
                FoodMatterData.UNSET_ENVIRONMENT,
                FoodMatterData.UNSET_ENVIRONMENT,
                FoodMatterData.UNSET_ENVIRONMENT,
                baseSummary.processDepth() > 0 ? 0.35F : 0.0F,
                0,
                0,
                0,
                baseSummary.processDepth(),
                finalizedServing
        ).clamp();
    }

    private IngredientStateData defaultSummary(long gameTime) {
        return new IngredientStateData(
                IngredientState.PANTRY_READY,
                gameTime,
                0.72F,
                0.72F,
                Mth.clamp(0.28F + this.sugar * 0.32F + this.acidity * 0.06F + this.seasoningLoad * 0.10F, 0.0F, 1.0F),
                Mth.clamp(0.20F + this.fat * 0.20F + this.protein * 0.12F + this.starch * 0.10F, 0.0F, 1.0F),
                Mth.clamp(0.18F + this.protein * 0.20F + this.starch * 0.18F + this.fiber * 0.10F, 0.0F, 1.0F),
                this.water,
                0.70F,
                this.aeration,
                0,
                Math.max(1, Math.round(this.protein * 8.0F + this.fat * 4.0F + this.starch * 3.0F + this.sugar * 2.0F)),
                Math.max(1, Math.round(1.0F + this.sugar * 3.0F + this.seasoningLoad * 3.0F + this.herbLoad * 2.0F + this.pepperLoad * 2.0F))
        );
    }
}
