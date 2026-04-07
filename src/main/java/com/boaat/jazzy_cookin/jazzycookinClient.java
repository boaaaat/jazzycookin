package com.boaat.jazzy_cookin;

import com.boaat.jazzy_cookin.registry.JazzyMenus;
import com.boaat.jazzy_cookin.recipebook.client.RecipeBookClientState;
import com.boaat.jazzy_cookin.screen.KitchenStationScreen;
import com.boaat.jazzy_cookin.screen.KitchenStorageScreen;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@Mod(value = JazzyCookin.MODID, dist = Dist.CLIENT)
public class JazzyCookinClient {
    public JazzyCookinClient(IEventBus modEventBus) {
        modEventBus.addListener(this::onRegisterMenuScreens);
        modEventBus.addListener(RecipeBookClientState::onRegisterKeyMappings);

        NeoForge.EVENT_BUS.addListener(RecipeBookClientState::onClientTick);
        NeoForge.EVENT_BUS.addListener(RecipeBookClientState::onRecipesUpdated);
        NeoForge.EVENT_BUS.addListener(RecipeBookClientState::onRenderHud);
    }

    private void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(JazzyMenus.KITCHEN_STATION.get(), KitchenStationScreen::new);
        event.register(JazzyMenus.KITCHEN_STORAGE.get(), KitchenStorageScreen::new);
    }
}
