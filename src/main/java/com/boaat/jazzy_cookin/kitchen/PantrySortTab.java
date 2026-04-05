package com.boaat.jazzy_cookin.kitchen;

import java.util.List;

import com.boaat.jazzy_cookin.item.KitchenIngredientItem;
import com.boaat.jazzy_cookin.registry.JazzyItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public enum PantrySortTab {
    SWEETENERS(0, "screen.jazzycookin.pantry_tab.sweeteners"),
    GRAINS_AND_FLOURS(1, "screen.jazzycookin.pantry_tab.grains_flours"),
    LEAVENING_AGENTS(2, "screen.jazzycookin.pantry_tab.leavening_agents"),
    SEASONINGS(3, "screen.jazzycookin.pantry_tab.seasonings"),
    OILS_AND_FATS(4, "screen.jazzycookin.pantry_tab.oils_fats"),
    DAIRY_AND_ALTERNATIVES(5, "screen.jazzycookin.pantry_tab.dairy_alternatives"),
    CANNED_GOODS(6, "screen.jazzycookin.pantry_tab.canned_goods"),
    BAKING_ADD_INS(7, "screen.jazzycookin.pantry_tab.baking_add_ins"),
    DRY_GOODS(8, "screen.jazzycookin.pantry_tab.dry_goods"),
    SAUCES_AND_CONDIMENTS(9, "screen.jazzycookin.pantry_tab.sauces_condiments"),
    SNACKS(10, "screen.jazzycookin.pantry_tab.snacks"),
    OTHER(-1, "");

    private static final List<PantrySortTab> TABS = List.of(
            SWEETENERS,
            GRAINS_AND_FLOURS,
            LEAVENING_AGENTS,
            SEASONINGS,
            OILS_AND_FATS,
            DAIRY_AND_ALTERNATIVES,
            CANNED_GOODS,
            BAKING_ADD_INS,
            DRY_GOODS,
            SAUCES_AND_CONDIMENTS,
            SNACKS
    );

    private final int buttonId;
    private final String translationKey;

    PantrySortTab(int buttonId, String translationKey) {
        this.buttonId = buttonId;
        this.translationKey = translationKey;
    }

    public int buttonId() {
        return this.buttonId;
    }

    public Component label() {
        return Component.translatable(this.translationKey);
    }

    public ItemStack iconStack() {
        return switch (this) {
            case SWEETENERS -> JazzyItems.creativeIngredientStack(JazzyItems.IngredientId.WHITE_SUGAR);
            case GRAINS_AND_FLOURS -> JazzyItems.creativeIngredientStack(JazzyItems.IngredientId.ALL_PURPOSE_FLOUR);
            case LEAVENING_AGENTS -> JazzyItems.creativeIngredientStack(JazzyItems.IngredientId.BAKING_POWDER);
            case SEASONINGS -> JazzyItems.creativeIngredientStack(JazzyItems.IngredientId.BLACK_PEPPER);
            case OILS_AND_FATS -> JazzyItems.creativeIngredientStack(JazzyItems.IngredientId.OLIVE_OIL);
            case DAIRY_AND_ALTERNATIVES -> JazzyItems.creativeIngredientStack(JazzyItems.IngredientId.SHELF_STABLE_CREAM);
            case CANNED_GOODS -> JazzyItems.creativeIngredientStack(JazzyItems.IngredientId.CANNED_TOMATOES);
            case BAKING_ADD_INS -> JazzyItems.creativeIngredientStack(JazzyItems.IngredientId.CHOCOLATE_CHIPS);
            case DRY_GOODS -> JazzyItems.creativeIngredientStack(JazzyItems.IngredientId.LENTILS);
            case SAUCES_AND_CONDIMENTS -> JazzyItems.creativeIngredientStack(JazzyItems.IngredientId.SOY_SAUCE);
            case SNACKS -> JazzyItems.creativeIngredientStack(JazzyItems.IngredientId.CRACKERS);
            case OTHER -> ItemStack.EMPTY;
        };
    }

    public static List<PantrySortTab> tabs() {
        return TABS;
    }

    public static PantrySortTab byButtonId(int buttonId) {
        for (PantrySortTab tab : TABS) {
            if (tab.buttonId == buttonId) {
                return tab;
            }
        }
        return null;
    }

    public static PantrySortTab classify(ItemStack stack) {
        if (stack.isEmpty()) {
            return OTHER;
        }

        if (stack.getItem() instanceof KitchenIngredientItem ingredientItem) {
            return ingredientItem.pantryTab();
        }

        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (containsAny(path, "sugar", "sweet", "honey", "syrup")) {
            return SWEETENERS;
        }
        if (containsAny(path, "flour", "grain", "wheat", "rice", "oat")) {
            return GRAINS_AND_FLOURS;
        }
        if (containsAny(path, "yeast", "starter", "leaven", "baking_powder", "baking_soda")) {
            return LEAVENING_AGENTS;
        }
        if (containsAny(path, "season", "spice", "salt", "pepper", "herb")) {
            return SEASONINGS;
        }
        if (containsAny(path, "oil", "butter", "fat", "lard", "shortening")) {
            return OILS_AND_FATS;
        }
        if (containsAny(path, "milk", "cream", "dairy", "almond", "oat_milk", "soy")) {
            return DAIRY_AND_ALTERNATIVES;
        }
        if (containsAny(path, "canned", "pickle", "preserve", "jar")) {
            return CANNED_GOODS;
        }
        if (containsAny(path, "chip", "cocoa", "vanilla", "nut", "raisin", "cranberry")) {
            return BAKING_ADD_INS;
        }
        if (containsAny(path, "dry", "bean", "lentil", "pasta", "ramen", "couscous")) {
            return DRY_GOODS;
        }
        if (containsAny(path, "sauce", "condiment", "vinegar", "mustard", "ketchup", "mayo")) {
            return SAUCES_AND_CONDIMENTS;
        }
        if (containsAny(path, "snack", "cookie", "cracker", "candy", "chips")) {
            return SNACKS;
        }

        return OTHER;
    }

    private static boolean containsAny(String path, String... needles) {
        for (String needle : needles) {
            if (path.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
