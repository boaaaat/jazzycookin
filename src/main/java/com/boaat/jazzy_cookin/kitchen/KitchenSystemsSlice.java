package com.boaat.jazzy_cookin.kitchen;

import java.util.Set;

import com.boaat.jazzy_cookin.registry.JazzyItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

public final class KitchenSystemsSlice {
    private static final Set<JazzyItems.IngredientId> KEPT_INGREDIENTS = Set.of(
            JazzyItems.IngredientId.APPLES,
            JazzyItems.IngredientId.CARROTS,
            JazzyItems.IngredientId.LEMONS,
            JazzyItems.IngredientId.ALL_PURPOSE_FLOUR,
            JazzyItems.IngredientId.BUTTER,
            JazzyItems.IngredientId.WHITE_SUGAR,
            JazzyItems.IngredientId.CHICKEN,
            JazzyItems.IngredientId.TABLE_SALT
    );

    private static final Set<String> KEPT_PREPARED_ITEMS = Set.of(
            "chopped_produce_blend",
            "fruit_pulp",
            "lemon_juice",
            "mixed_juice",
            "fruit_juice_blend",
            "jarred_lemon_juice",
            "pie_dough",
            "assembled_tray_pie",
            "smoked_meat",
            "savory_pie_filling",
            "assembled_savory_pie",
            "pan_seared_chicken_prep",
            "glazed_chicken_prep"
    );

    private static final Set<String> KEPT_MEALS = Set.of(
            "fruit_juice",
            "sliceable_pie",
            "meat_platter",
            "savory_pie",
            "pan_seared_chicken",
            "glazed_chicken"
    );

    private static final Set<String> KEPT_TOOLS = Set.of(
            "glass_cup",
            "glass_jar",
            "ceramic_plate",
            "wooden_board",
            "serving_tray",
            "table_knife",
            "fork",
            "paring_knife",
            "chef_knife",
            "cleaver",
            "baking_tray"
    );

    private static final Set<String> KEPT_SOURCES = Set.of(
            "apple_sapling",
            "chicken_coop",
            "dairy_stall"
    );

    private KitchenSystemsSlice() {
    }

    public static boolean keepIngredient(JazzyItems.IngredientId ingredientId) {
        return KEPT_INGREDIENTS.contains(ingredientId);
    }

    public static boolean keepPreparedItem(Item item) {
        return KEPT_PREPARED_ITEMS.contains(itemPath(item));
    }

    public static boolean keepMealItem(Item item) {
        return KEPT_MEALS.contains(itemPath(item));
    }

    public static boolean keepToolItem(Item item) {
        return KEPT_TOOLS.contains(itemPath(item));
    }

    public static boolean keepSourceItem(Item item) {
        return KEPT_SOURCES.contains(itemPath(item));
    }

    private static String itemPath(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getPath();
    }
}
