package com.boaat.jazzy_cookin;

import org.slf4j.Logger;

import com.boaat.jazzy_cookin.registry.JazzyBlockEntities;
import com.boaat.jazzy_cookin.registry.JazzyBlocks;
import com.boaat.jazzy_cookin.registry.JazzyCreativeTabs;
import com.boaat.jazzy_cookin.registry.JazzyDataComponents;
import com.boaat.jazzy_cookin.registry.JazzyItems;
import com.boaat.jazzy_cookin.registry.JazzyMenus;
import com.boaat.jazzy_cookin.kitchen.sim.schema.DishSchemaManager;
import com.boaat.jazzy_cookin.recipebook.network.RecipeBookNetworking;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(JazzyCookin.MODID)
public class JazzyCookin {
    public static final String MODID = "jazzycookin";
    public static final Logger LOGGER = LogUtils.getLogger();

    public JazzyCookin(IEventBus modEventBus) {
        JazzyBlocks.BLOCKS.register(modEventBus);
        JazzyItems.ITEMS.register(modEventBus);
        JazzyBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        JazzyMenus.MENUS.register(modEventBus);
        JazzyDataComponents.DATA_COMPONENTS.register(modEventBus);
        JazzyCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        modEventBus.addListener(RecipeBookNetworking::register);
        NeoForge.EVENT_BUS.addListener(DishSchemaManager::onAddReloadListeners);
        NeoForge.EVENT_BUS.addListener(RecipeBookNetworking::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(RecipeBookNetworking::onPlayerCloned);
        NeoForge.EVENT_BUS.addListener(RecipeBookNetworking::onItemCrafted);
        NeoForge.EVENT_BUS.addListener(RecipeBookNetworking::onPlayerTick);
    }
}
