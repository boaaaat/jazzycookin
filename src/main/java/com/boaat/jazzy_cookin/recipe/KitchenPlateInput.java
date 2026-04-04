package com.boaat.jazzy_cookin.recipe;

import java.util.List;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record KitchenPlateInput(List<ItemStack> inputs) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return index >= 0 && index < this.inputs.size() ? this.inputs.get(index) : ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return this.inputs.size();
    }
}
