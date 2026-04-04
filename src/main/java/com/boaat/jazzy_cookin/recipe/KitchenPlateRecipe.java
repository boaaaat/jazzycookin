package com.boaat.jazzy_cookin.recipe;

import java.util.List;

import com.boaat.jazzy_cookin.registry.JazzyRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public record KitchenPlateRecipe(
        List<KitchenInputRequirement> inputs,
        KitchenProcessOutput output
) implements Recipe<KitchenPlateInput> {
    @Override
    public boolean matches(KitchenPlateInput input, Level level) {
        if (input.inputs().size() < this.inputs.size()) {
            return false;
        }

        for (int i = 0; i < this.inputs.size(); i++) {
            if (!this.inputs.get(i).matches(input.inputs().get(i), level.getGameTime())) {
                return false;
            }
        }

        for (int i = this.inputs.size(); i < input.inputs().size(); i++) {
            if (!input.inputs().get(i).isEmpty()) {
                return false;
            }
        }

        return true;
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
