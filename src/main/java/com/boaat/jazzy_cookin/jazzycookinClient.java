package com.boaat.jazzy_cookin;

import com.boaat.jazzy_cookin.registry.JazzyBlocks;
import com.boaat.jazzy_cookin.registry.JazzyMenus;
import com.boaat.jazzy_cookin.screen.KitchenStationScreen;
import com.boaat.jazzy_cookin.screen.KitchenStorageScreen;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(value = JazzyCookin.MODID, dist = Dist.CLIENT)
public class JazzyCookinClient {
    public JazzyCookinClient(IEventBus modEventBus) {
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::onRegisterMenuScreens);
    }

    private void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(JazzyMenus.KITCHEN_STATION.get(), KitchenStationScreen::new);
        event.register(JazzyMenus.KITCHEN_STORAGE.get(), KitchenStorageScreen::new);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(JazzyBlocks.APPLE_SAPLING.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(JazzyBlocks.TOMATO_VINE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(JazzyBlocks.HERB_BED.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(JazzyBlocks.WHEAT_PATCH.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(JazzyBlocks.CABBAGE_PATCH.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(JazzyBlocks.ONION_PATCH.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(JazzyBlocks.CHICKEN_COOP.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(JazzyBlocks.DAIRY_STALL.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(JazzyBlocks.FISHING_TRAP.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(JazzyBlocks.FORAGE_SHRUB.get(), RenderType.cutout());
        });
    }
}
