package com.boaat.jazzy_cookin.kitchen;

import java.util.List;

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

        if (stack.is(JazzyItems.CANE_SUGAR.get()) || stack.is(JazzyItems.CANNING_SYRUP.get())) {
            return SWEETENERS;
        }
        if (stack.is(JazzyItems.FLOUR.get()) || stack.is(JazzyItems.WHEAT_SHEAF.get())) {
            return GRAINS_AND_FLOURS;
        }
        if (stack.is(JazzyItems.SALT.get()) || stack.is(JazzyItems.BAKING_SPICE.get()) || stack.is(JazzyItems.FRESH_HERB.get()) || stack.is(JazzyItems.GROUND_HERB.get())) {
            return SEASONINGS;
        }
        if (stack.is(JazzyItems.FRYING_OIL.get()) || stack.is(JazzyItems.USED_OIL_ITEM.get()) || stack.is(JazzyItems.DIRTY_OIL_ITEM.get()) || stack.is(JazzyItems.BURNT_OIL_ITEM.get()) || stack.is(JazzyItems.BUTTER.get())) {
            return OILS_AND_FATS;
        }
        if (stack.is(JazzyItems.FRESH_MILK.get()) || stack.is(JazzyItems.CULTURED_DAIRY_ITEM.get())) {
            return DAIRY_AND_ALTERNATIVES;
        }
        if (stack.is(JazzyItems.CANNED_TOMATO_ITEM.get()) || stack.is(JazzyItems.APPLE_PRESERVE_ITEM.get()) || stack.is(JazzyItems.CANNING_JAR.get())) {
            return CANNED_GOODS;
        }
        if (stack.is(JazzyItems.FARM_EGG.get()) || stack.is(JazzyItems.WILD_BERRIES_ITEM.get()) || stack.is(JazzyItems.ORCHARD_APPLE.get()) || stack.is(JazzyItems.PIE_FILLING.get())) {
            return BAKING_ADD_INS;
        }
        if (stack.is(JazzyItems.DRIED_APPLE_ITEM.get())) {
            return DRY_GOODS;
        }
        if (stack.is(JazzyItems.MARINADE.get()) || stack.is(JazzyItems.BRINE.get())) {
            return SAUCES_AND_CONDIMENTS;
        }
        if (stack.is(JazzyItems.APPLE_PIE.get()) || stack.is(JazzyItems.APPLE_PIE_SLICE.get()) || stack.is(JazzyItems.BOILED_EGG_ITEM.get()) || stack.is(JazzyItems.SLICED_BREAD.get())) {
            return SNACKS;
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
        if (containsAny(path, "oil", "butter", "fat", "lard", "ghee", "shortening")) {
            return OILS_AND_FATS;
        }
        if (containsAny(path, "milk", "cream", "dairy", "yogurt", "cheese", "almond", "oat_milk", "soy")) {
            return DAIRY_AND_ALTERNATIVES;
        }
        if (containsAny(path, "canned", "pickle", "preserve", "jar")) {
            return CANNED_GOODS;
        }
        if (containsAny(path, "chip", "cocoa", "vanilla", "berry", "apple", "egg", "nut")) {
            return BAKING_ADD_INS;
        }
        if (containsAny(path, "dry", "dried", "bean", "lentil", "pasta")) {
            return DRY_GOODS;
        }
        if (containsAny(path, "sauce", "condiment", "marinade", "brine", "vinegar", "mustard", "ketchup")) {
            return SAUCES_AND_CONDIMENTS;
        }
        if (containsAny(path, "snack", "cookie", "cracker", "pie", "slice", "bread")) {
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
