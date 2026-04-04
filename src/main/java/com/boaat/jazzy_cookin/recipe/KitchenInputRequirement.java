package com.boaat.jazzy_cookin.recipe;

import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.KitchenStackUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
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

    public boolean matches(ItemStack stack, long gameTime) {
        return stack.getCount() >= this.count && this.ingredient.test(stack) && KitchenStackUtil.matchesState(stack, this.requiredState, gameTime);
    }
}
