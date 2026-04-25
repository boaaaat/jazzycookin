package com.boaat.jazzy_cookin.kitchen.sim.domain;

import java.util.Optional;

import com.boaat.jazzy_cookin.kitchen.KitchenMethod;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.sim.SimulationSnapshot;
import com.boaat.jazzy_cookin.kitchen.sim.schema.DishSchemaScore;
import com.boaat.jazzy_cookin.kitchen.sim.station.StationSimulationAccess;
import com.boaat.jazzy_cookin.recipe.KitchenPlateRecipe;

import net.minecraft.world.item.ItemStack;

public final class PlateSimulationDomain implements StationSimulationDomain {
    @Override
    public SimulationDomainType type() {
        return SimulationDomainType.PLATE;
    }

    @Override
    public boolean supports(StationSimulationAccess access) {
        return access.simulationStationType() == StationType.PLATING_STATION
                && (access.simulationActive() || RecipeSimulationSupport.hasPlateCandidate(access));
    }

    @Override
    public KitchenMethod method(StationSimulationAccess access) {
        return RecipeSimulationSupport.hasPlateCandidate(access) ? KitchenMethod.PLATE : KitchenMethod.NONE;
    }

    @Override
    public SimulationSnapshot snapshot(StationSimulationAccess access, int executionMode) {
        Optional<KitchenPlateRecipe> recipe = RecipeSimulationSupport.currentPlateRecipe(access);
        Optional<DishSchemaScore> schema = recipe.isEmpty() ? RecipeSimulationSupport.currentPlateSchema(access) : Optional.empty();
        if (recipe.isEmpty() && schema.isEmpty() && !access.simulationActive()) {
            return SimulationSnapshot.inactive(executionMode);
        }

        int progressPercent = access.simulationMaxProgress() > 0
                ? Math.round((access.simulationProgress() / (float) access.simulationMaxProgress()) * 100.0F)
                : 0;
        ItemStack previewStack = recipe.map(value -> RecipeSimulationSupport.previewPlateStack(access, value))
                .orElseGet(() -> schema.map(value -> RecipeSimulationSupport.previewPlateSchemaStack(access, value)).orElse(ItemStack.EMPTY));
        int recognizerId = access.simulationLevel() != null
                ? RecipeSimulationSupport.previewRecognizerId(previewStack, access.simulationLevel().getGameTime())
                : 0;
        return new SimulationSnapshot(
                executionMode,
                access.simulationActive() ? 1 : 0,
                72,
                72,
                72,
                progressPercent,
                recipe.isPresent() || schema.isPresent() ? 100 : 0,
                0,
                0,
                0,
                0,
                recognizerId
        );
    }

    @Override
    public boolean handleAction(StationSimulationAccess access, int buttonId) {
        return buttonId == 6 && !access.simulationActive() && RecipeSimulationSupport.startPlate(access);
    }

    @Override
    public void serverTick(StationSimulationAccess access) {
        RecipeSimulationSupport.serverTickPlate(access);
    }

    @Override
    public int environmentStatus(StationSimulationAccess access) {
        return RecipeSimulationSupport.environmentStatus(access);
    }
}
