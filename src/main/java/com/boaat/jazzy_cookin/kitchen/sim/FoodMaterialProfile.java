package com.boaat.jazzy_cookin.kitchen.sim;

import com.boaat.jazzy_cookin.kitchen.IngredientState;
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

    public FoodMatterData create(IngredientState state, long createdTick, int processDepth, boolean finalizedServing) {
        int derivedDepth = Math.max(processDepth, canonicalProcessDepth(state, finalizedServing));
        float baseFragmentation = Mth.clamp(0.06F + this.fiber * 0.18F + prepFragmentationBias(state), 0.04F, 0.55F);
        float baseCohesiveness = Mth.clamp(0.18F + this.starch * 0.20F + this.fat * 0.10F + prepCohesivenessBias(state), 0.08F, 0.92F);
        return new FoodMatterData(
                createdTick,
                this.traitMask,
                22.0F,
                22.0F,
                this.water,
                this.fat,
                this.protein,
                Mth.clamp(Math.max(this.aeration, stateAerationFloor(state)), 0.0F, 1.0F),
                derivedDepth > 0 ? Math.max(0.20F, baseFragmentation) : baseFragmentation,
                derivedDepth > 0 ? Math.max(0.24F, baseCohesiveness) : baseCohesiveness,
                cookedOrServed(state, finalizedServing) ? 0.78F : 0.0F,
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
                whiskBaseline(state),
                0,
                0,
                0,
                derivedDepth,
                finalizedServing
        ).clamp();
    }

    private static float stateAerationFloor(IngredientState state) {
        return switch (state) {
            case FRESH_JUICE -> 0.02F;
            case SMOOTH, CREAMY, SMOOTH_MIXTURE, WHISKED -> 0.08F;
            case BATTER, BATTERED, BATTERED_PROTEIN -> 0.16F;
            case DOUGH, BREAD_DOUGH, SHAGGY_DOUGH, ROUGH_DOUGH, DEVELOPING_DOUGH, DEVELOPED_DOUGH, SMOOTH_DOUGH, ELASTIC_DOUGH -> 0.10F;
            default -> 0.04F;
        };
    }

    private static float prepFragmentationBias(IngredientState state) {
        return switch (state) {
            case ROUGH_CUT, CHOPPED, DICED, MINCED, SLICED, STRAINED, STUFFED, SHAPED_BASE -> 0.08F;
            case COARSE_POWDER, FINE_POWDER, GROUND_SPICE, GROUND_HERB -> 0.22F;
            case SMOOTH, CREAMY, PASTE, SMOOTH_PASTE -> 0.12F;
            default -> 0.0F;
        };
    }

    private static float prepCohesivenessBias(IngredientState state) {
        return switch (state) {
            case DOUGH, BREAD_DOUGH, DUMPLING_DOUGH, SHAGGY_DOUGH, ROUGH_DOUGH, DEVELOPING_DOUGH, DEVELOPED_DOUGH, SMOOTH_DOUGH, ELASTIC_DOUGH -> 0.32F;
            case BATTER, BATTERED, BATTERED_PROTEIN, SMOOTH_MIXTURE, CREAMY, SMOOTH, PASTE, SMOOTH_PASTE -> 0.18F;
            case PAN_FRIED, DEEP_FRIED, BAKED, BAKED_BREAD, BAKED_PIE, ROASTED, STEAMED, SMOKED -> 0.26F;
            case PLATED, PLATED_SLICE, PLATED_SOUP_MEAL, PLATED_DUMPLING_MEAL, PLATED_FRIED_MEAL, PLATED_ROAST_MEAL -> 0.20F;
            default -> 0.0F;
        };
    }

    private static float whiskBaseline(IngredientState state) {
        return switch (state) {
            case SMOOTH_MIXTURE, WHISKED, BATTER, CREAMY, SMOOTH -> 0.35F;
            case UNDERWHISKED -> 0.18F;
            case OVERWHISKED -> 0.62F;
            default -> 0.0F;
        };
    }

    private static int canonicalProcessDepth(IngredientState state, boolean finalizedServing) {
        if (finalizedServing || state.isPlatedState()) {
            return 2;
        }
        return switch (state) {
            case PANTRY_READY -> 0;
            case ROUGH_CUT, CHOPPED, DICED, MINCED, SLICED, STRAINED, STUFFED, SHAPED_BASE -> 1;
            default -> 1;
        };
    }

    private static boolean cookedOrServed(IngredientState state, boolean finalizedServing) {
        return finalizedServing || switch (state) {
            case PAN_FRIED, DEEP_FRIED, BAKED, ROASTED, STEAMED, SMOKED, BOILED, SIMMERED,
                    BAKED_BREAD, BAKED_PIE, PLATED, PLATED_SLICE, PLATED_SOUP_MEAL,
                    PLATED_DUMPLING_MEAL, PLATED_FRIED_MEAL, PLATED_ROAST_MEAL -> true;
            default -> false;
        };
    }
}
