package com.boaat.jazzy_cookin.kitchen.sim.domain;

import com.boaat.jazzy_cookin.item.KitchenIngredientItem;
import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.KitchenMethod;
import com.boaat.jazzy_cookin.kitchen.KitchenStackUtil;
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
import com.boaat.jazzy_cookin.registry.JazzyItems.IngredientId;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public final class HeatChamberSimulationDomain implements StationSimulationDomain {
    @Override
    public SimulationDomainType type() {
        return SimulationDomainType.OVEN;
    }

    @Override
    public boolean supports(StationSimulationAccess access) {
        return switch (access.simulationStationType()) {
            case OVEN, MICROWAVE, SMOKER, STEAMER -> access.simulationActive() || CompositionalSimulationSupport.hasFoodInputs(access);
            default -> false;
        };
    }

    @Override
    public KitchenMethod method(StationSimulationAccess access) {
        return switch (access.simulationStationType()) {
            case MICROWAVE -> KitchenMethod.MICROWAVE;
            case SMOKER -> KitchenMethod.SMOKE;
            case STEAMER -> KitchenMethod.STEAM;
            case OVEN -> inferOvenMethod(access);
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
        int schemaPreviewId = CompositionalSimulationSupport.schemaPreviewId(matter, HeatChamberSimulationDomain::isHeatSchema);
        int chamberTempF = access.simulationStationType() == StationType.OVEN
                ? Math.max(72, Math.round(Math.max(200, access.simulationTargetTemperatureF()) * (access.simulationPreheatProgress() / 100.0F)))
                : Math.round(Mth.clamp(CompositionalSimulationSupport.targetTempC(access.simulationHeatLevel()) * 1.8F + 32.0F, 72.0F, 500.0F));
        return new SimulationSnapshot(
                executionMode,
                access.simulationActive() ? 1 : 0,
                chamberTempF,
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
        if (access.simulationStationType() == StationType.OVEN && access.simulationPreheatProgress() < 100) {
            return false;
        }
        ItemStack preview = previewOutput(access);
        if (preview.isEmpty() || !access.simulationCanAcceptStack(access.outputSlot(), preview)) {
            return false;
        }
        int duration = switch (access.simulationStationType()) {
            case MICROWAVE -> access.simulationMicrowaveDurationSeconds() * 20;
            case SMOKER -> 160;
            case STEAMER -> 120;
            case OVEN -> access.simulationOvenCookTimeTicks() > 0
                    ? access.simulationOvenCookTimeTicks()
                    : 140;
            default -> 140;
        };
        access.simulationSetBatch(new CookingBatchState(transformedMatter(
                access,
                SimulationIngredientAnalysis.analyzeInputs(access),
                0.0F
        )));
        access.simulationSetProgress(0, CompositionalSimulationSupport.timedDuration(access, duration), true);
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

    @Override
    public int environmentStatus(StationSimulationAccess access) {
        if (access.simulationStationType() == StationType.OVEN && access.simulationPreheatProgress() < 100) {
            return 0;
        }
        return previewOutput(access).isEmpty() ? 2 : 1;
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
        ItemStack schemaOutput = CompositionalSimulationSupport.recognizedSchemaOutput(access, analysis, matter, HeatChamberSimulationDomain::isHeatSchema);
        if (!schemaOutput.isEmpty()) {
            return access.simulationStationType() == StationType.MICROWAVE ? applyMicrowavePenalty(schemaOutput, access.simulationLevel().getGameTime()) : schemaOutput;
        }
        ItemStack output = CompositionalSimulationSupport.recognizedPreparedOutput(access, analysis, matter);
        return access.simulationStationType() == StationType.MICROWAVE ? applyMicrowavePenalty(output, access.simulationLevel().getGameTime()) : output;
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
                : 0.88F;
        return transformedMatter(access, analysis, completion);
    }

    private static FoodMatterData transformedMatter(StationSimulationAccess access, SimulationIngredientAnalysis analysis, float completion) {
        IngredientState state = inferState(access, analysis);
        float browning = switch (access.simulationStationType()) {
            case MICROWAVE -> 0.02F;
            case STEAMER -> 0.04F;
            case SMOKER -> 0.22F + completion * 0.16F;
            default -> 0.12F + completion * 0.22F;
        };
        float charLevel = access.simulationStationType() == StationType.OVEN
                && access.simulationHeatLevel() == com.boaat.jazzy_cookin.kitchen.HeatLevel.HIGH
                && completion > 0.94F ? 0.12F : 0.0F;
        float waterBias = switch (access.simulationStationType()) {
            case STEAMER -> 0.10F;
            case MICROWAVE -> -0.06F;
            case SMOKER -> -0.10F;
            default -> -0.14F;
        };
        return CompositionalSimulationSupport.composeMatter(
                access,
                analysis,
                state,
                false,
                completion,
                waterBias,
                0.02F,
                access.simulationStationType() == StationType.STEAMER ? 0.26F : 0.20F,
                access.simulationStationType() == StationType.OVEN ? 0.56F : 0.44F,
                0.34F + completion * 0.34F,
                browning,
                charLevel
        );
    }

    private static boolean isHeatSchema(DishSchemaDefinition schema) {
        return !schema.meal() && schema.requiredTechniques().contains(DishTechnique.BAKED);
    }

    private static KitchenMethod inferOvenMethod(StationSimulationAccess access) {
        SimulationIngredientAnalysis analysis = SimulationIngredientAnalysis.analyzeInputs(access);
        if (analysis.has(JazzyItems.POTATO_GRATIN_PREP.get())
                || analysis.has(JazzyItems.ASSEMBLED_FOCACCIA_PIZZA.get())
                || analysis.has(JazzyItems.ASSEMBLED_TRAY_PIE.get())
                || analysis.has(JazzyItems.ASSEMBLED_SAVORY_PIE.get())
                || analysis.has(JazzyItems.CAKE_BATTER.get())
                || analysis.has(JazzyItems.BROWNIE_BATTER.get())
                || analysis.has(JazzyItems.GARLIC_BREAD_PREP.get())) {
            return KitchenMethod.BAKE;
        }
        return access.simulationHeatLevel() == com.boaat.jazzy_cookin.kitchen.HeatLevel.HIGH ? KitchenMethod.ROAST : KitchenMethod.BAKE;
    }

    private static IngredientState inferState(StationSimulationAccess access, SimulationIngredientAnalysis analysis) {
        return switch (access.simulationStationType()) {
            case MICROWAVE -> IngredientState.SIMMERED;
            case SMOKER -> IngredientState.SMOKED;
            case STEAMER -> analysis.has(JazzyItems.RAW_DUMPLINGS.get()) ? IngredientState.STEAMED_DUMPLINGS : IngredientState.STEAMED;
            case OVEN -> {
                if (analysis.has(JazzyItems.SHAPED_FOCACCIA_BASE.get()) || analysis.has(JazzyItems.ASSEMBLED_FOCACCIA_PIZZA.get())) {
                    yield IngredientState.BAKED_BREAD;
                }
                if (analysis.has(JazzyItems.ASSEMBLED_TRAY_PIE.get()) || analysis.has(JazzyItems.ASSEMBLED_SAVORY_PIE.get())) {
                    yield IngredientState.BAKED_PIE;
                }
                if (analysis.has(JazzyItems.CAKE_BATTER.get()) || analysis.has(JazzyItems.BROWNIE_BATTER.get())) {
                    yield IngredientState.BAKED;
                }
                if (analysis.has(JazzyItems.ingredient(IngredientId.POTATOES).get())) {
                    yield IngredientState.ROASTED;
                }
                yield IngredientState.BAKED;
            }
            default -> IngredientState.BAKED;
        };
    }
    private static ItemStack applyMicrowavePenalty(ItemStack output, long gameTime) {
        if (output.isEmpty()) {
            return output;
        }
        KitchenStackUtil.mutateFoodMatter(output, gameTime, matter -> matter.withPreservationState(
                matter.preservationLevel(),
                Mth.clamp(matter.oxidation() + 0.08F, 0.0F, 1.0F),
                Mth.clamp(matter.microbialLoad() + 0.05F, 0.0F, 1.0F)
        ).withWorkingState(
                Mth.clamp(matter.water() - 0.04F, 0.0F, 1.0F),
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
                matter.processDepth(),
                matter.finalizedServing()
        ));
        return output;
    }
}
