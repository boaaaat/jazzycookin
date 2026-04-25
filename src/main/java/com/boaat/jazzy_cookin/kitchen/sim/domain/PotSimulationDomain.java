package com.boaat.jazzy_cookin.kitchen.sim.domain;

import com.boaat.jazzy_cookin.item.KitchenIngredientItem;
import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.KitchenMethod;
import com.boaat.jazzy_cookin.kitchen.ToolProfile;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.sim.CookingBatchState;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.kitchen.sim.SimulationSnapshot;
import com.boaat.jazzy_cookin.kitchen.sim.recognition.DishRecognitionResult;
import com.boaat.jazzy_cookin.kitchen.sim.recognition.DishSchema;
import com.boaat.jazzy_cookin.kitchen.sim.schema.DishCategory;
import com.boaat.jazzy_cookin.kitchen.sim.schema.DishSchemaDefinition;
import com.boaat.jazzy_cookin.kitchen.sim.schema.DishTechnique;
import com.boaat.jazzy_cookin.kitchen.sim.station.StationSimulationAccess;
import com.boaat.jazzy_cookin.registry.JazzyItems;
import com.boaat.jazzy_cookin.registry.JazzyItems.IngredientId;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public final class PotSimulationDomain implements StationSimulationDomain {
    @Override
    public SimulationDomainType type() {
        return SimulationDomainType.POT;
    }

    @Override
    public boolean supports(StationSimulationAccess access) {
        if (access.simulationStationType() != StationType.STOVE) {
            return false;
        }
        ToolProfile tool = ToolProfile.fromStack(access.simulationItem(access.toolSlot()));
        return switch (tool) {
            case POT, SAUCEPAN, STOCK_POT -> access.simulationActive() || CompositionalSimulationSupport.hasFoodInputs(access);
            default -> false;
        };
    }

    @Override
    public KitchenMethod method(StationSimulationAccess access) {
        if (hasFryingSignal(access)) {
            return KitchenMethod.DEEP_FRY;
        }
        return access.simulationHeatLevel() == com.boaat.jazzy_cookin.kitchen.HeatLevel.HIGH ? KitchenMethod.BOIL : KitchenMethod.SIMMER;
    }

    @Override
    public SimulationSnapshot snapshot(StationSimulationAccess access, int executionMode) {
        FoodMatterData matter = previewMatter(access);
        if (matter == null) {
            return SimulationSnapshot.inactive(executionMode);
        }
        DishRecognitionResult preview = DishSchema.previewPrepared(matter);
        int schemaPreviewId = CompositionalSimulationSupport.schemaPreviewId(matter, PotSimulationDomain::isPotSchema);
        return new SimulationSnapshot(
                executionMode,
                access.simulationActive() ? 1 : 0,
                Math.round(Mth.clamp(CompositionalSimulationSupport.targetTempC(access.simulationHeatLevel()) * 1.8F + 32.0F, 72.0F, 460.0F)),
                Math.round(matter.coreTempC() * 1.8F + 32.0F),
                Math.round(matter.surfaceTempC() * 1.8F + 32.0F),
                access.simulationMaxProgress() > 0 ? Math.round((access.simulationProgress() / (float) access.simulationMaxProgress()) * 100.0F) : 0,
                Math.round(matter.water() * 100.0F),
                Math.round(matter.browning() * 100.0F),
                Math.round(matter.charLevel() * 100.0F),
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
        access.simulationSetProgress(0, CompositionalSimulationSupport.timedDuration(access, 180), true);
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
        access.simulationSetStationPhysics(new com.boaat.jazzy_cookin.kitchen.sim.StationPhysicsState(CompositionalSimulationSupport.targetTempC(access.simulationHeatLevel())));
        access.simulationSetBatch(new CookingBatchState(transformedMatter(
                access,
                SimulationIngredientAnalysis.analyzeInputs(access),
                access.simulationMaxProgress() > 0 ? Mth.clamp(next / (float) access.simulationMaxProgress(), 0.0F, 1.0F) : 1.0F
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
        if (access.simulationLevel() == null) {
            return ItemStack.EMPTY;
        }
        SimulationIngredientAnalysis analysis = SimulationIngredientAnalysis.analyzeInputs(access);
        FoodMatterData matter = previewMatter(access);
        if (matter == null) {
            return ItemStack.EMPTY;
        }
        ItemStack schemaOutput = CompositionalSimulationSupport.recognizedSchemaOutput(access, analysis, matter, PotSimulationDomain::isPotSchema);
        if (!schemaOutput.isEmpty()) {
            return schemaOutput;
        }
        KitchenIngredientItem targeted = targetedOutput(analysis);
        if (targeted != null) {
            return SimulationOutputFactory.createOutput(targeted, access.simulationLevel().getGameTime(), analysis, matter);
        }
        return CompositionalSimulationSupport.recognizedPreparedOutput(access, analysis, matter);
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
                : 0.84F;
        return transformedMatter(access, analysis, completion);
    }

    private static FoodMatterData transformedMatter(StationSimulationAccess access, SimulationIngredientAnalysis analysis, float completion) {
        IngredientState state = inferState(access, analysis);
        float proteinSet = hasFryingSignal(access) ? 0.72F : 0.48F + completion * 0.28F;
        float browning = hasFryingSignal(access) ? 0.26F + completion * 0.22F : completion * 0.12F;
        float waterBias = hasFryingSignal(access) ? -0.24F : 0.08F - completion * 0.10F;
        return CompositionalSimulationSupport.composeMatter(
                access,
                analysis,
                state,
                false,
                completion,
                waterBias,
                hasFryingSignal(access) ? 0.04F : 0.02F,
                hasFryingSignal(access) ? 0.26F : 0.22F,
                hasFryingSignal(access) ? 0.62F : 0.44F,
                proteinSet,
                browning,
                completion > 0.92F && access.simulationHeatLevel() == com.boaat.jazzy_cookin.kitchen.HeatLevel.HIGH ? 0.10F : 0.0F
        );
    }

    private static boolean isPotSchema(DishSchemaDefinition schema) {
        return !schema.meal()
                && (schema.category() == DishCategory.SOUP || schema.requiredTechniques().contains(DishTechnique.SIMMERED));
    }

    private static IngredientState inferState(StationSimulationAccess access, SimulationIngredientAnalysis analysis) {
        if (hasFryingSignal(access)) {
            return IngredientState.DEEP_FRIED;
        }
        if (analysis.has(JazzyItems.ingredient(IngredientId.BROTH).get()) || analysis.has(JazzyItems.ingredient(IngredientId.STOCK).get())) {
            return IngredientState.SIMMERED;
        }
        if (analysis.has(JazzyItems.ingredient(IngredientId.CANNED_TOMATOES).get())
                || analysis.has(JazzyItems.ingredient(IngredientId.TOMATOES).get())
                || analysis.has(JazzyItems.ingredient(IngredientId.TOMATO_PASTE).get())) {
            return IngredientState.SIMMERED;
        }
        if (analysis.has(JazzyItems.ingredient(IngredientId.RICE).get())
                || analysis.has(JazzyItems.ingredient(IngredientId.PASTA).get())
                || analysis.has(JazzyItems.ingredient(IngredientId.SPAGHETTI).get())
                || analysis.has(JazzyItems.ingredient(IngredientId.MACARONI).get())
                || analysis.has(JazzyItems.ingredient(IngredientId.RAMEN).get())) {
            return IngredientState.MIXED;
        }
        return KitchenMethod.SIMMER == methodStatic(access) ? IngredientState.SIMMERED : IngredientState.BOILED;
    }

    private static KitchenMethod methodStatic(StationSimulationAccess access) {
        return hasFryingSignal(access)
                ? KitchenMethod.DEEP_FRY
                : access.simulationHeatLevel() == com.boaat.jazzy_cookin.kitchen.HeatLevel.HIGH ? KitchenMethod.BOIL : KitchenMethod.SIMMER;
    }

    private static boolean hasFryingSignal(StationSimulationAccess access) {
        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            ItemStack stack = access.simulationItem(slot);
            if (stack.is(JazzyItems.BATTER_MIX.get())
                    || stack.is(JazzyItems.BREADED_FISH_FILLET.get())
                    || stack.is(JazzyItems.STUFFED_JALAPENOS.get())
                    || stack.is(JazzyItems.ingredient(IngredientId.BREADCRUMBS).get())) {
                return true;
            }
        }
        return access.simulationHeatLevel() == com.boaat.jazzy_cookin.kitchen.HeatLevel.HIGH;
    }

    private static KitchenIngredientItem targetedOutput(SimulationIngredientAnalysis analysis) {
        if (analysis.has(JazzyItems.ingredient(IngredientId.CHICKPEAS).get())
                && analysis.has(JazzyItems.ingredient(IngredientId.CURRY_POWDER).get())
                && analysis.has(JazzyItems.ingredient(IngredientId.ONIONS).get())
                && analysis.has(JazzyItems.ingredient(IngredientId.GARLIC).get())
                && (analysis.has(JazzyItems.ingredient(IngredientId.TOMATOES).get())
                || analysis.has(JazzyItems.ingredient(IngredientId.CANNED_TOMATOES).get())
                || analysis.has(JazzyItems.ingredient(IngredientId.TOMATO_PASTE).get()))) {
            return JazzyItems.CHANA_MASALA_PREP.get();
        }
        if ((analysis.has(JazzyItems.ingredient(IngredientId.PASTA).get())
                || analysis.has(JazzyItems.ingredient(IngredientId.SPAGHETTI).get())
                || analysis.has(JazzyItems.ingredient(IngredientId.MACARONI).get()))
                && (analysis.has(JazzyItems.ingredient(IngredientId.BLACK_BEANS).get())
                || analysis.has(JazzyItems.ingredient(IngredientId.KIDNEY_BEANS).get())
                || analysis.has(JazzyItems.ingredient(IngredientId.CHICKPEAS).get()))
                && (analysis.has(JazzyItems.ingredient(IngredientId.CANNED_TOMATOES).get())
                || analysis.has(JazzyItems.ingredient(IngredientId.TOMATOES).get())
                || analysis.has(JazzyItems.ingredient(IngredientId.TOMATO_PASTE).get()))
                && (analysis.has(JazzyItems.ingredient(IngredientId.BROTH).get())
                || analysis.has(JazzyItems.ingredient(IngredientId.STOCK).get()))) {
            return JazzyItems.PASTA_E_FAGIOLI_PREP.get();
        }
        return null;
    }
}
