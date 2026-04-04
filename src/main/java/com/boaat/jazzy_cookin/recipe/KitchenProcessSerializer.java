package com.boaat.jazzy_cookin.recipe;

import java.util.ArrayList;

import com.boaat.jazzy_cookin.kitchen.HeatLevel;
import com.boaat.jazzy_cookin.kitchen.ProcessMode;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.ToolProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class KitchenProcessSerializer implements RecipeSerializer<KitchenProcessRecipe> {
    public static final MapCodec<KitchenProcessRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StationType.CODEC.fieldOf("station").forGetter(KitchenProcessRecipe::station),
            KitchenInputRequirement.CODEC.listOf().fieldOf("inputs").forGetter(KitchenProcessRecipe::inputs),
            ToolProfile.CODEC.optionalFieldOf("preferred_tool").forGetter(KitchenProcessRecipe::preferredTool),
            Codec.INT.optionalFieldOf("duration", 60).forGetter(KitchenProcessRecipe::duration),
            HeatLevel.CODEC.optionalFieldOf("preferred_heat", HeatLevel.OFF).forGetter(KitchenProcessRecipe::preferredHeat),
            Codec.BOOL.optionalFieldOf("requires_preheat", false).forGetter(KitchenProcessRecipe::requiresPreheat),
            ProcessMode.CODEC.optionalFieldOf("mode", ProcessMode.ACTIVE).forGetter(KitchenProcessRecipe::mode),
            Codec.BOOL.optionalFieldOf("requires_nearby_water", false).forGetter(KitchenProcessRecipe::requiresNearbyWater),
            KitchenProcessOutput.CODEC.fieldOf("output").forGetter(KitchenProcessRecipe::output)
    ).apply(instance, KitchenProcessRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, KitchenProcessRecipe> STREAM_CODEC = StreamCodec.composite(
            StationType.STREAM_CODEC, KitchenProcessRecipe::station,
            ByteBufCodecs.collection(ArrayList::new, KitchenInputRequirement.STREAM_CODEC), KitchenProcessRecipe::inputs,
            ByteBufCodecs.optional(ToolProfile.STREAM_CODEC), KitchenProcessRecipe::preferredTool,
            ByteBufCodecs.VAR_INT, KitchenProcessRecipe::duration,
            HeatLevel.STREAM_CODEC, KitchenProcessRecipe::preferredHeat,
            ByteBufCodecs.BOOL, KitchenProcessRecipe::requiresPreheat,
            ProcessMode.STREAM_CODEC, KitchenProcessRecipe::mode,
            ByteBufCodecs.BOOL, KitchenProcessRecipe::requiresNearbyWater,
            KitchenProcessOutput.STREAM_CODEC, KitchenProcessRecipe::output,
            KitchenProcessRecipe::new
    );

    @Override
    public MapCodec<KitchenProcessRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, KitchenProcessRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
