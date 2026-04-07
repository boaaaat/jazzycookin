package com.boaat.jazzy_cookin.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record KitchenRecipeBookData(String chainKey) {
    public static final Codec<KitchenRecipeBookData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("chain_key").forGetter(KitchenRecipeBookData::chainKey)
    ).apply(instance, KitchenRecipeBookData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, KitchenRecipeBookData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            KitchenRecipeBookData::chainKey,
            KitchenRecipeBookData::new
    );

    public String normalizedChainKey() {
        return this.chainKey == null ? "" : this.chainKey.trim();
    }
}
