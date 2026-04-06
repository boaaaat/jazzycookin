package com.boaat.jazzy_cookin.kitchen.sim.domain;

import java.util.Optional;

import com.boaat.jazzy_cookin.kitchen.KitchenMethod;
import com.boaat.jazzy_cookin.kitchen.sim.SimulationSnapshot;
import com.boaat.jazzy_cookin.kitchen.sim.station.StationSimulationAccess;
import com.boaat.jazzy_cookin.recipe.KitchenProcessRecipe;

import net.minecraft.world.item.ItemStack;

public final class ProcessRecipeSimulationDomain implements StationSimulationDomain {
    @Override
    public SimulationDomainType type() {
        return SimulationDomainType.NONE;
    }

    @Override
    public boolean supports(StationSimulationAccess access) {
        return access.simulationStationType() != com.boaat.jazzy_cookin.kitchen.StationType.PLATING_STATION
                && (access.simulationActive() || RecipeSimulationSupport.currentProcessRecipe(access).isPresent());
    }

    @Override
    public KitchenMethod method(StationSimulationAccess access) {
        return RecipeSimulationSupport.currentProcessRecipe(access).map(KitchenProcessRecipe::method).orElse(KitchenMethod.NONE);
    }

    @Override
    public SimulationSnapshot snapshot(StationSimulationAccess access, int executionMode) {
        Optional<KitchenProcessRecipe> recipe = RecipeSimulationSupport.currentProcessRecipe(access);
        if (recipe.isEmpty() && !access.simulationActive()) {
            return SimulationSnapshot.inactive(executionMode);
        }

        int progressPercent = access.simulationMaxProgress() > 0
                ? Math.round((access.simulationProgress() / (float) access.simulationMaxProgress()) * 100.0F)
                : 0;
        int readiness = RecipeSimulationSupport.environmentStatus(access) == 1 ? 100 : 0;
        ItemStack previewStack = recipe.map(value -> RecipeSimulationSupport.previewProcessStack(access, value)).orElse(ItemStack.EMPTY);
        int recognizerId = access.simulationLevel() != null
                ? RecipeSimulationSupport.previewRecognizerId(previewStack, access.simulationLevel().getGameTime())
                : 0;
        int preheat = access.simulationPreheatProgress();
        int control = access.simulationStationType().supportsStationControl()
                ? access.simulationControlSetting() * 50
                : access.simulationHeatLevel() == com.boaat.jazzy_cookin.kitchen.HeatLevel.HIGH ? 100
                : access.simulationHeatLevel() == com.boaat.jazzy_cookin.kitchen.HeatLevel.MEDIUM ? 66
                : access.simulationHeatLevel() == com.boaat.jazzy_cookin.kitchen.HeatLevel.LOW ? 33
                : 0;

        return new SimulationSnapshot(
                executionMode,
                access.simulationActive() ? 1 : 0,
                RecipeSimulationSupport.approximateStationTempF(access),
                72,
                72,
                progressPercent,
                access.simulationStationType() == com.boaat.jazzy_cookin.kitchen.StationType.OVEN ? preheat : readiness,
                control,
                0,
                0,
                0,
                recognizerId
        );
    }

    @Override
    public boolean handleAction(StationSimulationAccess access, int buttonId) {
        if (buttonId != 6 || access.simulationActive()) {
            return false;
        }
        return RecipeSimulationSupport.startProcess(access);
    }

    @Override
    public void serverTick(StationSimulationAccess access) {
        RecipeSimulationSupport.serverTickProcess(access);
    }

    @Override
    public int environmentStatus(StationSimulationAccess access) {
        return RecipeSimulationSupport.environmentStatus(access);
    }
}
