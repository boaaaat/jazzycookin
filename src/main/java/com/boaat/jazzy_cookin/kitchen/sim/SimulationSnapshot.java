package com.boaat.jazzy_cookin.kitchen.sim;

public record SimulationSnapshot(
        int executionMode,
        int batchPresent,
        int panTempF,
        int foodCoreTempF,
        int foodSurfaceTempF,
        int doneness,
        int moisture,
        int browning,
        int charLevel,
        int aeration,
        int fragmentation,
        int recognizerId
) {
    public static SimulationSnapshot inactive(int executionMode) {
        return new SimulationSnapshot(executionMode, 0, 72, 72, 72, 0, 0, 0, 0, 0, 0, 0);
    }
}
