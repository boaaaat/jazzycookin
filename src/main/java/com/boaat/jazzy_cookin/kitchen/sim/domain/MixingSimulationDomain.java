package com.boaat.jazzy_cookin.kitchen.sim.domain;

import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.KitchenMethod;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.sim.CookingBatchState;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.kitchen.sim.FoodTrait;
import com.boaat.jazzy_cookin.kitchen.sim.SimulationSnapshot;
import com.boaat.jazzy_cookin.kitchen.sim.schema.DishTechnique;
import com.boaat.jazzy_cookin.kitchen.sim.station.StationSimulationAccess;
import com.boaat.jazzy_cookin.registry.JazzyItems;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public final class MixingSimulationDomain implements StationSimulationDomain {
    @Override
    public SimulationDomainType type() {
        return SimulationDomainType.MIX;
    }

    @Override
    public boolean supports(StationSimulationAccess access) {
        return access.simulationStationType() == StationType.MIXING_BOWL
                && (access.simulationActive() || !previewOutput(access).isEmpty());
    }

    @Override
    public KitchenMethod method(StationSimulationAccess access) {
        return inferredMethod(SimulationIngredientAnalysis.analyzeInputs(access), access.simulationControlSetting());
    }

    @Override
    public SimulationSnapshot snapshot(StationSimulationAccess access, int executionMode) {
        FoodMatterData matter = previewMatter(access);
        if (matter == null) {
            return SimulationSnapshot.inactive(executionMode);
        }
        return new SimulationSnapshot(
                executionMode,
                access.simulationActive() ? 1 : 0,
                72,
                72,
                72,
                access.simulationMaxProgress() > 0 ? Math.round((access.simulationProgress() / (float) access.simulationMaxProgress()) * 100.0F) : Math.round(matter.whiskWork() * 50.0F),
                Math.round(matter.water() * 100.0F),
                Math.round(matter.browning() * 100.0F),
                Math.round(matter.charLevel() * 100.0F),
                Math.round(matter.aeration() * 100.0F),
                Math.round(matter.fragmentation() * 100.0F),
                CompositionalSimulationSupport.schemaPreviewId(matter, MixingSimulationDomain::isMixSchema)
        );
    }

    @Override
    public boolean handleAction(StationSimulationAccess access, int buttonId) {
        if (buttonId != 6 || access.simulationLevel() == null || access.simulationActive()) {
            return false;
        }
        ItemStack output = previewOutput(access);
        if (output.isEmpty() || !access.simulationCanAcceptStack(access.outputSlot(), output)) {
            return false;
        }
        access.simulationSetBatch(new CookingBatchState(previewMatter(access)));
        access.simulationSetProgress(0, CompositionalSimulationSupport.timedDuration(access, 72), true);
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
        int nextProgress = access.simulationProgress() + 1;
        if (nextProgress >= access.simulationMaxProgress()) {
            finish(access);
            return;
        }
        access.simulationSetProgress(nextProgress, access.simulationMaxProgress(), true);
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
        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            ItemStack stack = access.simulationItem(slot);
            if (!stack.isEmpty()) {
                access.simulationRemoveItem(slot, stack.getCount());
            }
        }
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
        ItemStack schemaOutput = CompositionalSimulationSupport.recognizedSchemaOutput(access, analysis, matter, MixingSimulationDomain::isMixSchema);
        return schemaOutput.isEmpty() ? CompositionalSimulationSupport.recognizedPreparedOutput(access, analysis, matter) : schemaOutput;
    }

    private static FoodMatterData previewMatter(StationSimulationAccess access) {
        if (access.simulationLevel() == null) {
            return null;
        }
        SimulationIngredientAnalysis analysis = SimulationIngredientAnalysis.analyzeInputs(access);
        if (analysis.isEmpty()) {
            return null;
        }
        KitchenMethod method = inferredMethod(analysis, access.simulationControlSetting());
        float controlFactor = switch (access.simulationControlSetting()) {
            case 0 -> 0.18F;
            case 2 -> 0.44F;
            default -> 0.30F;
        };
        return CompositionalSimulationSupport.composeMatter(
                access,
                analysis,
                stateFor(method),
                false,
                access.simulationActive() && access.simulationMaxProgress() > 0
                        ? (access.simulationProgress() + 1) / (float) access.simulationMaxProgress()
                        : 0.82F,
                0.02F,
                method == KitchenMethod.KNEAD ? 0.04F + controlFactor * 0.08F : 0.08F + controlFactor * 0.18F,
                method == KitchenMethod.KNEAD ? 0.06F + (1.0F - controlFactor) * 0.06F : 0.10F + (1.0F - controlFactor) * 0.12F,
                method == KitchenMethod.KNEAD ? 0.48F + controlFactor * 0.26F : 0.32F + controlFactor * 0.16F,
                0.0F,
                0.0F,
                0.0F
        ).withWorkingState(
                Mth.clamp(analysis.avgWater(), 0.04F, 1.0F),
                method == KitchenMethod.KNEAD ? 0.04F + controlFactor * 0.08F : 0.08F + controlFactor * 0.18F,
                method == KitchenMethod.KNEAD ? 0.06F + (1.0F - controlFactor) * 0.06F : 0.10F + (1.0F - controlFactor) * 0.12F,
                method == KitchenMethod.KNEAD ? 0.48F + controlFactor * 0.26F : 0.32F + controlFactor * 0.16F,
                0.0F,
                0.0F,
                0.0F,
                method == KitchenMethod.BATTER || method == KitchenMethod.WHISK ? controlFactor : 0.0F,
                0,
                0,
                0,
                1,
                false
        );
    }

    private static KitchenMethod inferredMethod(SimulationIngredientAnalysis analysis, int controlSetting) {
        if (analysis.has(JazzyItems.BATTER_MIX.get()) && analysis.hasTrait(FoodTrait.BREAD)) {
            return KitchenMethod.BATTER;
        }
        if (analysis.hasTrait(FoodTrait.FLOUR)) {
            return controlSetting == 2 ? KitchenMethod.KNEAD : KitchenMethod.MIX;
        }
        if (analysis.hasTrait(FoodTrait.EGG) || analysis.hasTrait(FoodTrait.DAIRY)) {
            return KitchenMethod.WHISK;
        }
        return KitchenMethod.MIX;
    }

    private static IngredientState stateFor(KitchenMethod method) {
        return switch (method) {
            case KNEAD -> IngredientState.DOUGH;
            case BATTER -> IngredientState.BATTER;
            case WHISK -> IngredientState.WHISKED;
            default -> IngredientState.MIXED;
        };
    }

    private static boolean isMixSchema(com.boaat.jazzy_cookin.kitchen.sim.schema.DishSchemaDefinition schema) {
        return !schema.meal() && (schema.requiredTechniques().contains(DishTechnique.MIXED)
                || schema.requiredTechniques().contains(DishTechnique.DIP_OR_COAT));
    }
}
