package com.boaat.jazzy_cookin.recipe;

import com.boaat.jazzy_cookin.kitchen.KitchenOutcomeBand;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record KitchenProcessOutcome(
        KitchenOutcomeBand band,
        KitchenProcessOutput output
) {
    public static final Codec<KitchenProcessOutcome> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            KitchenOutcomeBand.CODEC.fieldOf("band").forGetter(KitchenProcessOutcome::band),
            KitchenProcessOutput.CODEC.fieldOf("output").forGetter(KitchenProcessOutcome::output)
    ).apply(instance, KitchenProcessOutcome::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, KitchenProcessOutcome> STREAM_CODEC = StreamCodec.composite(
            KitchenOutcomeBand.STREAM_CODEC, KitchenProcessOutcome::band,
            KitchenProcessOutput.STREAM_CODEC, KitchenProcessOutcome::output,
            KitchenProcessOutcome::new
    );
}
