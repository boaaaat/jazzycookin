package com.boaat.jazzy_cookin.kitchen.sim.domain;

import com.boaat.jazzy_cookin.item.KitchenIngredientItem;
import com.boaat.jazzy_cookin.kitchen.KitchenMethod;
import com.boaat.jazzy_cookin.kitchen.KitchenStackUtil;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.kitchen.sim.SimulationSnapshot;
import com.boaat.jazzy_cookin.kitchen.sim.recognition.DishRecognitionResult;
import com.boaat.jazzy_cookin.kitchen.sim.recognition.DishSchema;
import com.boaat.jazzy_cookin.kitchen.sim.station.StationSimulationAccess;
import com.boaat.jazzy_cookin.registry.JazzyItems;
import com.boaat.jazzy_cookin.registry.JazzyItems.IngredientId;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public final class FreezeDrySimulationDomain implements StationSimulationDomain {
    @Override
    public SimulationDomainType type() {
        return SimulationDomainType.DRY;
    }

    @Override
    public boolean supports(StationSimulationAccess access) {
        return access.simulationStationType() == StationType.FREEZE_DRYER && classify(access) != null;
    }

    @Override
    public KitchenMethod method(StationSimulationAccess access) {
        return KitchenMethod.FREEZE_DRY;
    }

    @Override
    public SimulationSnapshot snapshot(StationSimulationAccess access, int executionMode) {
        FoodMatterData matter = previewMatter(access);
        if (matter == null) {
            return SimulationSnapshot.inactive(executionMode);
        }
        DishRecognitionResult preview = DishSchema.preview(matter);
        return new SimulationSnapshot(
                executionMode,
                0,
                72,
                72,
                72,
                Math.round(matter.cohesiveness() * 100.0F),
                Math.round(matter.water() * 100.0F),
                0,
                0,
                Math.round(matter.aeration() * 100.0F),
                Math.round(matter.fragmentation() * 100.0F),
                preview != null ? preview.previewId() : 0
        );
    }

    @Override
    public boolean handleAction(StationSimulationAccess access, int buttonId) {
        if (buttonId != 6 || access.simulationLevel() == null) {
            return false;
        }
        FreezeDryOutcome outcome = classify(access);
        if (outcome == null) {
            return false;
        }

        long gameTime = access.simulationLevel().getGameTime();
        SimulationIngredientAnalysis analysis = SimulationIngredientAnalysis.analyzeInputs(access);
        ItemStack output = SimulationOutputFactory.createOutput(outcome.output(), gameTime, analysis, matter -> shapeMatter(matter, analysis));
        if (!access.simulationCanAcceptStack(access.outputSlot(), output)) {
            return false;
        }

        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            if (!access.simulationItem(slot).isEmpty()) {
                access.simulationRemoveItem(slot, 1);
            }
        }

        access.simulationMergeIntoSlot(access.outputSlot(), output);
        access.simulationMarkChanged();
        return true;
    }

    private record FreezeDryOutcome(KitchenIngredientItem output) {
    }

    private static FreezeDryOutcome classify(StationSimulationAccess access) {
        SimulationIngredientAnalysis analysis = SimulationIngredientAnalysis.analyzeInputs(access);
        if (analysis.isEmpty()) {
            return null;
        }
        if (analysis.has(JazzyItems.ingredient(IngredientId.APPLES).get())
                && analysis.totalItems() == analysis.count(JazzyItems.ingredient(IngredientId.APPLES).get())) {
            return new FreezeDryOutcome(JazzyItems.PACKED_FREEZE_DRY_APPLES.get());
        }
        if (analysis.has(JazzyItems.PORTIONED_MEAL.get()) && analysis.totalItems() == analysis.count(JazzyItems.PORTIONED_MEAL.get())) {
            return new FreezeDryOutcome(JazzyItems.FREEZE_DRIED_MEAL.get());
        }
        return null;
    }

    private static FoodMatterData previewMatter(StationSimulationAccess access) {
        if (access.simulationLevel() == null) {
            return null;
        }
        ItemStack output = access.simulationItem(access.outputSlot());
        if (!output.isEmpty()) {
            return KitchenStackUtil.getOrCreateFoodMatter(output, access.simulationLevel().getGameTime());
        }
        FreezeDryOutcome outcome = classify(access);
        if (outcome == null) {
            return null;
        }
        SimulationIngredientAnalysis analysis = SimulationIngredientAnalysis.analyzeInputs(access);
        ItemStack preview = SimulationOutputFactory.createOutput(outcome.output(), access.simulationLevel().getGameTime(), analysis, matter -> shapeMatter(matter, analysis));
        return KitchenStackUtil.getFoodMatter(preview);
    }

    private static FoodMatterData shapeMatter(FoodMatterData matter, SimulationIngredientAnalysis analysis) {
        return matter.withWorkingState(
                Mth.clamp(matter.water() * 0.10F + analysis.avgWater() * 0.06F, 0.0F, 0.18F),
                0.02F,
                0.34F,
                0.26F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0,
                0,
                0,
                Math.max(1, matter.processDepth() + 1),
                false
        );
    }
}
