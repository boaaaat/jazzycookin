package com.boaat.jazzy_cookin.recipebook.network;

import org.jetbrains.annotations.Nullable;

import com.boaat.jazzy_cookin.JazzyCookin;
import com.boaat.jazzy_cookin.recipebook.JazzyRecipeBookSelection;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RecipeBookSelectionPayload(boolean pinned, @Nullable JazzyRecipeBookSelection selection) implements CustomPacketPayload {
    public static final Type<RecipeBookSelectionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JazzyCookin.MODID, "recipe_book_selection"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RecipeBookSelectionPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public RecipeBookSelectionPayload decode(RegistryFriendlyByteBuf buffer) {
            boolean pinned = buffer.readBoolean();
            return new RecipeBookSelectionPayload(pinned, pinned ? JazzyRecipeBookSelection.STREAM_CODEC.decode(buffer) : null);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, RecipeBookSelectionPayload value) {
            buffer.writeBoolean(value.pinned());
            if (value.pinned() && value.selection() != null) {
                JazzyRecipeBookSelection.STREAM_CODEC.encode(buffer, value.selection());
            }
        }
    };

    public static RecipeBookSelectionPayload pin(JazzyRecipeBookSelection selection) {
        return new RecipeBookSelectionPayload(true, selection);
    }

    public static RecipeBookSelectionPayload unpin() {
        return new RecipeBookSelectionPayload(false, null);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
