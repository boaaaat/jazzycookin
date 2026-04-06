package com.boaat.jazzy_cookin.kitchen.sim.action;

import com.boaat.jazzy_cookin.kitchen.KitchenStackUtil;
import com.boaat.jazzy_cookin.kitchen.sim.CookingBatchState;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMaterialProfile;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMaterialProfiles;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.kitchen.sim.reaction.EggPanReactionSolver;
import com.boaat.jazzy_cookin.kitchen.sim.recognition.DishRecognitionResult;
import com.boaat.jazzy_cookin.kitchen.sim.recognition.DishSchema;
import com.boaat.jazzy_cookin.kitchen.sim.station.StationSimulationAccess;
import com.boaat.jazzy_cookin.kitchen.sim.station.StationSimulationResolver;
import com.boaat.jazzy_cookin.registry.JazzyItems;

import net.minecraft.world.item.ItemStack;

public final class EggStoveSimulationActions {
    private EggStoveSimulationActions() {
    }

    public static boolean primaryAction(StationSimulationAccess access) {
        return access.simulationBatch() == null ? pour(access) : remove(access);
    }

    public static boolean stir(StationSimulationAccess access) {
        if (access.simulationLevel() == null || access.simulationBatch() == null) {
            return false;
        }

        FoodMatterData matter = access.simulationBatch().matter();
        FoodMatterData stirred = matter.withWorkingState(
                matter.water(),
                Math.max(0.0F, matter.aeration() - 0.01F),
                matter.fragmentation() + 0.16F,
                matter.cohesiveness() - 0.05F,
                matter.proteinSet(),
                Math.max(0.0F, matter.browning() - 0.01F),
                matter.charLevel(),
                matter.whiskWork(),
                matter.stirCount() + 1,
                matter.flipCount(),
                matter.timeInPan(),
                matter.processDepth(),
                false
        );
        access.simulationSetBatch(new CookingBatchState(stirred));
        access.simulationMarkChanged();
        return true;
    }

    public static boolean foldOrFlip(StationSimulationAccess access) {
        if (access.simulationLevel() == null || access.simulationBatch() == null) {
            return false;
        }

        FoodMatterData matter = access.simulationBatch().matter();
        FoodMatterData flipped = matter.withWorkingState(
                matter.water(),
                Math.max(0.0F, matter.aeration() - 0.01F),
                Math.max(0.0F, matter.fragmentation() - 0.08F),
                matter.cohesiveness() + 0.14F,
                matter.proteinSet(),
                matter.browning() + 0.01F,
                matter.charLevel(),
                matter.whiskWork(),
                matter.stirCount(),
                matter.flipCount() + 1,
                matter.timeInPan(),
                matter.processDepth(),
                false
        );
        access.simulationSetBatch(new CookingBatchState(flipped));
        access.simulationMarkChanged();
        return true;
    }

    private static boolean pour(StationSimulationAccess access) {
        if (access.simulationLevel() == null || access.simulationBatch() != null || !StationSimulationResolver.supportsEggStove(access)) {
            return false;
        }

        int mixtureSlot = StationSimulationResolver.firstInputSlotMatching(access, JazzyItems.EGG_MIXTURE.get());
        if (mixtureSlot < 0) {
            return false;
        }

        long gameTime = access.simulationLevel().getGameTime();
        FoodMatterData matter = KitchenStackUtil.getOrCreateFoodMatter(access.simulationItem(mixtureSlot), gameTime);
        if (matter == null) {
            return false;
        }

        float addedFat = 0.0F;
        int fatSlot = StationSimulationResolver.firstStoveFatSlot(access);
        if (fatSlot >= 0) {
            FoodMaterialProfile fatProfile = FoodMaterialProfiles.profileFor(access.simulationItem(fatSlot)).orElse(FoodMaterialProfiles.BUTTER);
            addedFat = fatProfile.fat() * 0.18F;
            access.simulationRemoveItem(fatSlot, 1);
        }

        access.simulationRemoveItem(mixtureSlot, 1);
        FoodMatterData poured = matter.withFlavorLoads(
                matter.fat() + addedFat,
                matter.seasoningLoad(),
                matter.cheeseLoad(),
                matter.onionLoad(),
                matter.herbLoad(),
                matter.pepperLoad()
        ).withWorkingState(
                matter.water(),
                matter.aeration() * 0.94F,
                Math.max(0.10F, matter.fragmentation()),
                Math.max(0.18F, matter.cohesiveness() * 0.82F),
                0.0F,
                0.0F,
                0.0F,
                matter.whiskWork(),
                0,
                0,
                0,
                Math.max(2, matter.processDepth() + 1),
                false
        ).withTemps(EggPanReactionSolver.ROOM_TEMP_C, EggPanReactionSolver.ROOM_TEMP_C);
        access.simulationSetBatch(new CookingBatchState(poured));
        access.simulationMarkChanged();
        return true;
    }

    private static boolean remove(StationSimulationAccess access) {
        if (access.simulationLevel() == null || access.simulationBatch() == null) {
            return false;
        }

        DishRecognitionResult result = DishSchema.finalizeResult(access.simulationBatch().matter());
        ItemStack outputStack = new ItemStack(result.resultItem().get());
        FoodMatterData finalized = access.simulationBatch().matter().withWorkingState(
                access.simulationBatch().matter().water(),
                access.simulationBatch().matter().aeration(),
                access.simulationBatch().matter().fragmentation(),
                access.simulationBatch().matter().cohesiveness(),
                access.simulationBatch().matter().proteinSet(),
                access.simulationBatch().matter().browning(),
                access.simulationBatch().matter().charLevel(),
                access.simulationBatch().matter().whiskWork(),
                access.simulationBatch().matter().stirCount(),
                access.simulationBatch().matter().flipCount(),
                access.simulationBatch().matter().timeInPan(),
                access.simulationBatch().matter().processDepth(),
                true
        );
        KitchenStackUtil.initializeStack(outputStack, null, finalized, access.simulationLevel().getGameTime());
        if (!access.simulationCanAcceptStack(access.outputSlot(), outputStack)) {
            return false;
        }

        access.simulationMergeIntoSlot(access.outputSlot(), outputStack);
        access.simulationSetBatch(null);
        access.simulationMarkChanged();
        return true;
    }
}
