package com.boaat.jazzy_cookin.gametest;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.boaat.jazzy_cookin.JazzyCookin;
import com.boaat.jazzy_cookin.block.entity.KitchenStationBlockEntity;
import com.boaat.jazzy_cookin.block.entity.KitchenStorageBlockEntity;
import com.boaat.jazzy_cookin.item.KitchenIngredientItem;
import com.boaat.jazzy_cookin.kitchen.DishEvaluation;
import com.boaat.jazzy_cookin.kitchen.FreshnessBand;
import com.boaat.jazzy_cookin.kitchen.HeatLevel;
import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.IngredientStateData;
import com.boaat.jazzy_cookin.kitchen.KitchenOutcomeBand;
import com.boaat.jazzy_cookin.kitchen.KitchenStackUtil;
import com.boaat.jazzy_cookin.kitchen.QualityBreakdown;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.recipe.KitchenPlateRecipe;
import com.boaat.jazzy_cookin.recipe.KitchenProcessOutput;
import com.boaat.jazzy_cookin.recipe.KitchenProcessRecipe;
import com.boaat.jazzy_cookin.registry.JazzyBlocks;
import com.boaat.jazzy_cookin.registry.JazzyItems;
import com.boaat.jazzy_cookin.registry.JazzyRecipes;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;

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
    public static void toolHeatAndPreheatGateRecipeMatching(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        Optional<KitchenProcessRecipe> wrongKnife = JazzyRecipes.findProcessRecipe(
                level,
                StationType.PREP_TABLE,
                paddedInputs(stateStack(JazzyItems.ORCHARD_APPLE.get(), IngredientState.PEELED_APPLE, level)),
                new ItemStack(JazzyItems.CLEAVER.get()),
                HeatLevel.OFF,
                false
        );
        require(wrongKnife.isEmpty(), "Slice apple should reject disallowed tools");

        Optional<KitchenProcessRecipe> wrongHeat = JazzyRecipes.findProcessRecipe(
                level,
                StationType.STOVE,
                paddedInputs(
                        JazzyItems.BATTERED_PROTEIN_ITEM.get().createStack(1, level.getGameTime()),
                        JazzyItems.FRYING_OIL.get().createStack(1, level.getGameTime())
                ),
                new ItemStack(JazzyItems.STOCK_POT.get()),
                HeatLevel.LOW,
                false
        );
        require(wrongHeat.isEmpty(), "Deep frying should reject low heat");

        ItemStack rawPie = stateStack(JazzyItems.APPLE_PIE.get(), IngredientState.RAW_ASSEMBLED_PIE, level);
        Optional<KitchenProcessRecipe> coldOven = JazzyRecipes.findProcessRecipe(
                level,
                StationType.OVEN,
                paddedInputs(rawPie),
                new ItemStack(JazzyItems.PIE_TIN.get()),
                HeatLevel.HIGH,
                false
        );
        require(coldOven.isEmpty(), "Bake pie should require a preheated oven");

        Optional<KitchenProcessRecipe> hotOven = JazzyRecipes.findProcessRecipe(
                level,
                StationType.OVEN,
                paddedInputs(rawPie),
                new ItemStack(JazzyItems.PIE_TIN.get()),
                HeatLevel.HIGH,
                true
        );
        require(hotOven.isPresent(), "Bake pie should match once the oven is preheated");
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

        IngredientStateData hotData = DishEvaluation.evaluateProcess(
                level,
                deepFryRecipe.get(),
                deepFryRecipe.get().output(),
                deepFryInputs,
                new ItemStack(JazzyItems.STOCK_POT.get()),
                HeatLevel.HIGH,
                false
        );
        IngredientStateData warmData = DishEvaluation.evaluateProcess(
                level,
                deepFryRecipe.get(),
                deepFryRecipe.get().output(),
                deepFryInputs,
                new ItemStack(JazzyItems.STOCK_POT.get()),
                HeatLevel.MEDIUM,
                false
        );
        require(hotData.quality() > warmData.quality(), "Preferred deep fry heat should outscore acceptable off-heat cooking");
        require(hotData.flavor() > warmData.flavor(), "Preferred deep fry heat should improve flavor development");

        ProcessResult firstFry = process(level, StationType.STOVE, deepFryInputs, new ItemStack(JazzyItems.STOCK_POT.get()), HeatLevel.HIGH, false);
        require(firstFry.byproduct().is(JazzyItems.USED_OIL_ITEM.get()), "Fresh oil should degrade to used oil");
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
    public static void passiveStationsEnforceDurationAndEnvironment(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos stationPos = helper.absolutePos(new BlockPos(1, 2, 1));
        level.setBlock(stationPos, JazzyBlocks.STEAMER.get().defaultBlockState(), Block.UPDATE_ALL);

        KitchenStationBlockEntity steamer = blockEntity(level, stationPos, KitchenStationBlockEntity.class);
        steamer.setItem(0, JazzyItems.RAW_DUMPLINGS_ITEM.get().createStack(1, level.getGameTime()));
        steamer.setItem(KitchenStationBlockEntity.TOOL_SLOT, new ItemStack(JazzyItems.STEAMER_BASKET.get()));
        steamer.handleButton(2, null);

        require(!steamer.handleButton(0, null), "Steamer should refuse to start without nearby water");

        level.setBlock(stationPos.east(), Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
        require(steamer.handleButton(0, null), "Steamer should start once nearby water is present");

        int maxProgress = steamer.dataAccess().get(1);
        require(maxProgress > 80, "Passive recipes should expose a long duration");

        for (int tick = 0; tick < maxProgress - 1; tick++) {
            KitchenStationBlockEntity.serverTick(level, stationPos, level.getBlockState(stationPos), steamer);
        }
        require(steamer.getItem(KitchenStationBlockEntity.OUTPUT_SLOT).isEmpty(), "Passive station should not finish early");

        KitchenStationBlockEntity.serverTick(level, stationPos, level.getBlockState(stationPos), steamer);
        require(KitchenStackUtil.getOrCreateData(steamer.getItem(KitchenStationBlockEntity.OUTPUT_SLOT), level.getGameTime()).state() == IngredientState.STEAMED_DUMPLINGS,
                "Passive station should finish on its final tick");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void toolWearAndStorageAdjustmentsApply(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        BlockPos prepPos = helper.absolutePos(new BlockPos(1, 2, 1));
        level.setBlock(prepPos, JazzyBlocks.PREP_TABLE.get().defaultBlockState(), Block.UPDATE_ALL);
        KitchenStationBlockEntity prepTable = blockEntity(level, prepPos, KitchenStationBlockEntity.class);
        prepTable.setItem(0, JazzyItems.ORCHARD_APPLE.get().createStack(1, level.getGameTime()));
        prepTable.setItem(KitchenStationBlockEntity.TOOL_SLOT, new ItemStack(JazzyItems.PARING_KNIFE.get()));
        int beforeDamage = prepTable.getItem(KitchenStationBlockEntity.TOOL_SLOT).getDamageValue();
        require(prepTable.handleButton(0, null), "Prep table should start slicing");
        runStation(level, prepPos, prepTable);
        require(prepTable.getItem(KitchenStationBlockEntity.TOOL_SLOT).getDamageValue() == beforeDamage + 1, "Successful station use should damage the tool");

        BlockPos pantryPos = helper.absolutePos(new BlockPos(3, 2, 1));
        BlockPos cellarPos = helper.absolutePos(new BlockPos(5, 2, 1));
        level.setBlock(pantryPos, JazzyBlocks.PANTRY.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(cellarPos, JazzyBlocks.CELLAR.get().defaultBlockState(), Block.UPDATE_ALL);

        KitchenStorageBlockEntity pantry = blockEntity(level, pantryPos, KitchenStorageBlockEntity.class);
        KitchenStorageBlockEntity cellar = blockEntity(level, cellarPos, KitchenStorageBlockEntity.class);
        ItemStack pantryStored = JazzyItems.TOMATO.get().createStack(1, level.getGameTime());
        ItemStack cellarStored = JazzyItems.TOMATO.get().createStack(1, level.getGameTime());
        IngredientStateData pantryStoredData = KitchenStackUtil.getOrCreateData(pantryStored, level.getGameTime());
        IngredientStateData cellarStoredData = KitchenStackUtil.getOrCreateData(cellarStored, level.getGameTime());
        KitchenStackUtil.setData(pantryStored, pantryStoredData.withCreatedTick(level.getGameTime() - 400L));
        KitchenStackUtil.setData(cellarStored, cellarStoredData.withCreatedTick(level.getGameTime() - 400L));
        pantry.setItem(0, pantryStored);
        cellar.setItem(0, cellarStored);
        long storedSince = Math.max(1L, level.getGameTime() - 10L);
        setInsertedAt(pantry, 0, storedSince);
        setInsertedAt(cellar, 0, storedSince);

        ItemStack pantryTomato = pantry.removeItem(0, 1);
        ItemStack cellarTomato = cellar.removeItem(0, 1);
        IngredientStateData pantryData = KitchenStackUtil.getOrCreateData(pantryTomato, level.getGameTime());
        IngredientStateData cellarData = KitchenStackUtil.getOrCreateData(cellarTomato, level.getGameTime());
        float pantryFreshness = KitchenStackUtil.currentFreshnessScore(pantryTomato, level);
        float cellarFreshness = KitchenStackUtil.currentFreshnessScore(cellarTomato, level);
        require(cellarData != null && pantryData != null && cellarData.createdTick() > pantryData.createdTick(),
                "Cellar storage should reduce effective ingredient age more than pantry storage");
        require(cellarFreshness > pantryFreshness,
                "Cellar storage should preserve freshness better than pantry storage");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void preservationRecipesRequireMediaAndCooling(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        Optional<KitchenProcessRecipe> missingBrine = JazzyRecipes.findProcessRecipe(
                level,
                StationType.CANNING_STATION,
                paddedInputs(JazzyItems.CHOPPED_TOMATO.get().createStack(1, level.getGameTime())),
                new ItemStack(JazzyItems.CANNING_JAR.get()),
                HeatLevel.MEDIUM,
                false
        );
        require(missingBrine.isEmpty(), "Canning tomatoes should require a preserving medium");

        ItemStack brine = process(
                level,
                StationType.MIXING_BOWL,
                List.of(JazzyItems.SALT.get().createStack(1, level.getGameTime())),
                new ItemStack(JazzyItems.WHISK.get()),
                HeatLevel.OFF,
                false
        ).output();
        ItemStack hotTomatoes = process(
                level,
                StationType.CANNING_STATION,
                List.of(JazzyItems.CHOPPED_TOMATO.get().createStack(1, level.getGameTime()), brine),
                new ItemStack(JazzyItems.CANNING_JAR.get()),
                HeatLevel.MEDIUM,
                false
        ).output();
        require(KitchenStackUtil.getOrCreateData(hotTomatoes, level.getGameTime()).state() == IngredientState.HOT_PRESERVE, "Canning should create a hot preserve that still needs cooling");

        ItemStack cannedTomato = process(level, StationType.COOLING_RACK, List.of(hotTomatoes), ItemStack.EMPTY, HeatLevel.OFF, false).output();
        require(KitchenStackUtil.getOrCreateData(cannedTomato, level.getGameTime()).state() == IngredientState.CANNED_TOMATO, "Cooling should finalize canned tomatoes");
        require(KitchenStackUtil.decayTicks(cannedTomato) > KitchenStackUtil.decayTicks(JazzyItems.CHOPPED_TOMATO.get().createStack(1, level.getGameTime())),
                "Final canned tomatoes should store longer than chopped tomatoes");

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
    public static void alternateOutcomeStatesGateRecipes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        ItemStack roughApple = process(
                level,
                StationType.PREP_TABLE,
                List.of(stateStack(JazzyItems.ORCHARD_APPLE.get(), IngredientState.PEELED_APPLE, level)),
                new ItemStack(JazzyItems.PARING_KNIFE.get()),
                HeatLevel.OFF,
                false,
                KitchenOutcomeBand.UNDER
        ).output();
        require(KitchenStackUtil.getOrCreateData(roughApple, level.getGameTime()).state() == IngredientState.ROUGH_CUT, "Under-controlled slicing should create a rough-cut apple");

        Optional<KitchenProcessRecipe> badFilling = JazzyRecipes.findProcessRecipe(
                level,
                StationType.STOVE,
                paddedInputs(
                        roughApple,
                        JazzyItems.CANE_SUGAR.get().createStack(1, level.getGameTime()),
                        stateStack(JazzyItems.BAKING_SPICE.get(), IngredientState.GROUND_SPICE, level)
                ),
                new ItemStack(JazzyItems.STOCK_POT.get()),
                HeatLevel.MEDIUM,
                false
        );
        require(badFilling.isEmpty(), "Apple filling should reject the wrong prep outcome");

        ItemStack eggWash = process(
                level,
                StationType.MIXING_BOWL,
                List.of(JazzyItems.FARM_EGG.get().createStack(1, level.getGameTime())),
                new ItemStack(JazzyItems.WHISK.get()),
                HeatLevel.OFF,
                false,
                KitchenOutcomeBand.UNDER
        ).output();
        require(KitchenStackUtil.getOrCreateData(eggWash, level.getGameTime()).state() == IngredientState.UNDERWHISKED, "Under-whisked egg wash should preserve its state");

        Optional<KitchenProcessRecipe> badAssembly = JazzyRecipes.findProcessRecipe(
                level,
                StationType.PREP_TABLE,
                paddedInputs(
                        stateStack(JazzyItems.PIE_CRUST.get(), IngredientState.DEVELOPED_DOUGH, level),
                        stateStack(JazzyItems.PIE_FILLING.get(), IngredientState.SIMMERED_FILLING, level),
                        eggWash
                ),
                new ItemStack(JazzyItems.PIE_TIN.get()),
                HeatLevel.OFF,
                false
        );
        require(badAssembly.isEmpty(), "Pie assembly should require correctly whisked egg wash");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void applePieChainProducesPlatedSlice(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        ItemStack peeledApple = process(level, StationType.PREP_TABLE, List.of(JazzyItems.ORCHARD_APPLE.get().createStack(1, level.getGameTime())), new ItemStack(JazzyItems.PARING_KNIFE.get()), HeatLevel.OFF, false).output();
        ItemStack slicedApple = process(level, StationType.PREP_TABLE, List.of(peeledApple), new ItemStack(JazzyItems.PARING_KNIFE.get()), HeatLevel.OFF, false).output();
        ItemStack groundSpice = process(level, StationType.SPICE_GRINDER, List.of(JazzyItems.BAKING_SPICE.get().createStack(1, level.getGameTime())), new ItemStack(JazzyItems.MORTAR_PESTLE.get()), HeatLevel.OFF, false).output();
        ItemStack filling = process(
                level,
                StationType.STOVE,
                List.of(slicedApple, JazzyItems.CANE_SUGAR.get().createStack(1, level.getGameTime()), groundSpice),
                new ItemStack(JazzyItems.STOCK_POT.get()),
                HeatLevel.MEDIUM,
                false
        ).output();
        ItemStack eggWash = process(level, StationType.MIXING_BOWL, List.of(JazzyItems.FARM_EGG.get().createStack(1, level.getGameTime())), new ItemStack(JazzyItems.WHISK.get()), HeatLevel.OFF, false).output();

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
        ItemStack rawPie = process(level, StationType.PREP_TABLE, List.of(dough, filling, eggWash), new ItemStack(JazzyItems.PIE_TIN.get()), HeatLevel.OFF, false).output();
        ItemStack bakedPie = process(level, StationType.OVEN, List.of(rawPie), new ItemStack(JazzyItems.PIE_TIN.get()), HeatLevel.HIGH, true).output();
        ItemStack cooledPie = process(level, StationType.COOLING_RACK, List.of(bakedPie), ItemStack.EMPTY, HeatLevel.OFF, false).output();
        ItemStack restedPie = process(level, StationType.RESTING_BOARD, List.of(cooledPie), ItemStack.EMPTY, HeatLevel.OFF, false).output();
        ItemStack slicedPie = process(level, StationType.PREP_TABLE, List.of(restedPie), new ItemStack(JazzyItems.CHEF_KNIFE.get()), HeatLevel.OFF, false).output();
        ItemStack platedSlice = plate(level, List.of(slicedPie, JazzyItems.CERAMIC_PLATE.get().createStack(1, level.getGameTime())));

        require(KitchenStackUtil.getOrCreateData(platedSlice, level.getGameTime()).state() == IngredientState.PLATED_SLICE, "Expected plated pie slice");
        QualityBreakdown breakdown = DishEvaluation.evaluateStack(platedSlice, level);
        require(breakdown.finalScore() > 0.65F, "Full pie chain should produce a strong score");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tomatoSoupAndBreadChainProducesMeal(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        ItemStack choppedTomato = process(level, StationType.PREP_TABLE, List.of(JazzyItems.TOMATO.get().createStack(1, level.getGameTime())), new ItemStack(JazzyItems.CHEF_KNIFE.get()), HeatLevel.OFF, false).output();
        ItemStack dicedOnion = process(level, StationType.PREP_TABLE, List.of(JazzyItems.ONION.get().createStack(1, level.getGameTime())), new ItemStack(JazzyItems.CHEF_KNIFE.get()), HeatLevel.OFF, false).output();
        ItemStack groundHerb = process(level, StationType.SPICE_GRINDER, List.of(JazzyItems.FRESH_HERB.get().createStack(1, level.getGameTime())), new ItemStack(JazzyItems.MORTAR_PESTLE.get()), HeatLevel.OFF, false).output();
        ItemStack soupBase = process(level, StationType.STOVE, List.of(choppedTomato, dicedOnion, groundHerb), new ItemStack(JazzyItems.STOCK_POT.get()), HeatLevel.LOW, false).output();
        ItemStack soup = process(level, StationType.STRAINER, List.of(soupBase), new ItemStack(JazzyItems.FINE_STRAINER.get()), HeatLevel.OFF, false).output();

        ItemStack breadMix = process(level, StationType.MIXING_BOWL, List.of(JazzyItems.FLOUR.get().createStack(1, level.getGameTime()), JazzyItems.FRESH_MILK.get().createStack(1, level.getGameTime())), new ItemStack(JazzyItems.WHISK.get()), HeatLevel.OFF, false).output();
        ItemStack breadDough = process(level, StationType.MIXING_BOWL, List.of(breadMix), new ItemStack(JazzyItems.ROLLING_PIN.get()), HeatLevel.OFF, false).output();
        ItemStack bakedBread = process(level, StationType.OVEN, List.of(breadDough), ItemStack.EMPTY, HeatLevel.MEDIUM, true).output();
        ItemStack restedBread = process(level, StationType.RESTING_BOARD, List.of(bakedBread), ItemStack.EMPTY, HeatLevel.OFF, false).output();
        ItemStack slicedBread = process(level, StationType.PREP_TABLE, List.of(restedBread), new ItemStack(JazzyItems.CHEF_KNIFE.get()), HeatLevel.OFF, false).output();
        ItemStack platedMeal = plate(level, List.of(soup, slicedBread, JazzyItems.CERAMIC_PLATE.get().createStack(1, level.getGameTime())));

        require(KitchenStackUtil.getOrCreateData(platedMeal, level.getGameTime()).state() == IngredientState.PLATED_SOUP_MEAL, "Expected plated soup meal");
        require(DishEvaluation.evaluateStack(platedMeal, level).finalScore() > 0.60F, "Soup and bread chain should grade well");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dumplingChainProducesMeal(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        ItemStack choppedCabbage = process(level, StationType.PREP_TABLE, List.of(JazzyItems.CABBAGE.get().createStack(1, level.getGameTime())), new ItemStack(JazzyItems.CHEF_KNIFE.get()), HeatLevel.OFF, false).output();
        ItemStack dicedOnion = process(level, StationType.PREP_TABLE, List.of(JazzyItems.ONION.get().createStack(1, level.getGameTime())), new ItemStack(JazzyItems.CHEF_KNIFE.get()), HeatLevel.OFF, false).output();
        ItemStack groundHerb = process(level, StationType.SPICE_GRINDER, List.of(JazzyItems.FRESH_HERB.get().createStack(1, level.getGameTime())), new ItemStack(JazzyItems.MORTAR_PESTLE.get()), HeatLevel.OFF, false).output();
        ItemStack filling = process(level, StationType.MIXING_BOWL, List.of(choppedCabbage, dicedOnion, groundHerb), ItemStack.EMPTY, HeatLevel.OFF, false).output();
        ItemStack doughMix = process(
                level,
                StationType.MIXING_BOWL,
                List.of(
                        JazzyItems.FLOUR.get().createStack(1, level.getGameTime()),
                        JazzyItems.SALT.get().createStack(1, level.getGameTime()),
                        JazzyItems.FARM_EGG.get().createStack(1, level.getGameTime())
                ),
                new ItemStack(JazzyItems.WHISK.get()),
                HeatLevel.OFF,
                false
        ).output();
        ItemStack dough = process(level, StationType.MIXING_BOWL, List.of(doughMix), new ItemStack(JazzyItems.ROLLING_PIN.get()), HeatLevel.OFF, false).output();
        ItemStack rawDumplings = process(level, StationType.PREP_TABLE, List.of(filling, dough), new ItemStack(JazzyItems.ROLLING_PIN.get()), HeatLevel.OFF, false).output();
        ItemStack steamedDumplings = process(level, StationType.STEAMER, List.of(rawDumplings), new ItemStack(JazzyItems.STEAMER_BASKET.get()), HeatLevel.MEDIUM, false).output();
        ItemStack platedMeal = plate(level, List.of(steamedDumplings, JazzyItems.CERAMIC_PLATE.get().createStack(1, level.getGameTime())));

        require(KitchenStackUtil.getOrCreateData(platedMeal, level.getGameTime()).state() == IngredientState.PLATED_DUMPLING_MEAL, "Expected plated dumpling meal");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void friedMealChainRequiresRestedProtein(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        ItemStack herb = process(level, StationType.SPICE_GRINDER, List.of(JazzyItems.FRESH_HERB.get().createStack(1, level.getGameTime())), new ItemStack(JazzyItems.MORTAR_PESTLE.get()), HeatLevel.OFF, false).output();
        ItemStack spice = process(level, StationType.SPICE_GRINDER, List.of(JazzyItems.BAKING_SPICE.get().createStack(1, level.getGameTime())), new ItemStack(JazzyItems.MORTAR_PESTLE.get()), HeatLevel.OFF, false).output();
        ItemStack marinade = process(level, StationType.MIXING_BOWL, List.of(herb, spice, JazzyItems.FRESH_MILK.get().createStack(1, level.getGameTime())), new ItemStack(JazzyItems.WHISK.get()), HeatLevel.OFF, false).output();
        ItemStack marinated = process(level, StationType.FERMENTATION_CROCK, List.of(JazzyItems.RAW_PROTEIN.get().createStack(1, level.getGameTime()), marinade), new ItemStack(JazzyItems.CANNING_JAR.get()), HeatLevel.OFF, false).output();
        ItemStack batter = process(level, StationType.MIXING_BOWL, List.of(JazzyItems.FLOUR.get().createStack(1, level.getGameTime()), JazzyItems.FARM_EGG.get().createStack(1, level.getGameTime())), new ItemStack(JazzyItems.WHISK.get()), HeatLevel.OFF, false).output();
        ItemStack coated = process(level, StationType.PREP_TABLE, List.of(marinated, batter), ItemStack.EMPTY, HeatLevel.OFF, false).output();
        ItemStack fried = process(level, StationType.STOVE, List.of(coated, JazzyItems.FRYING_OIL.get().createStack(1, level.getGameTime())), new ItemStack(JazzyItems.STOCK_POT.get()), HeatLevel.HIGH, false).output();
        ItemStack roastVegetables = process(level, StationType.OVEN, List.of(JazzyItems.TOMATO.get().createStack(1, level.getGameTime()), JazzyItems.ONION.get().createStack(1, level.getGameTime())), ItemStack.EMPTY, HeatLevel.HIGH, true).output();

        Optional<KitchenPlateRecipe> skippedRest = JazzyRecipes.findPlateRecipe(level, paddedInputs(fried, roastVegetables, JazzyItems.CERAMIC_PLATE.get().createStack(1, level.getGameTime())));
        require(skippedRest.isEmpty(), "Fried meal plating should reject un-rested protein");

        ItemStack rested = process(level, StationType.RESTING_BOARD, List.of(fried), ItemStack.EMPTY, HeatLevel.OFF, false).output();
        ItemStack platedMeal = plate(level, List.of(rested, roastVegetables, JazzyItems.CERAMIC_PLATE.get().createStack(1, level.getGameTime())));
        require(KitchenStackUtil.getOrCreateData(platedMeal, level.getGameTime()).state() == IngredientState.PLATED_FRIED_MEAL, "Expected plated fried meal");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void roastMealChainRequiresRestedProtein(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        ItemStack roastCut = process(level, StationType.PREP_TABLE, List.of(JazzyItems.RAW_PROTEIN.get().createStack(1, level.getGameTime())), new ItemStack(JazzyItems.CLEAVER.get()), HeatLevel.OFF, false).output();
        ItemStack roasted = process(level, StationType.OVEN, List.of(roastCut), ItemStack.EMPTY, HeatLevel.MEDIUM, true).output();
        ItemStack broiled = process(level, StationType.OVEN, List.of(roasted), ItemStack.EMPTY, HeatLevel.HIGH, true).output();
        ItemStack roastVegetables = process(level, StationType.OVEN, List.of(JazzyItems.TOMATO.get().createStack(1, level.getGameTime()), JazzyItems.ONION.get().createStack(1, level.getGameTime())), ItemStack.EMPTY, HeatLevel.HIGH, true).output();

        Optional<KitchenPlateRecipe> skippedRest = JazzyRecipes.findPlateRecipe(level, paddedInputs(broiled, roastVegetables, JazzyItems.CERAMIC_PLATE.get().createStack(1, level.getGameTime())));
        require(skippedRest.isEmpty(), "Roast meal plating should reject un-rested broiled protein");

        ItemStack rested = process(level, StationType.RESTING_BOARD, List.of(broiled), ItemStack.EMPTY, HeatLevel.OFF, false).output();
        ItemStack platedMeal = plate(level, List.of(rested, roastVegetables, JazzyItems.CERAMIC_PLATE.get().createStack(1, level.getGameTime())));
        require(KitchenStackUtil.getOrCreateData(platedMeal, level.getGameTime()).state() == IngredientState.PLATED_ROAST_MEAL, "Expected plated roast meal");
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
        return process(level, station, inputs, tool, heat, preheated, KitchenOutcomeBand.IDEAL);
    }

    private static ProcessResult process(
            ServerLevel level,
            StationType station,
            List<ItemStack> inputs,
            ItemStack tool,
            HeatLevel heat,
            boolean preheated,
            KitchenOutcomeBand outcomeBand
    ) {
        Optional<KitchenProcessRecipe> recipe = JazzyRecipes.findProcessRecipe(level, station, paddedInputs(inputs), tool, heat, preheated);
        require(recipe.isPresent(), "Missing process recipe for station " + station.getSerializedName());

        KitchenProcessOutput resolvedOutput = recipe.get().outputForBand(outcomeBand);
        IngredientStateData outputData = DishEvaluation.evaluateProcess(level, recipe.get(), resolvedOutput, inputs, tool, heat, preheated);
        ItemStack output = buildIngredientStack(level, resolvedOutput.result(), outputData);
        ItemStack byproduct = buildByproductStack(level, resolvedOutput);
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

    private static ItemStack buildByproductStack(ServerLevel level, KitchenProcessOutput output) {
        ItemStack byproduct = output.byproduct().copy();
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

    private static void runStation(ServerLevel level, BlockPos pos, KitchenStationBlockEntity station) {
        int maxProgress = station.dataAccess().get(1);
        for (int tick = 0; tick < maxProgress; tick++) {
            KitchenStationBlockEntity.serverTick(level, pos, level.getBlockState(pos), station);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T blockEntity(ServerLevel level, BlockPos pos, Class<T> type) {
        Object blockEntity = level.getBlockEntity(pos);
        require(type.isInstance(blockEntity), "Expected block entity " + type.getSimpleName());
        return (T) blockEntity;
    }

    private static void setInsertedAt(KitchenStorageBlockEntity storage, int slot, long value) {
        try {
            Field field = KitchenStorageBlockEntity.class.getDeclaredField("insertedAt");
            field.setAccessible(true);
            long[] insertedAt = (long[]) field.get(storage);
            insertedAt[slot] = value;
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
