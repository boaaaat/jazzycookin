package com.boaat.jazzy_cookin.kitchen.sim.domain;

import java.util.function.Predicate;

import com.boaat.jazzy_cookin.item.KitchenIngredientItem;
import com.boaat.jazzy_cookin.item.KitchenMealItem;
import com.boaat.jazzy_cookin.kitchen.HeatLevel;
import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.KitchenMethod;
import com.boaat.jazzy_cookin.kitchen.sim.CookingBatchState;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMaterialProfiles;
import com.boaat.jazzy_cookin.kitchen.sim.recognition.DishRecognitionResult;
import com.boaat.jazzy_cookin.kitchen.sim.recognition.DishSchema;
import com.boaat.jazzy_cookin.kitchen.sim.schema.DishAttemptContext;
import com.boaat.jazzy_cookin.kitchen.sim.schema.DishSchemaDefinition;
import com.boaat.jazzy_cookin.kitchen.sim.schema.DishSchemaScore;
import com.boaat.jazzy_cookin.kitchen.sim.schema.DishSchemaScorer;
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
        float carryFactor = Mth.clamp(
                0.24F
                        + analysis.avgProcessDepth() * 0.10F
                        + analysis.finalizedServingRatio() * 0.08F
                        + analysis.avgProteinSetCarry() * 0.06F,
                0.24F,
                0.76F
        );
        float inheritedWater = analysis.avgWater();
        float inheritedAeration = analysis.avgAerationCarry();
        float inheritedFragmentation = analysis.avgFragmentationCarry();
        float inheritedCohesiveness = analysis.avgCohesivenessCarry();
        float inheritedProteinSet = analysis.avgProteinSetCarry();
        float inheritedBrowning = analysis.avgBrowningCarry();
        float inheritedChar = analysis.avgCharLevelCarry();
        float desiredWater = Mth.clamp(inheritedWater + waterBias, 0.02F, 1.0F);
        float water = Mth.clamp(Mth.lerp(0.28F + carryFactor * 0.34F, desiredWater, inheritedWater), 0.02F, 1.0F);
        float actualAeration = Mth.clamp(Mth.lerp(0.24F + carryFactor * 0.42F, aeration, inheritedAeration), 0.0F, 1.0F);
        float actualFragmentation = Mth.clamp(Mth.lerp(0.28F + carryFactor * 0.40F, fragmentation, inheritedFragmentation), 0.0F, 1.0F);
        float actualCohesiveness = Mth.clamp(Mth.lerp(0.30F + carryFactor * 0.40F, cohesiveness, inheritedCohesiveness), 0.0F, 1.0F);
        float actualProteinSet = Mth.clamp(
                Math.max(proteinSet * 0.72F, Mth.lerp(0.20F + carryFactor * 0.46F, proteinSet, inheritedProteinSet)),
                0.0F,
                1.0F
        );
        float actualBrowning = Mth.clamp(
                Math.max(browning * 0.70F, Mth.lerp(0.18F + carryFactor * 0.44F, browning, inheritedBrowning)),
                0.0F,
                1.0F
        );
        float actualCharLevel = Mth.clamp(
                Math.max(charLevel * 0.80F, Mth.lerp(0.10F + carryFactor * 0.38F, charLevel, inheritedChar)),
                0.0F,
                1.0F
        );
        float flavor = Mth.clamp(
                0.22F
                        + analysis.avgSeasoning() * 0.50F
                        + analysis.avgCheese() * 0.18F
                        + analysis.avgOnion() * 0.15F
                        + analysis.avgHerb() * 0.15F
                        + analysis.avgPepper() * 0.12F
                        + actualBrowning * 0.08F
                        - analysis.avgOxidation() * 0.10F,
                0.0F,
                1.0F
        );
        float texture = Mth.clamp(
                0.18F
                        + actualCohesiveness * 0.30F
                        + actualProteinSet * 0.22F
                        + actualBrowning * 0.10F
                        - actualCharLevel * 0.18F
                        + actualAeration * 0.06F,
                0.0F,
                1.0F
        );
        float structure = Mth.clamp(
                0.20F
                        + actualCohesiveness * 0.36F
                        + analysis.avgProtein() * 0.16F
                        + analysis.avgFat() * 0.10F
                        - actualFragmentation * 0.08F,
                0.0F,
                1.0F
        );
        float purity = Mth.clamp(0.58F + completion * 0.20F - analysis.avgOnion() * 0.04F - analysis.avgMicrobialLoad() * 0.06F, 0.0F, 1.0F);
        int nourishment = Math.max(1, Math.round(analysis.avgProtein() * 10.0F + analysis.avgFat() * 4.0F + analysis.totalItems() * 0.4F));
        int enjoyment = Math.max(
                1,
                Math.round(3.0F + flavor * 4.0F + texture * 3.0F + analysis.avgCheese() * 2.0F + analysis.avgHerb() * 2.0F - analysis.avgOxidation() * 2.0F)
        );
        float targetSurfaceTempC = access.simulationBatch() != null ? access.simulationBatch().matter().surfaceTempC() : targetTempC(access.simulationHeatLevel());
        float targetCoreTempC = access.simulationBatch() != null
                ? access.simulationBatch().matter().coreTempC()
                : Mth.lerp(0.45F, analysis.avgCoreTempC(), targetSurfaceTempC);
        float surfaceTempC = Mth.lerp(0.26F + carryFactor * 0.30F, targetSurfaceTempC, analysis.avgSurfaceTempC());
        float coreTempC = Mth.lerp(0.34F + carryFactor * 0.28F, targetCoreTempC, analysis.avgCoreTempC());
        int processDepth = Math.max(1, Math.round(Math.max(analysis.avgProcessDepth(), 0.0F)) + 1);
        float whiskWork = Mth.clamp(
                analysis.avgWhiskWork() + Math.max(0.0F, actualAeration - inheritedAeration) * 0.80F,
                0.0F,
                2.0F
        );
        int stirCount = Math.max(0, Math.round(analysis.avgStirCount() + completion * (access.simulationHeatLevel() == HeatLevel.OFF ? 1.0F : 2.0F)));
        int flipCount = Math.max(0, Math.round(analysis.avgFlipCount() + (actualBrowning > inheritedBrowning + 0.05F ? 1.0F : 0.0F)));
        int timeInPan = Math.max(
                0,
                Math.round(
                        analysis.avgTimeInPan()
                                + completion * (access.simulationHeatLevel() == HeatLevel.OFF ? 3.0F : 10.0F + access.simulationHeatLevel().ordinal() * 4.0F)
                )
        );
        float preservationLevel = Mth.lerp(
                0.30F + carryFactor * 0.30F,
                FoodMatterData.derivePreservationLevel(analysis.traitMask(), water, analysis.avgProtein(), actualFragmentation, processDepth, finalizedServing),
                analysis.avgPreservation()
        );
        float oxidationLevel = Mth.lerp(
                0.24F + carryFactor * 0.24F,
                FoodMatterData.deriveOxidation(analysis.traitMask(), analysis.avgFat(), actualBrowning, actualCharLevel, preservationLevel),
                analysis.avgOxidation()
        );
        float microbialLevel = Mth.lerp(
                0.24F + carryFactor * 0.24F,
                FoodMatterData.deriveMicrobialLoad(analysis.traitMask(), water, analysis.avgProtein(), preservationLevel, finalizedServing),
                analysis.avgMicrobialLoad()
        );

        return new FoodMatterData(
                createdTick,
                analysis.traitMask(),
                surfaceTempC,
                coreTempC,
                water,
                Mth.clamp(analysis.avgFat(), 0.0F, 1.0F),
                Mth.clamp(analysis.avgProtein(), 0.0F, 1.0F),
                actualAeration,
                actualFragmentation,
                actualCohesiveness,
                actualProteinSet,
                actualBrowning,
                actualCharLevel,
                Mth.clamp(analysis.avgSeasoning(), 0.0F, 1.0F),
                Mth.clamp(analysis.avgCheese(), 0.0F, 1.0F),
                Mth.clamp(analysis.avgOnion(), 0.0F, 1.0F),
                Mth.clamp(analysis.avgHerb(), 0.0F, 1.0F),
                Mth.clamp(analysis.avgPepper(), 0.0F, 1.0F),
                preservationLevel,
                oxidationLevel,
                microbialLevel,
                whiskWork,
                stirCount,
                flipCount,
                timeInPan,
                processDepth,
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

    static ItemStack recognizedSchemaOutput(
            StationSimulationAccess access,
            SimulationIngredientAnalysis analysis,
            FoodMatterData matter,
            Predicate<DishSchemaDefinition> schemaFilter
    ) {
        DishSchemaScore score = bestSchemaScore(matter, schemaFilter, true);
        if (score == null || !(score.resultItem().get() instanceof KitchenIngredientItem ingredientItem)) {
            return ItemStack.EMPTY;
        }
        return SimulationOutputFactory.createOutput(ingredientItem, access.simulationLevel().getGameTime(), analysis, matter);
    }

    static int schemaPreviewId(FoodMatterData matter, Predicate<DishSchemaDefinition> schemaFilter) {
        DishSchemaScore score = bestSchemaScore(matter, schemaFilter, false);
        return score != null ? score.schema().previewId() : 0;
    }

    static ItemStack recognizedMealOutput(StationSimulationAccess access, SimulationIngredientAnalysis analysis, FoodMatterData matter, Predicate<Item> filter) {
        DishRecognitionResult result = DishSchema.finalizeResult(matter, item -> item instanceof KitchenMealItem && filter.test(item));
        if (result == null || !(result.resultItem().get() instanceof KitchenIngredientItem ingredientItem)) {
            return ItemStack.EMPTY;
        }
        return SimulationOutputFactory.createOutput(ingredientItem, access.simulationLevel().getGameTime(), analysis, matter);
    }

    private static DishSchemaScore bestSchemaScore(FoodMatterData matter, Predicate<DishSchemaDefinition> schemaFilter, boolean finalize) {
        if (matter == null) {
            return null;
        }
        DishAttemptContext context = DishAttemptContext.fromMatter(matter);
        return DishSchemaScorer.schemas().stream()
                .filter(schemaFilter)
                .map(schema -> DishSchemaScorer.score(schema, context))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .filter(score -> score.resultItem().get() instanceof KitchenIngredientItem)
                .filter(score -> score.score() >= (finalize ? score.schema().finalizeThreshold() : score.schema().previewThreshold()))
                .max(java.util.Comparator.comparing(DishSchemaScore::score).thenComparing(score -> score.schema().desirability()))
                .orElse(null);
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
