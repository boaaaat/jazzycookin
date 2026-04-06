package com.boaat.jazzy_cookin.gametest;

import com.boaat.jazzy_cookin.JazzyCookin;
import com.boaat.jazzy_cookin.block.entity.KitchenStorageBlockEntity;
import com.boaat.jazzy_cookin.kitchen.FreshnessBand;
import com.boaat.jazzy_cookin.kitchen.IngredientStateData;
import com.boaat.jazzy_cookin.kitchen.KitchenStackUtil;
import com.boaat.jazzy_cookin.kitchen.PantrySortTab;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.StorageRules;
import com.boaat.jazzy_cookin.kitchen.StorageType;
import com.boaat.jazzy_cookin.kitchen.ToolProfile;
import com.boaat.jazzy_cookin.menu.KitchenStorageMenu;
import com.boaat.jazzy_cookin.registry.JazzyBlocks;
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
        require(data.quality() == 1.0F, "Creative ingredient stack should have max quality");
        require(data.recipeAccuracy() == 1.0F, "Creative ingredient stack should have max recipe accuracy");
        require(KitchenStackUtil.freshnessBand(creativeApples, level) == FreshnessBand.FRESH, "Creative ingredient stack should stay fresh");
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

        require(PantrySortTab.classify(bakingPowder) == PantrySortTab.LEAVENING_AGENTS, "Baking powder should sort under leavening agents");
        require(PantrySortTab.classify(lentils) == PantrySortTab.DRY_GOODS, "Lentils should sort under dry goods");
        require(PantrySortTab.classify(ketchup) == PantrySortTab.SAUCES_AND_CONDIMENTS, "Ketchup should sort under sauces and condiments");
        require(PantrySortTab.classify(tomatoes) == PantrySortTab.OTHER, "Fresh produce should stay out of pantry tabs");
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
        require(level.getRecipeManager().byKey(stoveRecipeId).isPresent(), "Expected vanilla crafting recipe jazzycookin:stove to be present");
        require(level.getRecipeManager().byKey(kitchenRecipeId).isPresent(), "Expected kitchen process recipe jazzycookin:fresh_lemon_juice_cut to be present");
        require(level.getRecipeManager().byKey(newMealRecipeId).isPresent(), "Expected kitchen process recipe jazzycookin:spaghetti_pomodoro_simmer to be present");
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

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
