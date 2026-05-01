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
import com.boaat.jazzy_cookin.kitchen.sim.station.StationSimulationAccess;
import com.boaat.jazzy_cookin.registry.JazzyItems;
import com.boaat.jazzy_cookin.registry.JazzyItems.IngredientId;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class PrepSimulationDomain implements StationSimulationDomain {
    @Override
    public SimulationDomainType type() {
        return SimulationDomainType.PREP;
    }

    @Override
    public boolean supports(StationSimulationAccess access) {
        return switch (access.simulationStationType()) {
            case PREP_TABLE, SPICE_GRINDER, STRAINER -> access.simulationActive() || CompositionalSimulationSupport.hasFoodInputs(access);
            default -> false;
        };
    }

    @Override
    public KitchenMethod method(StationSimulationAccess access) {
        return switch (access.simulationStationType()) {
            case SPICE_GRINDER -> KitchenMethod.GRIND;
            case STRAINER -> KitchenMethod.STRAIN;
            case PREP_TABLE -> inferPrepMethod(access);
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
                preview != null ? 100 : 0,
                access.simulationControlSetting() * 50,
                0,
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
        access.simulationSetBatch(new CookingBatchState(previewMatter(access)));
        access.simulationSetProgress(0, CompositionalSimulationSupport.timedDuration(access, 48), true);
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
            access.simulationSetProgress(0, 0, false);
            access.simulationSetBatch(null);
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

    @Override
    public int environmentStatus(StationSimulationAccess access) {
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

        int primarySlot = dominantPrimarySlot(access);
        if (primarySlot >= 0) {
            access.simulationRemoveItem(primarySlot, 1);
        } else {
            CompositionalSimulationSupport.removeAllFoodInputs(access);
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
        if (analysis.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int primarySlot = dominantPrimarySlot(access);
        FoodMatterData matter = previewMatter(access);
        if (matter == null) {
            return ItemStack.EMPTY;
        }

        ItemStack schemaOutput = CompositionalSimulationSupport.recognizedSchemaOutput(access, analysis, matter, schema ->
                !schema.meal() && (schema.requiredTechniques().contains(com.boaat.jazzy_cookin.kitchen.sim.schema.DishTechnique.CUT)
                        || schema.requiredTechniques().contains(com.boaat.jazzy_cookin.kitchen.sim.schema.DishTechnique.PREPPED)));
        if (!schemaOutput.isEmpty()) {
            return schemaOutput;
        }

        if (primarySlot >= 0 && access.simulationItem(primarySlot).getItem() instanceof KitchenIngredientItem ingredientItem) {
            return SimulationOutputFactory.createOutput(ingredientItem, access.simulationLevel().getGameTime(), analysis, matter);
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
        IngredientState state = inferPrepState(access, dominantPrimaryStack(access));
        return CompositionalSimulationSupport.composeMatter(
                access,
                analysis,
                state,
                false,
                access.simulationActive() && access.simulationMaxProgress() > 0
                        ? (access.simulationProgress() + 1) / (float) access.simulationMaxProgress()
                        : 0.85F,
                -0.04F,
                access.simulationStationType() == StationType.SPICE_GRINDER ? 0.06F : 0.02F,
                access.simulationStationType() == StationType.SPICE_GRINDER ? 0.62F : access.simulationControlSetting() == 2 ? 0.55F : 0.34F,
                access.simulationStationType() == StationType.STRAINER ? 0.24F : 0.36F,
                0.0F,
                0.0F,
                0.0F
        );
    }

    private static KitchenMethod inferPrepMethod(StationSimulationAccess access) {
        ItemStack primary = dominantPrimaryStack(access);
        if (primary.is(JazzyItems.ingredient(IngredientId.BREAD).get())) {
            return KitchenMethod.SLICE;
        }
        return KitchenMethod.CUT;
    }

    private static IngredientState inferPrepState(StationSimulationAccess access, ItemStack primary) {
        if (access.simulationStationType() == StationType.SPICE_GRINDER) {
            return primary.is(JazzyItems.ingredient(IngredientId.BASIL).get())
                    || primary.is(JazzyItems.ingredient(IngredientId.PARSLEY).get())
                    || primary.is(JazzyItems.ingredient(IngredientId.THYME).get())
                    ? IngredientState.GROUND_HERB
                    : IngredientState.GROUND_SPICE;
        }
        if (access.simulationStationType() == StationType.STRAINER) {
            return primary.is(JazzyItems.TOMATO_SOUP_BASE.get()) ? IngredientState.STRAINED_SOUP : IngredientState.STRAINED;
        }
        if (primary.is(JazzyItems.ingredient(IngredientId.APPLES).get())) {
            return IngredientState.SLICED_APPLE;
        }
        if (primary.is(JazzyItems.ingredient(IngredientId.LEMONS).get())) {
            return IngredientState.SLICED;
        }
        if (primary.is(JazzyItems.ingredient(IngredientId.TOMATOES).get())) {
            return IngredientState.CHOPPED_TOMATO;
        }
        if (primary.is(JazzyItems.ingredient(IngredientId.CABBAGE).get())) {
            return IngredientState.CHOPPED_CABBAGE;
        }
        if (primary.is(JazzyItems.ingredient(IngredientId.ONIONS).get()) || primary.is(JazzyItems.ingredient(IngredientId.SHALLOTS).get())) {
            return IngredientState.DICED_ONION;
        }
        if (primary.is(JazzyItems.ingredient(IngredientId.BASIL).get())
                || primary.is(JazzyItems.ingredient(IngredientId.PARSLEY).get())
                || primary.is(JazzyItems.ingredient(IngredientId.THYME).get())
                || primary.is(JazzyItems.ingredient(IngredientId.OREGANO).get())) {
            return IngredientState.CHOPPED_HERB;
        }
        if (primary.is(JazzyItems.ingredient(IngredientId.FISH_FILLET).get())) {
            return IngredientState.CLEANED_FISH;
        }
        if (primary.is(JazzyItems.ingredient(IngredientId.BREAD).get())) {
            return IngredientState.SLICED_BREAD;
        }
        if (primary.is(JazzyItems.ingredient(IngredientId.JALAPENOS).get()) && access.activeInputCount() > 1) {
            return IngredientState.STUFFED;
        }
        if (contains(access, JazzyItems.FOCACCIA_DOUGH.get())) {
            return IngredientState.SHAPED_BASE;
        }
        if (contains(access, JazzyItems.PIE_DOUGH.get())) {
            return IngredientState.RAW_ASSEMBLED_PIE;
        }
        if (contains(access, JazzyItems.SHAPED_FOCACCIA_BASE.get())) {
            return IngredientState.RAW_ASSEMBLED_PIZZA;
        }
        if (significantFoodInputCount(access) > 1) {
            return switch (access.simulationControlSetting()) {
                case 2 -> IngredientState.DICED;
                default -> IngredientState.ROUGH_CUT;
            };
        }
        return switch (access.simulationControlSetting()) {
            case 0 -> IngredientState.SLICED;
            case 2 -> IngredientState.MINCED;
            default -> IngredientState.CHOPPED;
        };
    }

    private static boolean contains(StationSimulationAccess access, Item item) {
        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            if (access.simulationItem(slot).is(item)) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack dominantPrimaryStack(StationSimulationAccess access) {
        int slot = dominantPrimarySlot(access);
        return slot >= 0 ? access.simulationItem(slot) : CompositionalSimulationSupport.dominantFoodInput(access);
    }

    private static int dominantPrimarySlot(StationSimulationAccess access) {
        int primarySlot = -1;
        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            ItemStack stack = access.simulationItem(slot);
            if (!CompositionalSimulationSupport.isFood(stack)) {
                continue;
            }
            if (!CompositionalSimulationSupport.isSupportiveExtra(stack)) {
                if (primarySlot >= 0) {
                    return -1;
                }
                primarySlot = slot;
            }
        }
        return primarySlot;
    }

    private static int significantFoodInputCount(StationSimulationAccess access) {
        int count = 0;
        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            ItemStack stack = access.simulationItem(slot);
            if (CompositionalSimulationSupport.isFood(stack) && !CompositionalSimulationSupport.isSupportiveExtra(stack)) {
                count++;
            }
        }
        return count;
    }
}
