package com.boaat.jazzy_cookin.recipebook.network;

import java.util.ArrayList;
import java.util.List;

import com.boaat.jazzy_cookin.JazzyCookin;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RecipeBookSchemaSyncPayload(List<String> schemas) implements CustomPacketPayload {
    public static final Type<RecipeBookSchemaSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JazzyCookin.MODID, "recipe_book_schema_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RecipeBookSchemaSyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public RecipeBookSchemaSyncPayload decode(RegistryFriendlyByteBuf buffer) {
            return new RecipeBookSchemaSyncPayload(ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8).decode(buffer));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, RecipeBookSchemaSyncPayload value) {
            ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8).encode(buffer, new ArrayList<>(value.schemas()));
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
