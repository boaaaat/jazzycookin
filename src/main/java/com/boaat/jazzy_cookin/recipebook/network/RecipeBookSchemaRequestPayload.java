package com.boaat.jazzy_cookin.recipebook.network;

import com.boaat.jazzy_cookin.JazzyCookin;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RecipeBookSchemaRequestPayload() implements CustomPacketPayload {
    public static final Type<RecipeBookSchemaRequestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JazzyCookin.MODID, "recipe_book_schema_request"));
    public static final RecipeBookSchemaRequestPayload INSTANCE = new RecipeBookSchemaRequestPayload();

    public static final StreamCodec<RegistryFriendlyByteBuf, RecipeBookSchemaRequestPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public RecipeBookSchemaRequestPayload decode(RegistryFriendlyByteBuf buffer) {
            return INSTANCE;
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, RecipeBookSchemaRequestPayload value) {
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
