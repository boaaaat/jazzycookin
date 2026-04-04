package com.boaat.jazzy_cookin.recipe;

import java.util.List;

import com.boaat.jazzy_cookin.kitchen.HeatLevel;
import com.boaat.jazzy_cookin.kitchen.StationType;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record KitchenProcessInput(
        List<ItemStack> inputs,
        ItemStack tool,
        StationType station,
        HeatLevel heat,
        boolean preheated
) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return index >= 0 && index < this.inputs.size() ? this.inputs.get(index) : ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return this.inputs.size();
    }
}
