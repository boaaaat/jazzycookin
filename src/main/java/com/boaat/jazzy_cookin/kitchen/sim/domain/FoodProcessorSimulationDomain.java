package com.boaat.jazzy_cookin.kitchen.sim.domain;

import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.KitchenMethod;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.sim.CookingBatchState;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.kitchen.sim.SimulationSnapshot;
import com.boaat.jazzy_cookin.kitchen.sim.schema.DishTechnique;
import com.boaat.jazzy_cookin.kitchen.sim.station.StationSimulationAccess;

import net.minecraft.world.item.ItemStack;

public final class FoodProcessorSimulationDomain implements StationSimulationDomain {
    @Override
    public SimulationDomainType type() {
        return SimulationDomainType.PROCESS;
    }

    @Override
    public boolean supports(StationSimulationAccess access) {
        return access.simulationStationType() == StationType.FOOD_PROCESSOR
                && (access.simulationActive() || !previewOutput(access).isEmpty());
    }

    @Override
    public KitchenMethod method(StationSimulationAccess access) {
        return KitchenMethod.PROCESS;
    }

    @Override
    public SimulationSnapshot snapshot(StationSimulationAccess access, int executionMode) {
        FoodMatterData matter = previewMatter(access);
        if (matter == null) {
            return SimulationSnapshot.inactive(executionMode);
        }
        return new SimulationSnapshot(executionMode, access.simulationActive() ? 1 : 0, 72, 72, 72,
                access.simulationMaxProgress() > 0 ? Math.round((access.simulationProgress() / (float) access.simulationMaxProgress()) * 100.0F) : Math.round(matter.cohesiveness() * 100.0F),
                Math.round(matter.water() * 100.0F),
                0,
                0,
                Math.round(matter.aeration() * 100.0F),
                Math.round(matter.fragmentation() * 100.0F),
                CompositionalSimulationSupport.schemaPreviewId(matter, FoodProcessorSimulationDomain::isProcessorSchema));
    }

    @Override
    public boolean handleAction(StationSimulationAccess access, int buttonId) {
        if (buttonId != 6 || access.simulationActive()) {
            return false;
        }
        ItemStack output = previewOutput(access);
        if (output.isEmpty() || !access.simulationCanAcceptStack(access.outputSlot(), output)) {
            return false;
        }
        access.simulationSetBatch(new CookingBatchState(previewMatter(access)));
        access.simulationSetProgress(0, CompositionalSimulationSupport.timedDuration(access, 56), true);
        access.simulationMarkChanged();
        return true;
    }

    @Override
    public void serverTick(StationSimulationAccess access) {
        if (!access.simulationActive()) {
            return;
        }
        FoodMatterData matter = previewMatter(access);
        if (matter == null) {
            access.simulationSetBatch(null);
            access.simulationSetProgress(0, 0, false);
            access.simulationMarkChanged();
            return;
        }
        access.simulationSetBatch(new CookingBatchState(matter));
        int next = access.simulationProgress() + 1;
        if (next >= access.simulationMaxProgress()) {
            finish(access);
            return;
        }
        access.simulationSetProgress(next, access.simulationMaxProgress(), true);
        access.simulationMarkChanged();
    }

    private static void finish(StationSimulationAccess access) {
        ItemStack output = previewOutput(access);
        if (output.isEmpty() || !access.simulationCanAcceptStack(access.outputSlot(), output)) {
            access.simulationSetBatch(null);
            access.simulationSetProgress(0, 0, false);
            access.simulationMarkChanged();
            return;
        }
        CompositionalSimulationSupport.removeAllFoodInputs(access);
        access.simulationMergeIntoSlot(access.outputSlot(), output);
        access.simulationSetBatch(null);
        access.simulationSetProgress(0, 0, false);
        access.simulationMarkChanged();
    }

    private static ItemStack previewOutput(StationSimulationAccess access) {
        if (access.simulationLevel() == null) {
            return ItemStack.EMPTY;
        }
        SimulationIngredientAnalysis analysis = SimulationIngredientAnalysis.analyzeInputs(access);
        FoodMatterData matter = previewMatter(access);
        if (matter == null) {
            return ItemStack.EMPTY;
        }
        return CompositionalSimulationSupport.recognizedSchemaOutput(access, analysis, matter, FoodProcessorSimulationDomain::isProcessorSchema);
    }

    private static FoodMatterData previewMatter(StationSimulationAccess access) {
        SimulationIngredientAnalysis analysis = SimulationIngredientAnalysis.analyzeInputs(access);
        if (access.simulationLevel() == null || analysis.isEmpty()) {
            return null;
        }
        float completion = access.simulationActive() && access.simulationMaxProgress() > 0
                ? (access.simulationProgress() + 1) / (float) access.simulationMaxProgress()
                : 0.84F;
        return CompositionalSimulationSupport.composeMatter(access, analysis, IngredientState.SMOOTH_PASTE, false, completion,
                -0.04F, 0.08F, 0.42F, 0.38F, 0.0F, 0.0F, 0.0F);
    }

    private static boolean isProcessorSchema(com.boaat.jazzy_cookin.kitchen.sim.schema.DishSchemaDefinition schema) {
        return !schema.meal() && (schema.requiredTechniques().contains(DishTechnique.MIXED)
                || schema.requiredTechniques().contains(DishTechnique.PREPPED));
    }
}
