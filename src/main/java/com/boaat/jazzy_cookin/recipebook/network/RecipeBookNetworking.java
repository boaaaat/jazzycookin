package com.boaat.jazzy_cookin.recipebook.network;

import com.boaat.jazzy_cookin.recipebook.RecipeBookProgress;
import com.boaat.jazzy_cookin.recipebook.client.RecipeBookClientState;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class RecipeBookNetworking {
    private RecipeBookNetworking() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("jazzy_recipe_book_v1");
        registrar.playToServer(RecipeBookSelectionPayload.TYPE, RecipeBookSelectionPayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                        return;
                    }
                    if (payload.pinned() && payload.selection() != null) {
                        RecipeBookProgress.pin(serverPlayer, payload.selection());
                    } else {
                        RecipeBookProgress.unpin(serverPlayer);
                    }
                    sync(serverPlayer);
                })
        );
        registrar.playToClient(RecipeBookSyncPayload.TYPE, RecipeBookSyncPayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> RecipeBookClientState.applySync(payload))
        );
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, RecipeBookSyncPayload.from(RecipeBookProgress.syncState(player)));
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    public static void onPlayerCloned(PlayerEvent.Clone event) {
        RecipeBookProgress.copyToClone(event.getOriginal(), event.getEntity());
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && RecipeBookProgress.recordCraft(player, event.getCrafting())) {
            sync(player);
        }
    }
}
