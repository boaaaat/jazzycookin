package com.boaat.jazzy_cookin.recipe;

import java.util.ArrayList;

import com.boaat.jazzy_cookin.kitchen.HeatLevel;
import com.boaat.jazzy_cookin.kitchen.KitchenMethod;
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
            KitchenMethod.CODEC.optionalFieldOf("method", KitchenMethod.NONE).forGetter(KitchenProcessRecipe::method),
            ToolProfile.CODEC.optionalFieldOf("preferred_tool").forGetter(KitchenProcessRecipe::preferredTool),
            Codec.BOOL.optionalFieldOf("tool_required", false).forGetter(KitchenProcessRecipe::toolRequired),
            Codec.INT.optionalFieldOf("duration", 60).forGetter(KitchenProcessRecipe::duration),
            HeatLevel.CODEC.optionalFieldOf("preferred_heat", HeatLevel.OFF).forGetter(KitchenProcessRecipe::preferredHeat),
            HeatLevel.CODEC.optionalFieldOf("minimum_heat", HeatLevel.OFF).forGetter(KitchenProcessRecipe::minimumHeat),
            HeatLevel.CODEC.optionalFieldOf("maximum_heat", HeatLevel.OFF).forGetter(KitchenProcessRecipe::maximumHeat),
            Codec.BOOL.optionalFieldOf("requires_preheat", false).forGetter(KitchenProcessRecipe::requiresPreheat),
            ProcessMode.CODEC.optionalFieldOf("mode", ProcessMode.ACTIVE).forGetter(KitchenProcessRecipe::mode),
            Codec.BOOL.optionalFieldOf("requires_nearby_water", false).forGetter(KitchenProcessRecipe::requiresNearbyWater),
            KitchenProcessOutput.CODEC.fieldOf("output").forGetter(KitchenProcessRecipe::output)
    ).apply(instance, (station, inputs, method, preferredTool, toolRequired, duration, preferredHeat, minimumHeat, maximumHeat, requiresPreheat, mode, requiresNearbyWater, output) ->
            new KitchenProcessRecipe(
                    station,
                    inputs,
                    method == KitchenMethod.NONE
                            ? KitchenMethod.infer(station, preferredHeat, preferredTool.orElse(ToolProfile.NONE), output.state(), mode, requiresNearbyWater, requiresPreheat)
                            : method,
                    preferredTool,
                    toolRequired,
                    duration,
                    preferredHeat,
                    minimumHeat == HeatLevel.OFF && preferredHeat != HeatLevel.OFF ? preferredHeat : minimumHeat,
                    maximumHeat == HeatLevel.OFF && preferredHeat != HeatLevel.OFF ? preferredHeat : maximumHeat,
                    requiresPreheat,
                    mode,
                    requiresNearbyWater,
                    output
            )));

    public static final StreamCodec<RegistryFriendlyByteBuf, KitchenProcessRecipe> STREAM_CODEC = StreamCodec.composite(
            StationType.STREAM_CODEC, KitchenProcessRecipe::station,
            ByteBufCodecs.collection(ArrayList::new, KitchenInputRequirement.STREAM_CODEC), KitchenProcessRecipe::inputs,
            KitchenMethod.STREAM_CODEC, KitchenProcessRecipe::method,
            ByteBufCodecs.optional(ToolProfile.STREAM_CODEC), KitchenProcessRecipe::preferredTool,
            ByteBufCodecs.BOOL, KitchenProcessRecipe::toolRequired,
            ByteBufCodecs.VAR_INT, KitchenProcessRecipe::duration,
            HeatLevel.STREAM_CODEC, KitchenProcessRecipe::preferredHeat,
            HeatLevel.STREAM_CODEC, KitchenProcessRecipe::minimumHeat,
            HeatLevel.STREAM_CODEC, KitchenProcessRecipe::maximumHeat,
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
