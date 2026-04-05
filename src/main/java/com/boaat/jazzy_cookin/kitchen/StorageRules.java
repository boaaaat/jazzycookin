package com.boaat.jazzy_cookin.kitchen;

import java.util.function.Supplier;

import com.boaat.jazzy_cookin.item.KitchenMealItem;
import com.boaat.jazzy_cookin.registry.JazzyItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class StorageRules {
    private StorageRules() {
    }

    public static boolean canStore(StorageType storageType, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }

        return switch (storageType) {
            case PANTRY -> PantrySortTab.classify(stack) != PantrySortTab.OTHER;
            case FRIDGE -> isFridgeItem(stack);
            case FREEZER -> isFreezerItem(stack);
        };
    }

    private static boolean isFridgeItem(ItemStack stack) {
        if (stack.getItem() instanceof KitchenMealItem) {
            return true;
        }

        return matchesAny(
                stack,
                JazzyItems.ORCHARD_APPLE,
                JazzyItems.WILD_BERRIES_ITEM,
                JazzyItems.TOMATO,
                JazzyItems.CHOPPED_TOMATO,
                JazzyItems.FRESH_HERB,
                JazzyItems.GROUND_HERB,
                JazzyItems.CABBAGE,
                JazzyItems.CHOPPED_CABBAGE,
                JazzyItems.ONION,
                JazzyItems.DICED_ONION,
                JazzyItems.FARM_EGG,
                JazzyItems.BOILED_EGG_ITEM,
                JazzyItems.FRESH_MILK,
                JazzyItems.BUTTER,
                JazzyItems.RAW_FISH_ITEM,
                JazzyItems.CLEANED_FISH,
                JazzyItems.PAN_FRIED_FISH,
                JazzyItems.RAW_PROTEIN,
                JazzyItems.ROAST_CUT,
                JazzyItems.EGG_WASH,
                JazzyItems.MARINADE,
                JazzyItems.BATTER,
                JazzyItems.PIE_CRUST,
                JazzyItems.PIE_FILLING,
                JazzyItems.APPLE_PIE,
                JazzyItems.APPLE_PIE_SLICE,
                JazzyItems.BREAD_DOUGH,
                JazzyItems.BREAD_LOAF,
                JazzyItems.SLICED_BREAD,
                JazzyItems.TOMATO_SOUP_BASE,
                JazzyItems.STRAINED_SOUP,
                JazzyItems.DUMPLING_FILLING_ITEM,
                JazzyItems.DUMPLING_DOUGH_ITEM,
                JazzyItems.RAW_DUMPLINGS_ITEM,
                JazzyItems.STEAMED_DUMPLINGS_ITEM,
                JazzyItems.MARINATED_PROTEIN_ITEM,
                JazzyItems.BATTERED_PROTEIN_ITEM,
                JazzyItems.FRIED_PROTEIN_ITEM,
                JazzyItems.ROASTED_PROTEIN_ITEM,
                JazzyItems.BROILED_PROTEIN_ITEM,
                JazzyItems.SMOKED_PROTEIN_ITEM,
                JazzyItems.ROAST_VEGETABLES_ITEM,
                JazzyItems.FERMENTED_VEGETABLES_ITEM,
                JazzyItems.CULTURED_DAIRY_ITEM
        );
    }

    private static boolean isFreezerItem(ItemStack stack) {
        if (stack.getItem() instanceof KitchenMealItem) {
            return true;
        }

        return matchesAny(
                stack,
                JazzyItems.WILD_BERRIES_ITEM,
                JazzyItems.CHOPPED_TOMATO,
                JazzyItems.CHOPPED_CABBAGE,
                JazzyItems.DICED_ONION,
                JazzyItems.FRESH_MILK,
                JazzyItems.BUTTER,
                JazzyItems.RAW_FISH_ITEM,
                JazzyItems.CLEANED_FISH,
                JazzyItems.PAN_FRIED_FISH,
                JazzyItems.RAW_PROTEIN,
                JazzyItems.ROAST_CUT,
                JazzyItems.PIE_CRUST,
                JazzyItems.PIE_FILLING,
                JazzyItems.APPLE_PIE,
                JazzyItems.APPLE_PIE_SLICE,
                JazzyItems.BREAD_DOUGH,
                JazzyItems.BREAD_LOAF,
                JazzyItems.SLICED_BREAD,
                JazzyItems.TOMATO_SOUP_BASE,
                JazzyItems.STRAINED_SOUP,
                JazzyItems.DUMPLING_FILLING_ITEM,
                JazzyItems.DUMPLING_DOUGH_ITEM,
                JazzyItems.RAW_DUMPLINGS_ITEM,
                JazzyItems.STEAMED_DUMPLINGS_ITEM,
                JazzyItems.MARINATED_PROTEIN_ITEM,
                JazzyItems.BATTERED_PROTEIN_ITEM,
                JazzyItems.FRIED_PROTEIN_ITEM,
                JazzyItems.ROASTED_PROTEIN_ITEM,
                JazzyItems.BROILED_PROTEIN_ITEM,
                JazzyItems.SMOKED_PROTEIN_ITEM,
                JazzyItems.ROAST_VEGETABLES_ITEM
        );
    }

    @SafeVarargs
    private static boolean matchesAny(ItemStack stack, Supplier<? extends Item>... items) {
        for (Supplier<? extends Item> item : items) {
            if (stack.is(item.get())) {
                return true;
            }
        }
        return false;
    }
}
