package com.boaat.jazzy_cookin.kitchen.sim.domain;

import java.util.function.Predicate;

import com.boaat.jazzy_cookin.item.KitchenIngredientItem;
import com.boaat.jazzy_cookin.item.KitchenMealItem;
import com.boaat.jazzy_cookin.kitchen.HeatLevel;
import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.IngredientStateData;
import com.boaat.jazzy_cookin.kitchen.KitchenMethod;
import com.boaat.jazzy_cookin.kitchen.sim.CookingBatchState;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMaterialProfiles;
import com.boaat.jazzy_cookin.kitchen.sim.recognition.DishRecognitionResult;
import com.boaat.jazzy_cookin.kitchen.sim.recognition.DishSchema;
import com.boaat.jazzy_cookin.kitchen.sim.station.StationSimulationAccess;

import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

final class CompositionalSimulationSupport {
    private CompositionalSimulationSupport() {
    }

    static boolean hasFoodInputs(StationSimulationAccess access) {
        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            if (isFood(access.simulationItem(slot))) {
                return true;
            }
        }
        return false;
    }

    static int foodInputCount(StationSimulationAccess access) {
        int count = 0;
        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            if (isFood(access.simulationItem(slot))) {
                count++;
            }
        }
        return count;
    }

    static boolean isFood(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof KitchenIngredientItem;
    }

    static boolean isSupportiveExtra(ItemStack stack) {
        return FoodMaterialProfiles.hasTrait(stack, com.boaat.jazzy_cookin.kitchen.sim.FoodTrait.SALT)
                || FoodMaterialProfiles.hasTrait(stack, com.boaat.jazzy_cookin.kitchen.sim.FoodTrait.SPICE)
                || FoodMaterialProfiles.hasTrait(stack, com.boaat.jazzy_cookin.kitchen.sim.FoodTrait.HERB)
                || FoodMaterialProfiles.hasTrait(stack, com.boaat.jazzy_cookin.kitchen.sim.FoodTrait.SWEETENER)
                || FoodMaterialProfiles.hasTrait(stack, com.boaat.jazzy_cookin.kitchen.sim.FoodTrait.CONDIMENT)
                || FoodMaterialProfiles.hasTrait(stack, com.boaat.jazzy_cookin.kitchen.sim.FoodTrait.OIL)
                || FoodMaterialProfiles.hasTrait(stack, com.boaat.jazzy_cookin.kitchen.sim.FoodTrait.CHOCOLATE)
                || FoodMaterialProfiles.hasTrait(stack, com.boaat.jazzy_cookin.kitchen.sim.FoodTrait.NUT)
                || FoodMaterialProfiles.hasTrait(stack, com.boaat.jazzy_cookin.kitchen.sim.FoodTrait.CAFFEINATED);
    }

    static ItemStack dominantFoodInput(StationSimulationAccess access) {
        ItemStack firstFood = ItemStack.EMPTY;
        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            ItemStack stack = access.simulationItem(slot);
            if (!isFood(stack)) {
                continue;
            }
            if (!isSupportiveExtra(stack)) {
                return stack;
            }
            if (firstFood.isEmpty()) {
                firstFood = stack;
            }
        }
        return firstFood;
    }

    static void removeAllFoodInputs(StationSimulationAccess access) {
        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            if (isFood(access.simulationItem(slot))) {
                access.simulationRemoveItem(slot, access.simulationItem(slot).getCount());
            }
        }
    }

    static float targetTempC(HeatLevel heatLevel) {
        return switch (heatLevel) {
            case LOW -> 120.0F;
            case MEDIUM -> 170.0F;
            case HIGH -> 220.0F;
            default -> 24.0F;
        };
    }

    static int timedDuration(StationSimulationAccess access, int baseDuration) {
        float modifier = switch (access.simulationHeatLevel()) {
            case LOW -> 1.20F;
            case MEDIUM -> 1.0F;
            case HIGH -> 0.82F;
            default -> 1.0F;
        };
        if (access.simulationStationType().supportsStationControl()) {
            modifier *= switch (access.simulationControlSetting()) {
                case 0 -> 1.12F;
                case 2 -> 0.90F;
                default -> 1.0F;
            };
        }
        return Math.max(20, Math.round(baseDuration * modifier));
    }

    static FoodMatterData composeMatter(
            StationSimulationAccess access,
            SimulationIngredientAnalysis analysis,
            IngredientState state,
            boolean finalizedServing,
            float completion,
            float waterBias,
            float aeration,
            float fragmentation,
            float cohesiveness,
            float proteinSet,
            float browning,
            float charLevel
    ) {
        long gameTime = access.simulationLevel() != null ? access.simulationLevel().getGameTime() : 0L;
        long createdTick = analysis.createdTick() > 0L ? analysis.createdTick() : gameTime;
        float water = Mth.clamp(analysis.avgWater() + waterBias, 0.02F, 1.0F);
        float flavor = Mth.clamp(
                0.22F
                        + analysis.avgSeasoning() * 0.50F
                        + analysis.avgCheese() * 0.18F
                        + analysis.avgOnion() * 0.15F
                        + analysis.avgHerb() * 0.15F
                        + analysis.avgPepper() * 0.12F
                        + browning * 0.08F,
                0.0F,
                1.0F
        );
        float texture = Mth.clamp(0.18F + cohesiveness * 0.32F + proteinSet * 0.20F + browning * 0.10F - charLevel * 0.18F, 0.0F, 1.0F);
        float structure = Mth.clamp(0.20F + cohesiveness * 0.40F + analysis.avgProtein() * 0.16F + analysis.avgFat() * 0.10F, 0.0F, 1.0F);
        float purity = Mth.clamp(0.58F + completion * 0.20F - analysis.avgOnion() * 0.04F, 0.0F, 1.0F);
        int nourishment = Math.max(1, Math.round(analysis.avgProtein() * 10.0F + analysis.avgFat() * 4.0F + analysis.totalItems() * 0.4F));
        int enjoyment = Math.max(1, Math.round(3.0F + flavor * 4.0F + texture * 3.0F + analysis.avgCheese() * 2.0F + analysis.avgHerb() * 2.0F));
        float tempC = access.simulationBatch() != null ? access.simulationBatch().matter().surfaceTempC() : targetTempC(access.simulationHeatLevel());
        IngredientStateData summary = new IngredientStateData(
                state,
                createdTick,
                Mth.clamp(0.68F + completion * 0.16F + proteinSet * 0.10F - charLevel * 0.12F, 0.05F, 1.0F),
                Mth.clamp(0.64F + completion * 0.18F - charLevel * 0.10F, 0.0F, 1.0F),
                flavor,
                texture,
                structure,
                water,
                purity,
                aeration,
                Math.max(1, (int) Math.ceil(completion * 3.0F)),
                nourishment,
                enjoyment
        );

        return new FoodMatterData(
                createdTick,
                summary,
                analysis.traitMask(),
                tempC,
                Mth.lerp(0.65F, 24.0F, tempC),
                water,
                Mth.clamp(analysis.avgFat(), 0.0F, 1.0F),
                Mth.clamp(analysis.avgProtein(), 0.0F, 1.0F),
                Mth.clamp(aeration, 0.0F, 1.0F),
                Mth.clamp(fragmentation, 0.0F, 1.0F),
                Mth.clamp(cohesiveness, 0.0F, 1.0F),
                Mth.clamp(proteinSet, 0.0F, 1.0F),
                Mth.clamp(browning, 0.0F, 1.0F),
                Mth.clamp(charLevel, 0.0F, 1.0F),
                Mth.clamp(analysis.avgSeasoning(), 0.0F, 1.0F),
                Mth.clamp(analysis.avgCheese(), 0.0F, 1.0F),
                Mth.clamp(analysis.avgOnion(), 0.0F, 1.0F),
                Mth.clamp(analysis.avgHerb(), 0.0F, 1.0F),
                Mth.clamp(analysis.avgPepper(), 0.0F, 1.0F),
                FoodMatterData.UNSET_ENVIRONMENT,
                FoodMatterData.UNSET_ENVIRONMENT,
                FoodMatterData.UNSET_ENVIRONMENT,
                0.0F,
                0,
                0,
                0,
                Math.max(1, summary.processDepth()),
                finalizedServing
        ).clamp();
    }

    static ItemStack recognizedPreparedOutput(StationSimulationAccess access, SimulationIngredientAnalysis analysis, FoodMatterData matter) {
        DishRecognitionResult result = DishSchema.finalizePrepared(matter);
        if (result == null || !(result.resultItem().get() instanceof KitchenIngredientItem ingredientItem)) {
            return ItemStack.EMPTY;
        }
        return SimulationOutputFactory.createOutput(ingredientItem, access.simulationLevel().getGameTime(), analysis, matter);
    }

    static ItemStack recognizedMealOutput(StationSimulationAccess access, SimulationIngredientAnalysis analysis, FoodMatterData matter, Predicate<Item> filter) {
        DishRecognitionResult result = DishSchema.finalizeResult(matter, item -> item instanceof KitchenMealItem && filter.test(item));
        if (result == null || !(result.resultItem().get() instanceof KitchenIngredientItem ingredientItem)) {
            return ItemStack.EMPTY;
        }
        return SimulationOutputFactory.createOutput(ingredientItem, access.simulationLevel().getGameTime(), analysis, matter);
    }

    static void advanceTimedBatch(StationSimulationAccess access, IngredientState state, boolean finalizedServing, float waterBias, float aeration, float fragmentation, float cohesiveness, float proteinSet, float browning, float charLevel) {
        SimulationIngredientAnalysis analysis = SimulationIngredientAnalysis.analyzeInputs(access);
        float completion = access.simulationMaxProgress() > 0 ? Mth.clamp((access.simulationProgress() + 1) / (float) access.simulationMaxProgress(), 0.0F, 1.0F) : 0.0F;
        access.simulationSetBatch(new CookingBatchState(composeMatter(
                access,
                analysis,
                state,
                finalizedServing,
                completion,
                waterBias,
                aeration,
                fragmentation,
                cohesiveness,
                proteinSet,
                browning,
                charLevel
        )));
    }
}
