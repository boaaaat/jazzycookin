package com.boaat.jazzy_cookin.recipe;

import java.util.List;
import java.util.Optional;

import com.boaat.jazzy_cookin.kitchen.HeatLevel;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.ToolProfile;
import com.boaat.jazzy_cookin.registry.JazzyRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public record KitchenProcessRecipe(
        StationType station,
        List<KitchenInputRequirement> inputs,
        Optional<ToolProfile> preferredTool,
        int duration,
        HeatLevel preferredHeat,
        boolean requiresPreheat,
        KitchenProcessOutput output
) implements Recipe<KitchenProcessInput> {
    @Override
    public boolean matches(KitchenProcessInput input, Level level) {
        if (input.station() != this.station) {
            return false;
        }

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
    public ItemStack assemble(KitchenProcessInput input, HolderLookup.Provider registries) {
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
    public RecipeSerializer<? extends Recipe<KitchenProcessInput>> getSerializer() {
        return JazzyRecipes.KITCHEN_PROCESS_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends Recipe<KitchenProcessInput>> getType() {
        return JazzyRecipes.KITCHEN_PROCESS_TYPE.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }
}
