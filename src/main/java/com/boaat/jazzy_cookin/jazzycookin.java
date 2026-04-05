package com.boaat.jazzy_cookin;

import org.slf4j.Logger;

import com.boaat.jazzy_cookin.registry.JazzyBlockEntities;
import com.boaat.jazzy_cookin.registry.JazzyBlocks;
import com.boaat.jazzy_cookin.registry.JazzyCreativeTabs;
import com.boaat.jazzy_cookin.registry.JazzyDataComponents;
import com.boaat.jazzy_cookin.registry.JazzyItems;
import com.boaat.jazzy_cookin.registry.JazzyMenus;
import com.boaat.jazzy_cookin.registry.JazzyRecipes;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

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
        JazzyRecipes.RECIPE_TYPES.register(modEventBus);
        JazzyRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        JazzyCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
    }
}
