package com.boaat.jazzy_cookin.kitchen.sim.domain;

import com.boaat.jazzy_cookin.kitchen.KitchenMethod;
import com.boaat.jazzy_cookin.kitchen.sim.SimulationSnapshot;
import com.boaat.jazzy_cookin.kitchen.sim.station.StationSimulationAccess;

public interface StationSimulationDomain {
    SimulationDomainType type();

    boolean supports(StationSimulationAccess access);

    KitchenMethod method(StationSimulationAccess access);

    SimulationSnapshot snapshot(StationSimulationAccess access, int executionMode);

    boolean handleAction(StationSimulationAccess access, int buttonId);

    default void serverTick(StationSimulationAccess access) {
    }

    default int environmentStatus(StationSimulationAccess access) {
        return 1;
    }
}
