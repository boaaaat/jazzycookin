package com.boaat.jazzy_cookin.kitchen.sim.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.boaat.jazzy_cookin.item.KitchenIngredientItem;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.ToolProfile;
import com.boaat.jazzy_cookin.kitchen.sim.CookingBatchState;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.kitchen.sim.FoodTrait;
import com.boaat.jazzy_cookin.kitchen.sim.action.EggStoveSimulationActions;
import com.boaat.jazzy_cookin.kitchen.sim.reaction.EggPanReactionSolver;
import com.boaat.jazzy_cookin.kitchen.sim.schema.DishAttemptContext;
import com.boaat.jazzy_cookin.kitchen.sim.schema.DishCategory;
import com.boaat.jazzy_cookin.kitchen.sim.schema.DishSchemaDefinition;
import com.boaat.jazzy_cookin.kitchen.sim.schema.DishSchemaScore;
import com.boaat.jazzy_cookin.kitchen.sim.schema.DishSchemaScorer;
import com.boaat.jazzy_cookin.kitchen.sim.station.StationSimulationAccess;
import com.boaat.jazzy_cookin.kitchen.sim.station.StationSimulationResolver;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

final class PanSchemaSimulationActions {
    private PanSchemaSimulationActions() {
    }

    static boolean hasInputCandidate(StationSimulationAccess access) {
        return canUsePan(access) && inputScore(access).isPresent();
    }

    static int previewId(FoodMatterData matter) {
        return bestPanScore(matter, false)
                .map(score -> score.schema().previewId())
                .orElse(0);
    }

    static boolean primaryAction(StationSimulationAccess access) {
        if (access.simulationBatch() == null && StationSimulationResolver.supportsEggStove(access)) {
            return EggStoveSimulationActions.primaryAction(access);
        }
        return access.simulationBatch() == null ? start(access) : finish(access);
    }

    private static boolean start(StationSimulationAccess access) {
        if (access.simulationLevel() == null || !canUsePan(access)) {
            return false;
        }

        FoodMatterData matter = inputMatter(access, access.simulationLevel().getGameTime());
        if (matter == null || bestPanScore(matter, false).isEmpty()) {
            return false;
        }

        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            ItemStack stack = access.simulationItem(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof KitchenIngredientItem) {
                access.simulationRemoveItem(slot, stack.getCount());
            }
        }

        access.simulationSetBatch(new CookingBatchState(matter));
        access.simulationMarkChanged();
        return true;
    }

    private static boolean finish(StationSimulationAccess access) {
        if (access.simulationLevel() == null || access.simulationBatch() == null) {
            return false;
        }

        FoodMatterData matter = access.simulationBatch().matter();
        Optional<DishSchemaScore> score = bestPanScore(matter, true);
        if (score.isEmpty()) {
            return matter.hasTrait(FoodTrait.EGG) && EggStoveSimulationActions.primaryAction(access);
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
        ItemStack output = SimulationOutputFactory.createOutput(
                ingredientItem,
                access.simulationLevel().getGameTime(),
                SimulationIngredientAnalysis.analyzeStacks(List.of(), access.simulationLevel().getGameTime()),
                finalized
        );
        if (output.isEmpty() || !access.simulationCanAcceptStack(access.outputSlot(), output)) {
            return false;
        }

        access.simulationMergeIntoSlot(access.outputSlot(), output);
        access.simulationSetBatch(null);
        access.simulationMarkChanged();
        return true;
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

    private static FoodMatterData inputMatter(StationSimulationAccess access, long gameTime) {
        List<ItemStack> foodInputs = foodInputStacks(access);
        SimulationIngredientAnalysis analysis = SimulationIngredientAnalysis.analyzeStacks(foodInputs, gameTime);
        if (analysis.isEmpty()) {
            return null;
        }
        return new FoodMatterData(
                analysis.createdTick() > 0L ? analysis.createdTick() : gameTime,
                analysis.traitMask(),
                EggPanReactionSolver.ROOM_TEMP_C,
                EggPanReactionSolver.ROOM_TEMP_C,
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
}
