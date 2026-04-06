package com.boaat.jazzy_cookin.registry;

import com.boaat.jazzy_cookin.JazzyCookin;
import com.boaat.jazzy_cookin.item.KitchenIngredientItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
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
                    .icon(() -> JazzyItems.creativeIngredientStack(JazzyItems.IngredientId.APPLES))
                    .displayItems((parameters, output) -> {
                        for (DeferredItem<KitchenIngredientItem> ingredient : JazzyItems.ingredientItems()) {
                            output.accept(ingredient.get().createCreativeStack(1));
                        }
                    })
                    .build()
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TOOLS = CREATIVE_MODE_TABS.register(
            "tools",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.jazzycookin.tools"))
                    .icon(() -> JazzyItems.CHEF_KNIFE.get().getDefaultInstance())
                    .displayItems((parameters, output) -> acceptAll(
                            output,
                            JazzyItems.TUPPERWARE,
                            JazzyItems.CERAMIC_PLATE,
                            JazzyItems.CERAMIC_BOWL,
                            JazzyItems.GLASS_CUP,
                            JazzyItems.WOODEN_BOARD,
                            JazzyItems.SERVING_TRAY,
                            JazzyItems.CANNING_JAR,
                            JazzyItems.GLASS_JAR,
                            JazzyItems.PARING_KNIFE,
                            JazzyItems.CHEF_KNIFE,
                            JazzyItems.CLEAVER,
                            JazzyItems.TABLE_KNIFE,
                            JazzyItems.FORK,
                            JazzyItems.SPOON,
                            JazzyItems.WHISK,
                            JazzyItems.ROLLING_PIN,
                            JazzyItems.MORTAR_PESTLE,
                            JazzyItems.STOCK_POT,
                            JazzyItems.POT,
                            JazzyItems.SAUCEPAN,
                            JazzyItems.FRYING_SKILLET,
                            JazzyItems.FRYING_PAN,
                            JazzyItems.FINE_STRAINER,
                            JazzyItems.COARSE_STRAINER,
                            JazzyItems.STEAMER_BASKET,
                            JazzyItems.BAKING_TRAY,
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
                            JazzyItems.FRIDGE_ITEM,
                            JazzyItems.FREEZER_ITEM,
                            JazzyItems.PREP_TABLE_ITEM,
                            JazzyItems.SPICE_GRINDER_ITEM,
                            JazzyItems.STRAINER_ITEM,
                            JazzyItems.MIXING_BOWL_ITEM,
                            JazzyItems.MICROWAVE_ITEM,
                            JazzyItems.FOOD_PROCESSOR_ITEM,
                            JazzyItems.BLENDER_ITEM,
                            JazzyItems.JUICER_ITEM,
                            JazzyItems.FREEZE_DRYER_ITEM,
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
    private static void acceptAll(CreativeModeTab.Output output, DeferredItem<? extends Item>... items) {
        for (DeferredItem<? extends Item> item : items) {
            output.accept(item.get());
        }
    }

    public static ItemStack creativeStackFor(Item item) {
        if (item instanceof KitchenIngredientItem ingredientItem) {
            return ingredientItem.createCreativeStack(1);
        }
        return item.getDefaultInstance();
    }
}
