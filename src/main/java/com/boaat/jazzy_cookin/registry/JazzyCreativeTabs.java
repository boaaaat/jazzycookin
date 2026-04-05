package com.boaat.jazzy_cookin.registry;

import com.boaat.jazzy_cookin.JazzyCookin;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class JazzyCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, JazzyCookin.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> INGREDIENTS = CREATIVE_MODE_TABS.register(
            "ingredients",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.jazzycookin.ingredients"))
                    .icon(() -> JazzyItems.ORCHARD_APPLE.get().getDefaultInstance())
                    .displayItems((parameters, output) -> acceptAll(
                            output,
                            JazzyItems.ORCHARD_APPLE,
                            JazzyItems.WILD_BERRIES_ITEM,
                            JazzyItems.TOMATO,
                            JazzyItems.CHOPPED_TOMATO,
                            JazzyItems.FRESH_HERB,
                            JazzyItems.GROUND_HERB,
                            JazzyItems.WHEAT_SHEAF,
                            JazzyItems.CABBAGE,
                            JazzyItems.CHOPPED_CABBAGE,
                            JazzyItems.ONION,
                            JazzyItems.DICED_ONION,
                            JazzyItems.FARM_EGG,
                            JazzyItems.BOILED_EGG_ITEM,
                            JazzyItems.FRESH_MILK,
                            JazzyItems.RAW_FISH_ITEM,
                            JazzyItems.CLEANED_FISH,
                            JazzyItems.PAN_FRIED_FISH,
                            JazzyItems.RAW_PROTEIN,
                            JazzyItems.ROAST_CUT,
                            JazzyItems.EGG_WASH,
                            JazzyItems.MARINADE,
                            JazzyItems.BRINE,
                            JazzyItems.CANNING_SYRUP,
                            JazzyItems.BATTER,
                            JazzyItems.FRYING_OIL,
                            JazzyItems.USED_OIL_ITEM,
                            JazzyItems.DIRTY_OIL_ITEM,
                            JazzyItems.BURNT_OIL_ITEM,
                            JazzyItems.FLOUR,
                            JazzyItems.SALT,
                            JazzyItems.CANE_SUGAR,
                            JazzyItems.BUTTER,
                            JazzyItems.BAKING_SPICE,
                            JazzyItems.PIE_CRUST,
                            JazzyItems.PIE_FILLING,
                            JazzyItems.APPLE_PIE,
                            JazzyItems.APPLE_PIE_SLICE,
                            JazzyItems.BREAD_DOUGH,
                            JazzyItems.BREAD_LOAF,
                            JazzyItems.SLICED_BREAD,
                            JazzyItems.TOMATO_SOUP_BASE,
                            JazzyItems.STRAINED_SOUP,
                            JazzyItems.DUMPLING_FILLING_ITEM,
                            JazzyItems.DUMPLING_DOUGH_ITEM,
                            JazzyItems.RAW_DUMPLINGS_ITEM,
                            JazzyItems.STEAMED_DUMPLINGS_ITEM,
                            JazzyItems.MARINATED_PROTEIN_ITEM,
                            JazzyItems.BATTERED_PROTEIN_ITEM,
                            JazzyItems.FRIED_PROTEIN_ITEM,
                            JazzyItems.ROASTED_PROTEIN_ITEM,
                            JazzyItems.BROILED_PROTEIN_ITEM,
                            JazzyItems.SMOKED_PROTEIN_ITEM,
                            JazzyItems.ROAST_VEGETABLES_ITEM,
                            JazzyItems.CANNED_TOMATO_ITEM,
                            JazzyItems.APPLE_PRESERVE_ITEM,
                            JazzyItems.DRIED_APPLE_ITEM,
                            JazzyItems.FERMENTED_VEGETABLES_ITEM,
                            JazzyItems.CULTURED_DAIRY_ITEM
                    ))
                    .build()
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MEALS = CREATIVE_MODE_TABS.register(
            "meals",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.jazzycookin.meals"))
                    .icon(() -> JazzyItems.PLATED_APPLE_PIE_SLICE.get().getDefaultInstance())
                    .displayItems((parameters, output) -> acceptAll(
                            output,
                            JazzyItems.CERAMIC_PLATE,
                            JazzyItems.PLATED_APPLE_PIE_SLICE,
                            JazzyItems.PLATED_TOMATO_SOUP_MEAL,
                            JazzyItems.PLATED_DUMPLING_MEAL,
                            JazzyItems.PLATED_FRIED_MEAL,
                            JazzyItems.PLATED_ROAST_MEAL
                    ))
                    .build()
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TOOLS = CREATIVE_MODE_TABS.register(
            "tools",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.jazzycookin.tools"))
                    .icon(() -> JazzyItems.CHEF_KNIFE.get().getDefaultInstance())
                    .displayItems((parameters, output) -> acceptAll(
                            output,
                            JazzyItems.PARING_KNIFE,
                            JazzyItems.CHEF_KNIFE,
                            JazzyItems.CLEAVER,
                            JazzyItems.WHISK,
                            JazzyItems.ROLLING_PIN,
                            JazzyItems.MORTAR_PESTLE,
                            JazzyItems.STOCK_POT,
                            JazzyItems.FRYING_SKILLET,
                            JazzyItems.FINE_STRAINER,
                            JazzyItems.COARSE_STRAINER,
                            JazzyItems.STEAMER_BASKET,
                            JazzyItems.CANNING_JAR,
                            JazzyItems.PIE_TIN
                    ))
                    .build()
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> KITCHEN = CREATIVE_MODE_TABS.register(
            "kitchen",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.jazzycookin.kitchen"))
                    .icon(() -> JazzyItems.PREP_TABLE_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> acceptAll(
                            output,
                            JazzyItems.PANTRY_ITEM,
                            JazzyItems.CELLAR_ITEM,
                            JazzyItems.PREP_TABLE_ITEM,
                            JazzyItems.SPICE_GRINDER_ITEM,
                            JazzyItems.STRAINER_ITEM,
                            JazzyItems.MIXING_BOWL_ITEM,
                            JazzyItems.CANNING_STATION_ITEM,
                            JazzyItems.DRYING_RACK_ITEM,
                            JazzyItems.SMOKER_ITEM,
                            JazzyItems.FERMENTATION_CROCK_ITEM,
                            JazzyItems.STEAMER_ITEM,
                            JazzyItems.STOVE_ITEM,
                            JazzyItems.OVEN_ITEM,
                            JazzyItems.COOLING_RACK_ITEM,
                            JazzyItems.RESTING_BOARD_ITEM,
                            JazzyItems.PLATING_STATION_ITEM
                    ))
                    .build()
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SOURCES = CREATIVE_MODE_TABS.register(
            "sources",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.jazzycookin.sources"))
                    .icon(() -> JazzyItems.APPLE_SAPLING_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> acceptAll(
                            output,
                            JazzyItems.APPLE_SAPLING_ITEM,
                            JazzyItems.TOMATO_VINE_ITEM,
                            JazzyItems.HERB_BED_ITEM,
                            JazzyItems.WHEAT_PATCH_ITEM,
                            JazzyItems.CABBAGE_PATCH_ITEM,
                            JazzyItems.ONION_PATCH_ITEM,
                            JazzyItems.CHICKEN_COOP_ITEM,
                            JazzyItems.DAIRY_STALL_ITEM,
                            JazzyItems.FISHING_TRAP_ITEM,
                            JazzyItems.FORAGE_SHRUB_ITEM
                    ))
                    .build()
    );

    private JazzyCreativeTabs() {
    }

    @SafeVarargs
    private static void acceptAll(CreativeModeTab.Output output, DeferredItem<?>... items) {
        for (DeferredItem<?> item : items) {
            output.accept(item.get());
        }
    }
}
