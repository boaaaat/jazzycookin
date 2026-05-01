package com.boaat.jazzy_cookin.kitchen.sim.reaction;

import com.boaat.jazzy_cookin.item.KitchenIngredientItem;
import com.boaat.jazzy_cookin.kitchen.HeatLevel;
import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.KitchenStackUtil;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.sim.CookingBatchState;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.kitchen.sim.FoodTrait;
import com.boaat.jazzy_cookin.kitchen.sim.StationPhysicsState;
import com.boaat.jazzy_cookin.kitchen.sim.station.StationSimulationAccess;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public final class EggPanReactionSolver {
    public static final float ROOM_TEMP_C = 22.0F;
    public static final float READY_PROGRESS = 0.72F;
    public static final float OVERCOOKED_CHAR = 0.34F;

    private EggPanReactionSolver() {
    }

    public static void serverTick(StationSimulationAccess access) {
        if (access.simulationStationType() != StationType.STOVE) {
            return;
        }

        int foodLoad = foodInputCount(access);
        float coldLoad = coldLoad(access, foodLoad);
        float oilCoverage = oilCoverage(access, foodLoad);
        float targetPanTemp = targetPanTemperatureC(access.simulationHeatLevel()) - panLoadPenalty(access, foodLoad, coldLoad);
        float currentPanTemp = access.simulationStationPhysics().panTemperatureC();
        float nextPanTemp = currentPanTemp + (targetPanTemp - currentPanTemp) * 0.08F;
        boolean changed = Math.abs(nextPanTemp - currentPanTemp) > 0.001F;
        access.simulationSetStationPhysics(new StationPhysicsState(nextPanTemp));

        changed |= tickVisiblePanInputs(access, nextPanTemp, foodLoad, coldLoad, oilCoverage);

        if (access.simulationBatch() != null && foodLoad <= 0) {
            FoodMatterData updated = tickPanMatter(access.simulationBatch().matter(), nextPanTemp, 1, 0.0F, 0.0F);
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
        return tickPanMatter(matter, panTempC, 1, 0.0F, 0.0F);
    }

    public static FoodMatterData tickPanMatter(FoodMatterData matter, float panTempC, int foodLoad) {
        return tickPanMatter(matter, panTempC, foodLoad, 0.0F, 0.0F);
    }

    public static FoodMatterData tickPanMatter(FoodMatterData matter, float panTempC, int foodLoad, float coldLoad) {
        return tickPanMatter(matter, panTempC, foodLoad, coldLoad, 0.0F);
    }

    public static FoodMatterData tickPanMatter(FoodMatterData matter, float panTempC, int foodLoad, float coldLoad, float oilCoverage) {
        int load = Math.max(1, foodLoad);
        float oil = isPanOil(matter) ? 0.0F : Mth.clamp(oilCoverage, 0.0F, 1.0F);
        float dryPan = 1.0F - oil;
        float moistureDampening = 0.45F + matter.water() * 0.35F;
        float loadDampening = 1.0F / (float) Math.sqrt(load);
        float surfaceRate = (0.09F - moistureDampening * 0.03F + dryPan * 0.025F - oil * 0.012F) * loadDampening;
        float coreRate = (0.05F - moistureDampening * 0.015F + oil * 0.030F - dryPan * 0.010F) * loadDampening;
        float nextSurfaceTemp = matter.surfaceTempC() + (panTempC - matter.surfaceTempC()) * surfaceRate;
        float nextCoreTemp = matter.coreTempC() + (nextSurfaceTemp - matter.coreTempC()) * coreRate;
        if (oil > 0.0F) {
            nextSurfaceTemp = Mth.lerp(oil * 0.16F, nextSurfaceTemp, nextCoreTemp + 18.0F);
        }
        float nextProteinSet = matter.proteinSet();
        if (nextCoreTemp > 62.0F && matter.protein() > 0.05F) {
            nextProteinSet += Mth.clamp((nextCoreTemp - 62.0F) / 20.0F, 0.0F, 1.0F) * 0.010F;
        }

        float nextWater = matter.water();
        if (nextSurfaceTemp > 100.0F) {
            nextWater -= Mth.clamp((nextSurfaceTemp - 100.0F) / 90.0F, 0.0F, 1.0F) * (0.0032F + dryPan * 0.0014F);
        }

        float nextBrowning = matter.browning();
        if (nextSurfaceTemp > 140.0F && nextWater < 0.35F) {
            nextBrowning += Mth.clamp((nextSurfaceTemp - 140.0F) / 60.0F, 0.0F, 1.0F)
                    * Mth.clamp((0.35F - nextWater) / 0.35F, 0.0F, 1.0F)
                    * (0.008F + dryPan * 0.006F);
        }

        float nextChar = matter.charLevel();
        if ((nextSurfaceTemp > 200.0F && nextBrowning > 0.65F) || (dryPan > 0.75F && nextSurfaceTemp > 185.0F && nextBrowning > 0.48F)) {
            nextChar += Mth.clamp((nextSurfaceTemp - 200.0F) / 30.0F, 0.0F, 1.0F)
                    * Mth.clamp((nextBrowning - 0.65F) / 0.35F, 0.0F, 1.0F)
                    * (0.012F + dryPan * 0.014F);
            if (dryPan > 0.75F) {
                nextChar += Mth.clamp((nextSurfaceTemp - 185.0F) / 45.0F, 0.0F, 1.0F) * 0.004F;
            }
        }

        float nextAeration = Math.max(0.0F, matter.aeration() - 0.0012F);
        float naturalCurdFormation = nextProteinSet > 0.04F
                ? 0.0025F + nextProteinSet * 0.0035F + Mth.clamp((150.0F - panTempC) / 60.0F, 0.0F, 1.0F) * 0.0015F
                : 0.0F;
        float directColdLoad = matter.coreTempC() < 65.0F ? Mth.clamp((65.0F - matter.coreTempC()) / 65.0F, 0.0F, 1.0F) : 0.0F;
        float sharedColdLoad = Mth.clamp(coldLoad, 0.0F, 1.0F);
        if (load > 1 && (directColdLoad > 0.0F || sharedColdLoad > 0.0F)) {
            float loadShock = (directColdLoad * 0.70F + sharedColdLoad * 0.30F) * (load - 1);
            nextProteinSet = Mth.clamp(nextProteinSet - loadShock * 0.0008F, 0.0F, 1.0F);
            nextBrowning = Mth.clamp(nextBrowning - loadShock * 0.0006F, 0.0F, 1.0F);
            nextSurfaceTemp = Math.max(ROOM_TEMP_C, nextSurfaceTemp - sharedColdLoad * (load - 1) * 0.22F);
        }
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

    private static boolean tickVisiblePanInputs(StationSimulationAccess access, float panTempC, int foodLoad, float coldLoad, float oilCoverage) {
        if (access.simulationLevel() == null || foodLoad <= 0) {
            return false;
        }
        boolean changed = false;
        long gameTime = access.simulationLevel().getGameTime();
        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            ItemStack stack = access.simulationItem(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof KitchenIngredientItem)) {
                continue;
            }
            FoodMatterData before = KitchenStackUtil.getOrCreateFoodMatter(stack, gameTime);
            if (before == null) {
                continue;
            }
            FoodMatterData after = tickPanMatter(before, panTempC, foodLoad, coldLoad, oilCoverage);
            if (!after.equals(before)) {
                KitchenStackUtil.setFoodMatter(stack, after, gameTime);
                if (isOvercooked(after)) {
                    KitchenStackUtil.setFoodState(stack, overcookedState(after));
                }
                changed = true;
            }
        }
        return changed;
    }

    private static int foodInputCount(StationSimulationAccess access) {
        int count = 0;
        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            if (access.simulationItem(slot).getItem() instanceof KitchenIngredientItem) {
                count++;
            }
        }
        return count;
    }

    private static float coldLoad(StationSimulationAccess access, int foodLoad) {
        if (access.simulationLevel() == null || foodLoad <= 0) {
            return 0.0F;
        }
        float load = 0.0F;
        long gameTime = access.simulationLevel().getGameTime();
        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            ItemStack stack = access.simulationItem(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof KitchenIngredientItem)) {
                continue;
            }
            FoodMatterData matter = KitchenStackUtil.getOrCreateFoodMatter(stack, gameTime);
            if (matter != null) {
                load += Mth.clamp((62.0F - matter.coreTempC()) / 62.0F, 0.0F, 1.0F)
                        * (0.25F + matter.water() * 0.75F);
            }
        }
        return Mth.clamp(load / foodLoad, 0.0F, 1.0F);
    }

    private static float oilCoverage(StationSimulationAccess access, int foodLoad) {
        if (access.simulationLevel() == null || foodLoad <= 1) {
            return 0.0F;
        }
        int oilItems = 0;
        float oilHeat = 0.0F;
        long gameTime = access.simulationLevel().getGameTime();
        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            ItemStack stack = access.simulationItem(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof KitchenIngredientItem)) {
                continue;
            }
            FoodMatterData matter = KitchenStackUtil.getOrCreateFoodMatter(stack, gameTime);
            if (matter != null && isPanOil(matter)) {
                oilItems += Math.max(1, stack.getCount());
                oilHeat += Mth.clamp((Math.max(matter.surfaceTempC(), matter.coreTempC()) - 45.0F) / 95.0F, 0.0F, 1.0F);
            }
        }
        if (oilItems <= 0) {
            return 0.0F;
        }
        int nonOilItems = Math.max(1, foodLoad - oilItems);
        float amountCoverage = Mth.clamp(oilItems / (float) nonOilItems, 0.0F, 1.0F);
        float heatCoverage = Mth.clamp(oilHeat / oilItems, 0.0F, 1.0F);
        return amountCoverage * (0.35F + heatCoverage * 0.65F);
    }

    private static float panLoadPenalty(StationSimulationAccess access, int foodLoad, float coldLoad) {
        if (foodLoad <= 0) {
            return 0.0F;
        }
        float waterLoad = 0.0F;
        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            ItemStack stack = access.simulationItem(slot);
            FoodMatterData matter = KitchenStackUtil.getFoodMatter(stack);
            if (matter != null) {
                waterLoad += matter.water();
            }
        }
        return Mth.clamp((foodLoad - 1) * 12.0F + waterLoad * 8.0F + coldLoad * 18.0F, 0.0F, 70.0F);
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

    public static float cookProgress(FoodMatterData matter) {
        if (matter == null) {
            return 0.0F;
        }
        if (isPanOil(matter)) {
            return Mth.clamp((Math.max(matter.surfaceTempC(), matter.coreTempC()) - 45.0F) / 115.0F, 0.0F, 1.0F);
        }
        float proteinProgress = matter.protein() > 0.05F ? matter.proteinSet() : 0.0F;
        float browningProgress = matter.browning() * 0.82F;
        float timeProgress = matter.timeInPan() / 210.0F;
        return Mth.clamp(Math.max(proteinProgress, Math.max(browningProgress, timeProgress)), 0.0F, 1.0F);
    }

    public static boolean isReady(FoodMatterData matter) {
        return matter != null && (isPanOil(matter) || cookProgress(matter) >= READY_PROGRESS);
    }

    public static boolean isOvercooked(FoodMatterData matter) {
        return matter != null && (matter.charLevel() >= OVERCOOKED_CHAR || (matter.surfaceTempC() > 235.0F && matter.browning() > 0.78F));
    }

    public static boolean isPanOil(FoodMatterData matter) {
        return matter != null
                && (matter.hasTrait(FoodTrait.OIL) || matter.hasTrait(FoodTrait.FAT))
                && matter.protein() < 0.08F
                && matter.water() < 0.25F;
    }

    private static IngredientState overcookedState(FoodMatterData matter) {
        return isPanOil(matter) ? IngredientState.BURNT_OIL : IngredientState.PAN_FRIED;
    }
}
