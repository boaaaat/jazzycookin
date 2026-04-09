package com.boaat.jazzy_cookin.recipe;

import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.KitchenStackUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Mth;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public record KitchenInputRequirement(Ingredient ingredient, IngredientState requiredState, int count) {
    public static final Codec<KitchenInputRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(KitchenInputRequirement::ingredient),
            IngredientState.CODEC.fieldOf("required_state").forGetter(KitchenInputRequirement::requiredState),
            Codec.INT.optionalFieldOf("count", 1).forGetter(KitchenInputRequirement::count)
    ).apply(instance, KitchenInputRequirement::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, KitchenInputRequirement> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, KitchenInputRequirement::ingredient,
            IngredientState.STREAM_CODEC, KitchenInputRequirement::requiredState,
            ByteBufCodecs.VAR_INT, KitchenInputRequirement::count,
            KitchenInputRequirement::new
    );

    public float matchScore(ItemStack stack, long gameTime) {
        if (stack.getCount() < this.count || !this.ingredient.test(stack)) {
            return 0.0F;
        }
        return this.stateScore(stack, gameTime) * this.ingredientFitScore(stack);
    }

    public float stateScore(ItemStack stack, long gameTime) {
        if (stack.getCount() < this.count || !this.ingredient.test(stack)) {
            return 0.0F;
        }
        return KitchenStackUtil.stateMatchScore(stack, this.requiredState, gameTime);
    }

    public float ingredientFitScore(ItemStack stack) {
        if (stack.getCount() < this.count || !this.ingredient.test(stack)) {
            return 0.0F;
        }
        int optionCount = this.optionCount();
        if (optionCount <= 1) {
            return 1.0F;
        }
        return this.exactItemMatch(stack)
                ? Mth.clamp(0.98F - (optionCount - 1) * 0.03F, 0.78F, 0.98F)
                : Mth.clamp(0.82F - (optionCount - 1) * 0.02F, 0.52F, 0.82F);
    }

    public float specificityScore() {
        return Mth.clamp(1.0F - (this.optionCount() - 1) * 0.08F, 0.45F, 1.0F);
    }

    public boolean exactItemMatch(ItemStack stack) {
        if (stack.getCount() < this.count || !this.ingredient.test(stack)) {
            return false;
        }
        for (ItemStack option : this.ingredient.getItems()) {
            if (!option.isEmpty() && option.getItem() == stack.getItem()) {
                return true;
            }
        }
        return false;
    }

    private int optionCount() {
        java.util.Set<Item> options = new java.util.HashSet<>();
        for (ItemStack option : this.ingredient.getItems()) {
            if (!option.isEmpty()) {
                options.add(option.getItem());
            }
        }
        return Math.max(1, options.size());
    }
}
