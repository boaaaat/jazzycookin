package com.boaat.jazzy_cookin.kitchen.sim.domain;

import com.boaat.jazzy_cookin.item.KitchenIngredientItem;
import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.KitchenMethod;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.sim.CookingBatchState;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.kitchen.sim.SimulationSnapshot;
import com.boaat.jazzy_cookin.kitchen.sim.recognition.DishRecognitionResult;
import com.boaat.jazzy_cookin.kitchen.sim.recognition.DishSchema;
import com.boaat.jazzy_cookin.kitchen.sim.schema.DishSchemaDefinition;
import com.boaat.jazzy_cookin.kitchen.sim.schema.DishTechnique;
import com.boaat.jazzy_cookin.kitchen.sim.station.StationSimulationAccess;
import com.boaat.jazzy_cookin.registry.JazzyItems;

import net.minecraft.world.item.ItemStack;

public final class RestSimulationDomain implements StationSimulationDomain {
    @Override
    public SimulationDomainType type() {
        return SimulationDomainType.REST;
    }

    @Override
    public boolean supports(StationSimulationAccess access) {
        return switch (access.simulationStationType()) {
            case COOLING_RACK, RESTING_BOARD -> access.simulationActive() || CompositionalSimulationSupport.hasFoodInputs(access);
            default -> false;
        };
    }

    @Override
    public KitchenMethod method(StationSimulationAccess access) {
        if (access.simulationStationType() == StationType.COOLING_RACK) {
            return KitchenMethod.COOL;
        }
        return shouldSlice(access) ? KitchenMethod.SLICE : KitchenMethod.REST;
    }

    @Override
    public SimulationSnapshot snapshot(StationSimulationAccess access, int executionMode) {
        FoodMatterData matter = previewMatter(access);
        if (matter == null) {
            return SimulationSnapshot.inactive(executionMode);
        }
        DishRecognitionResult preview = DishSchema.previewPrepared(matter);
        int schemaPreviewId = CompositionalSimulationSupport.schemaPreviewId(matter, RestSimulationDomain::isRestSchema);
        return new SimulationSnapshot(
                executionMode,
                access.simulationActive() ? 1 : 0,
                72,
                Math.round(matter.coreTempC() * 1.8F + 32.0F),
                Math.round(matter.surfaceTempC() * 1.8F + 32.0F),
                access.simulationMaxProgress() > 0 ? Math.round((access.simulationProgress() / (float) access.simulationMaxProgress()) * 100.0F) : 0,
                Math.round(matter.water() * 100.0F),
                0,
                0,
                Math.round(matter.aeration() * 100.0F),
                Math.round(matter.fragmentation() * 100.0F),
                schemaPreviewId != 0 ? schemaPreviewId : preview != null ? preview.previewId() : 0
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
        access.simulationSetBatch(new CookingBatchState(transformedMatter(
                access,
                SimulationIngredientAnalysis.analyzeInputs(access),
                0.0F
        )));
        access.simulationSetProgress(0, access.simulationStationType() == StationType.COOLING_RACK ? 40 : 28, true);
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
        access.simulationSetBatch(new CookingBatchState(transformedMatter(
                access,
                SimulationIngredientAnalysis.analyzeInputs(access),
                access.simulationMaxProgress() > 0 ? Math.min(1.0F, next / (float) access.simulationMaxProgress()) : 1.0F
        )));
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
        CompositionalSimulationSupport.removeAllFoodInputs(access);
        access.simulationMergeIntoSlot(access.outputSlot(), output);
        access.simulationSetProgress(0, 0, false);
        access.simulationSetBatch(null);
        access.simulationMarkChanged();
    }

    private static ItemStack previewOutput(StationSimulationAccess access) {
        ItemStack dominant = CompositionalSimulationSupport.dominantFoodInput(access);
        if (dominant.isEmpty() || !(dominant.getItem() instanceof KitchenIngredientItem ingredientItem) || access.simulationLevel() == null) {
            return ItemStack.EMPTY;
        }
        SimulationIngredientAnalysis analysis = SimulationIngredientAnalysis.analyzeInputs(access);
        FoodMatterData matter = previewMatter(access);
        if (matter == null) {
            return ItemStack.EMPTY;
        }
        ItemStack schemaOutput = CompositionalSimulationSupport.recognizedSchemaOutput(access, analysis, matter, RestSimulationDomain::isRestSchema);
        return schemaOutput.isEmpty()
                ? SimulationOutputFactory.createOutput(ingredientItem, access.simulationLevel().getGameTime(), analysis, matter)
                : schemaOutput;
    }

    private static FoodMatterData previewMatter(StationSimulationAccess access) {
        if (access.simulationLevel() == null) {
            return null;
        }
        if (access.simulationActive() && access.simulationBatch() != null) {
            return access.simulationBatch().matter();
        }
        SimulationIngredientAnalysis analysis = SimulationIngredientAnalysis.analyzeInputs(access);
        if (analysis.isEmpty()) {
            return null;
        }
        float completion = access.simulationActive() && access.simulationMaxProgress() > 0
                ? (access.simulationProgress() + 1) / (float) access.simulationMaxProgress()
                : 0.90F;
        return transformedMatter(access, analysis, completion);
    }

    private static FoodMatterData transformedMatter(StationSimulationAccess access, SimulationIngredientAnalysis analysis, float completion) {
        ItemStack dominant = CompositionalSimulationSupport.dominantFoodInput(access);
        IngredientState state = access.simulationStationType() == StationType.COOLING_RACK
                ? inferCoolingState(dominant)
                : shouldSlice(access) ? inferSliceState(dominant) : IngredientState.RESTED;
        FoodMatterData matter = CompositionalSimulationSupport.composeMatter(
                access,
                analysis,
                state,
                false,
                completion,
                access.simulationStationType() == StationType.COOLING_RACK ? 0.02F : 0.0F,
                0.02F,
                shouldSlice(access) ? 0.48F : 0.16F,
                shouldSlice(access) ? 0.26F : 0.44F,
                0.24F,
                0.02F,
                0.0F
        );
        return matter.withTemps(34.0F, 30.0F);
    }

    private static boolean isRestSchema(DishSchemaDefinition schema) {
        return !schema.meal() && schema.requiredTechniques().contains(DishTechnique.RESTED);
    }

    private static boolean shouldSlice(StationSimulationAccess access) {
        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            ItemStack stack = access.simulationItem(slot);
            if (stack.is(JazzyItems.ingredient(JazzyItems.IngredientId.BREAD).get())
                    || stack.is(JazzyItems.BAKED_CAKE.get())
                    || stack.is(JazzyItems.BAKED_BROWNIES.get())) {
                return true;
            }
        }
        return false;
    }

    private static IngredientState inferCoolingState(ItemStack dominant) {
        if (dominant.is(JazzyItems.ASSEMBLED_TRAY_PIE.get()) || dominant.is(JazzyItems.ASSEMBLED_SAVORY_PIE.get())) {
            return IngredientState.COOLED_PIE;
        }
        return IngredientState.COOLED;
    }

    private static IngredientState inferSliceState(ItemStack dominant) {
        if (dominant.is(JazzyItems.ingredient(JazzyItems.IngredientId.BREAD).get())) {
            return IngredientState.SLICED_BREAD;
        }
        return IngredientState.SLICED;
    }
}
