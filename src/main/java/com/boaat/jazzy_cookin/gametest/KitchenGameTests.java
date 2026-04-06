package com.boaat.jazzy_cookin.gametest;

import com.boaat.jazzy_cookin.JazzyCookin;
import com.boaat.jazzy_cookin.block.entity.KitchenStationBlockEntity;
import com.boaat.jazzy_cookin.block.entity.KitchenStorageBlockEntity;
import com.boaat.jazzy_cookin.kitchen.DishEvaluation;
import com.boaat.jazzy_cookin.kitchen.FreshnessBand;
import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.IngredientStateData;
import com.boaat.jazzy_cookin.kitchen.KitchenStackUtil;
import com.boaat.jazzy_cookin.kitchen.PantrySortTab;
import com.boaat.jazzy_cookin.kitchen.QualityBreakdown;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.StorageRules;
import com.boaat.jazzy_cookin.kitchen.StorageType;
import com.boaat.jazzy_cookin.kitchen.ToolProfile;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMaterialProfiles;
import com.boaat.jazzy_cookin.kitchen.sim.FoodTrait;
import com.boaat.jazzy_cookin.kitchen.sim.recognition.DishRecognitionResult;
import com.boaat.jazzy_cookin.kitchen.sim.recognition.DishSchema;
import com.boaat.jazzy_cookin.menu.KitchenStorageMenu;
import com.boaat.jazzy_cookin.registry.JazzyBlocks;
import com.boaat.jazzy_cookin.registry.JazzyDataComponents;
import com.boaat.jazzy_cookin.registry.JazzyItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;

@GameTestHolder(JazzyCookin.MODID)
public final class KitchenGameTests {
    private KitchenGameTests() {
    }

