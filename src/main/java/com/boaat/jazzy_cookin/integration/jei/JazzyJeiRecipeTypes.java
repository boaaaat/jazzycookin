package com.boaat.jazzy_cookin.integration.jei;

import com.boaat.jazzy_cookin.JazzyCookin;
import com.boaat.jazzy_cookin.recipe.KitchenPlateRecipe;
import com.boaat.jazzy_cookin.recipe.KitchenProcessRecipe;

import mezz.jei.api.recipe.RecipeType;
import net.minecraft.resources.ResourceLocation;

public final class JazzyJeiRecipeTypes {
    public static final RecipeType<KitchenProcessRecipe> KITCHEN_PROCESS = new RecipeType<>(
            ResourceLocation.fromNamespaceAndPath(JazzyCookin.MODID, "kitchen_process"),
            KitchenProcessRecipe.class
    );
    public static final RecipeType<KitchenPlateRecipe> KITCHEN_PLATE = new RecipeType<>(
            ResourceLocation.fromNamespaceAndPath(JazzyCookin.MODID, "kitchen_plate"),
            KitchenPlateRecipe.class
    );

    private JazzyJeiRecipeTypes() {
    }
}
