package com.boaat.jazzy_cookin;

import com.boaat.jazzy_cookin.registry.JazzyMenus;
import com.boaat.jazzy_cookin.client.KitchenStationBlockEntityRenderer;
import com.boaat.jazzy_cookin.recipebook.client.RecipeBookClientState;
import com.boaat.jazzy_cookin.registry.JazzyBlockEntities;
import com.boaat.jazzy_cookin.screen.KitchenStationScreen;
import com.boaat.jazzy_cookin.screen.KitchenStorageScreen;
import com.boaat.jazzy_cookin.tutorial.client.BlockTutorialClientState;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(value = JazzyCookin.MODID, dist = Dist.CLIENT)
public class JazzyCookinClient {
    public JazzyCookinClient(IEventBus modEventBus) {
        modEventBus.addListener(this::onRegisterMenuScreens);
        modEventBus.addListener(this::onRegisterEntityRenderers);
        modEventBus.addListener(RecipeBookClientState::onRegisterKeyMappings);
        modEventBus.addListener(BlockTutorialClientState::onRegisterKeyMappings);

        NeoForge.EVENT_BUS.addListener(RecipeBookClientState::onClientTick);
        NeoForge.EVENT_BUS.addListener(RecipeBookClientState::onRecipesUpdated);
        NeoForge.EVENT_BUS.addListener(RecipeBookClientState::onRenderHud);
        NeoForge.EVENT_BUS.addListener(BlockTutorialClientState::onClientTick);
    }

    private void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(JazzyMenus.KITCHEN_STATION.get(), KitchenStationScreen::new);
        event.register(JazzyMenus.KITCHEN_STORAGE.get(), KitchenStorageScreen::new);
    }

    private void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(JazzyBlockEntities.KITCHEN_STATION.get(), KitchenStationBlockEntityRenderer::new);
    }
}
