package com.boaat.jazzy_cookin.kitchen;

import com.boaat.jazzy_cookin.item.KitchenIngredientItem;
import com.boaat.jazzy_cookin.item.KitchenMealItem;
import com.boaat.jazzy_cookin.kitchen.sim.FoodMatterData;
import com.boaat.jazzy_cookin.kitchen.sim.FoodTrait;

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
            if (ingredientItem.pantryTab() != PantrySortTab.OTHER) {
                return true;
            }
        }

        FoodMatterData matter = KitchenStackUtil.getFoodMatter(stack);
        if (matter != null && matter.isPreservedShelfStable()) {
            return true;
        }
        return PantrySortTab.classify(stack) != PantrySortTab.OTHER;
    }

    private static boolean isFridgeItem(ItemStack stack) {
        if (stack.getItem() instanceof KitchenMealItem) {
            return true;
        }
        if (stack.getItem() instanceof KitchenIngredientItem ingredientItem) {
            if (ingredientItem.isFridgeSafe()) {
                return true;
            }
        }

        FoodMatterData matter = KitchenStackUtil.getFoodMatter(stack);
        return matter != null
                && !matter.isPreservedShelfStable()
                && (matter.protein() >= 0.16F
                || matter.water() >= 0.56F
                || matter.hasTrait(FoodTrait.LEAFY_GREEN)
                || matter.hasTrait(FoodTrait.DAIRY));
    }

    private static boolean isFreezerItem(ItemStack stack) {
        if (stack.getItem() instanceof KitchenMealItem) {
            return true;
        }
        if (stack.getItem() instanceof KitchenIngredientItem ingredientItem) {
            if (ingredientItem.isFreezerSafe()) {
                return true;
            }
        }

        FoodMatterData matter = KitchenStackUtil.getFoodMatter(stack);
        return matter != null
                && !matter.hasTrait(FoodTrait.HERB)
                && !matter.hasTrait(FoodTrait.LEAFY_GREEN)
                && (matter.protein() >= 0.18F
                || (matter.finalizedServing() && matter.water() >= 0.24F));
    }
}
