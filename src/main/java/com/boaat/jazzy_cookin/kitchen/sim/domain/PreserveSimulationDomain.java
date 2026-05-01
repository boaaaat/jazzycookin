package com.boaat.jazzy_cookin.kitchen.sim.domain;

import com.boaat.jazzy_cookin.item.KitchenIngredientItem;
import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.KitchenMethod;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.kitchen.sim.FoodTrait;
import com.boaat.jazzy_cookin.kitchen.sim.SimulationSnapshot;
import com.boaat.jazzy_cookin.kitchen.sim.recognition.DishRecognitionResult;
import com.boaat.jazzy_cookin.kitchen.sim.recognition.DishSchema;
import com.boaat.jazzy_cookin.kitchen.sim.station.StationSimulationAccess;
import com.boaat.jazzy_cookin.registry.JazzyItems;
import com.boaat.jazzy_cookin.registry.JazzyItems.IngredientId;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public final class PreserveSimulationDomain implements StationSimulationDomain {
    @Override
    public SimulationDomainType type() {
        return SimulationDomainType.PRESERVE;
    }

    @Override
    public boolean supports(StationSimulationAccess access) {
        return switch (access.simulationStationType()) {
            case CANNING_STATION, FERMENTATION_CROCK, DRYING_RACK -> access.simulationActive() || CompositionalSimulationSupport.hasFoodInputs(access);
            default -> false;
        };
    }

    @Override
    public KitchenMethod method(StationSimulationAccess access) {
        return switch (access.simulationStationType()) {
            case CANNING_STATION -> KitchenMethod.CAN;
            case FERMENTATION_CROCK -> KitchenMethod.FERMENT;
            case DRYING_RACK -> KitchenMethod.DRY;
            default -> KitchenMethod.NONE;
        };
    }

    @Override
    public SimulationSnapshot snapshot(StationSimulationAccess access, int executionMode) {
        FoodMatterData matter = previewMatter(access);
        if (matter == null) {
            return SimulationSnapshot.inactive(executionMode);
        }
        DishRecognitionResult preview = DishSchema.previewPrepared(matter);
        return new SimulationSnapshot(
                executionMode,
                access.simulationActive() ? 1 : 0,
                72,
                72,
                72,
                access.simulationMaxProgress() > 0 ? Math.round((access.simulationProgress() / (float) access.simulationMaxProgress()) * 100.0F) : 0,
                Math.round(matter.preservationLevel() * 100.0F),
                Math.round(matter.browning() * 100.0F),
                Math.round(matter.charLevel() * 100.0F),
                Math.round(matter.aeration() * 100.0F),
                Math.round(matter.fragmentation() * 100.0F),
                preview != null ? preview.previewId() : 0
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
        int duration = switch (access.simulationStationType()) {
            case CANNING_STATION -> 120;
            case FERMENTATION_CROCK -> 160;
            case DRYING_RACK -> 100;
            default -> 80;
        };
        access.simulationSetProgress(0, duration, true);
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
        CompositionalSimulationSupport.removeAllFoodInputs(access);
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
        ItemStack schemaOutput = CompositionalSimulationSupport.recognizedSchemaOutput(access, analysis, matter, schema ->
                !schema.meal() && (schema.requiredTechniques().contains(com.boaat.jazzy_cookin.kitchen.sim.schema.DishTechnique.PREPPED)
                        || schema.requiredTechniques().isEmpty()));
        if (!schemaOutput.isEmpty()) {
            return schemaOutput;
        }
        return CompositionalSimulationSupport.recognizedPreparedOutput(access, analysis, matter);
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
                : 0.88F;
        IngredientState state = inferState(access, analysis);
        float waterBias = access.simulationStationType() == StationType.DRYING_RACK ? -0.42F : -0.10F;
        FoodMatterData matter = CompositionalSimulationSupport.composeMatter(
                access,
                analysis,
                state,
                false,
                completion,
                waterBias,
                0.02F,
                access.simulationStationType() == StationType.DRYING_RACK ? 0.34F : 0.18F,
                0.34F,
                0.08F,
                0.02F,
                0.0F
        );
        float preservation = switch (access.simulationStationType()) {
            case CANNING_STATION -> 0.78F;
            case FERMENTATION_CROCK -> 0.70F;
            case DRYING_RACK -> 0.62F;
            default -> 0.30F;
        };
        return matter.withPreservationState(
                preservation,
                Mth.clamp(0.02F + (1.0F - preservation) * 0.08F, 0.0F, 1.0F),
                Mth.clamp(0.02F + (1.0F - preservation) * 0.12F, 0.0F, 1.0F)
        );
    }

    private static IngredientState inferState(StationSimulationAccess access, SimulationIngredientAnalysis analysis) {
        return switch (access.simulationStationType()) {
            case CANNING_STATION -> {
                if (analysis.has(JazzyItems.ingredient(IngredientId.JAM).get())
                        || analysis.has(JazzyItems.ingredient(IngredientId.JELLY).get())
                        || analysis.has(JazzyItems.ingredient(IngredientId.SYRUP_PRESERVES).get())
                        || analysis.hasTrait(FoodTrait.SWEETENER)
                        || analysis.hasTrait(FoodTrait.SYRUP)) {
                    yield IngredientState.HOT_PRESERVE;
                }
                if (analysis.has(JazzyItems.ingredient(IngredientId.CANNED_TOMATOES).get())
                        || analysis.has(JazzyItems.ingredient(IngredientId.TOMATOES).get())) {
                    yield IngredientState.CANNED_TOMATO;
                }
                yield IngredientState.CANNING_SYRUP;
            }
            case FERMENTATION_CROCK -> analysis.has(JazzyItems.ingredient(IngredientId.CABBAGE).get())
                    || analysis.has(JazzyItems.ingredient(IngredientId.BRINED_VEGETABLES).get())
                    ? IngredientState.FERMENTED_VEGETABLE
                    : IngredientState.FERMENTED;
            case DRYING_RACK -> analysis.has(JazzyItems.ingredient(IngredientId.APPLES).get())
                    || analysis.has(JazzyItems.ingredient(IngredientId.RAISINS).get())
                    || analysis.has(JazzyItems.ingredient(IngredientId.DRIED_CRANBERRIES).get())
                    ? IngredientState.DRIED_FRUIT
                    : IngredientState.COARSE_POWDER;
            default -> IngredientState.FERMENTED;
        };
    }
}
