package com.boaat.jazzy_cookin.kitchen.sim.domain;

import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.KitchenMethod;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.kitchen.sim.SimulationSnapshot;
import com.boaat.jazzy_cookin.kitchen.sim.station.StationSimulationAccess;

import net.minecraft.world.item.ItemStack;

public final class JuicerSimulationDomain implements StationSimulationDomain {
    @Override
    public SimulationDomainType type() {
        return SimulationDomainType.JUICE;
    }

    @Override
    public boolean supports(StationSimulationAccess access) {
        return access.simulationStationType() == StationType.JUICER && !previewOutput(access).isEmpty();
    }

    @Override
    public KitchenMethod method(StationSimulationAccess access) {
        return KitchenMethod.JUICE;
    }

    @Override
    public SimulationSnapshot snapshot(StationSimulationAccess access, int executionMode) {
        FoodMatterData matter = previewMatter(access);
        if (matter == null) {
            return SimulationSnapshot.inactive(executionMode);
        }
        return new SimulationSnapshot(executionMode, 0, 72, 72, 72,
                Math.round(matter.cohesiveness() * 100.0F),
                Math.round(matter.water() * 100.0F),
                0,
                0,
                Math.round(matter.aeration() * 100.0F),
                Math.round(matter.fragmentation() * 100.0F),
                CompositionalSimulationSupport.schemaPreviewId(matter, JuicerSimulationDomain::isJuiceSchema));
    }

    @Override
    public boolean handleAction(StationSimulationAccess access, int buttonId) {
        if (buttonId != 6) {
            return false;
        }
        ItemStack output = previewOutput(access);
        if (output.isEmpty() || !access.simulationCanAcceptStack(access.outputSlot(), output)) {
            return false;
        }
        CompositionalSimulationSupport.removeAllFoodInputs(access);
        access.simulationMergeIntoSlot(access.outputSlot(), output);
        access.simulationMarkChanged();
        return true;
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
        return CompositionalSimulationSupport.recognizedSchemaOutput(access, analysis, matter, JuicerSimulationDomain::isJuiceSchema);
    }

    private static FoodMatterData previewMatter(StationSimulationAccess access) {
        SimulationIngredientAnalysis analysis = SimulationIngredientAnalysis.analyzeInputs(access);
        if (access.simulationLevel() == null || analysis.isEmpty()) {
            return null;
        }
        return CompositionalSimulationSupport.composeMatter(access, analysis, IngredientState.FRESH_JUICE, false, 0.90F,
                0.18F, 0.02F, 0.08F, 0.12F, 0.0F, 0.0F, 0.0F);
    }

    private static boolean isJuiceSchema(com.boaat.jazzy_cookin.kitchen.sim.schema.DishSchemaDefinition schema) {
        return !schema.meal() && schema.targets().water().isPresent()
                && schema.targets().water().get().min() >= 0.40F
                && schema.targets().fragmentation().isPresent()
                && schema.targets().fragmentation().get().max() <= 0.24F;
    }
}
