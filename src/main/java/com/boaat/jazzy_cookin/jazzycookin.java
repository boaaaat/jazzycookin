package com.boaat.jazzy_cookin;

import org.slf4j.Logger;

import com.boaat.jazzy_cookin.registry.JazzyBlockEntities;
import com.boaat.jazzy_cookin.registry.JazzyBlocks;
import com.boaat.jazzy_cookin.registry.JazzyDataComponents;
import com.boaat.jazzy_cookin.registry.JazzyItems;
import com.boaat.jazzy_cookin.registry.JazzyMenus;
import com.boaat.jazzy_cookin.registry.JazzyRecipes;
import com.mojang.logging.LogUtils;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

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

        modEventBus.addListener(this::addCreativeTabs);
    }

    private void addCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(JazzyItems.ORCHARD_APPLE);
            event.accept(JazzyItems.FLOUR);
            event.accept(JazzyItems.CANE_SUGAR);
            event.accept(JazzyItems.BUTTER);
            event.accept(JazzyItems.BAKING_SPICE);
            event.accept(JazzyItems.PIE_CRUST);
            event.accept(JazzyItems.PIE_FILLING);
            event.accept(JazzyItems.APPLE_PIE);
            event.accept(JazzyItems.APPLE_PIE_SLICE);
            event.accept(JazzyItems.PLATED_APPLE_PIE_SLICE);
            event.accept(JazzyItems.CERAMIC_PLATE);
        }

        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(JazzyItems.PANTRY_ITEM);
            event.accept(JazzyItems.CELLAR_ITEM);
            event.accept(JazzyItems.PREP_TABLE_ITEM);
            event.accept(JazzyItems.SPICE_GRINDER_ITEM);
            event.accept(JazzyItems.MIXING_BOWL_ITEM);
            event.accept(JazzyItems.STOVE_ITEM);
            event.accept(JazzyItems.OVEN_ITEM);
            event.accept(JazzyItems.COOLING_RACK_ITEM);
            event.accept(JazzyItems.RESTING_BOARD_ITEM);
            event.accept(JazzyItems.PLATING_STATION_ITEM);
        }

        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(JazzyItems.PARING_KNIFE);
            event.accept(JazzyItems.CHEF_KNIFE);
            event.accept(JazzyItems.WHISK);
            event.accept(JazzyItems.ROLLING_PIN);
            event.accept(JazzyItems.STOCK_POT);
            event.accept(JazzyItems.PIE_TIN);
        }

        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(JazzyItems.APPLE_SAPLING_ITEM);
        }

        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(JazzyItems.PLATED_APPLE_PIE_SLICE);
        }
    }
}
