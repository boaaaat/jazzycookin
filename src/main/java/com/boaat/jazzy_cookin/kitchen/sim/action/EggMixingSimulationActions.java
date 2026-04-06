package com.boaat.jazzy_cookin.kitchen.sim.action;

import java.util.Optional;

import com.boaat.jazzy_cookin.kitchen.IngredientStateData;
import com.boaat.jazzy_cookin.kitchen.KitchenStackUtil;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMaterialProfile;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMaterialProfiles;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.kitchen.sim.reaction.EggPanReactionSolver;
import com.boaat.jazzy_cookin.kitchen.sim.station.StationSimulationAccess;
import com.boaat.jazzy_cookin.kitchen.sim.station.StationSimulationResolver;
import com.boaat.jazzy_cookin.registry.JazzyItems;

import net.minecraft.world.item.ItemStack;

public final class EggMixingSimulationActions {
    private EggMixingSimulationActions() {
    }

    public static boolean whisk(StationSimulationAccess access) {
        if (access.simulationLevel() == null || !StationSimulationResolver.supportsEggMixing(access)) {
            return false;
        }

        long gameTime = access.simulationLevel().getGameTime();
        ItemStack output = access.simulationItem(access.outputSlot());
        FoodMatterData matter;
        if (output.is(JazzyItems.EGG_MIXTURE.get())) {
            matter = KitchenStackUtil.getOrCreateFoodMatter(output, gameTime);
            if (matter == null) {
                return false;
            }
            matter = mergeMixingBowlAddIns(access, matter);
        } else {
            if (!output.isEmpty()) {
                return false;
            }
            matter = createEggMixture(access, gameTime);
            if (matter == null) {
                return false;
            }
            consumeMixingInputs(access, true);
            output = new ItemStack(JazzyItems.EGG_MIXTURE.get());
            KitchenStackUtil.initializeStack(output, null, matter, gameTime);
            access.simulationSetItem(access.outputSlot(), output);
        }

        float whiskWorkDelta = switch (access.simulationControlSetting()) {
            case 0 -> 0.10F;
            case 2 -> 0.28F;
            default -> 0.18F;
        };
        float aerationDelta = switch (access.simulationControlSetting()) {
            case 0 -> 0.06F;
            case 2 -> 0.14F;
            default -> 0.10F;
        };

        float nextAeration = matter.aeration() + aerationDelta;
        float nextFragmentation = matter.fragmentation() + 0.02F + whiskWorkDelta * 0.04F;
        float nextCohesiveness = matter.cohesiveness() + 0.05F + whiskWorkDelta * 0.10F;
        if (access.simulationControlSetting() == 2 && nextAeration > 0.80F) {
            nextCohesiveness -= 0.05F;
            nextFragmentation += 0.04F;
        }

        FoodMatterData whisked = matter.withWorkingState(
                matter.water(),
                nextAeration,
                nextFragmentation,
                nextCohesiveness,
                matter.proteinSet(),
                matter.browning(),
                matter.charLevel(),
                matter.whiskWork() + whiskWorkDelta,
                matter.stirCount(),
                matter.flipCount(),
                matter.timeInPan(),
                Math.max(1, matter.processDepth() + 1),
                false
        );
        KitchenStackUtil.setFoodMatter(access.simulationItem(access.outputSlot()), whisked, gameTime);
        access.simulationMarkChanged();
        return true;
    }