    @GameTest(template = "empty")
    public static void creativeStacksStartMaxedAndFresh(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack creativeApples = JazzyItems.ingredient(JazzyItems.IngredientId.APPLES).get().createCreativeStack(1);
        IngredientStateData data = KitchenStackUtil.getOrCreateData(creativeApples, level.getGameTime());

        require(data != null, "Creative ingredient stack should carry state data");
        require(KitchenStackUtil.getFoodMatter(creativeApples) != null, "Creative ingredient stack should initialize FOOD_MATTER");
        require(data.quality() == 1.0F, "Creative ingredient stack should have max quality");
        require(data.recipeAccuracy() == 1.0F, "Creative ingredient stack should have max recipe accuracy");
        require(KitchenStackUtil.freshnessBand(creativeApples, level) == FreshnessBand.FRESH, "Creative ingredient stack should stay fresh");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mealStacksStartCanonicalAndFinalized(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack omelet = JazzyItems.OMELET.get().createStack(1, level.getGameTime());
        FoodMatterData matter = KitchenStackUtil.getFoodMatter(omelet);
        IngredientStateData data = KitchenStackUtil.getOrCreateData(omelet, level.getGameTime());

        require(data != null, "Meal stacks should carry derived ingredient data");
        require(matter != null, "Meal stacks should initialize FOOD_MATTER");
        require(matter.finalizedServing(), "Meal stacks should initialize as finalized servings");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void allIngredientsExposeMaterialProfiles(GameTestHelper helper) {
        for (JazzyItems.IngredientId ingredientId : JazzyItems.IngredientId.values()) {
            ItemStack stack = new ItemStack(JazzyItems.ingredient(ingredientId).get());
            require(FoodMaterialProfiles.profileFor(stack).isPresent(), "Missing material profile for " + ingredientId.id());
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ingredientProfilesExposeRecognitionTraits(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FoodMatterData eggs = KitchenStackUtil.getOrCreateFoodMatter(
                JazzyItems.ingredient(JazzyItems.IngredientId.EGGS).get().createStack(1, level.getGameTime()),
                level.getGameTime()
        );
        FoodMatterData lemons = KitchenStackUtil.getOrCreateFoodMatter(
                JazzyItems.ingredient(JazzyItems.IngredientId.LEMONS).get().createStack(1, level.getGameTime()),
                level.getGameTime()
        );
        FoodMatterData chickpeas = KitchenStackUtil.getOrCreateFoodMatter(
                JazzyItems.ingredient(JazzyItems.IngredientId.CHICKPEAS).get().createStack(1, level.getGameTime()),
                level.getGameTime()
        );
        FoodMatterData paneer = KitchenStackUtil.getOrCreateFoodMatter(
                JazzyItems.ingredient(JazzyItems.IngredientId.PANEER).get().createStack(1, level.getGameTime()),
                level.getGameTime()
        );
        FoodMatterData tomatoSauce = KitchenStackUtil.getOrCreateFoodMatter(
                JazzyItems.ingredient(JazzyItems.IngredientId.TOMATO_SAUCE).get().createStack(1, level.getGameTime()),
                level.getGameTime()
        );

        require(eggs != null && eggs.hasTrait(FoodTrait.EGG) && eggs.hasTrait(FoodTrait.ANIMAL_PROTEIN), "Eggs should expose egg and animal-protein traits");
        require(lemons != null && lemons.hasTrait(FoodTrait.FRUIT) && lemons.hasTrait(FoodTrait.ACIDIC), "Lemons should expose fruit and acidic traits");
        require(chickpeas != null && chickpeas.hasTrait(FoodTrait.LEGUME) && chickpeas.hasTrait(FoodTrait.PLANT_PROTEIN), "Chickpeas should expose legume and plant-protein traits");
        require(paneer != null && paneer.hasTrait(FoodTrait.DAIRY) && paneer.hasTrait(FoodTrait.PROTEIN), "Paneer should expose dairy and protein traits");
        require(tomatoSauce != null && tomatoSauce.hasTrait(FoodTrait.TOMATO) && tomatoSauce.hasTrait(FoodTrait.SAUCE), "Tomato sauce should expose tomato and sauce traits");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pantryAndColdStorageFollowIngredientFlags(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack sugar = JazzyItems.ingredient(JazzyItems.IngredientId.WHITE_SUGAR).get().createStack(1, level.getGameTime());
        ItemStack chicken = JazzyItems.ingredient(JazzyItems.IngredientId.CHICKEN).get().createStack(1, level.getGameTime());
        ItemStack basil = JazzyItems.ingredient(JazzyItems.IngredientId.BASIL).get().createStack(1, level.getGameTime());

        require(StorageRules.canStore(StorageType.PANTRY, sugar), "Pantry should accept shelf-stable sweeteners");
        require(!StorageRules.canStore(StorageType.PANTRY, chicken), "Pantry should reject raw proteins");
        require(StorageRules.canStore(StorageType.FRIDGE, chicken), "Fridge should accept raw proteins");
        require(StorageRules.canStore(StorageType.FRIDGE, basil), "Fridge should accept fresh herbs");
        require(StorageRules.canStore(StorageType.FREEZER, chicken), "Freezer should accept freezer-safe proteins");
        require(!StorageRules.canStore(StorageType.FREEZER, basil), "Freezer should reject delicate herbs");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pantryClassificationTracksNewCatalog(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack bakingPowder = JazzyItems.ingredient(JazzyItems.IngredientId.BAKING_POWDER).get().createStack(1, level.getGameTime());
        ItemStack lentils = JazzyItems.ingredient(JazzyItems.IngredientId.LENTILS).get().createStack(1, level.getGameTime());
        ItemStack ketchup = JazzyItems.ingredient(JazzyItems.IngredientId.KETCHUP).get().createStack(1, level.getGameTime());
        ItemStack tomatoes = JazzyItems.ingredient(JazzyItems.IngredientId.TOMATOES).get().createStack(1, level.getGameTime());
        ItemStack paneer = JazzyItems.ingredient(JazzyItems.IngredientId.PANEER).get().createStack(1, level.getGameTime());

        require(PantrySortTab.classify(bakingPowder) == PantrySortTab.LEAVENING_AGENTS, "Baking powder should sort under leavening agents");
        require(PantrySortTab.classify(lentils) == PantrySortTab.DRY_GOODS, "Lentils should sort under dry goods");
        require(PantrySortTab.classify(ketchup) == PantrySortTab.SAUCES_AND_CONDIMENTS, "Ketchup should sort under sauces and condiments");
        require(PantrySortTab.classify(tomatoes) == PantrySortTab.OTHER, "Fresh produce should stay out of pantry tabs");
        require(PantrySortTab.classify(paneer) == PantrySortTab.DAIRY_AND_ALTERNATIVES, "Paneer should sort under dairy and alternatives");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void newStationsAndServiceToolsRegister(GameTestHelper helper) {
        require(JazzyBlocks.blockForStation(StationType.MICROWAVE).get() == JazzyBlocks.MICROWAVE.get(), "Microwave station should resolve to the microwave block");
        require(StationType.FOOD_PROCESSOR.supportsStationControl(), "Food processor should expose station controls");
        require(StationType.BLENDER.supportsStationControl(), "Blender should expose station controls");
        require(StationType.MICROWAVE.supportsHeat(), "Microwave should expose heat controls");
        require(ToolProfile.fromStack(new ItemStack(JazzyItems.FRYING_PAN.get())) == ToolProfile.PAN, "Frying pan should advertise the pan profile");
        require(ToolProfile.fromStack(new ItemStack(JazzyItems.POT.get())) == ToolProfile.POT, "Pot should advertise the pot profile");
        require(ToolProfile.fromStack(new ItemStack(JazzyItems.SAUCEPAN.get())) == ToolProfile.SAUCEPAN, "Saucepan should advertise the saucepan profile");
        require(ToolProfile.fromStack(new ItemStack(JazzyItems.TABLE_KNIFE.get())) == ToolProfile.TABLE_KNIFE, "Table knife should advertise the silverware profile");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void recipeDataLoads(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ResourceLocation stoveRecipeId = ResourceLocation.fromNamespaceAndPath(JazzyCookin.MODID, "stove");
        ResourceLocation kitchenRecipeId = ResourceLocation.fromNamespaceAndPath(JazzyCookin.MODID, "kitchen_process/fresh_lemon_juice_cut");
        ResourceLocation newMealRecipeId = ResourceLocation.fromNamespaceAndPath(JazzyCookin.MODID, "kitchen_process/spaghetti_pomodoro_simmer");
        ResourceLocation expandedMealRecipeId = ResourceLocation.fromNamespaceAndPath(JazzyCookin.MODID, "kitchen_process/chana_masala_simmer");
        require(level.getRecipeManager().byKey(stoveRecipeId).isPresent(), "Expected vanilla crafting recipe jazzycookin:stove to be present");
        require(level.getRecipeManager().byKey(kitchenRecipeId).isPresent(), "Expected kitchen process recipe jazzycookin:fresh_lemon_juice_cut to be present");
        require(level.getRecipeManager().byKey(newMealRecipeId).isPresent(), "Expected kitchen process recipe jazzycookin:spaghetti_pomodoro_simmer to be present");
        require(level.getRecipeManager().byKey(expandedMealRecipeId).isPresent(), "Expected kitchen process recipe jazzycookin:chana_masala_simmer to be present");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void coldStorageRejectsInvalidMenuPlacements(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos fridgePos = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockPos freezerPos = helper.absolutePos(new BlockPos(2, 1, 0));
        level.setBlockAndUpdate(fridgePos, JazzyBlocks.FRIDGE.get().defaultBlockState());
        level.setBlockAndUpdate(freezerPos, JazzyBlocks.FREEZER.get().defaultBlockState());

        KitchenStorageBlockEntity fridge = (KitchenStorageBlockEntity) level.getBlockEntity(fridgePos);
        KitchenStorageBlockEntity freezer = (KitchenStorageBlockEntity) level.getBlockEntity(freezerPos);
        require(fridge != null, "Expected fridge block entity to exist");
        require(freezer != null, "Expected freezer block entity to exist");

        var fakePlayer = FakePlayerFactory.getMinecraft(level);
        fakePlayer.getInventory().clearContent();

        ItemStack sugar = JazzyItems.ingredient(JazzyItems.IngredientId.WHITE_SUGAR).get().createStack(1, level.getGameTime());
        ItemStack basil = JazzyItems.ingredient(JazzyItems.IngredientId.BASIL).get().createStack(1, level.getGameTime());

        KitchenStorageMenu fridgeMenu = new KitchenStorageMenu(0, fakePlayer.getInventory(), fridge);
        KitchenStorageMenu freezerMenu = new KitchenStorageMenu(1, fakePlayer.getInventory(), freezer);

        require(!fridgeMenu.getSlot(0).mayPlace(sugar), "Fridge UI should reject pantry-only items");
        require(!freezerMenu.getSlot(0).mayPlace(basil), "Freezer UI should reject freezer-unsafe items");

        fakePlayer.getInventory().setItem(9, sugar.copy());
        require(fridgeMenu.quickMoveStack(fakePlayer, 18).isEmpty(), "Shift-click into fridge should fail for pantry-only items");
        require(ItemStack.isSameItemSameComponents(fakePlayer.getInventory().getItem(9), sugar), "Fridge rejection should not consume the stack");

        fakePlayer.getInventory().setItem(10, basil.copy());
        require(freezerMenu.quickMoveStack(fakePlayer, 19).isEmpty(), "Shift-click into freezer should fail for freezer-unsafe items");
        require(ItemStack.isSameItemSameComponents(fakePlayer.getInventory().getItem(10), basil), "Freezer rejection should not consume the stack");

        fridgeMenu.removed(fakePlayer);
        freezerMenu.removed(fakePlayer);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void legacyStacksMigrateFoodMatterOnTouch(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack legacyEggs = JazzyItems.ingredient(JazzyItems.IngredientId.EGGS).get().createStack(1, level.getGameTime());
        IngredientStateData existing = KitchenStackUtil.getData(legacyEggs);
        legacyEggs.remove(JazzyDataComponents.FOOD_MATTER.get());

        FoodMatterData migrated = KitchenStackUtil.getOrCreateFoodMatter(legacyEggs, level.getGameTime());
        require(existing != null, "Legacy stack should start with ingredient data");
        require(migrated != null, "Legacy stack should migrate FOOD_MATTER on first touch");
        require(KitchenStackUtil.getData(legacyEggs) != null, "Legacy stack should still expose ingredient data after migration");
        require(migrated.createdTick() == existing.createdTick(), "Migration should preserve the created tick");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void whiskingEggsCreatesUnstackableEggMixture(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos bowlPos = helper.absolutePos(new BlockPos(0, 1, 0));
        KitchenStationBlockEntity bowl = placeStation(level, bowlPos, JazzyBlocks.MIXING_BOWL.get());
        var fakePlayer = FakePlayerFactory.getMinecraft(level);

        bowl.setItem(0, JazzyItems.ingredient(JazzyItems.IngredientId.EGGS).get().createStack(1, level.getGameTime()));
        require(bowl.handleButton(6, fakePlayer), "Whisk action should succeed with eggs in the mixing bowl");

        ItemStack mixture = bowl.getItem(KitchenStationBlockEntity.OUTPUT_SLOT);
        FoodMatterData matter = KitchenStackUtil.getFoodMatter(mixture);
        require(mixture.is(JazzyItems.EGG_MIXTURE.get()), "Whisking eggs should produce egg mixture");
        require(matter != null && matter.aeration() > 0.15F, "Whisking should raise mixture aeration");
        require(mixture.getMaxStackSize() == 1, "Egg mixture should be unstackable while unfinished");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lowHeatEarlyRemoveYieldsSoftScrambledEggs(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var fakePlayer = FakePlayerFactory.getMinecraft(level);
        ItemStack mixture = whiskSimpleEggMixture(level, helper.absolutePos(new BlockPos(0, 1, 0)), fakePlayer);
        KitchenStationBlockEntity stove = prepareStove(level, helper.absolutePos(new BlockPos(2, 1, 0)), mixture);

        stove.handleButton(1, fakePlayer);
        require(stove.handleButton(6, fakePlayer), "Pour should start the stove simulation");
        tickStation(level, stove, 85);
        require(stove.handleButton(6, fakePlayer), "Remove should finalize the soft scramble");
        require(stove.getItem(KitchenStationBlockEntity.OUTPUT_SLOT).is(JazzyItems.SOFT_SCRAMBLED_EGGS.get()), "Low heat early remove should yield soft scrambled eggs");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mediumHeatStirYieldsScrambledEggs(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var fakePlayer = FakePlayerFactory.getMinecraft(level);
        ItemStack mixture = whiskSimpleEggMixture(level, helper.absolutePos(new BlockPos(0, 1, 0)), fakePlayer);
        KitchenStationBlockEntity stove = prepareStove(level, helper.absolutePos(new BlockPos(2, 1, 0)), mixture);

        stove.handleButton(2, fakePlayer);
        require(stove.handleButton(6, fakePlayer), "Pour should start the stove simulation");
        tickStation(level, stove, 60);
        stove.handleButton(7, fakePlayer);
        tickStation(level, stove, 40);
        stove.handleButton(7, fakePlayer);
        tickStation(level, stove, 35);
        require(stove.handleButton(6, fakePlayer), "Remove should finalize the scramble");
        require(stove.getItem(KitchenStationBlockEntity.OUTPUT_SLOT).is(JazzyItems.SCRAMBLED_EGGS.get()), "Medium heat with stirring should yield scrambled eggs");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void foldFlipYieldsOmeletAndHighHeatCanBurn(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var fakePlayer = FakePlayerFactory.getMinecraft(level);
        ItemStack mixture = whiskSimpleEggMixture(level, helper.absolutePos(new BlockPos(0, 1, 0)), fakePlayer);
        KitchenStationBlockEntity stove = prepareStove(level, helper.absolutePos(new BlockPos(2, 1, 0)), mixture);

        stove.handleButton(2, fakePlayer);
        require(stove.handleButton(6, fakePlayer), "Pour should start the omelet simulation");
        tickStation(level, stove, 45);
        stove.handleButton(8, fakePlayer);
        tickStation(level, stove, 25);
        stove.handleButton(8, fakePlayer);
        tickStation(level, stove, 20);
        require(stove.handleButton(6, fakePlayer), "Remove should finalize the omelet");
        require(stove.getItem(KitchenStationBlockEntity.OUTPUT_SLOT).is(JazzyItems.OMELET.get()), "Folding and flipping should yield an omelet");

        ItemStack secondMixture = whiskSimpleEggMixture(level, helper.absolutePos(new BlockPos(4, 1, 0)), fakePlayer);
        KitchenStationBlockEntity hotStove = prepareStove(level, helper.absolutePos(new BlockPos(6, 1, 0)), secondMixture);
        hotStove.handleButton(3, fakePlayer);
        require(hotStove.handleButton(6, fakePlayer), "High heat pour should start the burn path");
        tickStation(level, hotStove, 320);
        require(hotStove.dataAccess().get(19) == 5, "High heat should eventually preview burnt eggs");
        require(hotStove.handleButton(6, fakePlayer), "Remove should finalize the burnt batch");
        require(hotStove.getItem(KitchenStationBlockEntity.OUTPUT_SLOT).is(JazzyItems.BURNT_EGGS.get()), "Prolonged high heat should yield burnt eggs");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void addInsStillResolveToEggDishFamily(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos bowlPos = helper.absolutePos(new BlockPos(0, 1, 0));
        KitchenStationBlockEntity bowl = placeStation(level, bowlPos, JazzyBlocks.MIXING_BOWL.get());
        var fakePlayer = FakePlayerFactory.getMinecraft(level);

        bowl.setItem(0, JazzyItems.ingredient(JazzyItems.IngredientId.EGGS).get().createStack(1, level.getGameTime()));
        bowl.setItem(1, JazzyItems.ingredient(JazzyItems.IngredientId.CHEESE).get().createStack(1, level.getGameTime()));
        bowl.setItem(2, JazzyItems.ingredient(JazzyItems.IngredientId.ONIONS).get().createStack(1, level.getGameTime()));
        bowl.setItem(3, JazzyItems.ingredient(JazzyItems.IngredientId.BLACK_PEPPER).get().createStack(1, level.getGameTime()));
        require(bowl.handleButton(6, fakePlayer), "Whisking eggs with add-ins should still create a mixture");

        KitchenStationBlockEntity stove = prepareStove(level, helper.absolutePos(new BlockPos(2, 1, 0)), bowl.getItem(KitchenStationBlockEntity.OUTPUT_SLOT).copy());
        stove.handleButton(2, fakePlayer);
        require(stove.handleButton(6, fakePlayer), "Pour should accept add-in egg mixture");
        tickStation(level, stove, 60);
        stove.handleButton(7, fakePlayer);
        tickStation(level, stove, 45);
        stove.handleButton(7, fakePlayer);
        tickStation(level, stove, 35);
        require(stove.handleButton(6, fakePlayer), "Remove should finalize the add-in scramble");
        require(stove.getItem(KitchenStationBlockEntity.OUTPUT_SLOT).is(JazzyItems.SCRAMBLED_EGGS.get()), "Add-ins should still resolve to the egg dish family");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void guideRecipesIgnorePrepInputOrder(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        KitchenStationBlockEntity prepTable = placeStation(level, helper.absolutePos(new BlockPos(0, 1, 0)), JazzyBlocks.PREP_TABLE.get());
        var fakePlayer = FakePlayerFactory.getMinecraft(level);

        prepTable.setItem(0, JazzyItems.ingredient(JazzyItems.IngredientId.TOMATO_PASTE).get().createStack(1, level.getGameTime()));
        prepTable.setItem(1, JazzyItems.ingredient(JazzyItems.IngredientId.GARLIC).get().createStack(1, level.getGameTime()));
        prepTable.setItem(2, JazzyItems.ingredient(JazzyItems.IngredientId.BEEF).get().createStack(1, level.getGameTime()));
        prepTable.setItem(3, JazzyItems.ingredient(JazzyItems.IngredientId.ONIONS).get().createStack(1, level.getGameTime()));
        prepTable.setItem(KitchenStationBlockEntity.TOOL_SLOT, new ItemStack(JazzyItems.CHEF_KNIFE.get()));

        require(prepTable.handleButton(0, fakePlayer), "Guide recipes should start even when prep inputs are shuffled");
        tickStation(level, prepTable, 80);
        require(prepTable.getItem(KitchenStationBlockEntity.OUTPUT_SLOT).is(JazzyItems.BRAISED_BEEF_BASE.get()), "Shuffled prep inputs should still yield braised beef base");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void guideRecipesTolerateSupportiveExtras(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        KitchenStationBlockEntity prepTable = placeStation(level, helper.absolutePos(new BlockPos(0, 1, 0)), JazzyBlocks.PREP_TABLE.get());
        var fakePlayer = FakePlayerFactory.getMinecraft(level);

        prepTable.setItem(0, JazzyItems.ingredient(JazzyItems.IngredientId.BLACK_PEPPER).get().createStack(1, level.getGameTime()));
        prepTable.setItem(2, JazzyItems.ingredient(JazzyItems.IngredientId.LEMONS).get().createStack(1, level.getGameTime()));
        prepTable.setItem(KitchenStationBlockEntity.TOOL_SLOT, new ItemStack(JazzyItems.PARING_KNIFE.get()));

        require(prepTable.handleButton(0, fakePlayer), "Guide recipes should tolerate supportive seasoning extras");
        tickStation(level, prepTable, 50);

        ItemStack output = prepTable.getItem(KitchenStationBlockEntity.OUTPUT_SLOT);
        require(output.is(JazzyItems.ingredient(JazzyItems.IngredientId.LEMONS).get()), "Lemon cut guide should still output lemons");
        require(KitchenStackUtil.effectiveState(output, level.getGameTime()) == IngredientState.SLICED, "Guide result should preserve the expected sliced state");
        require(prepTable.getItem(0).is(JazzyItems.ingredient(JazzyItems.IngredientId.BLACK_PEPPER).get()), "Supportive extra seasoning should remain in the input slot");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void guidePlateRecipesIgnoreInputOrder(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        KitchenStationBlockEntity platingStation = placeStation(level, helper.absolutePos(new BlockPos(0, 1, 0)), JazzyBlocks.PLATING_STATION.get());
        var fakePlayer = FakePlayerFactory.getMinecraft(level);

        ItemStack restedCurryPrep = JazzyItems.CHICKEN_CURRY_PREP.get().createStack(
                1,
                level.getGameTime(),
                JazzyItems.CHICKEN_CURRY_PREP.get().defaultData(level.getGameTime()).withState(IngredientState.RESTED)
        );
        platingStation.setItem(0, new ItemStack(JazzyItems.CERAMIC_BOWL.get()));
        platingStation.setItem(1, restedCurryPrep);

        require(platingStation.handleButton(0, fakePlayer), "Plate guide should start with reversed input order");
        tickStation(level, platingStation, 30);
        require(platingStation.getItem(KitchenStationBlockEntity.OUTPUT_SLOT).is(JazzyItems.CHICKEN_CURRY.get()), "Reversed plating inputs should still yield chicken curry");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mixingBowlCanMakePieDoughViaSimulation(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        KitchenStationBlockEntity bowl = placeStation(level, helper.absolutePos(new BlockPos(0, 1, 0)), JazzyBlocks.MIXING_BOWL.get());
        var fakePlayer = FakePlayerFactory.getMinecraft(level);

        bowl.setItem(0, JazzyItems.ingredient(JazzyItems.IngredientId.ALL_PURPOSE_FLOUR).get().createStack(1, level.getGameTime()));
        bowl.setItem(1, JazzyItems.ingredient(JazzyItems.IngredientId.BUTTER).get().createStack(1, level.getGameTime()));
        require(bowl.handleButton(6, fakePlayer), "Mix action should form pie dough from flour and butter");
        require(bowl.getItem(KitchenStationBlockEntity.OUTPUT_SLOT).is(JazzyItems.PIE_DOUGH.get()), "Mixing bowl simulation should output pie dough");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void foodProcessorTurnsNutsIntoNutButter(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        KitchenStationBlockEntity processor = placeStation(level, helper.absolutePos(new BlockPos(0, 1, 0)), JazzyBlocks.FOOD_PROCESSOR.get());
        var fakePlayer = FakePlayerFactory.getMinecraft(level);

        processor.setItem(0, JazzyItems.ingredient(JazzyItems.IngredientId.ALMONDS).get().createStack(1, level.getGameTime()));
        require(processor.handleButton(6, fakePlayer), "Processor action should succeed with almonds");
        require(processor.getItem(KitchenStationBlockEntity.OUTPUT_SLOT).is(JazzyItems.NUT_BUTTER.get()), "Food processor simulation should output nut butter");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void blenderTurnsFruitAndMilkIntoSmoothieBlend(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        KitchenStationBlockEntity blender = placeStation(level, helper.absolutePos(new BlockPos(0, 1, 0)), JazzyBlocks.BLENDER.get());
        var fakePlayer = FakePlayerFactory.getMinecraft(level);

        blender.setItem(0, JazzyItems.ingredient(JazzyItems.IngredientId.APPLES).get().createStack(1, level.getGameTime()));
        blender.setItem(1, JazzyItems.ingredient(JazzyItems.IngredientId.OAT_MILK).get().createStack(1, level.getGameTime()));
        require(blender.handleButton(6, fakePlayer), "Blend action should succeed with produce and plant milk");
        require(blender.getItem(KitchenStationBlockEntity.OUTPUT_SLOT).is(JazzyItems.SMOOTHIE_BLEND.get()), "Blender simulation should output smoothie blend");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void juicerTurnsLemonsIntoJuiceAndPulp(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        KitchenStationBlockEntity juicer = placeStation(level, helper.absolutePos(new BlockPos(0, 1, 0)), JazzyBlocks.JUICER.get());
        var fakePlayer = FakePlayerFactory.getMinecraft(level);

        juicer.setItem(0, JazzyItems.ingredient(JazzyItems.IngredientId.LEMONS).get().createStack(1, level.getGameTime()));
        require(juicer.handleButton(6, fakePlayer), "Juice action should succeed with lemons");
        require(juicer.getItem(KitchenStationBlockEntity.OUTPUT_SLOT).is(JazzyItems.LEMON_JUICE.get()), "Juicer simulation should output lemon juice");
        require(juicer.getItem(KitchenStationBlockEntity.BYPRODUCT_SLOT).is(JazzyItems.FRUIT_PULP.get()), "Juicer simulation should output fruit pulp byproduct");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void freezeDryerPreservesApples(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        KitchenStationBlockEntity freezeDryer = placeStation(level, helper.absolutePos(new BlockPos(0, 1, 0)), JazzyBlocks.FREEZE_DRYER.get());
        var fakePlayer = FakePlayerFactory.getMinecraft(level);

        freezeDryer.setItem(0, JazzyItems.ingredient(JazzyItems.IngredientId.APPLES).get().createStack(1, level.getGameTime()));
        require(freezeDryer.handleButton(6, fakePlayer), "Dry action should succeed with apples");
        require(freezeDryer.getItem(KitchenStationBlockEntity.OUTPUT_SLOT).is(JazzyItems.PACKED_FREEZE_DRY_APPLES.get()), "Freeze dryer simulation should output packed freeze-dried apples");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void storageExposureUsesCanonicalSpoilageModel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long start = level.getGameTime();
        long future = start + 24_000L;

        ItemStack pantryChicken = JazzyItems.ingredient(JazzyItems.IngredientId.CHICKEN).get().createStack(1, start);
        ItemStack fridgeChicken = JazzyItems.ingredient(JazzyItems.IngredientId.CHICKEN).get().createStack(1, start);
        ItemStack freezerChicken = JazzyItems.ingredient(JazzyItems.IngredientId.CHICKEN).get().createStack(1, start);

        KitchenStackUtil.applyStorageExposure(pantryChicken, StorageType.PANTRY, 24_000L, future);
        KitchenStackUtil.applyStorageExposure(fridgeChicken, StorageType.FRIDGE, 24_000L, future);
        KitchenStackUtil.applyStorageExposure(freezerChicken, StorageType.FREEZER, 24_000L, future);

        float pantryFreshness = KitchenStackUtil.currentFreshnessScore(pantryChicken, future);
        float fridgeFreshness = KitchenStackUtil.currentFreshnessScore(fridgeChicken, future);
        float freezerFreshness = KitchenStackUtil.currentFreshnessScore(freezerChicken, future);
        FoodMatterData pantryMatter = KitchenStackUtil.getFoodMatter(pantryChicken);
        FoodMatterData fridgeMatter = KitchenStackUtil.getFoodMatter(fridgeChicken);
        FoodMatterData freezerMatter = KitchenStackUtil.getFoodMatter(freezerChicken);

        require(pantryMatter != null && fridgeMatter != null && freezerMatter != null, "Stored food should keep canonical FOOD_MATTER");
        require(freezerFreshness > fridgeFreshness && fridgeFreshness > pantryFreshness, "Cold storage should slow spoilage more than pantry storage");
        require(freezerMatter.coreTempC() < fridgeMatter.coreTempC(), "Freezer exposure should chill food below fridge temperature");
        require(pantryMatter.microbialLoad() > fridgeMatter.microbialLoad() && fridgeMatter.microbialLoad() > freezerMatter.microbialLoad(),
                "Cold storage should suppress microbial spoilage on FOOD_MATTER");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void preservedMealPacksBecomePantrySafe(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long start = level.getGameTime();
        long future = start + 24_000L * 20L;

        ItemStack freezeDriedMealPack = JazzyItems.FREEZE_DRIED_MEAL_PACK.get().createStack(1, start);
        ItemStack freshMeal = JazzyItems.CHICKEN_CURRY.get().createStack(1, start);
        FoodMatterData preservedMatter = KitchenStackUtil.getOrCreateFoodMatter(freezeDriedMealPack, start);

        require(preservedMatter != null, "Freeze-dried meal pack should initialize FOOD_MATTER");
        require(preservedMatter.preservationLevel() >= 0.70F, "Freeze-dried meal pack should carry a strong preservation level");
        require(StorageRules.canStore(StorageType.PANTRY, freezeDriedMealPack), "Freeze-dried meal pack should be pantry-safe from canonical preservation state");
        require(!StorageRules.canStore(StorageType.PANTRY, freshMeal), "Fresh plated meals should still stay out of pantry storage");
        require(KitchenStackUtil.currentFreshnessScore(freezeDriedMealPack, future) > KitchenStackUtil.currentFreshnessScore(freshMeal, future),
                "Preserved pantry meals should keep freshness far longer than fresh meals");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void recognizerLibraryClassifiesSimulatedOutputs(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var fakePlayer = FakePlayerFactory.getMinecraft(level);

        KitchenStationBlockEntity bowl = placeStation(level, helper.absolutePos(new BlockPos(0, 1, 0)), JazzyBlocks.MIXING_BOWL.get());
        bowl.setItem(0, JazzyItems.ingredient(JazzyItems.IngredientId.ALL_PURPOSE_FLOUR).get().createStack(1, level.getGameTime()));
        bowl.setItem(1, JazzyItems.ingredient(JazzyItems.IngredientId.BUTTER).get().createStack(1, level.getGameTime()));
        require(bowl.handleButton(6, fakePlayer), "Mix action should produce pie dough for recognizer coverage");

        KitchenStationBlockEntity processor = placeStation(level, helper.absolutePos(new BlockPos(2, 1, 0)), JazzyBlocks.FOOD_PROCESSOR.get());
        processor.setItem(0, JazzyItems.ingredient(JazzyItems.IngredientId.ALMONDS).get().createStack(1, level.getGameTime()));
        require(processor.handleButton(6, fakePlayer), "Processor action should produce nut butter for recognizer coverage");

        KitchenStationBlockEntity blender = placeStation(level, helper.absolutePos(new BlockPos(4, 1, 0)), JazzyBlocks.BLENDER.get());
        blender.setItem(0, JazzyItems.ingredient(JazzyItems.IngredientId.APPLES).get().createStack(1, level.getGameTime()));
        blender.setItem(1, JazzyItems.ingredient(JazzyItems.IngredientId.OAT_MILK).get().createStack(1, level.getGameTime()));
        require(blender.handleButton(6, fakePlayer), "Blend action should produce smoothie blend for recognizer coverage");

        KitchenStationBlockEntity juicer = placeStation(level, helper.absolutePos(new BlockPos(6, 1, 0)), JazzyBlocks.JUICER.get());
        juicer.setItem(0, JazzyItems.ingredient(JazzyItems.IngredientId.LEMONS).get().createStack(1, level.getGameTime()));
        require(juicer.handleButton(6, fakePlayer), "Juice action should produce lemon juice for recognizer coverage");

        KitchenStationBlockEntity freezeDryer = placeStation(level, helper.absolutePos(new BlockPos(8, 1, 0)), JazzyBlocks.FREEZE_DRYER.get());
        freezeDryer.setItem(0, JazzyItems.ingredient(JazzyItems.IngredientId.APPLES).get().createStack(1, level.getGameTime()));
        require(freezeDryer.handleButton(6, fakePlayer), "Dry action should produce freeze-dried apples for recognizer coverage");

        requireRecognizer(bowl.getItem(KitchenStationBlockEntity.OUTPUT_SLOT), level, "pie_dough");
        requireRecognizer(processor.getItem(KitchenStationBlockEntity.OUTPUT_SLOT), level, "nut_butter");
        requireRecognizer(blender.getItem(KitchenStationBlockEntity.OUTPUT_SLOT), level, "smoothie_blend");
        requireRecognizer(juicer.getItem(KitchenStationBlockEntity.OUTPUT_SLOT), level, "lemon_juice");
        requireRecognizer(freezeDryer.getItem(KitchenStationBlockEntity.OUTPUT_SLOT), level, "packed_freeze_dry_apples");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dishEvaluationRewardsGoodPhysicsAndPenalizesBurning(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long gameTime = level.getGameTime();

        ItemStack omelet = JazzyItems.OMELET.get().createStack(1, gameTime);
        FoodMatterData omeletMatter = KitchenStackUtil.getOrCreateFoodMatter(omelet, gameTime);
        require(omeletMatter != null, "Omelet stack should initialize FOOD_MATTER");
        KitchenStackUtil.setFoodMatter(
                omelet,
                omeletMatter.withAddedTraits(FoodTrait.maskOf(FoodTrait.EGG, FoodTrait.PROTEIN, FoodTrait.ANIMAL_PROTEIN)).withWorkingState(
                        0.42F,
                        0.18F,
                        0.20F,
                        0.74F,
                        0.68F,
                        0.12F,
                        0.0F,
                        omeletMatter.whiskWork(),
                        0,
                        2,
                        120,
                        Math.max(3, omeletMatter.processDepth()),
                        true
                ),
                gameTime
        );

        ItemStack burntEggs = JazzyItems.BURNT_EGGS.get().createStack(1, gameTime);
        FoodMatterData burntMatter = KitchenStackUtil.getOrCreateFoodMatter(burntEggs, gameTime);
        require(burntMatter != null, "Burnt eggs stack should initialize FOOD_MATTER");
        KitchenStackUtil.setFoodMatter(
                burntEggs,
                burntMatter.withAddedTraits(FoodTrait.maskOf(FoodTrait.EGG, FoodTrait.PROTEIN, FoodTrait.ANIMAL_PROTEIN)).withWorkingState(
                        0.12F,
                        0.04F,
                        0.46F,
                        0.18F,
                        0.82F,
                        0.88F,
                        0.44F,
                        burntMatter.whiskWork(),
                        1,
                        0,
                        360,
                        Math.max(3, burntMatter.processDepth()),
                        true
                ),
                gameTime
        );

        DishRecognitionResult omeletRecognition = DishSchema.preview(KitchenStackUtil.getFoodMatter(omelet));
        DishRecognitionResult burntRecognition = DishSchema.preview(KitchenStackUtil.getFoodMatter(burntEggs));
        QualityBreakdown omeletBreakdown = DishEvaluation.evaluateStack(omelet, level);
        QualityBreakdown burntBreakdown = DishEvaluation.evaluateStack(burntEggs, level);

        require(omeletRecognition != null && "omelet".equals(omeletRecognition.key()), "Good omelet physics should resolve to omelet recognition");
        require(burntRecognition != null && "burnt_eggs".equals(burntRecognition.key()), "Burnt physics should resolve to burnt eggs recognition");
        require(omeletBreakdown.finalScore() > burntBreakdown.finalScore(), "Good omelet should outscore burnt eggs");
        require(omeletBreakdown.cookingScore() > burntBreakdown.cookingScore(), "Good omelet should carry a better cooking score than burnt eggs");
        helper.succeed();
    }

    private static KitchenStationBlockEntity placeStation(ServerLevel level, BlockPos pos, net.minecraft.world.level.block.Block block) {
        level.setBlockAndUpdate(pos, block.defaultBlockState());
        KitchenStationBlockEntity blockEntity = (KitchenStationBlockEntity) level.getBlockEntity(pos);
        require(blockEntity != null, "Expected kitchen station block entity to exist");
        return blockEntity;
    }

    private static ItemStack whiskSimpleEggMixture(ServerLevel level, BlockPos bowlPos, net.minecraft.world.entity.player.Player fakePlayer) {
        KitchenStationBlockEntity bowl = placeStation(level, bowlPos, JazzyBlocks.MIXING_BOWL.get());
        bowl.setItem(0, JazzyItems.ingredient(JazzyItems.IngredientId.EGGS).get().createStack(1, level.getGameTime()));
        require(bowl.handleButton(6, fakePlayer), "Whisk should produce a simple egg mixture");
        return bowl.getItem(KitchenStationBlockEntity.OUTPUT_SLOT).copy();
    }

    private static KitchenStationBlockEntity prepareStove(ServerLevel level, BlockPos stovePos, ItemStack mixture) {
        KitchenStationBlockEntity stove = placeStation(level, stovePos, JazzyBlocks.STOVE.get());
        stove.setItem(0, mixture);
        stove.setItem(KitchenStationBlockEntity.TOOL_SLOT, new ItemStack(JazzyItems.FRYING_PAN.get()));
        return stove;
    }

    private static void tickStation(ServerLevel level, KitchenStationBlockEntity station, int ticks) {
        for (int i = 0; i < ticks; i++) {
            KitchenStationBlockEntity.serverTick(level, station.getBlockPos(), station.getBlockState(), station);
        }
    }

    private static void requireRecognizer(ItemStack stack, ServerLevel level, String expectedKey) {
        FoodMatterData matter = KitchenStackUtil.getOrCreateFoodMatter(stack, level.getGameTime());
        DishRecognitionResult recognition = DishSchema.preview(matter);
        require(recognition != null, "Expected recognizer result for " + stack.getItemHolder().unwrapKey().map(key -> key.location().getPath()).orElse("unknown_stack"));
        require(expectedKey.equals(recognition.key()), "Expected recognizer key " + expectedKey + " but got " + recognition.key());
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
