package com.boaat.jazzy_cookin.kitchen;

import com.boaat.jazzy_cookin.item.KitchenIngredientItem;
import com.boaat.jazzy_cookin.item.KitchenMealItem;

import net.minecraft.world.item.ItemStack;

public final class StorageRules {
    private StorageRules() {
    }

    public static boolean canStore(StorageType storageType, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }

        return switch (storageType) {
            case PANTRY -> isPantryItem(stack);
            case FRIDGE -> isFridgeItem(stack);
            case FREEZER -> isFreezerItem(stack);
        };
    }

    private static boolean isPantryItem(ItemStack stack) {
        if (stack.getItem() instanceof KitchenIngredientItem ingredientItem) {
            return ingredientItem.pantryTab() != PantrySortTab.OTHER;
        }
        return PantrySortTab.classify(stack) != PantrySortTab.OTHER;
    }

    private static boolean isFridgeItem(ItemStack stack) {
        if (stack.getItem() instanceof KitchenMealItem) {
            return true;
        }
        if (stack.getItem() instanceof KitchenIngredientItem ingredientItem) {
            return ingredientItem.isFridgeSafe();
        }
        return false;
    }

    private static boolean isFreezerItem(ItemStack stack) {
        if (stack.getItem() instanceof KitchenMealItem) {
            return true;
        }
        if (stack.getItem() instanceof KitchenIngredientItem ingredientItem) {
            return ingredientItem.isFreezerSafe();
        }
        return false;
    }
}
