package com.boaat.jazzy_cookin.kitchen.sim.domain;

import com.boaat.jazzy_cookin.item.KitchenIngredientItem;
import com.boaat.jazzy_cookin.kitchen.KitchenMethod;
import com.boaat.jazzy_cookin.kitchen.KitchenStackUtil;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.kitchen.sim.FoodTrait;
import com.boaat.jazzy_cookin.kitchen.sim.SimulationSnapshot;
import com.boaat.jazzy_cookin.kitchen.sim.recognition.DishRecognitionResult;
import com.boaat.jazzy_cookin.kitchen.sim.recognition.DishSchema;
import com.boaat.jazzy_cookin.kitchen.sim.station.StationSimulationAccess;
import com.boaat.jazzy_cookin.registry.JazzyItems;
import com.boaat.jazzy_cookin.registry.JazzyItems.IngredientId;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public final class JuicerSimulationDomain implements StationSimulationDomain {
    @Override
    public SimulationDomainType type() {
        return SimulationDomainType.JUICE;
    }

    @Override
    public boolean supports(StationSimulationAccess access) {
        return access.simulationStationType() == StationType.JUICER && classify(access) != null;
    }

    @Override
    public KitchenMethod method(StationSimulationAccess access) {
        return KitchenMethod.JUICE;
    }

    @Override
    public SimulationSnapshot snapshot(StationSimulationAccess access, int executionMode) {
        FoodMatterData matter = previewMatter(access);
        if (matter == null) {
            return SimulationSnapshot.inactive(executionMode);
        }
        DishRecognitionResult preview = DishSchema.preview(matter);
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
                preview != null ? preview.previewId() : 0
        );
    }

    @Override
    public boolean handleAction(StationSimulationAccess access, int buttonId) {
        if (buttonId != 6 || access.simulationLevel() == null) {
            return false;
        }
        JuiceOutcome outcome = classify(access);
        if (outcome == null) {
            return false;
        }

        long gameTime = access.simulationLevel().getGameTime();
        SimulationIngredientAnalysis analysis = SimulationIngredientAnalysis.analyzeInputs(access);
        ItemStack output = SimulationOutputFactory.createOutput(outcome.output(), gameTime, analysis, matter -> shapeJuiceMatter(matter, analysis));
        ItemStack byproduct = SimulationOutputFactory.createOutput(JazzyItems.FRUIT_PULP.get(), gameTime, analysis, matter -> shapePulpMatter(matter, analysis));
        if (!access.simulationCanAcceptStack(access.outputSlot(), output) || !access.simulationCanAcceptStack(access.byproductSlot(), byproduct)) {
            return false;
        }

        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            if (!access.simulationItem(slot).isEmpty()) {
                access.simulationRemoveItem(slot, 1);
            }
        }

        access.simulationMergeIntoSlot(access.outputSlot(), output);
        access.simulationMergeIntoSlot(access.byproductSlot(), byproduct);
        access.simulationMarkChanged();
        return true;
    }

    private record JuiceOutcome(KitchenIngredientItem output) {
    }

    private static JuiceOutcome classify(StationSimulationAccess access) {
        SimulationIngredientAnalysis analysis = SimulationIngredientAnalysis.analyzeInputs(access);
        if (analysis.isEmpty()) {
            return null;
        }
        if (analysis.has(JazzyItems.ingredient(IngredientId.LEMONS).get())
                && analysis.totalItems() == analysis.count(JazzyItems.ingredient(IngredientId.LEMONS).get())) {
            return new JuiceOutcome(JazzyItems.LEMON_JUICE.get());
        }
        if ((analysis.hasTrait(FoodTrait.FRUIT) || analysis.hasTrait(FoodTrait.VEGETABLE))
                && !analysis.hasTrait(FoodTrait.LEAFY_GREEN)
                && !analysis.hasTrait(FoodTrait.PROTEIN)
                && !analysis.hasTrait(FoodTrait.DAIRY)
                && !analysis.hasTrait(FoodTrait.BREAD)) {
            return new JuiceOutcome(JazzyItems.MIXED_JUICE.get());
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
        JuiceOutcome outcome = classify(access);
        if (outcome == null) {
            return null;
        }
        SimulationIngredientAnalysis analysis = SimulationIngredientAnalysis.analyzeInputs(access);
        ItemStack preview = SimulationOutputFactory.createOutput(outcome.output(), access.simulationLevel().getGameTime(), analysis, matter -> shapeJuiceMatter(matter, analysis));
        return KitchenStackUtil.getFoodMatter(preview);
    }

    private static FoodMatterData shapeJuiceMatter(FoodMatterData matter, SimulationIngredientAnalysis analysis) {
        return matter.withFlavorLoads(
                Mth.clamp(matter.fat() * 0.50F + analysis.avgFat() * 0.10F, 0.0F, 1.0F),
                Mth.clamp(matter.seasoningLoad() + analysis.avgSeasoning() * 0.10F, 0.0F, 1.0F),
                Mth.clamp(matter.cheeseLoad() + analysis.avgCheese() * 0.05F, 0.0F, 1.0F),
                Mth.clamp(matter.onionLoad() + analysis.avgOnion() * 0.05F, 0.0F, 1.0F),
                Mth.clamp(matter.herbLoad() + analysis.avgHerb() * 0.05F, 0.0F, 1.0F),
                Mth.clamp(matter.pepperLoad() + analysis.avgPepper() * 0.05F, 0.0F, 1.0F)
        ).withWorkingState(
                Mth.clamp(Math.max(matter.water(), analysis.avgWater()), 0.40F, 1.0F),
                0.02F,
                0.08F,
                0.12F,
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

    private static FoodMatterData shapePulpMatter(FoodMatterData matter, SimulationIngredientAnalysis analysis) {
        return matter.withWorkingState(
                Mth.clamp(analysis.avgWater() * 0.42F, 0.04F, 0.60F),
                0.0F,
                0.48F,
                0.18F,
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
