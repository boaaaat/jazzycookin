package com.boaat.jazzy_cookin.kitchen.sim.domain;

import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.KitchenMethod;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.sim.CookingBatchState;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.kitchen.sim.SimulationSnapshot;
import com.boaat.jazzy_cookin.kitchen.sim.schema.DishSchemaScorer;
import com.boaat.jazzy_cookin.kitchen.sim.schema.DishTechnique;
import com.boaat.jazzy_cookin.kitchen.sim.station.StationSimulationAccess;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

public final class PlateAssemblySimulationDomain implements StationSimulationDomain {
    @Override
    public SimulationDomainType type() {
        return SimulationDomainType.PLATE;
    }

    @Override
    public boolean supports(StationSimulationAccess access) {
        return access.simulationStationType() == StationType.PLATING_STATION
                && (access.simulationActive() || CompositionalSimulationSupport.hasFoodInputs(access));
    }

    @Override
    public KitchenMethod method(StationSimulationAccess access) {
        return KitchenMethod.PLATE;
    }

    @Override
    public SimulationSnapshot snapshot(StationSimulationAccess access, int executionMode) {
        FoodMatterData matter = previewMatter(access);
        if (matter == null) {
            return SimulationSnapshot.inactive(executionMode);
        }
        int previewId = CompositionalSimulationSupport.schemaPreviewId(access, matter, schema ->
                schema.meal() && schema.requiredTechniques().contains(DishTechnique.PLATED));
        return new SimulationSnapshot(
                executionMode,
                access.simulationActive() ? 1 : 0,
                72,
                72,
                72,
                access.simulationMaxProgress() > 0 ? Math.round((access.simulationProgress() / (float) access.simulationMaxProgress()) * 100.0F) : 0,
                previewId > 0 ? 100 : 0,
                0,
                0,
                Math.round(matter.aeration() * 100.0F),
                Math.round(matter.fragmentation() * 100.0F),
                previewId
        );
    }

    @Override
    public boolean handleAction(StationSimulationAccess access, int buttonId) {
        if (buttonId != 6 || access.simulationActive()) {
            return false;
        }
        ItemStack preview = previewOutput(access);
        if (preview.isEmpty() || !access.simulationCanAcceptStack(access.outputSlot(), preview)) {
            return false;
        }
        access.simulationSetBatch(new CookingBatchState(previewMatter(access), CompositionalSimulationSupport.schemaKey(preview)));
        access.simulationSetProgress(0, 18, true);
        access.simulationMarkChanged();
        return true;
    }

    @Override
    public void serverTick(StationSimulationAccess access) {
        if (!access.simulationActive()) {
            return;
        }
        int next = access.simulationProgress() + 1;
        access.simulationSetProgress(next, access.simulationMaxProgress(), true);
        if (next >= access.simulationMaxProgress()) {
            finish(access);
            return;
        }
        access.simulationMarkChanged();
    }

    private static void finish(StationSimulationAccess access) {
        ItemStack output = previewOutput(access);
        if (output.isEmpty() || !access.simulationCanAcceptStack(access.outputSlot(), output)) {
            access.simulationSetProgress(0, 0, false);
            access.simulationSetBatch(null);
            access.simulationMarkChanged();
            return;
        }
        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            if (!access.simulationItem(slot).isEmpty()) {
                access.simulationRemoveItem(slot, access.simulationItem(slot).getCount());
            }
        }
        access.simulationMergeIntoSlot(access.outputSlot(), output);
        access.simulationSetProgress(0, 0, false);
        access.simulationSetBatch(null);
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
        if (analysis.has(com.boaat.jazzy_cookin.registry.JazzyItems.CHICKEN_CURRY_PREP.get())
                && contains(access, com.boaat.jazzy_cookin.registry.JazzyItems.CERAMIC_BOWL.get())) {
            return CompositionalSimulationSupport.directPreparedOutput(
                    access,
                    analysis,
                    matter,
                    com.boaat.jazzy_cookin.registry.JazzyItems.CHICKEN_CURRY.get(),
                    IngredientState.PLATED,
                    "chicken_curry"
            );
        }
        if (analysis.has(com.boaat.jazzy_cookin.registry.JazzyItems.ASSEMBLED_SANDWICH.get())
                && contains(access, com.boaat.jazzy_cookin.registry.JazzyItems.CERAMIC_PLATE.get())) {
            ItemStack output = CompositionalSimulationSupport.directPreparedOutput(
                    access,
                    analysis,
                    matter,
                    com.boaat.jazzy_cookin.registry.JazzyItems.SANDWICH_PLATE.get(),
                    IngredientState.PLATED,
                    "sandwich_plate"
            );
            DishSchemaScorer.schemas().stream()
                    .filter(schema -> schema.key().equals("sandwich_plate"))
                    .findFirst()
                    .ifPresent(schema -> com.boaat.jazzy_cookin.kitchen.KitchenStackUtil.setDishAttempt(
                            output,
                            DishAttemptAssembler.build(schema, DishAttemptAssembler.view(access), 0.90F)
                    ));
            return output;
        }
        return CompositionalSimulationSupport.recognizedSchemaOutput(access, analysis, matter, schema ->
                schema.meal()
                        && schema.requiredTechniques().contains(DishTechnique.PLATED)
                        && hasServingItems(access, schema.servingItems()));
    }

    private static FoodMatterData previewMatter(StationSimulationAccess access) {
        if (access.simulationLevel() == null) {
            return null;
        }
        SimulationIngredientAnalysis analysis = SimulationIngredientAnalysis.analyzeInputs(access);
        if (analysis.isEmpty()) {
            return null;
        }
        float completion = access.simulationActive() && access.simulationMaxProgress() > 0
                ? (access.simulationProgress() + 1) / (float) access.simulationMaxProgress()
                : 0.95F;
        return CompositionalSimulationSupport.composeMatter(
                access,
                analysis,
                IngredientState.PLATED,
                true,
                completion,
                0.0F,
                0.04F,
                0.22F,
                0.48F,
                0.42F,
                0.06F,
                0.0F
        );
    }

    private static boolean hasServingItems(StationSimulationAccess access, java.util.List<net.minecraft.resources.ResourceLocation> servingItems) {
        if (servingItems.isEmpty()) {
            return true;
        }
        for (net.minecraft.resources.ResourceLocation servingItemId : servingItems) {
            if (!contains(access, BuiltInRegistries.ITEM.get(servingItemId))) {
                return false;
            }
        }
        return true;
    }

    private static boolean contains(StationSimulationAccess access, net.minecraft.world.item.Item item) {
        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            if (access.simulationItem(slot).is(item)) {
                return true;
            }
        }
        return false;
    }
}
