package com.boaat.jazzy_cookin.kitchen.sim.domain;

import java.util.function.Predicate;

import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.KitchenMethod;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.kitchen.sim.SimulationSnapshot;
import com.boaat.jazzy_cookin.kitchen.sim.recognition.DishRecognitionResult;
import com.boaat.jazzy_cookin.kitchen.sim.recognition.DishSchema;
import com.boaat.jazzy_cookin.kitchen.sim.station.StationSimulationAccess;
import com.boaat.jazzy_cookin.registry.JazzyItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
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
        DishRecognitionResult preview = DishSchema.previewMeal(matter);
        return new SimulationSnapshot(
                executionMode,
                access.simulationActive() ? 1 : 0,
                72,
                72,
                72,
                access.simulationMaxProgress() > 0 ? Math.round((access.simulationProgress() / (float) access.simulationMaxProgress()) * 100.0F) : 0,
                preview != null ? 100 : 0,
                0,
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
        Predicate<Item> filter = servingFilter(access);
        String familyHint = platedMealFamilyHint(access);
        if (familyHint != null) {
            ItemStack hinted = CompositionalSimulationSupport.recognizedMealOutput(
                    access,
                    analysis,
                    matter,
                    filter.and(item -> BuiltInRegistries.ITEM.getKey(item).getPath().contains(familyHint))
            );
            if (!hinted.isEmpty()) {
                return hinted;
            }
        }
        return CompositionalSimulationSupport.recognizedMealOutput(access, analysis, matter, filter);
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

    private static Predicate<Item> servingFilter(StationSimulationAccess access) {
        boolean cup = contains(access, JazzyItems.GLASS_CUP.get());
        boolean bowl = contains(access, JazzyItems.CERAMIC_BOWL.get());
        boolean tray = contains(access, JazzyItems.SERVING_TRAY.get()) || contains(access, JazzyItems.BAMBOO_TRAY.get()) || contains(access, JazzyItems.BASKET.get());
        if (cup) {
            return item -> {
                String id = BuiltInRegistries.ITEM.getKey(item).getPath();
                return id.contains("juice") || id.contains("smoothie");
            };
        }
        if (bowl) {
            return item -> {
                String id = BuiltInRegistries.ITEM.getKey(item).getPath();
                return id.contains("soup") || id.contains("stew") || id.contains("curry")
                        || id.contains("noodle") || id.contains("bowl") || id.contains("oatmeal")
                        || id.contains("shakshuka") || id.contains("masala") || id.contains("sabzi")
                        || id.contains("rajma") || id.contains("dal") || id.contains("paneer");
            };
        }
        if (tray) {
            return item -> {
                String id = BuiltInRegistries.ITEM.getKey(item).getPath();
                return id.contains("tray") || id.contains("basket") || id.contains("platter") || id.contains("dumpling");
            };
        }
        return item -> true;
    }

    private static boolean contains(StationSimulationAccess access, Item item) {
        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            if (access.simulationItem(slot).is(item)) {
                return true;
            }
        }
        return false;
    }

    private static String platedMealFamilyHint(StationSimulationAccess access) {
        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            ItemStack stack = access.simulationItem(slot);
            if (!CompositionalSimulationSupport.isFood(stack)) {
                continue;
            }
            String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            if (path.endsWith("_prep")) {
                return path.substring(0, path.length() - "_prep".length());
            }
            if (path.endsWith("_base")) {
                return path.substring(0, path.length() - "_base".length());
            }
        }
        return null;
    }
}
