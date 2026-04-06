package com.boaat.jazzy_cookin.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record KitchenEnvironmentRequirements(
        boolean nearbyWater,
        boolean sheltered
) {
    public static final KitchenEnvironmentRequirements NONE = new KitchenEnvironmentRequirements(false, false);

    public static final Codec<KitchenEnvironmentRequirements> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("nearby_water", false).forGetter(KitchenEnvironmentRequirements::nearbyWater),
            Codec.BOOL.optionalFieldOf("sheltered", false).forGetter(KitchenEnvironmentRequirements::sheltered)
    ).apply(instance, KitchenEnvironmentRequirements::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, KitchenEnvironmentRequirements> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, KitchenEnvironmentRequirements::nearbyWater,
            ByteBufCodecs.BOOL, KitchenEnvironmentRequirements::sheltered,
            KitchenEnvironmentRequirements::new
    );

    public boolean isEmpty() {
        return !this.nearbyWater && !this.sheltered;
    }
}
