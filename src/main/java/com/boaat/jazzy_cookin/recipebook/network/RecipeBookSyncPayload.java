package com.boaat.jazzy_cookin.recipebook.network;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.boaat.jazzy_cookin.JazzyCookin;
import com.boaat.jazzy_cookin.recipebook.JazzyRecipeBookSelection;
import com.boaat.jazzy_cookin.recipebook.RecipeBookProgress;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RecipeBookSyncPayload(@Nullable JazzyRecipeBookSelection selection, List<String> completedStepIds, @Nullable String focusedStepId) implements CustomPacketPayload {
    public static final Type<RecipeBookSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JazzyCookin.MODID, "recipe_book_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RecipeBookSyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public RecipeBookSyncPayload decode(RegistryFriendlyByteBuf buffer) {
            boolean pinned = buffer.readBoolean();
            JazzyRecipeBookSelection selection = pinned ? JazzyRecipeBookSelection.STREAM_CODEC.decode(buffer) : null;
            List<String> completed = ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8).decode(buffer);
            String focusedStepId = ByteBufCodecs.STRING_UTF8.decode(buffer);
            return new RecipeBookSyncPayload(selection, completed, focusedStepId.isBlank() ? null : focusedStepId);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, RecipeBookSyncPayload value) {
            buffer.writeBoolean(value.selection() != null);
            if (value.selection() != null) {
                JazzyRecipeBookSelection.STREAM_CODEC.encode(buffer, value.selection());
            }
            ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8).encode(buffer, new ArrayList<>(value.completedStepIds()));
            ByteBufCodecs.STRING_UTF8.encode(buffer, value.focusedStepId() == null ? "" : value.focusedStepId());
        }
    };

    public static RecipeBookSyncPayload from(RecipeBookProgress.SyncState state) {
        return new RecipeBookSyncPayload(state.selection(), state.completedStepIds(), state.focusedStepId());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
