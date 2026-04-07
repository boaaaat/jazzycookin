package com.boaat.jazzy_cookin.gametest;

import java.util.Set;

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
import com.boaat.jazzy_cookin.kitchen.StationCapacityProfile;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.StationUiProfile;
import com.boaat.jazzy_cookin.kitchen.StorageRules;
import com.boaat.jazzy_cookin.kitchen.StorageType;
import com.boaat.jazzy_cookin.kitchen.StorageUiProfile;
import com.boaat.jazzy_cookin.kitchen.ToolProfile;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMaterialProfiles;
import com.boaat.jazzy_cookin.kitchen.sim.FoodTrait;
import com.boaat.jazzy_cookin.kitchen.sim.recognition.DishRecognitionResult;
import com.boaat.jazzy_cookin.kitchen.sim.recognition.DishSchema;
import com.boaat.jazzy_cookin.kitchen.sim.station.SimulationExecutionMode;
import com.boaat.jazzy_cookin.menu.KitchenStationMenu;
import com.boaat.jazzy_cookin.menu.KitchenStorageMenu;
import com.boaat.jazzy_cookin.recipebook.JazzyRecipeBookPlanner;
import com.boaat.jazzy_cookin.recipebook.JazzyRecipeBookSelection;
import com.boaat.jazzy_cookin.recipebook.RecipeBookDisplayUtil;
import com.boaat.jazzy_cookin.recipebook.RecipeBookProgress;
import com.boaat.jazzy_cookin.recipebook.SourceGuideRegistry;
import com.boaat.jazzy_cookin.recipebook.network.RecipeBookSyncPayload;
import com.boaat.jazzy_cookin.registry.JazzyBlocks;
import com.boaat.jazzy_cookin.registry.JazzyItems;
import com.boaat.jazzy_cookin.registry.JazzyRecipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
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
        require(!creativeApples.getItem().isBarVisible(creativeApples), "Creative preview stacks should not show the spoilage bar");
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
    public static void spoilageBarsRefreshHourlyForIngredientsAndMeals(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long start = level.getGameTime();

        ItemStack chicken = JazzyItems.ingredient(JazzyItems.IngredientId.CHICKEN).get().createStack(1, start);
        ItemStack curry = JazzyItems.CHICKEN_CURRY.get().createStack(1, start);

        KitchenStackUtil.setCreatedTick(chicken, start - 24_000L, start);
        KitchenStackUtil.setCreatedTick(curry, start - 24_000L, start);
        KitchenStackUtil.refreshSpoilageDisplay(chicken, start);
        KitchenStackUtil.refreshSpoilageDisplay(curry, start);

        int chickenBefore = chicken.getItem().getBarWidth(chicken);
        int curryBefore = curry.getItem().getBarWidth(curry);
        KitchenStackUtil.refreshSpoilageDisplay(chicken, start + KitchenStackUtil.SPOILAGE_BAR_UPDATE_TICKS);
        KitchenStackUtil.refreshSpoilageDisplay(curry, start + KitchenStackUtil.SPOILAGE_BAR_UPDATE_TICKS);

        require(chicken.getItem().isBarVisible(chicken), "Ingredients should show a spoilage bar");
        require(curry.getItem().isBarVisible(curry), "Meals should show a spoilage bar");
        require(chicken.getItem().getBarWidth(chicken) < chickenBefore, "Ingredient spoilage bar should fall after an in-game hour");
        require(curry.getItem().getBarWidth(curry) < curryBefore, "Meal spoilage bar should fall after an in-game hour");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void droppedIngredientStacksRefreshSpoilageBars(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long start = level.getGameTime();

        ItemStack chicken = JazzyItems.ingredient(JazzyItems.IngredientId.CHICKEN).get().createCreativeStack(1);
        KitchenStackUtil.setCreatedTick(chicken, start - 24_000L, start);
        require(!chicken.getItem().isBarVisible(chicken), "Creative preview stack should start without a spoilage bar");

        ItemEntity droppedChicken = new ItemEntity(level, 0.0, 2.0, 0.0, chicken);
        chicken.getItem().onEntityItemUpdate(chicken, droppedChicken);

        require(chicken.getItem().isBarVisible(chicken), "Dropped ingredient stack should gain a spoilage bar");
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
    public static void kitchenUiProfilesStayWithinBounds(GameTestHelper helper) {
        for (StationType stationType : StationType.values()) {
            StationUiProfile profile = StationUiProfile.forStation(stationType);
            require(profile.layout().workspaceRegion().contains(profile.layout().toolRegion()) || profile.layout().toolRegion() == null,
                    "Tool region should stay within workspace for " + stationType.getSerializedName());
            require(profile.layout().previewRegion().contains(profile.layout().outputRegion()),
                    "Output region should stay within preview for " + stationType.getSerializedName());
            require(profile.layout().previewRegion().contains(profile.layout().byproductRegion()),
                    "Byproduct region should stay within preview for " + stationType.getSerializedName());
        }
        for (StorageType storageType : StorageType.values()) {
            StorageUiProfile profile = StorageUiProfile.forType(storageType);
            require(profile.storageRegion().bottom() < profile.inventoryShelfRegion().y(),
                    "Storage shelf and player inventory should not overlap for " + storageType.getSerializedName());
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
    public static void stationCapacityProfilesExposeExpandedLayouts(GameTestHelper helper) {
        require(StationCapacityProfile.forStation(StationType.PREP_TABLE).inputCount() == 10, "Prep table should expose 10 inputs");
        require(StationCapacityProfile.forStation(StationType.MIXING_BOWL).inputCount() == 8, "Mixing bowl should expose 8 inputs");
        require(StationCapacityProfile.forStation(StationType.PLATING_STATION).inputCount() == 8, "Plating station should expose 8 inputs");
        require(StationCapacityProfile.forStation(StationType.STOVE).inputCount() == 6, "Stove should expose 6 inputs");
        require(StationCapacityProfile.forStation(StationType.OVEN).inputCount() == 6, "Oven should expose 6 inputs");
        require(StationCapacityProfile.forStation(StationType.FREEZE_DRYER).inputCount() == 5, "Freeze dryer should expose 5 inputs");
        require(StationCapacityProfile.forStation(StationType.MICROWAVE).inputCount() == 4, "Microwave should expose 4 inputs");
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
    public static void recipeInputCountsRespectStationCapacities(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getRecipeManager().getAllRecipesFor(JazzyRecipes.KITCHEN_PROCESS_TYPE.get()).forEach(holder -> {
            int capacity = StationCapacityProfile.forStation(holder.value().station()).inputCount();
            require(
                    holder.value().inputs().size() <= capacity,
                    "Process recipe " + holder.id() + " exceeds " + holder.value().station().getSerializedName() + " capacity"
            );
        });
        level.getRecipeManager().getAllRecipesFor(JazzyRecipes.KITCHEN_PLATE_TYPE.get()).forEach(holder -> {
            int capacity = StationCapacityProfile.forStation(StationType.PLATING_STATION).inputCount();
            require(
                    holder.value().inputs().size() <= capacity,
                    "Plate recipe " + holder.id() + " exceeds plating-station capacity"
            );
        });
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
        prepTable.setItem(4, JazzyItems.ingredient(JazzyItems.IngredientId.BLACK_PEPPER).get().createStack(1, level.getGameTime()));
        prepTable.setItem(5, JazzyItems.ingredient(JazzyItems.IngredientId.TABLE_SALT).get().createStack(1, level.getGameTime()));
        prepTable.setItem(KitchenStationBlockEntity.TOOL_SLOT, new ItemStack(JazzyItems.CHEF_KNIFE.get()));

        require(prepTable.handleButton(0, fakePlayer), "Guide recipes should start even when expanded prep inputs are shuffled");
        tickStation(level, prepTable, 80);
        require(prepTable.getItem(KitchenStationBlockEntity.OUTPUT_SLOT).is(JazzyItems.BRAISED_BEEF_BASE.get()), "Expanded shuffled prep inputs should still yield braised beef base");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void guideRecipesTolerateSupportiveExtras(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        KitchenStationBlockEntity prepTable = placeStation(level, helper.absolutePos(new BlockPos(0, 1, 0)), JazzyBlocks.PREP_TABLE.get());
        var fakePlayer = FakePlayerFactory.getMinecraft(level);

        prepTable.setItem(0, JazzyItems.ingredient(JazzyItems.IngredientId.BLACK_PEPPER).get().createStack(1, level.getGameTime()));
        prepTable.setItem(1, JazzyItems.ingredient(JazzyItems.IngredientId.WHITE_SUGAR).get().createStack(1, level.getGameTime()));
        prepTable.setItem(2, JazzyItems.ingredient(JazzyItems.IngredientId.COCOA_POWDER).get().createStack(1, level.getGameTime()));
        prepTable.setItem(3, JazzyItems.ingredient(JazzyItems.IngredientId.MAPLE_SYRUP).get().createStack(1, level.getGameTime()));
        prepTable.setItem(4, JazzyItems.ingredient(JazzyItems.IngredientId.LEMONS).get().createStack(1, level.getGameTime()));
        prepTable.setItem(KitchenStationBlockEntity.TOOL_SLOT, new ItemStack(JazzyItems.PARING_KNIFE.get()));

        require(prepTable.handleButton(0, fakePlayer), "Guide recipes should tolerate fully populated supportive extras on expanded stations");
        tickStation(level, prepTable, 50);

        ItemStack output = prepTable.getItem(KitchenStationBlockEntity.OUTPUT_SLOT);
        require(output.is(JazzyItems.ingredient(JazzyItems.IngredientId.LEMONS).get()), "Lemon cut guide should still output lemons");
        require(KitchenStackUtil.effectiveState(output, level.getGameTime()) == IngredientState.SLICED, "Guide result should preserve the expected sliced state");
        require(prepTable.getItem(0).is(JazzyItems.ingredient(JazzyItems.IngredientId.BLACK_PEPPER).get()), "Supportive extra seasoning should remain in the input slot");
        require(prepTable.getItem(3).is(JazzyItems.ingredient(JazzyItems.IngredientId.MAPLE_SYRUP).get()), "Expanded supportive extras should not be consumed");
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
        platingStation.setItem(2, restedCurryPrep);
        platingStation.setItem(5, new ItemStack(JazzyItems.SPOON.get()));

        require(platingStation.handleButton(0, fakePlayer), "Plate guide should start with shuffled plating inputs");
        tickStation(level, platingStation, 30);
        require(platingStation.getItem(KitchenStationBlockEntity.OUTPUT_SLOT).is(JazzyItems.CHICKEN_CURRY.get()), "Shuffled plating inputs should still yield chicken curry");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void recipeDrivenStationsResolveThroughSimulationDomains(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var fakePlayer = FakePlayerFactory.getMinecraft(level);

        KitchenStationBlockEntity prepTable = placeStation(level, helper.absolutePos(new BlockPos(0, 1, 0)), JazzyBlocks.PREP_TABLE.get());
        prepTable.setItem(0, JazzyItems.ingredient(JazzyItems.IngredientId.TOMATO_PASTE).get().createStack(1, level.getGameTime()));
        prepTable.setItem(1, JazzyItems.ingredient(JazzyItems.IngredientId.GARLIC).get().createStack(1, level.getGameTime()));
        prepTable.setItem(2, JazzyItems.ingredient(JazzyItems.IngredientId.BEEF).get().createStack(1, level.getGameTime()));
        prepTable.setItem(3, JazzyItems.ingredient(JazzyItems.IngredientId.ONIONS).get().createStack(1, level.getGameTime()));
        prepTable.setItem(4, JazzyItems.ingredient(JazzyItems.IngredientId.BLACK_PEPPER).get().createStack(1, level.getGameTime()));
        prepTable.setItem(5, JazzyItems.ingredient(JazzyItems.IngredientId.SEA_SALT).get().createStack(1, level.getGameTime()));
        prepTable.setItem(KitchenStationBlockEntity.TOOL_SLOT, new ItemStack(JazzyItems.CHEF_KNIFE.get()));
        require(prepTable.dataAccess().get(8) == SimulationExecutionMode.SIMULATION.ordinal(), "Prep-table recipes should now resolve through simulation domains");
        require(prepTable.handleButton(0, fakePlayer), "Simulation-backed prep recipes should still start");

        KitchenStationBlockEntity platingStation = placeStation(level, helper.absolutePos(new BlockPos(2, 1, 0)), JazzyBlocks.PLATING_STATION.get());
        ItemStack restedCurryPrep = JazzyItems.CHICKEN_CURRY_PREP.get().createStack(
                1,
                level.getGameTime(),
                JazzyItems.CHICKEN_CURRY_PREP.get().defaultData(level.getGameTime()).withState(IngredientState.RESTED)
        );
        platingStation.setItem(0, restedCurryPrep);
        platingStation.setItem(3, new ItemStack(JazzyItems.CERAMIC_BOWL.get()));
        platingStation.setItem(6, new ItemStack(JazzyItems.SPOON.get()));
        require(platingStation.dataAccess().get(8) == SimulationExecutionMode.SIMULATION.ordinal(), "Plating recipes should resolve through simulation domains as well");
        require(platingStation.handleButton(0, fakePlayer), "Simulation-backed plating recipes should still start");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pantryTagsAllowCommonRecipeSubstitutions(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        KitchenStationBlockEntity stove = placeStation(level, helper.absolutePos(new BlockPos(0, 1, 0)), JazzyBlocks.STOVE.get());
        var fakePlayer = FakePlayerFactory.getMinecraft(level);

        stove.setItem(0, JazzyItems.ingredient(JazzyItems.IngredientId.PASTA).get().createStack(1, level.getGameTime()));
        stove.setItem(1, JazzyItems.ingredient(JazzyItems.IngredientId.BLACK_BEANS).get().createStack(1, level.getGameTime()));
        stove.setItem(2, JazzyItems.ingredient(JazzyItems.IngredientId.CANNED_TOMATOES).get().createStack(1, level.getGameTime()));
        stove.setItem(3, JazzyItems.ingredient(JazzyItems.IngredientId.STOCK).get().createStack(1, level.getGameTime()));
        stove.setItem(4, JazzyItems.ingredient(JazzyItems.IngredientId.GARLIC).get().createStack(1, level.getGameTime()));
        stove.setItem(5, JazzyItems.ingredient(JazzyItems.IngredientId.SEA_SALT).get().createStack(1, level.getGameTime()));
        stove.setItem(KitchenStationBlockEntity.TOOL_SLOT, new ItemStack(JazzyItems.POT.get()));
        stove.handleButton(1, fakePlayer);

        require(stove.handleButton(0, fakePlayer), "Tagged pantry substitutions should still start expanded stove recipes");
        tickStation(level, stove, 240);
        require(stove.getItem(KitchenStationBlockEntity.OUTPUT_SLOT).is(JazzyItems.PASTA_E_FAGIOLI_PREP.get()), "Stock and alternate salts should satisfy the expanded simmer recipe");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void quickMoveUsesExpandedInputRange(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        KitchenStationBlockEntity prepTable = placeStation(level, helper.absolutePos(new BlockPos(0, 1, 0)), JazzyBlocks.PREP_TABLE.get());
        var fakePlayer = FakePlayerFactory.getMinecraft(level);
        KitchenStationMenu menu = new KitchenStationMenu(0, fakePlayer.getInventory(), prepTable, prepTable.dataAccess());

        prepTable.setItem(0, JazzyItems.ingredient(JazzyItems.IngredientId.BEEF).get().createStack(1, level.getGameTime()));
        prepTable.setItem(1, JazzyItems.ingredient(JazzyItems.IngredientId.ONIONS).get().createStack(1, level.getGameTime()));
        prepTable.setItem(2, JazzyItems.ingredient(JazzyItems.IngredientId.GARLIC).get().createStack(1, level.getGameTime()));
        prepTable.setItem(3, JazzyItems.ingredient(JazzyItems.IngredientId.TOMATO_PASTE).get().createStack(1, level.getGameTime()));

        fakePlayer.getInventory().setItem(9, JazzyItems.ingredient(JazzyItems.IngredientId.BLACK_PEPPER).get().createStack(1, level.getGameTime()));
        ItemStack moved = menu.quickMoveStack(fakePlayer, menu.visibleStationSlotCount());

        require(!moved.isEmpty(), "Shift-click should move ingredients into expanded prep-table slots");
        require(prepTable.getItem(4).is(JazzyItems.ingredient(JazzyItems.IngredientId.BLACK_PEPPER).get()), "Shift-click should target the fifth active prep-table input slot");
        menu.removed(fakePlayer);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mixingBowlSupportsExpandedDryMixRecipes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        KitchenStationBlockEntity bowl = placeStation(level, helper.absolutePos(new BlockPos(0, 1, 0)), JazzyBlocks.MIXING_BOWL.get());
        var fakePlayer = FakePlayerFactory.getMinecraft(level);

        bowl.setItem(0, JazzyItems.ingredient(JazzyItems.IngredientId.ALL_PURPOSE_FLOUR).get().createStack(1, level.getGameTime()));
        bowl.setItem(1, JazzyItems.ingredient(JazzyItems.IngredientId.WHITE_SUGAR).get().createStack(1, level.getGameTime()));
        bowl.setItem(2, JazzyItems.ingredient(JazzyItems.IngredientId.BAKING_POWDER).get().createStack(1, level.getGameTime()));
        bowl.setItem(3, JazzyItems.ingredient(JazzyItems.IngredientId.POWDERED_MILK).get().createStack(1, level.getGameTime()));
        bowl.setItem(4, JazzyItems.ingredient(JazzyItems.IngredientId.TABLE_SALT).get().createStack(1, level.getGameTime()));

        require(bowl.handleButton(0, fakePlayer), "Expanded dry-mix recipes should start in the mixing bowl");
        tickStation(level, bowl, 70);
        require(bowl.getItem(KitchenStationBlockEntity.OUTPUT_SLOT).is(JazzyItems.PANCAKE_DRY_MIX.get()), "Expanded dry-mix recipes should output pancake dry mix");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void stoveSupportsExpandedSixIngredientRecipes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        KitchenStationBlockEntity stove = placeStation(level, helper.absolutePos(new BlockPos(0, 1, 0)), JazzyBlocks.STOVE.get());
        var fakePlayer = FakePlayerFactory.getMinecraft(level);

        stove.setItem(0, JazzyItems.ingredient(JazzyItems.IngredientId.CHICKPEAS).get().createStack(1, level.getGameTime()));
        stove.setItem(1, JazzyItems.ingredient(JazzyItems.IngredientId.TOMATOES).get().createStack(1, level.getGameTime()));
        stove.setItem(2, JazzyItems.ingredient(JazzyItems.IngredientId.ONIONS).get().createStack(1, level.getGameTime()));
        stove.setItem(3, JazzyItems.ingredient(JazzyItems.IngredientId.GARLIC).get().createStack(1, level.getGameTime()));
        stove.setItem(4, JazzyItems.ingredient(JazzyItems.IngredientId.CURRY_POWDER).get().createStack(1, level.getGameTime()));
        stove.setItem(5, JazzyItems.ingredient(JazzyItems.IngredientId.TABLE_SALT).get().createStack(1, level.getGameTime()));
        stove.setItem(KitchenStationBlockEntity.TOOL_SLOT, new ItemStack(JazzyItems.SAUCEPAN.get()));
        stove.handleButton(1, fakePlayer);

        require(stove.handleButton(0, fakePlayer), "Expanded stove recipes should start with six active inputs");
        tickStation(level, stove, 240);
        require(stove.getItem(KitchenStationBlockEntity.OUTPUT_SLOT).is(JazzyItems.CHANA_MASALA_PREP.get()), "Expanded stove recipes should still resolve to chana masala prep");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ovenSimulationRequiresPreheatAndBakesGarlicBread(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        KitchenStationBlockEntity oven = placeStation(level, helper.absolutePos(new BlockPos(0, 1, 0)), JazzyBlocks.OVEN.get());
        var fakePlayer = FakePlayerFactory.getMinecraft(level);

        oven.setItem(0, JazzyItems.ingredient(JazzyItems.IngredientId.BREAD).get().createStack(1, level.getGameTime()));
        oven.setItem(1, JazzyItems.GARLIC_BUTTER.get().createStack(1, level.getGameTime()));
        oven.setItem(KitchenStationBlockEntity.TOOL_SLOT, new ItemStack(JazzyItems.BAKING_TRAY.get()));
        oven.handleButton(2, fakePlayer);

        require(!oven.handleButton(0, fakePlayer), "Oven recipe should stay blocked until preheated");
        tickStation(level, oven, 50);
        require(oven.simulationPreheatProgress() >= 100, "Medium oven heat should fully preheat after enough ticks");

        require(oven.handleButton(0, fakePlayer), "Preheated oven should start the garlic bread bake");
        tickStation(level, oven, 130);
        require(oven.getItem(KitchenStationBlockEntity.OUTPUT_SLOT).is(JazzyItems.GARLIC_BREAD_PREP.get()), "Oven simulation should bake garlic bread prep");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void restingBoardSimulationAdvancesPassiveRestRecipes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        KitchenStationBlockEntity restingBoard = placeStation(level, helper.absolutePos(new BlockPos(0, 1, 0)), JazzyBlocks.RESTING_BOARD.get());
        var fakePlayer = FakePlayerFactory.getMinecraft(level);

        restingBoard.setItem(0, JazzyItems.BRAISED_BEEF_PREP.get().createStack(1, level.getGameTime()));
        require(restingBoard.handleButton(0, fakePlayer), "Resting board should start passive rest recipes");
        tickStation(level, restingBoard, 80);

        ItemStack output = restingBoard.getItem(KitchenStationBlockEntity.OUTPUT_SLOT);
        require(output.is(JazzyItems.BRAISED_BEEF_PREP.get()), "Resting board should keep the same prep item");
        require(KitchenStackUtil.effectiveState(output, level.getGameTime()) == IngredientState.RESTED, "Resting board should advance the item to the rested state");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void canningStationSimulationHandlesHeatedPassivePreserves(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        KitchenStationBlockEntity canningStation = placeStation(level, helper.absolutePos(new BlockPos(0, 1, 0)), JazzyBlocks.CANNING_STATION.get());
        var fakePlayer = FakePlayerFactory.getMinecraft(level);

        canningStation.setItem(0, JazzyItems.SYRUP_MIXTURE.get().createStack(1, level.getGameTime()));
        canningStation.setItem(KitchenStationBlockEntity.TOOL_SLOT, new ItemStack(JazzyItems.CANNING_JAR.get()));
        canningStation.handleButton(1, fakePlayer);

        require(canningStation.handleButton(0, fakePlayer), "Canning station should start heated preserve recipes");
        tickStation(level, canningStation, 140);
        require(canningStation.getItem(KitchenStationBlockEntity.OUTPUT_SLOT).is(JazzyItems.HOT_SYRUP_PRESERVE.get()), "Canning station simulation should output hot syrup preserve");
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

    @GameTest(template = "empty")
    public static void recipeBookPlannerBuildsSpaghettiPomodoroChain(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        JazzyRecipeBookPlanner planner = JazzyRecipeBookPlanner.create(level);
        JazzyRecipeBookPlanner.Plan plan = requirePlan(
                planner,
                selection(JazzyItems.SPAGHETTI_POMODORO.get(), IngredientState.PLATED, "spaghetti_pomodoro")
        );

        require(plan.steps().size() >= 2, "Spaghetti pomodoro should include at least prep and plating steps");
        require(plan.steps().get(plan.steps().size() - 1).kind() == JazzyRecipeBookPlanner.StepKind.PLATE,
                "Spaghetti pomodoro should end with a plating step");
        require(plan.steps().stream().anyMatch(step ->
                        step.kind() == JazzyRecipeBookPlanner.StepKind.PROCESS
                                && step.outputKey().equals(outputKey(JazzyItems.SPAGHETTI_POMODORO_PREP.get(), IngredientState.MIXED))),
                "Spaghetti pomodoro should include its upstream simmer/process step");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void recipeBookPlannerDisambiguatesDuplicatesAndMergesAlternatives(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        JazzyRecipeBookPlanner planner = JazzyRecipeBookPlanner.create(level);

        Set<String> pieChains = planner.plansFor(itemId(JazzyItems.PIE_DOUGH.get()), IngredientState.SMOOTH_DOUGH).stream()
                .map(JazzyRecipeBookPlanner.Plan::chainKey)
                .collect(java.util.stream.Collectors.toSet());
        require(pieChains.contains("savory_pie") && pieChains.contains("tray_pie") && pieChains.size() == 2,
                "Pie dough should expose distinct savory-pie and tray-pie paths");

        Set<String> seasoningChains = planner.plansFor(itemId(JazzyItems.SEASONING_BLEND.get()), IngredientState.FINE_POWDER).stream()
                .map(JazzyRecipeBookPlanner.Plan::chainKey)
                .collect(java.util.stream.Collectors.toSet());
        require(seasoningChains.contains("pan_seared_chicken")
                        && seasoningChains.contains("golden_rice")
                        && seasoningChains.contains("fried_jalapeno_bites")
                        && seasoningChains.size() == 3,
                "Seasoning blend should stay disambiguated by chain key");

        JazzyRecipeBookPlanner.Plan toastPlan = requirePlan(
                planner,
                selection(JazzyItems.ingredient(JazzyItems.IngredientId.BREAD).get(), IngredientState.BAKED_BREAD, "jam_on_toast")
        );
        require(toastPlan.steps().size() == 1, "Jam on toast bread step should stay merged into a single guide step");
        require(toastPlan.steps().get(0).options().size() == 2,
                "Jam on toast should merge oven and stove toast alternatives into one step");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void recipeBookPlannerIncludesCraftingAndSourceGuides(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        JazzyRecipeBookPlanner planner = JazzyRecipeBookPlanner.create(level);
        ItemStack apples = stackWithState(JazzyItems.ingredient(JazzyItems.IngredientId.APPLES).get(), defaultState(JazzyItems.ingredient(JazzyItems.IngredientId.APPLES).get()));
        JazzyRecipeBookPlanner.Plan applePlan = planner.plansFor(itemId(apples.getItem()), RecipeBookDisplayUtil.actualStateForStack(apples, level.getGameTime()))
                .stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError("Apples should have a recipe-book plan"));

        require(applePlan.steps().stream().anyMatch(step ->
                        step.kind() == JazzyRecipeBookPlanner.StepKind.CRAFT
                                && step.outputKey().equals(outputKey(JazzyBlocks.APPLE_SAPLING.get(), IngredientState.PANTRY_READY))),
                "Apple guide should include crafting the apple sapling first");
        require(applePlan.steps().stream().anyMatch(step ->
                        step.kind() == JazzyRecipeBookPlanner.StepKind.SOURCE
                                && step.outputKey().equals(outputKey(JazzyItems.ingredient(JazzyItems.IngredientId.APPLES).get(), RecipeBookDisplayUtil.actualStateForStack(apples, level.getGameTime())))),
                "Apple guide should include the harvest step after crafting the source block");

        require(planner.catalogEntry(itemId(JazzyBlocks.STOVE.get())).isPresent(), "Stove should appear in the recipe-book catalog");
        JazzyRecipeBookPlanner.Plan stovePlan = requirePlan(planner, selection(JazzyBlocks.STOVE.get(), IngredientState.PANTRY_READY, ""));
        require(stovePlan.steps().size() == 1 && stovePlan.steps().get(0).kind() == JazzyRecipeBookPlanner.StepKind.CRAFT,
                "Stove should resolve through a crafting step");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void recipeBookPlannerMarksRandomSourcesAsRandom(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        JazzyRecipeBookPlanner planner = JazzyRecipeBookPlanner.create(level);
        IngredientState basilState = defaultState(JazzyItems.ingredient(JazzyItems.IngredientId.BASIL).get());
        JazzyRecipeBookPlanner.Plan basilPlan = planner.plansFor(itemId(JazzyItems.ingredient(JazzyItems.IngredientId.BASIL).get()), basilState)
                .stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError("Basil should have a source-guide plan"));
        JazzyRecipeBookPlanner.PlanStep sourceStep = basilPlan.steps().stream()
                .filter(step -> step.kind() == JazzyRecipeBookPlanner.StepKind.SOURCE)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Basil plan should include a source step"));

        require(SourceGuideRegistry.guideForOutput(itemId(JazzyItems.ingredient(JazzyItems.IngredientId.BASIL).get()), basilState).isPresent(),
                "Basil should resolve through the shared source-guide registry");
        require(sourceStep.options().get(0).notes().stream().anyMatch(note -> note.contains("not guaranteed")),
                "Random-output sources should warn that harvest results are not guaranteed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void recipeBookProgressAdvancesPinnedKitchenGuides(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var fakePlayer = FakePlayerFactory.getMinecraft(level);
        JazzyRecipeBookPlanner planner = JazzyRecipeBookPlanner.create(level);
        JazzyRecipeBookSelection selection = selection(JazzyItems.PACKED_BREADCRUMBS.get(), IngredientState.COARSE_POWDER, "breadcrumbs");
        JazzyRecipeBookPlanner.Plan plan = requirePlan(planner, selection);

        RecipeBookProgress.pin(fakePlayer, selection);
        require(RecipeBookProgress.recordKitchenOutput(fakePlayer,
                        stackWithState(JazzyItems.ingredient(JazzyItems.IngredientId.BREADCRUMBS).get(), IngredientState.COARSE_POWDER),
                        "breadcrumbs"),
                "Prep output should complete the upstream process step");

        Set<String> completedAfterPrep = RecipeBookProgress.completedSteps(fakePlayer);
        require(completedAfterPrep.size() == 1, "Recording the prep output should complete exactly one step");
        require(plan.currentStep(completedAfterPrep) != null && plan.currentStep(completedAfterPrep).kind() == JazzyRecipeBookPlanner.StepKind.PLATE,
                "After prep completion the guide should advance to the plating step");

        require(RecipeBookProgress.recordKitchenOutput(fakePlayer, stackWithState(JazzyItems.PACKED_BREADCRUMBS.get(), IngredientState.COARSE_POWDER), "breadcrumbs"),
                "Plated output should complete the final serve step");

        Set<String> completedAfterPlate = RecipeBookProgress.completedSteps(fakePlayer);
        require(plan.isComplete(completedAfterPlate), "Process plus plating outputs should complete the pinned guide");
        RecipeBookSyncPayload sync = RecipeBookSyncPayload.from(RecipeBookProgress.syncState(fakePlayer));
        require(sync.selection() != null && sync.selection().itemId().equals(selection.itemId()),
                "Sync payload should keep the active pinned selection");
        require(sync.completedStepIds().size() == 2, "Sync payload should include both completed kitchen steps");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void recipeBookProgressTracksCraftAndSourceHarvest(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var fakePlayer = FakePlayerFactory.getMinecraft(level);
        JazzyRecipeBookPlanner planner = JazzyRecipeBookPlanner.create(level);

        JazzyRecipeBookSelection stoveSelection = selection(JazzyBlocks.STOVE.get(), IngredientState.PANTRY_READY, "");
        JazzyRecipeBookPlanner.Plan stovePlan = requirePlan(planner, stoveSelection);
        RecipeBookProgress.pin(fakePlayer, stoveSelection);
        require(RecipeBookProgress.recordCraft(fakePlayer, new ItemStack(JazzyBlocks.STOVE.get())),
                "Crafting a mod block should complete its pinned crafting guide");
        require(stovePlan.isComplete(RecipeBookProgress.completedSteps(fakePlayer)),
                "Crafting a stove should finish the stove guide");

        JazzyRecipeBookSelection appleSelection = selection(
                JazzyItems.ingredient(JazzyItems.IngredientId.APPLES).get(),
                defaultState(JazzyItems.ingredient(JazzyItems.IngredientId.APPLES).get()),
                "apple_sapling"
        );
        JazzyRecipeBookPlanner.Plan applePlan = requirePlan(planner, appleSelection);
        RecipeBookProgress.pin(fakePlayer, appleSelection);
        require(RecipeBookProgress.recordCraft(fakePlayer, new ItemStack(JazzyBlocks.APPLE_SAPLING.get())),
                "Crafting the apple sapling should complete the source prerequisite step");

        Set<String> completedAfterSapling = RecipeBookProgress.completedSteps(fakePlayer);
        require(applePlan.currentStep(completedAfterSapling) != null && applePlan.currentStep(completedAfterSapling).kind() == JazzyRecipeBookPlanner.StepKind.SOURCE,
                "After crafting the sapling the guide should advance to the harvest step");

        require(RecipeBookProgress.recordSourceHarvest(fakePlayer,
                        stackWithState(JazzyItems.ingredient(JazzyItems.IngredientId.APPLES).get(), defaultState(JazzyItems.ingredient(JazzyItems.IngredientId.APPLES).get())),
                        "apple_sapling"),
                "Harvesting the tracked source output should complete the source step");
        require(applePlan.isComplete(RecipeBookProgress.completedSteps(fakePlayer)),
                "Craft plus source harvest should finish the apple guide");
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

    private static JazzyRecipeBookPlanner.Plan requirePlan(JazzyRecipeBookPlanner planner, JazzyRecipeBookSelection selection) {
        return planner.planFor(selection).orElseThrow(() -> new AssertionError(
                "Expected recipe-book plan for " + selection.itemId() + "@" + selection.state().getSerializedName() + " (" + selection.normalizedChainKey() + ")"
        ));
    }

    private static JazzyRecipeBookSelection selection(ItemLike itemLike, IngredientState state, String chainKey) {
        return new JazzyRecipeBookSelection(itemId(itemLike), state, chainKey);
    }

    private static ResourceLocation itemId(ItemLike itemLike) {
        return BuiltInRegistries.ITEM.getKey(itemLike.asItem());
    }

    private static JazzyRecipeBookPlanner.OutputKey outputKey(ItemLike itemLike, IngredientState state) {
        return RecipeBookDisplayUtil.outputKey(itemLike.asItem(), state);
    }

    private static IngredientState defaultState(ItemLike itemLike) {
        return RecipeBookDisplayUtil.defaultStateForItem(itemLike.asItem());
    }

    private static ItemStack stackWithState(ItemLike itemLike, IngredientState state) {
        return RecipeBookDisplayUtil.displayStack(new ItemStack(itemLike), state, 1);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
