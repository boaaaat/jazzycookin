package com.boaat.jazzy_cookin.kitchen.sim.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.boaat.jazzy_cookin.item.KitchenIngredientItem;
import com.boaat.jazzy_cookin.kitchen.KitchenStackUtil;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.ToolProfile;
import com.boaat.jazzy_cookin.kitchen.sim.CookingBatchState;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.kitchen.sim.reaction.EggPanReactionSolver;
import com.boaat.jazzy_cookin.kitchen.sim.schema.DishAttemptContext;
import com.boaat.jazzy_cookin.kitchen.sim.schema.DishCategory;
import com.boaat.jazzy_cookin.kitchen.sim.schema.DishSchemaDefinition;
import com.boaat.jazzy_cookin.kitchen.sim.schema.DishSchemaScore;
import com.boaat.jazzy_cookin.kitchen.sim.schema.DishSchemaScorer;
import com.boaat.jazzy_cookin.kitchen.sim.station.StationSimulationAccess;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

final class PanSchemaSimulationActions {
    private PanSchemaSimulationActions() {
    }

    static boolean hasInputCandidate(StationSimulationAccess access) {
        return canUsePan(access) && inputScore(access).isPresent();
    }

    static boolean hasPanFoodInput(StationSimulationAccess access) {
        return canUsePan(access) && !foodInputStacks(access).isEmpty();
    }

    static FoodMatterData currentInputMatter(StationSimulationAccess access) {
        return access.simulationLevel() == null ? null : inputMatter(access, access.simulationLevel().getGameTime());
    }

    static void syncBatchFromInputs(StationSimulationAccess access) {
        if (access.simulationLevel() == null) {
            return;
        }
        FoodMatterData matter = inputMatter(access, access.simulationLevel().getGameTime());
        if (matter == null) {
            if (access.simulationBatch() != null) {
                access.simulationSetBatch(null);
                access.simulationMarkChanged();
            }
            return;
        }
        if (access.simulationBatch() == null || !matter.equals(access.simulationBatch().matter())) {
            access.simulationSetBatch(new CookingBatchState(matter));
            access.simulationMarkChanged();
        }
    }

    static int readinessPercent(StationSimulationAccess access) {
        return Math.round(readiness(access).progress() * 100.0F);
    }

    static int previewId(FoodMatterData matter) {
        return bestPanScore(matter, false)
                .map(score -> score.schema().previewId())
                .orElse(0);
    }

    static boolean primaryAction(StationSimulationAccess access) {
        syncBatchFromInputs(access);
        return finish(access);
    }

    static boolean stir(StationSimulationAccess access) {
        return mutatePanMatter(access, true);
    }

    static boolean foldOrFlip(StationSimulationAccess access) {
        return mutatePanMatter(access, false);
    }

