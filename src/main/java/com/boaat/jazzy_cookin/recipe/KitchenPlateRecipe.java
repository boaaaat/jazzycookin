package com.boaat.jazzy_cookin.recipe;

import java.util.List;
import java.util.Optional;

import com.boaat.jazzy_cookin.registry.JazzyRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public record KitchenPlateRecipe(
        List<KitchenInputRequirement> inputs,
        KitchenRecipeGuideData guide,
        KitchenRecipeBookData recipeBook,
        KitchenProcessOutput output
) implements Recipe<KitchenPlateInput> {
    @Override
    public boolean matches(KitchenPlateInput input, Level level) {
        return this.matchPlan(input, level).isPresent();
    }

    public Optional<KitchenRecipeMatchPlan> matchPlan(KitchenPlateInput input, Level level) {
        return Optional.ofNullable(KitchenRecipeMatching.findBestPlan(input.inputs(), this.inputs, this.guide, level.getGameTime()));
    }

    public float matchScore(KitchenPlateInput input, Level level) {
        return this.matchPlan(input, level).map(KitchenRecipeMatchPlan::score).orElse(0.0F);
    }

    @Override
    public ItemStack assemble(KitchenPlateInput input, HolderLookup.Provider registries) {
        return this.output.result().copy();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.output.result().copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public RecipeSerializer<? extends Recipe<KitchenPlateInput>> getSerializer() {
        return JazzyRecipes.KITCHEN_PLATE_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends Recipe<KitchenPlateInput>> getType() {
        return JazzyRecipes.KITCHEN_PLATE_TYPE.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }
}
