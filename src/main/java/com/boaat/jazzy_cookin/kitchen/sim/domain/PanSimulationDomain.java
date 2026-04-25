package com.boaat.jazzy_cookin.kitchen.sim.domain;

import com.boaat.jazzy_cookin.kitchen.KitchenMethod;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.ToolProfile;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.kitchen.sim.SimulationSnapshot;
import com.boaat.jazzy_cookin.kitchen.sim.action.EggStoveSimulationActions;
import com.boaat.jazzy_cookin.kitchen.sim.reaction.EggPanReactionSolver;
import com.boaat.jazzy_cookin.kitchen.sim.station.StationSimulationAccess;

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

        return com.boaat.jazzy_cookin.kitchen.sim.station.StationSimulationResolver.supportsEggStove(access)
                || PanSchemaSimulationActions.hasInputCandidate(access);
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

        int previewId = access.simulationBatch() != null ? PanSchemaSimulationActions.previewId(matter) : 0;
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
                previewId
        );
    }

    @Override
    public boolean handleAction(StationSimulationAccess access, int buttonId) {
        return switch (buttonId) {
            case 6 -> PanSchemaSimulationActions.primaryAction(access);
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