    private static boolean finish(StationSimulationAccess access) {
        if (access.simulationLevel() == null || access.simulationBatch() == null) {
            return false;
        }
        PanReadiness readiness = readiness(access);
        if (!readiness.hasCookableFood() || !readiness.allReady()) {
            return false;
        }

        FoodMatterData matter = access.simulationBatch().matter();
        Optional<DishSchemaScore> score = bestPanScore(matter, true);
        if (score.isEmpty()) {
            return false;
        }
        if (!(score.get().resultItem().get() instanceof KitchenIngredientItem ingredientItem)) {
            return false;
        }

        FoodMatterData finalized = matter.withWorkingState(
                matter.water(),
                matter.aeration(),
                matter.fragmentation(),
                matter.cohesiveness(),
                matter.proteinSet(),
                matter.browning(),
                matter.charLevel(),
                matter.whiskWork(),
                matter.stirCount(),
                matter.flipCount(),
                matter.timeInPan(),
                Math.max(1, matter.processDepth()),
                score.get().schema().meal()
        );
        finalized = applyQualityPenalty(finalized, readiness);
        ItemStack output = SimulationOutputFactory.createOutput(
                ingredientItem,
                access.simulationLevel().getGameTime(),
                SimulationIngredientAnalysis.analyzeStacks(foodInputStacks(access), access.simulationLevel().getGameTime()),
                finalized
        );
        if (output.isEmpty() || !access.simulationCanAcceptStack(access.outputSlot(), output)) {
            return false;
        }

        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            ItemStack stack = access.simulationItem(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof KitchenIngredientItem) {
                access.simulationRemoveItem(slot, stack.getCount());
            }
        }
        access.simulationMergeIntoSlot(access.outputSlot(), output);
        access.simulationSetBatch(null);
        access.simulationMarkChanged();
        return true;
    }

    private static boolean mutatePanMatter(StationSimulationAccess access, boolean stir) {
        if (access.simulationLevel() == null || access.simulationBatch() == null) {
            return false;
        }
        long gameTime = access.simulationLevel().getGameTime();
        boolean changed = false;
        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            ItemStack stack = access.simulationItem(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof KitchenIngredientItem)) {
                continue;
            }
            FoodMatterData before = KitchenStackUtil.getOrCreateFoodMatter(stack, gameTime);
            if (before == null) {
                continue;
            }
            FoodMatterData after = stir ? stirred(before) : folded(before);
            if (!after.equals(before)) {
                KitchenStackUtil.setFoodMatter(stack, after, gameTime);
                changed = true;
            }
        }
        if (changed) {
            syncBatchFromInputs(access);
            access.simulationMarkChanged();
            return true;
        }

        FoodMatterData matter = access.simulationBatch().matter();
        FoodMatterData updated = stir ? stirred(matter) : folded(matter);
        if (updated.equals(matter)) {
            return false;
        }
        access.simulationSetBatch(new CookingBatchState(updated));
        access.simulationMarkChanged();
        return true;
    }

    private static FoodMatterData applyQualityPenalty(FoodMatterData matter, PanReadiness readiness) {
        float penalty = Mth.clamp(1.0F - readiness.quality(), 0.0F, 1.0F);
        if (penalty <= 0.001F) {
            return matter;
        }
        return matter.withWorkingState(
                Mth.clamp(matter.water() - penalty * 0.08F, 0.0F, 1.0F),
                Mth.clamp(matter.aeration() - penalty * 0.10F, 0.0F, 1.0F),
                Mth.clamp(matter.fragmentation() + penalty * 0.16F, 0.0F, 1.0F),
                Mth.clamp(matter.cohesiveness() - penalty * 0.22F, 0.0F, 1.0F),
                matter.proteinSet(),
                Mth.clamp(matter.browning() + readiness.overcookedPenalty() * 0.16F, 0.0F, 1.0F),
                Mth.clamp(matter.charLevel() + penalty * 0.30F, 0.0F, 1.0F),
                matter.whiskWork(),
                matter.stirCount(),
                matter.flipCount(),
                matter.timeInPan(),
                matter.processDepth(),
                matter.finalizedServing()
        ).withPreservationState(
                matter.preservationLevel(),
                Mth.clamp(matter.oxidation() + penalty * 0.24F, 0.0F, 1.0F),
                matter.microbialLoad()
        );
    }

    private static FoodMatterData stirred(FoodMatterData matter) {
        return matter.withWorkingState(
                matter.water(),
                Math.max(0.0F, matter.aeration() - 0.01F),
                matter.fragmentation() + 0.16F,
                matter.cohesiveness() - 0.05F,
                matter.proteinSet(),
                Math.max(0.0F, matter.browning() - 0.01F),
                matter.charLevel(),
                matter.whiskWork(),
                matter.stirCount() + 1,
                matter.flipCount(),
                matter.timeInPan(),
                matter.processDepth(),
                false
        );
    }

    private static FoodMatterData folded(FoodMatterData matter) {
        return matter.withWorkingState(
                matter.water(),
                Math.max(0.0F, matter.aeration() - 0.01F),
                Math.max(0.0F, matter.fragmentation() - 0.08F),
                matter.cohesiveness() + 0.14F,
                matter.proteinSet(),
                matter.browning() + 0.01F,
                matter.charLevel(),
                matter.whiskWork(),
                matter.stirCount(),
                matter.flipCount() + 1,
                matter.timeInPan(),
                matter.processDepth(),
                false
        );
    }

    private static Optional<DishSchemaScore> inputScore(StationSimulationAccess access) {
        if (access.simulationLevel() == null) {
            return Optional.empty();
        }
        return bestPanScore(inputMatter(access, access.simulationLevel().getGameTime()), false);
    }

    private static Optional<DishSchemaScore> bestPanScore(FoodMatterData matter, boolean finalize) {
        if (matter == null) {
            return Optional.empty();
        }

        DishAttemptContext context = DishAttemptContext.fromMatter(matter);
        return DishSchemaScorer.schemas().stream()
                .filter(PanSchemaSimulationActions::isPanSchema)
                .map(schema -> DishSchemaScorer.score(schema, context))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(score -> score.resultItem().get() instanceof KitchenIngredientItem)
                .filter(score -> score.score() >= (finalize ? score.schema().finalizeThreshold() : score.schema().previewThreshold()))
                .max(Comparator.comparing(DishSchemaScore::score).thenComparing(score -> score.schema().desirability()));
    }

    private static boolean isPanSchema(DishSchemaDefinition schema) {
        return schema.category() == DishCategory.EGG || (schema.category() == DishCategory.PAN_DISH && !schema.meal());
    }

    private static boolean canUsePan(StationSimulationAccess access) {
        if (access.simulationStationType() != StationType.STOVE) {
            return false;
        }
        ToolProfile toolProfile = ToolProfile.fromStack(access.simulationItem(access.toolSlot()));
        if (toolProfile != ToolProfile.PAN && toolProfile != ToolProfile.FRYING_SKILLET) {
            return false;
        }
        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            ItemStack stack = access.simulationItem(slot);
            if (!stack.isEmpty() && !(stack.getItem() instanceof KitchenIngredientItem)) {
                return false;
            }
        }
        return true;
    }

    private static PanReadiness readiness(StationSimulationAccess access) {
        if (access.simulationLevel() == null) {
            return PanReadiness.EMPTY;
        }
        long gameTime = access.simulationLevel().getGameTime();
        int cookable = 0;
        int usableOil = 0;
        float minProgress = 1.0F;
        float qualityTotal = 0.0F;
        float unevennessTotal = 0.0F;
        float overcookedTotal = 0.0F;
        boolean allReady = true;

        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            ItemStack stack = access.simulationItem(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof KitchenIngredientItem)) {
                continue;
            }
            FoodMatterData matter = KitchenStackUtil.getOrCreateFoodMatter(stack, gameTime);
            if (matter == null) {
                continue;
            }
            if (EggPanReactionSolver.isPanOil(matter)) {
                if (EggPanReactionSolver.cookProgress(matter) >= 0.30F && !EggPanReactionSolver.isOvercooked(matter)) {
                    usableOil += Math.max(1, stack.getCount());
                }
                continue;
            }

            int count = Math.max(1, stack.getCount());
            cookable += count;
            float progress = EggPanReactionSolver.cookProgress(matter);
            minProgress = Math.min(minProgress, progress);
            allReady &= progress >= EggPanReactionSolver.READY_PROGRESS;
            float idealFit = 1.0F - Mth.clamp(Math.abs(progress - 0.88F) / 0.88F, 0.0F, 1.0F);
            float unevenness = Mth.clamp(Math.abs(matter.surfaceTempC() - matter.coreTempC()) / 85.0F, 0.0F, 1.0F);
            float overcooked = EggPanReactionSolver.isOvercooked(matter) ? 1.0F : Mth.clamp(matter.charLevel() / EggPanReactionSolver.OVERCOOKED_CHAR, 0.0F, 1.0F);
            qualityTotal += idealFit * count;
            unevennessTotal += unevenness * count;
            overcookedTotal += overcooked * count;
        }

        if (cookable <= 0) {
            return PanReadiness.EMPTY;
        }
        float avgQuality = qualityTotal / cookable;
        float avgUnevenness = unevennessTotal / cookable;
        float avgOvercooked = overcookedTotal / cookable;
        float oilCoverage = Mth.clamp(usableOil / (float) cookable, 0.0F, 1.0F);
        float noOilPenalty = 0.22F * (1.0F - oilCoverage);
        float quality = Mth.clamp(avgQuality - avgUnevenness * 0.18F - avgOvercooked * 0.34F - noOilPenalty, 0.0F, 1.0F);
        return new PanReadiness(
                true,
                allReady,
                Mth.clamp(minProgress / EggPanReactionSolver.READY_PROGRESS, 0.0F, 1.0F),
                quality,
                1.0F - oilCoverage,
                avgUnevenness,
                avgOvercooked
        );
    }

    private static FoodMatterData inputMatter(StationSimulationAccess access, long gameTime) {
        List<ItemStack> foodInputs = foodInputStacks(access);
        SimulationIngredientAnalysis analysis = SimulationIngredientAnalysis.analyzeStacks(foodInputs, gameTime);
        if (analysis.isEmpty()) {
            return null;
        }
        return new FoodMatterData(
                analysis.createdTick() > 0L ? analysis.createdTick() : gameTime,
                analysis.traitMask(),
                analysis.avgSurfaceTempC(),
                analysis.avgCoreTempC(),
                analysis.avgWater(),
                analysis.avgFat(),
                analysis.avgProtein(),
                analysis.avgAerationCarry(),
                analysis.avgFragmentationCarry(),
                Mth.clamp(Math.max(analysis.avgCohesivenessCarry(), 0.18F), 0.0F, 1.0F),
                analysis.avgProteinSetCarry(),
                analysis.avgBrowningCarry(),
                analysis.avgCharLevelCarry(),
                analysis.avgSeasoning(),
                analysis.avgCheese(),
                analysis.avgOnion(),
                analysis.avgHerb(),
                analysis.avgPepper(),
                analysis.avgPreservation(),
                analysis.avgOxidation(),
                analysis.avgMicrobialLoad(),
                analysis.avgWhiskWork(),
                Math.max(0, Math.round(analysis.avgStirCount())),
                Math.max(0, Math.round(analysis.avgFlipCount())),
                Math.max(1, Math.round(analysis.avgTimeInPan())),
                Math.max(1, Math.round(analysis.avgProcessDepth()) + 1),
                false
        ).clamp();
    }

    private static List<ItemStack> foodInputStacks(StationSimulationAccess access) {
        return java.util.stream.IntStream.rangeClosed(access.inputStart(), access.inputEnd())
                .mapToObj(access::simulationItem)
                .filter(stack -> !stack.isEmpty() && stack.getItem() instanceof KitchenIngredientItem)
                .map(ItemStack::copy)
                .toList();
    }

    private record PanReadiness(
            boolean hasCookableFood,
            boolean allReady,
            float progress,
            float quality,
            float dryPanPenalty,
            float unevennessPenalty,
            float overcookedPenalty
    ) {
        private static final PanReadiness EMPTY = new PanReadiness(false, false, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
    }
}
