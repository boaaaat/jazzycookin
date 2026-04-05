package com.boaat.jazzy_cookin.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.boaat.jazzy_cookin.JazzyCookin;
import com.boaat.jazzy_cookin.item.KitchenIngredientItem;
import com.boaat.jazzy_cookin.kitchen.DishEvaluation;
import com.boaat.jazzy_cookin.kitchen.FreshnessBand;
import com.boaat.jazzy_cookin.kitchen.HeatLevel;
import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.IngredientStateData;
import com.boaat.jazzy_cookin.kitchen.KitchenStackUtil;
import com.boaat.jazzy_cookin.kitchen.QualityBreakdown;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.recipe.KitchenPlateRecipe;
import com.boaat.jazzy_cookin.recipe.KitchenProcessRecipe;
import com.boaat.jazzy_cookin.registry.JazzyItems;
import com.boaat.jazzy_cookin.registry.JazzyRecipes;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

@GameTestHolder(JazzyCookin.MODID)
public final class KitchenGameTests {
    private record ProcessResult(ItemStack output, ItemStack byproduct) {
    }

    private KitchenGameTests() {
    }

    @GameTest(template = "empty")
    public static void spoiledInputsFailStateGating(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack slicedApple = stateStack(JazzyItems.ORCHARD_APPLE.get(), IngredientState.SLICED_APPLE, level);
        IngredientStateData spoiledData = KitchenStackUtil.getOrCreateData(slicedApple, level.getGameTime())
                .withCreatedTick(level.getGameTime() - JazzyItems.ORCHARD_APPLE.get().decayTicks() - 20L);
        KitchenStackUtil.setData(slicedApple, spoiledData);

        require(KitchenStackUtil.freshnessBand(slicedApple, level) == FreshnessBand.MOLDY, "Expected moldy sliced apple");

        Optional<KitchenProcessRecipe> recipe = JazzyRecipes.findProcessRecipe(
                level,
                StationType.STOVE,
                paddedInputs(slicedApple, JazzyItems.CANE_SUGAR.get().createStack(1, level.getGameTime()), stateStack(JazzyItems.BAKING_SPICE.get(), IngredientState.GROUND_SPICE, level)),
                new ItemStack(JazzyItems.STOCK_POT.get()),
                HeatLevel.MEDIUM,
                false
        );
        require(recipe.isEmpty(), "Moldy inputs should not match simmering recipes");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cookingMethodsAndOilDegrade(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        ProcessResult boiledEgg = process(
                level,
                StationType.STOVE,
                List.of(JazzyItems.FARM_EGG.get().createStack(1, level.getGameTime())),
                new ItemStack(JazzyItems.STOCK_POT.get()),
                HeatLevel.HIGH,
                false
        );
        require(KitchenStackUtil.getOrCreateData(boiledEgg.output(), level.getGameTime()).state() == IngredientState.BOILED, "Boiling should produce boiled state");

        ProcessResult panFriedFish = process(
                level,
                StationType.STOVE,
                List.of(JazzyItems.CLEANED_FISH.get().createStack(1, level.getGameTime()), JazzyItems.FRYING_OIL.get().createStack(1, level.getGameTime())),
                new ItemStack(JazzyItems.FRYING_SKILLET.get()),
                HeatLevel.MEDIUM,
                false
        );
        require(KitchenStackUtil.getOrCreateData(panFriedFish.output(), level.getGameTime()).state() == IngredientState.PAN_FRIED, "Pan frying should produce pan fried state");

        List<ItemStack> deepFryInputs = List.of(
                JazzyItems.BATTERED_PROTEIN_ITEM.get().createStack(1, level.getGameTime()),
                JazzyItems.FRYING_OIL.get().createStack(1, level.getGameTime())
        );
        Optional<KitchenProcessRecipe> deepFryRecipe = JazzyRecipes.findProcessRecipe(
                level,
                StationType.STOVE,
                paddedInputs(deepFryInputs),
                new ItemStack(JazzyItems.STOCK_POT.get()),
                HeatLevel.HIGH,
                false
        );
        require(deepFryRecipe.isPresent(), "Expected deep fry recipe");

        IngredientStateData hotData = DishEvaluation.evaluateProcess(level, deepFryRecipe.get(), deepFryInputs, new ItemStack(JazzyItems.STOCK_POT.get()), HeatLevel.HIGH, false);
        IngredientStateData coolData = DishEvaluation.evaluateProcess(level, deepFryRecipe.get(), deepFryInputs, new ItemStack(JazzyItems.STOCK_POT.get()), HeatLevel.LOW, false);
        require(hotData.quality() > coolData.quality(), "Correct deep fry heat should outscore low heat");
        require(hotData.recipeAccuracy() > coolData.recipeAccuracy(), "Correct deep fry heat should improve recipe accuracy");

        ProcessResult firstFry = process(level, StationType.STOVE, deepFryInputs, new ItemStack(JazzyItems.STOCK_POT.get()), HeatLevel.HIGH, false);
        require(firstFry.byproduct().is(JazzyItems.USED_OIL.get()), "Fresh oil should degrade to used oil");
        ProcessResult secondFry = process(
                level,
                StationType.STOVE,
                List.of(JazzyItems.BATTERED_PROTEIN_ITEM.get().createStack(1, level.getGameTime()), firstFry.byproduct()),
                new ItemStack(JazzyItems.STOCK_POT.get()),
                HeatLevel.HIGH,
                false
        );
        require(secondFry.byproduct().is(JazzyItems.DIRTY_OIL_ITEM.get()), "Used oil should degrade to dirty oil");
        ProcessResult thirdFry = process(
                level,
                StationType.STOVE,
                List.of(JazzyItems.BATTERED_PROTEIN_ITEM.get().createStack(1, level.getGameTime()), secondFry.byproduct()),
                new ItemStack(JazzyItems.STOCK_POT.get()),
                HeatLevel.HIGH,
                false
        );
        require(thirdFry.byproduct().is(JazzyItems.BURNT_OIL_ITEM.get()), "Dirty oil should degrade to burnt oil");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void preservationRecipesProduceStableOutputs(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        ProcessResult cannedTomato = process(
                level,
                StationType.CANNING_STATION,
                List.of(JazzyItems.CHOPPED_TOMATO.get().createStack(1, level.getGameTime())),
                new ItemStack(JazzyItems.CANNING_JAR.get()),
                HeatLevel.MEDIUM,
                false
        );
        require(KitchenStackUtil.getOrCreateData(cannedTomato.output(), level.getGameTime()).state() == IngredientState.CANNED_TOMATO, "Canning should create canned tomatoes");
        require(KitchenStackUtil.decayTicks(cannedTomato.output()) > KitchenStackUtil.decayTicks(JazzyItems.CHOPPED_TOMATO.get().createStack(1, level.getGameTime())), "Canning should improve storage life");

        ProcessResult driedApple = process(
                level,
                StationType.DRYING_RACK,
                List.of(stateStack(JazzyItems.ORCHARD_APPLE.get(), IngredientState.SLICED_APPLE, level)),
                ItemStack.EMPTY,
                HeatLevel.OFF,
                false
        );
        require(KitchenStackUtil.getOrCreateData(driedApple.output(), level.getGameTime()).state() == IngredientState.DRIED_FRUIT, "Drying should create dried fruit");

        ProcessResult smokedProtein = process(
                level,
                StationType.SMOKER,
                List.of(JazzyItems.ROAST_CUT.get().createStack(1, level.getGameTime())),
                ItemStack.EMPTY,
                HeatLevel.LOW,
                false
        );
        require(KitchenStackUtil.getOrCreateData(smokedProtein.output(), level.getGameTime()).state() == IngredientState.SMOKED_PROTEIN, "Smoking should create smoked protein");

        ProcessResult fermentedDairy = process(
                level,
                StationType.FERMENTATION_CROCK,
                List.of(JazzyItems.FRESH_MILK.get().createStack(1, level.getGameTime())),
                new ItemStack(JazzyItems.CANNING_JAR.get()),
                HeatLevel.OFF,
                false
        );
        require(KitchenStackUtil.getOrCreateData(fermentedDairy.output(), level.getGameTime()).state() == IngredientState.CULTURED_DAIRY, "Fermentation should create cultured dairy");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void applePieChainProducesPlatedSlice(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        ItemStack peeledApple = process(level, StationType.PREP_TABLE, List.of(JazzyItems.ORCHARD_APPLE.get().createStack(1, level.getGameTime())), new ItemStack(JazzyItems.CHEF_KNIFE.get()), HeatLevel.OFF, false).output();
        ItemStack slicedApple = process(level, StationType.PREP_TABLE, List.of(peeledApple), new ItemStack(JazzyItems.CHEF_KNIFE.get()), HeatLevel.OFF, false).output();
        ItemStack groundSpice = process(level, StationType.SPICE_GRINDER, List.of(JazzyItems.BAKING_SPICE.get().createStack(1, level.getGameTime())), ItemStack.EMPTY, HeatLevel.OFF, false).output();
        ItemStack filling = process(
                level,
                StationType.STOVE,
                List.of(slicedApple, JazzyItems.CANE_SUGAR.get().createStack(1, level.getGameTime()), groundSpice),
                new ItemStack(JazzyItems.STOCK_POT.get()),
                HeatLevel.MEDIUM,
                false
        ).output();

        ItemStack crustMix = process(
                level,
                StationType.MIXING_BOWL,
                List.of(
                        JazzyItems.FLOUR.get().createStack(1, level.getGameTime()),
                        JazzyItems.BUTTER.get().createStack(1, level.getGameTime()),
                        JazzyItems.CANE_SUGAR.get().createStack(1, level.getGameTime())
                ),
                new ItemStack(JazzyItems.WHISK.get()),
                HeatLevel.OFF,
                false
        ).output();
        ItemStack dough = process(level, StationType.MIXING_BOWL, List.of(crustMix), new ItemStack(JazzyItems.ROLLING_PIN.get()), HeatLevel.OFF, false).output();
        ItemStack rawPie = process(level, StationType.PREP_TABLE, List.of(dough, filling), new ItemStack(JazzyItems.PIE_TIN.get()), HeatLevel.OFF, false).output();
        ItemStack bakedPie = process(level, StationType.OVEN, List.of(rawPie), new ItemStack(JazzyItems.PIE_TIN.get()), HeatLevel.HIGH, true).output();
        ItemStack cooledPie = process(level, StationType.COOLING_RACK, List.of(bakedPie), ItemStack.EMPTY, HeatLevel.OFF, false).output();
        ItemStack restedPie = process(level, StationType.RESTING_BOARD, List.of(cooledPie), ItemStack.EMPTY, HeatLevel.OFF, false).output();
        ItemStack slicedPie = process(level, StationType.RESTING_BOARD, List.of(restedPie), new ItemStack(JazzyItems.CHEF_KNIFE.get()), HeatLevel.OFF, false).output();
        ItemStack platedSlice = plate(level, List.of(slicedPie, JazzyItems.CERAMIC_PLATE.get().createStack(1, level.getGameTime())));

        require(KitchenStackUtil.getOrCreateData(platedSlice, level.getGameTime()).state() == IngredientState.PLATED_SLICE, "Expected plated pie slice");
        QualityBreakdown breakdown = DishEvaluation.evaluateStack(platedSlice, level);
        require(breakdown.finalScore() > 0.65F, "Full pie chain should produce a strong score");
        helper.succeed();
    }

    private static ProcessResult process(
            ServerLevel level,
            StationType station,
            List<ItemStack> inputs,
            ItemStack tool,
            HeatLevel heat,
            boolean preheated
    ) {
        Optional<KitchenProcessRecipe> recipe = JazzyRecipes.findProcessRecipe(level, station, paddedInputs(inputs), tool, heat, preheated);
        require(recipe.isPresent(), "Missing process recipe for station " + station.getSerializedName());

        IngredientStateData outputData = DishEvaluation.evaluateProcess(level, recipe.get(), inputs, tool, heat, preheated);
        ItemStack output = buildIngredientStack(level, recipe.get().output().result(), outputData);
        ItemStack byproduct = buildByproductStack(level, recipe.get());
        return new ProcessResult(output, byproduct);
    }

    private static ItemStack plate(ServerLevel level, List<ItemStack> inputs) {
        Optional<KitchenPlateRecipe> recipe = JazzyRecipes.findPlateRecipe(level, paddedInputs(inputs));
        require(recipe.isPresent(), "Missing plate recipe");
        IngredientStateData outputData = DishEvaluation.evaluatePlate(level, recipe.get(), inputs);
        return buildIngredientStack(level, recipe.get().output().result(), outputData);
    }

    private static ItemStack buildIngredientStack(ServerLevel level, ItemStack template, IngredientStateData data) {
        if (template.getItem() instanceof KitchenIngredientItem ingredientItem) {
            return ingredientItem.createStack(template.getCount(), level.getGameTime(), data);
        }

        ItemStack copy = template.copy();
        KitchenStackUtil.setData(copy, data);
        return copy;
    }

    private static ItemStack buildByproductStack(ServerLevel level, KitchenProcessRecipe recipe) {
        ItemStack byproduct = recipe.output().byproduct().copy();
        if (byproduct.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (byproduct.getItem() instanceof KitchenIngredientItem ingredientItem) {
            return ingredientItem.createStack(byproduct.getCount(), level.getGameTime());
        }
        return byproduct;
    }

    private static ItemStack stateStack(KitchenIngredientItem item, IngredientState state, ServerLevel level) {
        ItemStack stack = item.createStack(1, level.getGameTime());
        IngredientStateData data = KitchenStackUtil.getOrCreateData(stack, level.getGameTime());
        KitchenStackUtil.setData(stack, data.withState(state));
        return stack;
    }

    private static List<ItemStack> paddedInputs(List<ItemStack> inputs) {
        ArrayList<ItemStack> padded = new ArrayList<>(4);
        padded.addAll(inputs);
        while (padded.size() < 4) {
            padded.add(ItemStack.EMPTY);
        }
        return padded;
    }

    private static List<ItemStack> paddedInputs(ItemStack... inputs) {
        return paddedInputs(List.of(inputs));
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
