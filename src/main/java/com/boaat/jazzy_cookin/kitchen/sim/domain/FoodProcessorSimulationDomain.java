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

public final class FoodProcessorSimulationDomain implements StationSimulationDomain {
    @Override
    public SimulationDomainType type() {
        return SimulationDomainType.PROCESS;
    }

    @Override
    public boolean supports(StationSimulationAccess access) {
        return access.simulationStationType() == StationType.FOOD_PROCESSOR && classify(access) != null;
    }

    @Override
    public KitchenMethod method(StationSimulationAccess access) {
        return KitchenMethod.PROCESS;
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
        ProcessorOutcome outcome = classify(access);
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

    private record ProcessorOutcome(KitchenIngredientItem output) {
    }

    private static ProcessorOutcome classify(StationSimulationAccess access) {
        SimulationIngredientAnalysis analysis = SimulationIngredientAnalysis.analyzeInputs(access);
        if (analysis.isEmpty()) {
            return null;
        }
        if (analysis.hasTrait(FoodTrait.NUT) && !analysis.hasTrait(FoodTrait.BREAD) && !analysis.hasTrait(FoodTrait.LEGUME)) {
            return new ProcessorOutcome(JazzyItems.NUT_BUTTER.get());
        }
        if (analysis.hasTrait(FoodTrait.BREAD) && analysis.totalItems() == analysis.traitCount(FoodTrait.BREAD)) {
            return new ProcessorOutcome(JazzyItems.PACKED_BREADCRUMBS.get());
        }
        if (analysis.has(JazzyItems.ingredient(IngredientId.CHICKPEAS).get())
                && (analysis.has(JazzyItems.ingredient(IngredientId.GARLIC).get())
                || analysis.has(JazzyItems.ingredient(IngredientId.GARLIC_POWDER).get())
                || analysis.has(JazzyItems.ingredient(IngredientId.DRIED_GARLIC).get()))
                && (analysis.hasTrait(FoodTrait.ACIDIC) || analysis.has(JazzyItems.ingredient(IngredientId.LEMONS).get()))
                && (analysis.hasTrait(FoodTrait.FAT) || analysis.hasTrait(FoodTrait.OIL))) {
            return new ProcessorOutcome(JazzyItems.HUMMUS_PREP.get());
        }
        if ((analysis.hasTrait(FoodTrait.FRUIT) || analysis.hasTrait(FoodTrait.VEGETABLE))
                && !analysis.hasTrait(FoodTrait.PROTEIN)
                && !analysis.hasTrait(FoodTrait.DAIRY)
                && !analysis.hasTrait(FoodTrait.BREAD)) {
            return new ProcessorOutcome(JazzyItems.CHOPPED_PRODUCE_BLEND.get());
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
        ProcessorOutcome outcome = classify(access);
        if (outcome == null) {
            return null;
        }
        ItemStack preview = SimulationOutputFactory.createOutput(
                outcome.output(),
                access.simulationLevel().getGameTime(),
                SimulationIngredientAnalysis.analyzeInputs(access),
                matter -> shapeMatter(matter, SimulationIngredientAnalysis.analyzeInputs(access), access.simulationControlSetting())
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
                Mth.clamp(matter.seasoningLoad() + analysis.avgSeasoning() * 0.40F, 0.0F, 1.0F),
                Mth.clamp(matter.cheeseLoad() + analysis.avgCheese() * 0.40F, 0.0F, 1.0F),
                Mth.clamp(matter.onionLoad() + analysis.avgOnion() * 0.40F, 0.0F, 1.0F),
                Mth.clamp(matter.herbLoad() + analysis.avgHerb() * 0.40F, 0.0F, 1.0F),
                Mth.clamp(matter.pepperLoad() + analysis.avgPepper() * 0.40F, 0.0F, 1.0F)
        ).withWorkingState(
                Mth.clamp(matter.water() * 0.75F + analysis.avgWater() * 0.25F, 0.02F, 1.0F),
                0.04F + controlFactor * 0.08F,
                0.42F - controlFactor * 0.18F,
                0.20F + controlFactor * 0.24F,
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
