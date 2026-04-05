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
            event.accept(JazzyItems.WILD_BERRIES_ITEM);
            event.accept(JazzyItems.TOMATO);
            event.accept(JazzyItems.CHOPPED_TOMATO);
            event.accept(JazzyItems.FRESH_HERB);
            event.accept(JazzyItems.GROUND_HERB);
            event.accept(JazzyItems.WHEAT_SHEAF);
            event.accept(JazzyItems.CABBAGE);
            event.accept(JazzyItems.CHOPPED_CABBAGE);
            event.accept(JazzyItems.ONION);
            event.accept(JazzyItems.DICED_ONION);
            event.accept(JazzyItems.FARM_EGG);
            event.accept(JazzyItems.FRESH_MILK);
            event.accept(JazzyItems.RAW_FISH_ITEM);
            event.accept(JazzyItems.CLEANED_FISH);
            event.accept(JazzyItems.RAW_PROTEIN);
            event.accept(JazzyItems.ROAST_CUT);
            event.accept(JazzyItems.EGG_WASH);
            event.accept(JazzyItems.MARINADE);
            event.accept(JazzyItems.BRINE);
            event.accept(JazzyItems.CANNING_SYRUP);
            event.accept(JazzyItems.BATTER);
            event.accept(JazzyItems.FRYING_OIL);
            event.accept(JazzyItems.USED_OIL_ITEM);
            event.accept(JazzyItems.FLOUR);
            event.accept(JazzyItems.SALT);
            event.accept(JazzyItems.CANE_SUGAR);
            event.accept(JazzyItems.BUTTER);
            event.accept(JazzyItems.BAKING_SPICE);
            event.accept(JazzyItems.PIE_CRUST);
            event.accept(JazzyItems.PIE_FILLING);
            event.accept(JazzyItems.APPLE_PIE);
            event.accept(JazzyItems.APPLE_PIE_SLICE);
            event.accept(JazzyItems.BREAD_DOUGH);
            event.accept(JazzyItems.BREAD_LOAF);
            event.accept(JazzyItems.SLICED_BREAD);
            event.accept(JazzyItems.TOMATO_SOUP_BASE);
            event.accept(JazzyItems.STRAINED_SOUP);
            event.accept(JazzyItems.DUMPLING_FILLING_ITEM);
            event.accept(JazzyItems.DUMPLING_DOUGH_ITEM);
            event.accept(JazzyItems.RAW_DUMPLINGS_ITEM);
            event.accept(JazzyItems.STEAMED_DUMPLINGS_ITEM);
            event.accept(JazzyItems.MARINATED_PROTEIN_ITEM);
            event.accept(JazzyItems.BATTERED_PROTEIN_ITEM);
            event.accept(JazzyItems.FRIED_PROTEIN_ITEM);
            event.accept(JazzyItems.ROASTED_PROTEIN_ITEM);
            event.accept(JazzyItems.BROILED_PROTEIN_ITEM);
            event.accept(JazzyItems.SMOKED_PROTEIN_ITEM);
            event.accept(JazzyItems.ROAST_VEGETABLES_ITEM);
            event.accept(JazzyItems.CANNED_TOMATO_ITEM);
            event.accept(JazzyItems.APPLE_PRESERVE_ITEM);
            event.accept(JazzyItems.DRIED_APPLE_ITEM);
            event.accept(JazzyItems.FERMENTED_VEGETABLES_ITEM);
            event.accept(JazzyItems.CULTURED_DAIRY_ITEM);
            event.accept(JazzyItems.PLATED_APPLE_PIE_SLICE);
            event.accept(JazzyItems.PLATED_TOMATO_SOUP_MEAL);
            event.accept(JazzyItems.PLATED_DUMPLING_MEAL);
            event.accept(JazzyItems.PLATED_FRIED_MEAL);
            event.accept(JazzyItems.PLATED_ROAST_MEAL);
            event.accept(JazzyItems.CERAMIC_PLATE);
        }

        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(JazzyItems.PANTRY_ITEM);
            event.accept(JazzyItems.CELLAR_ITEM);
            event.accept(JazzyItems.PREP_TABLE_ITEM);
            event.accept(JazzyItems.SPICE_GRINDER_ITEM);
            event.accept(JazzyItems.STRAINER_ITEM);
            event.accept(JazzyItems.MIXING_BOWL_ITEM);
            event.accept(JazzyItems.CANNING_STATION_ITEM);
            event.accept(JazzyItems.DRYING_RACK_ITEM);
            event.accept(JazzyItems.SMOKER_ITEM);
            event.accept(JazzyItems.FERMENTATION_CROCK_ITEM);
            event.accept(JazzyItems.STEAMER_ITEM);
            event.accept(JazzyItems.STOVE_ITEM);
            event.accept(JazzyItems.OVEN_ITEM);
            event.accept(JazzyItems.COOLING_RACK_ITEM);
            event.accept(JazzyItems.RESTING_BOARD_ITEM);
            event.accept(JazzyItems.PLATING_STATION_ITEM);
        }

        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(JazzyItems.PARING_KNIFE);
            event.accept(JazzyItems.CHEF_KNIFE);
            event.accept(JazzyItems.CLEAVER);
            event.accept(JazzyItems.WHISK);
            event.accept(JazzyItems.ROLLING_PIN);
            event.accept(JazzyItems.MORTAR_PESTLE);
            event.accept(JazzyItems.STOCK_POT);
            event.accept(JazzyItems.FRYING_SKILLET);
            event.accept(JazzyItems.FINE_STRAINER);
            event.accept(JazzyItems.COARSE_STRAINER);
            event.accept(JazzyItems.STEAMER_BASKET);
            event.accept(JazzyItems.CANNING_JAR);
            event.accept(JazzyItems.PIE_TIN);
        }

        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(JazzyItems.APPLE_SAPLING_ITEM);
            event.accept(JazzyItems.TOMATO_VINE_ITEM);
            event.accept(JazzyItems.HERB_BED_ITEM);
            event.accept(JazzyItems.WHEAT_PATCH_ITEM);
            event.accept(JazzyItems.CABBAGE_PATCH_ITEM);
            event.accept(JazzyItems.ONION_PATCH_ITEM);
            event.accept(JazzyItems.CHICKEN_COOP_ITEM);
            event.accept(JazzyItems.DAIRY_STALL_ITEM);
            event.accept(JazzyItems.FISHING_TRAP_ITEM);
            event.accept(JazzyItems.FORAGE_SHRUB_ITEM);
        }

        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(JazzyItems.PLATED_APPLE_PIE_SLICE);
            event.accept(JazzyItems.PLATED_TOMATO_SOUP_MEAL);
            event.accept(JazzyItems.PLATED_DUMPLING_MEAL);
            event.accept(JazzyItems.PLATED_FRIED_MEAL);
            event.accept(JazzyItems.PLATED_ROAST_MEAL);
        }
    }
}
