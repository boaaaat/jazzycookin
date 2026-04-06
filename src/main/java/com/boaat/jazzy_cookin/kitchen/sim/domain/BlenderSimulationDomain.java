package com.boaat.jazzy_cookin.kitchen.sim.domain;

import com.boaat.jazzy_cookin.item.KitchenIngredientItem;
import com.boaat.jazzy_cookin.kitchen.KitchenMethod;
import com.boaat.jazzy_cookin.kitchen.KitchenStackUtil;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.kitchen.sim.FoodTrait;
import com.boaat.jazzy_cookin.kitchen.sim.SimulationSnapshot;
import com.boaat.jazzy_cookin.kitchen.sim.station.StationSimulationAccess;
import com.boaat.jazzy_cookin.registry.JazzyItems;
import com.boaat.jazzy_cookin.registry.JazzyItems.IngredientId;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public final class BlenderSimulationDomain implements StationSimulationDomain {
    @Override
    public SimulationDomainType type() {
        return SimulationDomainType.BLEND;
    }

    @Override
    public boolean supports(StationSimulationAccess access) {
        return access.simulationStationType() == StationType.BLENDER && classify(access) != null;
    }

    @Override
    public KitchenMethod method(StationSimulationAccess access) {
        return KitchenMethod.BLEND;
    }

    @Override
    public SimulationSnapshot snapshot(StationSimulationAccess access, int executionMode) {
        FoodMatterData matter = previewMatter(access);
        if (matter == null) {
            return SimulationSnapshot.inactive(executionMode);
        }
        return new SimulationSnapshot(
                executionMode,
                0,
                72,
                72,
                72,
                Math.round(matter.cohesiveness() * 100.0F),
                Math.round(matter.water() * 100.0F),
                0,
                0,
                Math.round(matter.aeration() * 100.0F),
                Math.round(matter.fragmentation() * 100.0F),
                0
        );
    }

    @Override
    public boolean handleAction(StationSimulationAccess access, int buttonId) {
        if (buttonId != 6 || access.simulationLevel() == null) {
            return false;
        }
        BlendOutcome outcome = classify(access);
        if (outcome == null) {
            return false;
        }

        long gameTime = access.simulationLevel().getGameTime();
        SimulationIngredientAnalysis analysis = SimulationIngredientAnalysis.analyzeInputs(access);
        ItemStack output = SimulationOutputFactory.createOutput(outcome.output(), gameTime, analysis, matter -> shapeMatter(matter, analysis, access.simulationControlSetting()));
        if (!access.simulationCanAcceptStack(access.outputSlot(), output)) {
            return false;
        }

        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            if (!access.simulationItem(slot).isEmpty()) {
                access.simulationRemoveItem(slot, 1);
            }
        }

        access.simulationMergeIntoSlot(access.outputSlot(), output);
        access.simulationMarkChanged();
        return true;
    }

    private record BlendOutcome(KitchenIngredientItem output) {
    }

    private static BlendOutcome classify(StationSimulationAccess access) {
        SimulationIngredientAnalysis analysis = SimulationIngredientAnalysis.analyzeInputs(access);
        if (analysis.isEmpty()) {
            return null;
        }

        boolean hasMilk = analysis.has(JazzyItems.ingredient(IngredientId.ALMOND_MILK).get())
                || analysis.has(JazzyItems.ingredient(IngredientId.OAT_MILK).get())
                || analysis.has(JazzyItems.ingredient(IngredientId.SOY_MILK).get())
                || analysis.has(JazzyItems.ingredient(IngredientId.EVAPORATED_MILK).get())
                || analysis.has(JazzyItems.ingredient(IngredientId.SHELF_STABLE_CREAM).get());
        boolean hasJuiceBase = analysis.has(JazzyItems.MIXED_JUICE.get()) || analysis.has(JazzyItems.LEMON_JUICE.get());
        boolean hasFruitOrVeg = analysis.hasTrait(FoodTrait.FRUIT) || analysis.hasTrait(FoodTrait.VEGETABLE);

        if (hasFruitOrVeg && hasMilk) {
            return new BlendOutcome(JazzyItems.SMOOTHIE_BLEND.get());
        }
        if (hasJuiceBase || hasFruitOrVeg) {
            return new BlendOutcome(JazzyItems.FRUIT_JUICE_BLEND.get());
        }
        return null;
    }

    private static FoodMatterData previewMatter(StationSimulationAccess access) {
        if (access.simulationLevel() == null) {
            return null;
        }
        ItemStack output = access.simulationItem(access.outputSlot());
        if (!output.isEmpty()) {
            return KitchenStackUtil.getOrCreateFoodMatter(output, access.simulationLevel().getGameTime());
        }
        BlendOutcome outcome = classify(access);
        if (outcome == null) {
            return null;
        }
        SimulationIngredientAnalysis analysis = SimulationIngredientAnalysis.analyzeInputs(access);
        ItemStack preview = SimulationOutputFactory.createOutput(
                outcome.output(),
                access.simulationLevel().getGameTime(),
                analysis,
                matter -> shapeMatter(matter, analysis, access.simulationControlSetting())
        );
        return KitchenStackUtil.getFoodMatter(preview);
    }

    private static FoodMatterData shapeMatter(FoodMatterData matter, SimulationIngredientAnalysis analysis, int controlSetting) {
        float controlFactor = switch (controlSetting) {
            case 0 -> 0.18F;
            case 2 -> 0.44F;
            default -> 0.30F;
        };
        return matter.withFlavorLoads(
                Mth.clamp(matter.fat() * 0.70F + analysis.avgFat() * 0.30F, 0.0F, 1.0F),
                Mth.clamp(matter.seasoningLoad() + analysis.avgSeasoning() * 0.30F, 0.0F, 1.0F),
                Mth.clamp(matter.cheeseLoad() + analysis.avgCheese() * 0.20F, 0.0F, 1.0F),
                Mth.clamp(matter.onionLoad() + analysis.avgOnion() * 0.20F, 0.0F, 1.0F),
                Mth.clamp(matter.herbLoad() + analysis.avgHerb() * 0.20F, 0.0F, 1.0F),
                Mth.clamp(matter.pepperLoad() + analysis.avgPepper() * 0.20F, 0.0F, 1.0F)
        ).withWorkingState(
                Mth.clamp(matter.water() * 0.70F + analysis.avgWater() * 0.30F, 0.20F, 1.0F),
                0.14F + controlFactor * 0.18F,
                0.10F + (1.0F - controlFactor) * 0.08F,
                0.38F + controlFactor * 0.10F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0,
                0,
                0,
                Math.max(1, matter.processDepth() + 1),
                false
        );
    }
}
