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
            KitchenRecipeGuideData.CODEC.optionalFieldOf("guide", KitchenRecipeGuideData.DEFAULT).forGetter(KitchenPlateRecipe::guide),
            KitchenRecipeBookData.CODEC.fieldOf("recipe_book").forGetter(KitchenPlateRecipe::recipeBook),
            KitchenProcessOutput.CODEC.fieldOf("output").forGetter(KitchenPlateRecipe::output)
    ).apply(instance, KitchenPlateRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, KitchenPlateRecipe> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, KitchenInputRequirement.STREAM_CODEC), KitchenPlateRecipe::inputs,
            KitchenRecipeGuideData.STREAM_CODEC, KitchenPlateRecipe::guide,
            KitchenRecipeBookData.STREAM_CODEC, KitchenPlateRecipe::recipeBook,
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