    private static FoodMatterData createEggMixture(StationSimulationAccess access, long gameTime) {
        int eggCount = 0;
        long createdTick = gameTime;
        float water = 0.0F;
        float fat = 0.0F;
        float protein = 0.0F;
        float seasoning = 0.0F;
        float cheese = 0.0F;
        float onion = 0.0F;
        float herb = 0.0F;
        float pepper = 0.0F;

        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            ItemStack stack = access.simulationItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            Optional<FoodMaterialProfile> profile = FoodMaterialProfiles.profileFor(stack);
            if (profile.isEmpty()) {
                return null;
            }

            FoodMatterData sourceMatter = KitchenStackUtil.getOrCreateFoodMatter(stack, gameTime);
            if (sourceMatter != null) {
                createdTick = Math.min(createdTick, sourceMatter.createdTick());
            }

            if (stack.is(JazzyItems.ingredient(JazzyItems.IngredientId.EGGS).get())) {
                eggCount++;
                water += profile.get().water();
                fat += profile.get().fat();
                protein += profile.get().protein();
            } else {
                water += profile.get().water() * 0.08F;
                fat += profile.get().fat() * 0.12F;
                protein += profile.get().protein() * 0.08F;
                seasoning += profile.get().seasoningLoad();
                cheese += profile.get().cheeseLoad();
                onion += profile.get().onionLoad();
                herb += profile.get().herbLoad();
                pepper += profile.get().pepperLoad();
            }
        }

        if (eggCount <= 0) {
            return null;
        }

        IngredientStateData summaryHint = JazzyItems.EGG_MIXTURE.get().defaultData(createdTick);
        return new FoodMatterData(
                createdTick,
                summaryHint,
                EggPanReactionSolver.ROOM_TEMP_C,
                EggPanReactionSolver.ROOM_TEMP_C,
                eggCount > 0 ? water / eggCount : FoodMaterialProfiles.EGGS.water(),
                eggCount > 0 ? fat / eggCount : FoodMaterialProfiles.EGGS.fat(),
                eggCount > 0 ? protein / eggCount : FoodMaterialProfiles.EGGS.protein(),
                0.12F,
                0.08F,
                0.22F,
                0.0F,
                0.0F,
                0.0F,
                seasoning,
                cheese,
                onion,
                herb,
                pepper,
                0.0F,
                0,
                0,
                0,
                1,
                false
        ).clamp();
    }

    private static FoodMatterData mergeMixingBowlAddIns(StationSimulationAccess access, FoodMatterData matter) {
        float water = matter.water();
        float fat = matter.fat();
        float protein = matter.protein();
        float seasoning = matter.seasoningLoad();
        float cheese = matter.cheeseLoad();
        float onion = matter.onionLoad();
        float herb = matter.herbLoad();
        float pepper = matter.pepperLoad();
        boolean consumed = false;

        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            ItemStack stack = access.simulationItem(slot);
            if (stack.isEmpty() || !FoodMaterialProfiles.isMixingBowlAddIn(stack)) {
                continue;
            }

            FoodMaterialProfile profile = FoodMaterialProfiles.profileFor(stack).orElse(null);
            if (profile == null) {
                continue;
            }

            water += profile.water() * 0.08F;
            fat += profile.fat() * 0.12F;
            protein += profile.protein() * 0.08F;
            seasoning += profile.seasoningLoad();
            cheese += profile.cheeseLoad();
            onion += profile.onionLoad();
            herb += profile.herbLoad();
            pepper += profile.pepperLoad();
            access.simulationRemoveItem(slot, 1);
            consumed = true;
        }

        if (!consumed) {
            return matter;
        }

        FoodMatterData flavored = matter.withFlavorLoads(fat, seasoning, cheese, onion, herb, pepper).withWorkingState(
                water,
                matter.aeration(),
                matter.fragmentation(),
                matter.cohesiveness(),
                matter.proteinSet(),
                matter.browning(),
                matter.charLevel(),
                matter.whiskWork(),
                matter.stirCount(),
                matter.flipCount(),
                matter.timeInPan(),
                matter.processDepth(),
                false
        );
        return flavored.withTemps(matter.surfaceTempC(), matter.coreTempC());
    }

    private static void consumeMixingInputs(StationSimulationAccess access, boolean consumeEggs) {
        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            ItemStack stack = access.simulationItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(JazzyItems.ingredient(JazzyItems.IngredientId.EGGS).get())) {
                if (consumeEggs) {
                    access.simulationRemoveItem(slot, 1);
                }
            } else if (FoodMaterialProfiles.isMixingBowlAddIn(stack)) {
                access.simulationRemoveItem(slot, 1);
            }
        }
    }
}
