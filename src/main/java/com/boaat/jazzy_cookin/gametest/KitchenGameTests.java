package com.boaat.jazzy_cookin.gametest;

import com.boaat.jazzy_cookin.JazzyCookin;
import com.boaat.jazzy_cookin.kitchen.FreshnessBand;
import com.boaat.jazzy_cookin.kitchen.IngredientStateData;
import com.boaat.jazzy_cookin.kitchen.KitchenStackUtil;
import com.boaat.jazzy_cookin.kitchen.PantrySortTab;
import com.boaat.jazzy_cookin.kitchen.StorageRules;
import com.boaat.jazzy_cookin.kitchen.StorageType;
import com.boaat.jazzy_cookin.registry.JazzyItems;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
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

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
