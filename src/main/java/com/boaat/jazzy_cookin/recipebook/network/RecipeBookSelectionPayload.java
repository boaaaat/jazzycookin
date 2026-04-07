package com.boaat.jazzy_cookin.recipebook.network;

import org.jetbrains.annotations.Nullable;

import com.boaat.jazzy_cookin.JazzyCookin;
import com.boaat.jazzy_cookin.recipebook.JazzyRecipeBookSelection;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RecipeBookSelectionPayload(boolean pinned, @Nullable JazzyRecipeBookSelection selection, String focusedStepId) implements CustomPacketPayload {
    public static final Type<RecipeBookSelectionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JazzyCookin.MODID, "recipe_book_selection"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RecipeBookSelectionPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public RecipeBookSelectionPayload decode(RegistryFriendlyByteBuf buffer) {
            boolean pinned = buffer.readBoolean();
            if (!pinned) {
                return new RecipeBookSelectionPayload(false, null, "");
            }
            JazzyRecipeBookSelection selection = JazzyRecipeBookSelection.STREAM_CODEC.decode(buffer);
            String focusedStepId = ByteBufCodecs.STRING_UTF8.decode(buffer);
            return new RecipeBookSelectionPayload(true, selection, focusedStepId);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, RecipeBookSelectionPayload value) {
            buffer.writeBoolean(value.pinned());
            if (value.pinned() && value.selection() != null) {
                JazzyRecipeBookSelection.STREAM_CODEC.encode(buffer, value.selection());
                ByteBufCodecs.STRING_UTF8.encode(buffer, value.focusedStepId());
            }
        }
    };

    public static RecipeBookSelectionPayload pin(JazzyRecipeBookSelection selection) {
        return pin(selection, "");
    }

    public static RecipeBookSelectionPayload pin(JazzyRecipeBookSelection selection, @Nullable String focusedStepId) {
        return new RecipeBookSelectionPayload(true, selection, focusedStepId == null ? "" : focusedStepId);
    }

    public static RecipeBookSelectionPayload unpin() {
        return new RecipeBookSelectionPayload(false, null, "");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
