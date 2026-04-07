package com.boaat.jazzy_cookin.recipebook;

import com.boaat.jazzy_cookin.kitchen.IngredientState;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record JazzyRecipeBookSelection(ResourceLocation itemId, IngredientState state, String chainKey) {
    public static final StreamCodec<RegistryFriendlyByteBuf, JazzyRecipeBookSelection> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            JazzyRecipeBookSelection::itemId,
            IngredientState.STREAM_CODEC,
            JazzyRecipeBookSelection::state,
            ByteBufCodecs.STRING_UTF8,
            JazzyRecipeBookSelection::chainKey,
            JazzyRecipeBookSelection::new
    );

    public String normalizedChainKey() {
        return this.chainKey == null ? "" : this.chainKey.trim();
    }
}
