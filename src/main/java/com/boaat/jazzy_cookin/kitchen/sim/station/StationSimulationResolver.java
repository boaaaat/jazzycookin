package com.boaat.jazzy_cookin.kitchen.sim.station;

import com.boaat.jazzy_cookin.kitchen.KitchenMethod;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.ToolProfile;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMaterialProfiles;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.kitchen.sim.SimulationSnapshot;
import com.boaat.jazzy_cookin.kitchen.sim.recognition.DishRecognitionResult;
import com.boaat.jazzy_cookin.kitchen.sim.recognition.DishSchema;
import com.boaat.jazzy_cookin.kitchen.sim.reaction.EggPanReactionSolver;
import com.boaat.jazzy_cookin.registry.JazzyItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class StationSimulationResolver {
    private StationSimulationResolver() {
    }

    public static SimulationExecutionMode executionMode(StationSimulationAccess access) {
        if (access.simulationStationType() == StationType.PLATING_STATION) {
            return SimulationExecutionMode.PLATE;
        }
        if (supportsEggMixing(access) || supportsEggStove(access)) {
            return SimulationExecutionMode.SIMULATION;
        }
        return SimulationExecutionMode.LEGACY_RECIPE;
    }

    public static KitchenMethod currentMethod(StationSimulationAccess access) {
        if (executionMode(access) != SimulationExecutionMode.SIMULATION) {
            return KitchenMethod.NONE;
        }
        return switch (access.simulationStationType()) {
            case MIXING_BOWL -> KitchenMethod.WHISK;
            case STOVE -> KitchenMethod.PAN_FRY;
            default -> KitchenMethod.NONE;
        };
    }

    public static SimulationSnapshot currentSnapshot(StationSimulationAccess access) {
        int mode = executionMode(access).ordinal();
        if (executionMode(access) != SimulationExecutionMode.SIMULATION) {
            return SimulationSnapshot.inactive(mode);
        }

        if (access.simulationStationType() == StationType.STOVE) {
            FoodMatterData matter = access.simulationBatch() != null ? access.simulationBatch().matter() : previewInputMatter(access);
            if (matter == null) {
                return new SimulationSnapshot(mode, 0, EggPanReactionSolver.toF(access.simulationStationPhysics().panTemperatureC()), 72, 72, 0, 0, 0, 0, 0, 0, 0);
            }
            DishRecognitionResult preview = access.simulationBatch() != null ? DishSchema.preview(matter) : null;
            return new SimulationSnapshot(
                    mode,
                    access.simulationBatch() != null ? 1 : 0,
                    EggPanReactionSolver.toF(access.simulationStationPhysics().panTemperatureC()),
                    EggPanReactionSolver.toF(matter.coreTempC()),
                    EggPanReactionSolver.toF(matter.surfaceTempC()),
                    Math.round(matter.proteinSet() * 100.0F),
                    Math.round(matter.water() * 100.0F),
                    Math.round(matter.browning() * 100.0F),
                    Math.round(matter.charLevel() * 100.0F),
                    Math.round(matter.aeration() * 100.0F),
                    Math.round(matter.fragmentation() * 100.0F),
                    preview != null ? preview.previewId() : 0
            );
        }

        ItemStack output = access.simulationItem(access.outputSlot());
        long gameTime = access.simulationLevel() != null ? access.simulationLevel().getGameTime() : 0L;
        FoodMatterData matter = output.is(JazzyItems.EGG_MIXTURE.get())
                ? com.boaat.jazzy_cookin.kitchen.KitchenStackUtil.getOrCreateFoodMatter(output, gameTime)
                : null;
        if (matter == null) {
            return SimulationSnapshot.inactive(mode);
        }

        return new SimulationSnapshot(
                mode,
                0,
                72,
                EggPanReactionSolver.toF(matter.coreTempC()),
                EggPanReactionSolver.toF(matter.surfaceTempC()),
                Math.round(matter.whiskWork() * 50.0F),
                Math.round(matter.water() * 100.0F),
                Math.round(matter.browning() * 100.0F),
                Math.round(matter.charLevel() * 100.0F),
                Math.round(matter.aeration() * 100.0F),
                Math.round(matter.fragmentation() * 100.0F),
                0
        );
    }

    public static boolean supportsEggMixing(StationSimulationAccess access) {
        if (access.simulationStationType() != StationType.MIXING_BOWL) {
            return false;
        }

        ItemStack output = access.simulationItem(access.outputSlot());
        if (output.is(JazzyItems.EGG_MIXTURE.get())) {
            for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
                ItemStack stack = access.simulationItem(slot);
                if (!stack.isEmpty() && !FoodMaterialProfiles.isMixingBowlAddIn(stack)) {
                    return false;
                }
            }
            return true;
        }

        boolean sawEgg = false;
        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            ItemStack stack = access.simulationItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(JazzyItems.ingredient(JazzyItems.IngredientId.EGGS).get())) {
                sawEgg = true;
                continue;
            }
            if (!FoodMaterialProfiles.isMixingBowlAddIn(stack)) {
                return false;
            }
        }
        return sawEgg && access.simulationItem(access.outputSlot()).isEmpty();
    }

    public static boolean supportsEggStove(StationSimulationAccess access) {
        if (access.simulationStationType() != StationType.STOVE) {
            return false;
        }
        if (access.simulationBatch() != null) {
            return true;
        }

        ToolProfile toolProfile = ToolProfile.fromStack(access.simulationItem(access.toolSlot()));
        if (toolProfile != ToolProfile.PAN && toolProfile != ToolProfile.FRYING_SKILLET) {
            return false;
        }

        boolean sawMixture = false;
        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            ItemStack stack = access.simulationItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(JazzyItems.EGG_MIXTURE.get())) {
                sawMixture = true;
                continue;
            }
            if (!FoodMaterialProfiles.isStoveFat(stack)) {
                return false;
            }
        }
        return sawMixture;
    }

    public static FoodMatterData previewInputMatter(StationSimulationAccess access) {
        if (access.simulationLevel() == null) {
            return null;
        }
        int mixtureSlot = firstInputSlotMatching(access, JazzyItems.EGG_MIXTURE.get());
        if (mixtureSlot < 0) {
            return null;
        }
        return com.boaat.jazzy_cookin.kitchen.KitchenStackUtil.getOrCreateFoodMatter(
                access.simulationItem(mixtureSlot),
                access.simulationLevel().getGameTime()
        );
    }

    public static int firstInputSlotMatching(StationSimulationAccess access, Item item) {
        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            if (access.simulationItem(slot).is(item)) {
                return slot;
            }
        }
        return -1;
    }

    public static int firstStoveFatSlot(StationSimulationAccess access) {
        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            if (FoodMaterialProfiles.isStoveFat(access.simulationItem(slot))) {
                return slot;
            }
        }
        return -1;
    }
}
