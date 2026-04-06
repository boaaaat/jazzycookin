package com.boaat.jazzy_cookin.kitchen.sim.domain;

import com.boaat.jazzy_cookin.kitchen.KitchenMethod;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.ToolProfile;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.kitchen.sim.SimulationSnapshot;
import com.boaat.jazzy_cookin.kitchen.sim.action.EggStoveSimulationActions;
import com.boaat.jazzy_cookin.kitchen.sim.reaction.EggPanReactionSolver;
import com.boaat.jazzy_cookin.kitchen.sim.recognition.DishRecognitionResult;
import com.boaat.jazzy_cookin.kitchen.sim.recognition.DishSchema;
import com.boaat.jazzy_cookin.kitchen.sim.station.StationSimulationAccess;
import com.boaat.jazzy_cookin.registry.JazzyItems;

public final class PanSimulationDomain implements StationSimulationDomain {
    @Override
    public SimulationDomainType type() {
        return SimulationDomainType.PAN;
    }

    @Override
    public boolean supports(StationSimulationAccess access) {
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
            if (access.simulationItem(slot).is(JazzyItems.EGG_MIXTURE.get())) {
                sawMixture = true;
                continue;
            }
            if (!access.simulationItem(slot).isEmpty()
                    && !com.boaat.jazzy_cookin.kitchen.sim.FoodMaterialProfiles.isStoveFat(access.simulationItem(slot))) {
                return false;
            }
        }
        return sawMixture;
    }

    @Override
    public KitchenMethod method(StationSimulationAccess access) {
        return KitchenMethod.PAN_FRY;
    }

    @Override
    public SimulationSnapshot snapshot(StationSimulationAccess access, int executionMode) {
        FoodMatterData matter = access.simulationBatch() != null
                ? access.simulationBatch().matter()
                : com.boaat.jazzy_cookin.kitchen.sim.station.StationSimulationResolver.previewInputMatter(access);
        if (matter == null) {
            return new SimulationSnapshot(executionMode, 0, EggPanReactionSolver.toF(access.simulationStationPhysics().panTemperatureC()), 72, 72, 0, 0, 0, 0, 0, 0, 0);
        }

        DishRecognitionResult preview = access.simulationBatch() != null ? DishSchema.previewMeal(matter) : null;
        return new SimulationSnapshot(
                executionMode,
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

    @Override
    public boolean handleAction(StationSimulationAccess access, int buttonId) {
        return switch (buttonId) {
            case 6 -> EggStoveSimulationActions.primaryAction(access);
            case 7 -> EggStoveSimulationActions.stir(access);
            case 8 -> EggStoveSimulationActions.foldOrFlip(access);
            default -> false;
        };
    }

    @Override
    public void serverTick(StationSimulationAccess access) {
        EggPanReactionSolver.serverTick(access);
    }
}
