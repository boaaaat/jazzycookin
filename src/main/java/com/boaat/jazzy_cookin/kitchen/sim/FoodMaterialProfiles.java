package com.boaat.jazzy_cookin.kitchen.sim;

import java.util.Optional;

import com.boaat.jazzy_cookin.registry.JazzyItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class FoodMaterialProfiles {
    public static final FoodMaterialProfile EGGS = new FoodMaterialProfile(0.76F, 0.22F, 0.82F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.10F);
    public static final FoodMaterialProfile BUTTER = new FoodMaterialProfile(0.18F, 0.90F, 0.06F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
    public static final FoodMaterialProfile VEGETABLE_OIL = new FoodMaterialProfile(0.02F, 0.98F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
    public static final FoodMaterialProfile CHEESE = new FoodMaterialProfile(0.44F, 0.58F, 0.40F, 0.0F, 0.28F, 0.0F, 0.0F, 0.0F, 0.0F);
    public static final FoodMaterialProfile ONIONS = new FoodMaterialProfile(0.84F, 0.02F, 0.04F, 0.0F, 0.0F, 0.24F, 0.0F, 0.0F, 0.0F);
    public static final FoodMaterialProfile PARSLEY = new FoodMaterialProfile(0.64F, 0.02F, 0.04F, 0.0F, 0.0F, 0.0F, 0.20F, 0.0F, 0.0F);
    public static final FoodMaterialProfile SALT = new FoodMaterialProfile(0.0F, 0.0F, 0.0F, 0.28F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
    public static final FoodMaterialProfile BLACK_PEPPER = new FoodMaterialProfile(0.0F, 0.02F, 0.02F, 0.12F, 0.0F, 0.0F, 0.0F, 0.18F, 0.0F);

    private FoodMaterialProfiles() {
    }

    public static Optional<FoodMaterialProfile> profileFor(ItemStack stack) {
        Item item = stack.getItem();
        if (item == JazzyItems.ingredient(JazzyItems.IngredientId.EGGS).get()) {
            return Optional.of(EGGS);
        }
        if (item == JazzyItems.ingredient(JazzyItems.IngredientId.BUTTER).get()) {
            return Optional.of(BUTTER);
        }
        if (item == JazzyItems.ingredient(JazzyItems.IngredientId.VEGETABLE_OIL).get()) {
            return Optional.of(VEGETABLE_OIL);
        }
        if (item == JazzyItems.ingredient(JazzyItems.IngredientId.CHEESE).get()) {
            return Optional.of(CHEESE);
        }
        if (item == JazzyItems.ingredient(JazzyItems.IngredientId.ONIONS).get()) {
            return Optional.of(ONIONS);
        }
        if (item == JazzyItems.ingredient(JazzyItems.IngredientId.PARSLEY).get()) {
            return Optional.of(PARSLEY);
        }
        if (item == JazzyItems.ingredient(JazzyItems.IngredientId.TABLE_SALT).get()
                || item == JazzyItems.ingredient(JazzyItems.IngredientId.KOSHER_SALT).get()
                || item == JazzyItems.ingredient(JazzyItems.IngredientId.SEA_SALT).get()) {
            return Optional.of(SALT);
        }
        if (item == JazzyItems.ingredient(JazzyItems.IngredientId.BLACK_PEPPER).get()) {
            return Optional.of(BLACK_PEPPER);
        }
        return Optional.empty();
    }

    public static boolean isMixingBowlAddIn(ItemStack stack) {
        return profileFor(stack).filter(profile -> profile != EGGS && profile != BUTTER && profile != VEGETABLE_OIL).isPresent();
    }

    public static boolean isStoveFat(ItemStack stack) {
        return profileFor(stack).filter(profile -> profile == BUTTER || profile == VEGETABLE_OIL).isPresent();
    }
}
