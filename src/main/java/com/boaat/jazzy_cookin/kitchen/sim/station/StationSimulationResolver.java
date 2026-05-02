package com.boaat.jazzy_cookin.kitchen.sim.station;

import java.util.List;

import com.boaat.jazzy_cookin.kitchen.KitchenMethod;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMaterialProfiles;
import com.boaat.jazzy_cookin.kitchen.sim.SimulationSnapshot;
import com.boaat.jazzy_cookin.kitchen.sim.domain.BlenderSimulationDomain;
import com.boaat.jazzy_cookin.kitchen.sim.domain.FoodProcessorSimulationDomain;
import com.boaat.jazzy_cookin.kitchen.sim.domain.FreezeDrySimulationDomain;
import com.boaat.jazzy_cookin.kitchen.sim.domain.HeatChamberSimulationDomain;
import com.boaat.jazzy_cookin.kitchen.sim.domain.JuicerSimulationDomain;
import com.boaat.jazzy_cookin.kitchen.sim.domain.MeasuringSimulationDomain;
import com.boaat.jazzy_cookin.kitchen.sim.domain.MixingSimulationDomain;
import com.boaat.jazzy_cookin.kitchen.sim.domain.PanSimulationDomain;
import com.boaat.jazzy_cookin.kitchen.sim.domain.PlateAssemblySimulationDomain;
import com.boaat.jazzy_cookin.kitchen.sim.domain.PotSimulationDomain;
import com.boaat.jazzy_cookin.kitchen.sim.domain.PrepSimulationDomain;
import com.boaat.jazzy_cookin.kitchen.sim.domain.PreserveSimulationDomain;
import com.boaat.jazzy_cookin.kitchen.sim.domain.RestSimulationDomain;
import com.boaat.jazzy_cookin.kitchen.sim.domain.StationSimulationDomain;
import com.boaat.jazzy_cookin.registry.JazzyItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class StationSimulationResolver {
    private static final List<StationSimulationDomain> DOMAINS = List.of(
            new MeasuringSimulationDomain(),
            new PanSimulationDomain(),
            new MixingSimulationDomain(),
            new PrepSimulationDomain(),
            new PotSimulationDomain(),
            new HeatChamberSimulationDomain(),
            new PreserveSimulationDomain(),
            new RestSimulationDomain(),
            new FoodProcessorSimulationDomain(),
            new BlenderSimulationDomain(),
            new JuicerSimulationDomain(),
            new FreezeDrySimulationDomain(),
            new PlateAssemblySimulationDomain()
    );

    private StationSimulationResolver() {
    }

    public static SimulationExecutionMode executionMode(StationSimulationAccess access) {
        return domainFor(access) != null ? SimulationExecutionMode.SIMULATION : SimulationExecutionMode.NONE;
    }

    public static KitchenMethod currentMethod(StationSimulationAccess access) {
        StationSimulationDomain domain = domainFor(access);
        return domain != null ? domain.method(access) : KitchenMethod.NONE;
    }

    public static SimulationSnapshot currentSnapshot(StationSimulationAccess access) {
        int mode = executionMode(access).ordinal();
        StationSimulationDomain domain = domainFor(access);
        return domain != null ? domain.snapshot(access, mode) : SimulationSnapshot.inactive(mode);
    }

    public static boolean handleAction(StationSimulationAccess access, int buttonId) {
        StationSimulationDomain domain = domainFor(access);
        return domain != null && domain.handleAction(access, buttonId);
    }

    public static void serverTick(StationSimulationAccess access) {
        StationSimulationDomain domain = domainFor(access);
        if (domain != null) {
            domain.serverTick(access);
        }
    }

    public static int environmentStatus(StationSimulationAccess access) {
        StationSimulationDomain domain = domainFor(access);
        return domain != null ? domain.environmentStatus(access) : 2;
    }

    public static StationSimulationDomain domainFor(StationSimulationAccess access) {
        for (StationSimulationDomain domain : DOMAINS) {
            if (domain.supports(access)) {
                return domain;
            }
        }
        return null;
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

    public static int firstInputSlotMatching(StationSimulationAccess access, Item item) {
        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            if (access.simulationItem(slot).is(item)) {
                return slot;
            }
        }
        return -1;
    }

    public static int firstInputSlotMatching(StationSimulationAccess access, java.util.function.Predicate<ItemStack> predicate) {
        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            if (predicate.test(access.simulationItem(slot))) {
                return slot;
            }
        }
        return -1;
    }
}
