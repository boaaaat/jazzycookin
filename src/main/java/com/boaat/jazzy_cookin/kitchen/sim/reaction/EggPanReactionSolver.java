package com.boaat.jazzy_cookin.kitchen.sim.reaction;

import com.boaat.jazzy_cookin.kitchen.HeatLevel;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.sim.CookingBatchState;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.kitchen.sim.StationPhysicsState;
import com.boaat.jazzy_cookin.kitchen.sim.station.StationSimulationAccess;

import net.minecraft.util.Mth;

public final class EggPanReactionSolver {
    public static final float ROOM_TEMP_C = 22.0F;

    private EggPanReactionSolver() {
    }

    public static void serverTick(StationSimulationAccess access) {
        if (access.simulationStationType() != StationType.STOVE) {
            return;
        }

        float targetPanTemp = targetPanTemperatureC(access.simulationHeatLevel());
        float currentPanTemp = access.simulationStationPhysics().panTemperatureC();
        float nextPanTemp = currentPanTemp + (targetPanTemp - currentPanTemp) * 0.08F;
        boolean changed = Math.abs(nextPanTemp - currentPanTemp) > 0.001F;
        access.simulationSetStationPhysics(new StationPhysicsState(nextPanTemp));

        if (access.simulationBatch() != null) {
            FoodMatterData updated = tickEggBatch(access.simulationBatch().matter(), nextPanTemp);
            if (!updated.equals(access.simulationBatch().matter())) {
                access.simulationSetBatch(new CookingBatchState(updated));
                changed = true;
            }
        }

        if (changed) {
            access.simulationMarkChanged();
        }
    }

    public static FoodMatterData tickEggBatch(FoodMatterData matter, float panTempC) {
        float moistureDampening = 0.45F + matter.water() * 0.35F;
        float nextSurfaceTemp = matter.surfaceTempC() + (panTempC - matter.surfaceTempC()) * (0.09F - moistureDampening * 0.03F);
        float nextCoreTemp = matter.coreTempC() + (nextSurfaceTemp - matter.coreTempC()) * (0.05F - moistureDampening * 0.015F);
        float nextProteinSet = matter.proteinSet();
        if (nextCoreTemp > 62.0F) {
            nextProteinSet += Mth.clamp((nextCoreTemp - 62.0F) / 20.0F, 0.0F, 1.0F) * 0.010F;
        }

        float nextWater = matter.water();
        if (nextSurfaceTemp > 100.0F) {
            nextWater -= Mth.clamp((nextSurfaceTemp - 100.0F) / 90.0F, 0.0F, 1.0F) * 0.0038F;
        }

        float nextBrowning = matter.browning();
        if (nextSurfaceTemp > 140.0F && nextWater < 0.35F) {
            nextBrowning += Mth.clamp((nextSurfaceTemp - 140.0F) / 60.0F, 0.0F, 1.0F)
                    * Mth.clamp((0.35F - nextWater) / 0.35F, 0.0F, 1.0F)
                    * 0.010F;
        }

        float nextChar = matter.charLevel();
        if (nextSurfaceTemp > 200.0F && nextBrowning > 0.65F) {
            nextChar += Mth.clamp((nextSurfaceTemp - 200.0F) / 30.0F, 0.0F, 1.0F)
                    * Mth.clamp((nextBrowning - 0.65F) / 0.35F, 0.0F, 1.0F)
                    * 0.018F;
        }

        float nextAeration = Math.max(0.0F, matter.aeration() - 0.0012F);
        float naturalCurdFormation = nextProteinSet > 0.04F
                ? 0.0025F + nextProteinSet * 0.0035F + Mth.clamp((150.0F - panTempC) / 60.0F, 0.0F, 1.0F) * 0.0015F
                : 0.0F;
        float nextFragmentation = matter.fragmentation() + naturalCurdFormation;
        float nextCohesiveness = matter.cohesiveness() + nextProteinSet * 0.002F - nextFragmentation * 0.0008F + matter.flipCount() * 0.0004F;
        return matter.withTemps(nextSurfaceTemp, nextCoreTemp).withWorkingState(
                nextWater,
                nextAeration,
                nextFragmentation,
                nextCohesiveness,
                nextProteinSet,
                nextBrowning,
                nextChar,
                matter.whiskWork(),
                matter.stirCount(),
                matter.flipCount(),
                matter.timeInPan() + 1,
                matter.processDepth(),
                false
        );
    }

    public static float targetPanTemperatureC(HeatLevel heatLevel) {
        return switch (heatLevel) {
            case LOW -> 135.0F;
            case MEDIUM -> 175.0F;
            case HIGH -> 215.0F;
            default -> ROOM_TEMP_C;
        };
    }

    public static int toF(float celsius) {
        return Math.round(celsius * 9.0F / 5.0F + 32.0F);
    }
}
