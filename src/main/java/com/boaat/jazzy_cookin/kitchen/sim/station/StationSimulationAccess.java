package com.boaat.jazzy_cookin.kitchen.sim.station;

import com.boaat.jazzy_cookin.kitchen.HeatLevel;
import com.boaat.jazzy_cookin.kitchen.StationCapacityProfile;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.sim.CookingBatchState;
import com.boaat.jazzy_cookin.kitchen.sim.StationPhysicsState;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface StationSimulationAccess {
    Level simulationLevel();

    StationType simulationStationType();

    HeatLevel simulationHeatLevel();

    int simulationControlSetting();

    int simulationPreheatProgress();

    int simulationProgress();

    int simulationMaxProgress();

    boolean simulationActive();

    StationCapacityProfile simulationCapacity();

    int inputStart();

    int inputEnd();

    int toolSlot();

    int outputSlot();

    int byproductSlot();

    default int activeInputCount() {
        return this.simulationCapacity().inputCount();
    }

    ItemStack simulationItem(int slot);

    ItemStack simulationRemoveItem(int slot, int amount);

    void simulationSetItem(int slot, ItemStack stack);

    boolean simulationCanAcceptStack(int slot, ItemStack stack);

    void simulationMergeIntoSlot(int slot, ItemStack stack);

    StationPhysicsState simulationStationPhysics();

    void simulationSetStationPhysics(StationPhysicsState state);

    CookingBatchState simulationBatch();

    void simulationSetBatch(CookingBatchState batch);

    void simulationSetProgress(int progress, int maxProgress, boolean active);

    void simulationMarkChanged();
}
