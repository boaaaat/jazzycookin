package com.boaat.jazzy_cookin.recipe;

import java.util.ArrayList;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class KitchenPlateSerializer implements RecipeSerializer<KitchenPlateRecipe> {
    public static final MapCodec<KitchenPlateRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            KitchenInputRequirement.CODEC.listOf().fieldOf("inputs").forGetter(KitchenPlateRecipe::inputs),
            KitchenProcessOutput.CODEC.fieldOf("output").forGetter(KitchenPlateRecipe::output)
    ).apply(instance, KitchenPlateRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, KitchenPlateRecipe> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, KitchenInputRequirement.STREAM_CODEC), KitchenPlateRecipe::inputs,
            KitchenProcessOutput.STREAM_CODEC, KitchenPlateRecipe::output,
            KitchenPlateRecipe::new
    );

    @Override
    public MapCodec<KitchenPlateRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, KitchenPlateRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
