package com.boaat.jazzy_cookin.kitchen.sim.domain;

import java.util.function.UnaryOperator;

import com.boaat.jazzy_cookin.item.KitchenIngredientItem;
import com.boaat.jazzy_cookin.kitchen.KitchenMethod;
import com.boaat.jazzy_cookin.kitchen.KitchenStackUtil;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.kitchen.sim.SimulationSnapshot;
import com.boaat.jazzy_cookin.kitchen.sim.action.EggMixingSimulationActions;
import com.boaat.jazzy_cookin.kitchen.sim.FoodTrait;
import com.boaat.jazzy_cookin.kitchen.sim.recognition.DishRecognitionResult;
import com.boaat.jazzy_cookin.kitchen.sim.recognition.DishSchema;
import com.boaat.jazzy_cookin.kitchen.sim.station.StationSimulationAccess;
import com.boaat.jazzy_cookin.registry.JazzyItems;
import com.boaat.jazzy_cookin.registry.JazzyItems.IngredientId;

import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class MixingSimulationDomain implements StationSimulationDomain {
    private record MixOutcome(KitchenMethod method, KitchenIngredientItem outputItem, UnaryOperator<FoodMatterData> adjuster) {
    }

    @Override
    public SimulationDomainType type() {
        return SimulationDomainType.MIX;
    }

    @Override
    public boolean supports(StationSimulationAccess access) {
        return access.simulationStationType() == StationType.MIXING_BOWL && (supportsEggMixing(access) || classify(access) != null);
    }

    @Override
    public KitchenMethod method(StationSimulationAccess access) {
        if (supportsEggMixing(access)) {
            return KitchenMethod.WHISK;
        }
        MixOutcome outcome = classify(access);
        return outcome != null ? outcome.method() : KitchenMethod.NONE;
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
                Math.round(matter.whiskWork() * 50.0F),
                Math.round(matter.water() * 100.0F),
                Math.round(matter.browning() * 100.0F),
                Math.round(matter.charLevel() * 100.0F),
                Math.round(matter.aeration() * 100.0F),
                Math.round(matter.fragmentation() * 100.0F),
                preview != null ? preview.previewId() : 0
        );
    }

    @Override
    public boolean handleAction(StationSimulationAccess access, int buttonId) {
        if (buttonId != 6) {
            return false;
        }
        if (supportsEggMixing(access)) {
            return EggMixingSimulationActions.whisk(access);
        }
        MixOutcome outcome = classify(access);
        if (outcome == null || access.simulationLevel() == null) {
            return false;
        }

        long gameTime = access.simulationLevel().getGameTime();
        SimulationIngredientAnalysis analysis = SimulationIngredientAnalysis.analyzeInputs(access);
        ItemStack output = SimulationOutputFactory.createOutput(outcome.outputItem(), gameTime, analysis, outcome.adjuster());
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

    private static boolean supportsEggMixing(StationSimulationAccess access) {
        if (access.simulationStationType() != StationType.MIXING_BOWL) {
            return false;
        }

        ItemStack output = access.simulationItem(access.outputSlot());
        if (output.is(JazzyItems.EGG_MIXTURE.get())) {
            for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
                ItemStack stack = access.simulationItem(slot);
                if (!stack.isEmpty() && !com.boaat.jazzy_cookin.kitchen.sim.FoodMaterialProfiles.isMixingBowlAddIn(stack)) {
                    return false;
                }
            }
            return true;
        }

        boolean sawEgg = false;
        for (int slot = access.inputStart(); slot <= access.inputEnd(); slot++) {
            ItemStack stack = access.simulationItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(JazzyItems.ingredient(IngredientId.EGGS).get())) {
                sawEgg = true;
                continue;
            }
            if (!com.boaat.jazzy_cookin.kitchen.sim.FoodMaterialProfiles.isMixingBowlAddIn(stack)) {
                return false;
            }
        }
        return sawEgg && access.simulationItem(access.outputSlot()).isEmpty();
    }

    private static MixOutcome classify(StationSimulationAccess access) {
        SimulationIngredientAnalysis analysis = SimulationIngredientAnalysis.analyzeInputs(access);
        if (analysis.isEmpty()) {
            return null;
        }

        if (isGarlicButter(access, analysis)) {
            return new MixOutcome(KitchenMethod.MIX, JazzyItems.GARLIC_BUTTER.get(), matter -> shapeMatter(matter, analysis, access.simulationControlSetting(), KitchenMethod.MIX));
        }
        if (isCheeseSauce(access, analysis)) {
            return new MixOutcome(KitchenMethod.MIX, JazzyItems.CHEESE_SAUCE.get(), matter -> shapeMatter(matter, analysis, access.simulationControlSetting(), KitchenMethod.MIX));
        }
        if (isBrownieBatter(access, analysis)) {
            return new MixOutcome(KitchenMethod.BATTER, JazzyItems.BROWNIE_BATTER.get(), matter -> shapeMatter(matter, analysis, access.simulationControlSetting(), KitchenMethod.BATTER));
        }
        if (isCakeBatter(access, analysis)) {
            return new MixOutcome(KitchenMethod.BATTER, JazzyItems.CAKE_BATTER.get(), matter -> shapeMatter(matter, analysis, access.simulationControlSetting(), KitchenMethod.BATTER));
        }
        if (isPancakeBatter(access, analysis)) {
            return new MixOutcome(KitchenMethod.BATTER, JazzyItems.PANCAKE_BATTER.get(), matter -> shapeMatter(matter, analysis, access.simulationControlSetting(), KitchenMethod.BATTER));
        }
        if (isFocacciaDough(access, analysis)) {
            return new MixOutcome(KitchenMethod.KNEAD, JazzyItems.FOCACCIA_DOUGH.get(), matter -> shapeMatter(matter, analysis, access.simulationControlSetting(), KitchenMethod.KNEAD));
        }
        if (isPieDough(analysis)) {
            return new MixOutcome(KitchenMethod.KNEAD, JazzyItems.PIE_DOUGH.get(), matter -> shapeMatter(matter, analysis, access.simulationControlSetting(), KitchenMethod.KNEAD));
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
        MixOutcome outcome = classify(access);
        if (outcome == null) {
            return null;
        }
        ItemStack preview = SimulationOutputFactory.createOutput(
                outcome.outputItem(),
                access.simulationLevel().getGameTime(),
                SimulationIngredientAnalysis.analyzeInputs(access),
                outcome.adjuster()
        );
        return KitchenStackUtil.getFoodMatter(preview);
    }

    private static FoodMatterData shapeMatter(FoodMatterData matter, SimulationIngredientAnalysis analysis, int controlSetting, KitchenMethod method) {
        float controlFactor = switch (controlSetting) {
            case 0 -> 0.18F;
            case 2 -> 0.44F;
            default -> 0.30F;
        };
        float water = Mth.clamp(matter.water() * 0.70F + analysis.avgWater() * 0.30F, 0.04F, 1.0F);
        float aeration = switch (method) {
            case BATTER -> 0.22F + controlFactor * 0.30F;
            case KNEAD -> 0.04F + controlFactor * 0.08F;
            default -> 0.08F + controlFactor * 0.14F;
        };
        float fragmentation = switch (method) {
            case BATTER -> 0.12F + (1.0F - controlFactor) * 0.10F;
            case KNEAD -> 0.06F + (1.0F - controlFactor) * 0.06F;
            default -> 0.10F + (1.0F - controlFactor) * 0.12F;
        };
        float cohesiveness = switch (method) {
            case KNEAD -> 0.48F + controlFactor * 0.26F;
            case BATTER -> 0.28F + controlFactor * 0.18F;
            default -> 0.36F + controlFactor * 0.12F;
        };
        return matter.withFlavorLoads(
                Mth.clamp(matter.fat() * 0.70F + analysis.avgFat() * 0.30F, 0.0F, 1.0F),
                Mth.clamp(matter.seasoningLoad() + analysis.avgSeasoning() * 0.30F, 0.0F, 1.0F),
                Mth.clamp(matter.cheeseLoad() + analysis.avgCheese() * 0.40F, 0.0F, 1.0F),
                Mth.clamp(matter.onionLoad() + analysis.avgOnion() * 0.40F, 0.0F, 1.0F),
                Mth.clamp(matter.herbLoad() + analysis.avgHerb() * 0.40F, 0.0F, 1.0F),
                Mth.clamp(matter.pepperLoad() + analysis.avgPepper() * 0.40F, 0.0F, 1.0F)
        ).withWorkingState(
                water,
                aeration,
                fragmentation,
                cohesiveness,
                0.0F,
                0.0F,
                0.0F,
                method == KitchenMethod.BATTER ? controlFactor : 0.0F,
                0,
                0,
                0,
                Math.max(1, matter.processDepth() + 1),
                false
        );
    }

    private static boolean isPieDough(SimulationIngredientAnalysis analysis) {
        return analysis.hasTrait(FoodTrait.FLOUR)
                && (analysis.has(JazzyItems.ingredient(IngredientId.BUTTER).get())
                || analysis.has(JazzyItems.ingredient(IngredientId.LARD).get())
                || analysis.has(JazzyItems.ingredient(IngredientId.SHORTENING).get()))
                && !hasYeastInput(analysis);
    }

    private static boolean isFocacciaDough(StationSimulationAccess access, SimulationIngredientAnalysis analysis) {
        return analysis.has(JazzyItems.ingredient(IngredientId.BREAD_FLOUR).get())
                && hasYeastInput(analysis)
                && (analysis.has(JazzyItems.ingredient(IngredientId.OLIVE_OIL).get())
                || analysis.has(JazzyItems.ingredient(IngredientId.VEGETABLE_OIL).get())
                || analysis.has(JazzyItems.ingredient(IngredientId.CANOLA_OIL).get()))
                && !analysis.has(JazzyItems.ingredient(IngredientId.EGGS).get());
    }

    private static boolean isPancakeBatter(StationSimulationAccess access, SimulationIngredientAnalysis analysis) {
        return analysis.hasTrait(FoodTrait.FLOUR)
                && analysis.has(JazzyItems.ingredient(IngredientId.EGGS).get())
                && hasLeavenerInput(analysis)
                && !hasCakeSignal(analysis)
                && !hasChocolateSignal(analysis);
    }

    private static boolean isCakeBatter(StationSimulationAccess access, SimulationIngredientAnalysis analysis) {
        return analysis.has(JazzyItems.ingredient(IngredientId.EGGS).get())
                && (analysis.has(JazzyItems.ingredient(IngredientId.CAKE_FLOUR).get())
                || analysis.has(JazzyItems.CAKE_DRY_MIX.get())
                || hasCakeSignal(analysis))
                && !hasChocolateSignal(analysis);
    }

    private static boolean isBrownieBatter(StationSimulationAccess access, SimulationIngredientAnalysis analysis) {
        return analysis.has(JazzyItems.ingredient(IngredientId.EGGS).get())
                && analysis.hasTrait(FoodTrait.FLOUR)
                && hasChocolateSignal(analysis);
    }

    private static boolean isGarlicButter(StationSimulationAccess access, SimulationIngredientAnalysis analysis) {
        return analysis.has(JazzyItems.ingredient(IngredientId.BUTTER).get())
                && (analysis.has(JazzyItems.ingredient(IngredientId.GARLIC).get())
                || analysis.has(JazzyItems.ingredient(IngredientId.GARLIC_POWDER).get())
                || analysis.has(JazzyItems.ingredient(IngredientId.DRIED_GARLIC).get()))
                && (analysis.has(JazzyItems.ingredient(IngredientId.PARSLEY).get()) || analysis.hasTrait(FoodTrait.HERB));
    }

    private static boolean isCheeseSauce(StationSimulationAccess access, SimulationIngredientAnalysis analysis) {
        return analysis.has(JazzyItems.ingredient(IngredientId.CHEESE).get())
                && (analysis.has(JazzyItems.ingredient(IngredientId.POWDERED_MILK).get())
                || analysis.has(JazzyItems.ingredient(IngredientId.EVAPORATED_MILK).get())
                || analysis.has(JazzyItems.ingredient(IngredientId.SHELF_STABLE_CREAM).get())
                || analysis.has(JazzyItems.ingredient(IngredientId.OAT_MILK).get())
                || analysis.has(JazzyItems.ingredient(IngredientId.ALMOND_MILK).get())
                || analysis.has(JazzyItems.ingredient(IngredientId.SOY_MILK).get()));
    }

    private static boolean hasLeavenerInput(SimulationIngredientAnalysis analysis) {
        return analysis.hasTrait(FoodTrait.LEAVENER)
                || analysis.has(JazzyItems.ingredient(IngredientId.BAKING_POWDER).get())
                || analysis.has(JazzyItems.ingredient(IngredientId.BAKING_SODA).get());
    }

    private static boolean hasYeastInput(SimulationIngredientAnalysis analysis) {
        return analysis.has(JazzyItems.ingredient(IngredientId.ACTIVE_DRY_YEAST).get())
                || analysis.has(JazzyItems.ingredient(IngredientId.INSTANT_YEAST).get())
                || analysis.has(JazzyItems.ingredient(IngredientId.SOURDOUGH_STARTER).get())
                || analysis.has(JazzyItems.ingredient(IngredientId.FERMENTATION_STARTER).get());
    }

    private static boolean hasCakeSignal(SimulationIngredientAnalysis analysis) {
        return analysis.has(JazzyItems.ingredient(IngredientId.CAKE_FLOUR).get())
                || analysis.has(JazzyItems.CAKE_DRY_MIX.get())
                || analysis.has(JazzyItems.ingredient(IngredientId.VANILLA_EXTRACT).get());
    }

    private static boolean hasChocolateSignal(SimulationIngredientAnalysis analysis) {
        return analysis.has(JazzyItems.ingredient(IngredientId.COCOA_POWDER).get())
                || analysis.has(JazzyItems.ingredient(IngredientId.CHOCOLATE_CHIPS).get());
    }
}
