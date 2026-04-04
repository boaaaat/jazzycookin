package com.boaat.jazzy_cookin.registry;

import com.boaat.jazzy_cookin.JazzyCookin;
import com.boaat.jazzy_cookin.item.KitchenIngredientItem;
import com.boaat.jazzy_cookin.item.KitchenMealItem;
import com.boaat.jazzy_cookin.item.KitchenToolItem;
import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.ToolProfile;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class JazzyItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(JazzyCookin.MODID);

    public static final DeferredItem<BlockItem> APPLE_SAPLING_ITEM = ITEMS.registerSimpleBlockItem("apple_sapling", JazzyBlocks.APPLE_SAPLING);
    public static final DeferredItem<BlockItem> PANTRY_ITEM = ITEMS.registerSimpleBlockItem("pantry", JazzyBlocks.PANTRY);
    public static final DeferredItem<BlockItem> CELLAR_ITEM = ITEMS.registerSimpleBlockItem("cellar", JazzyBlocks.CELLAR);
    public static final DeferredItem<BlockItem> PREP_TABLE_ITEM = ITEMS.registerSimpleBlockItem("prep_table", JazzyBlocks.PREP_TABLE);
    public static final DeferredItem<BlockItem> SPICE_GRINDER_ITEM = ITEMS.registerSimpleBlockItem("spice_grinder", JazzyBlocks.SPICE_GRINDER);
    public static final DeferredItem<BlockItem> MIXING_BOWL_ITEM = ITEMS.registerSimpleBlockItem("mixing_bowl", JazzyBlocks.MIXING_BOWL);
    public static final DeferredItem<BlockItem> STOVE_ITEM = ITEMS.registerSimpleBlockItem("stove", JazzyBlocks.STOVE);
    public static final DeferredItem<BlockItem> OVEN_ITEM = ITEMS.registerSimpleBlockItem("oven", JazzyBlocks.OVEN);
    public static final DeferredItem<BlockItem> COOLING_RACK_ITEM = ITEMS.registerSimpleBlockItem("cooling_rack", JazzyBlocks.COOLING_RACK);
    public static final DeferredItem<BlockItem> RESTING_BOARD_ITEM = ITEMS.registerSimpleBlockItem("resting_board", JazzyBlocks.RESTING_BOARD);
    public static final DeferredItem<BlockItem> PLATING_STATION_ITEM = ITEMS.registerSimpleBlockItem("plating_station", JazzyBlocks.PLATING_STATION);

    public static final DeferredItem<KitchenIngredientItem> ORCHARD_APPLE = ingredient(
            "orchard_apple", IngredientState.WHOLE_APPLE, 0.82F, 0.7F, 0.66F, 0.2F, 0.76F, 0.3F, 0.1F, 72_000L, 3, 2
    );
    public static final DeferredItem<KitchenIngredientItem> FLOUR = ingredient(
            "flour", IngredientState.PANTRY_READY, 0.76F, 0.18F, 0.32F, 0.4F, 0.1F, 0.92F, 0.0F, Long.MAX_VALUE / 4L, 1, 0
    );
    public static final DeferredItem<KitchenIngredientItem> CANE_SUGAR = ingredient(
            "cane_sugar", IngredientState.PANTRY_READY, 0.78F, 0.42F, 0.15F, 0.0F, 0.0F, 0.98F, 0.0F, Long.MAX_VALUE / 4L, 0, 1
    );
    public static final DeferredItem<KitchenIngredientItem> BUTTER = ingredient(
            "butter", IngredientState.PANTRY_READY, 0.82F, 0.58F, 0.6F, 0.15F, 0.75F, 0.9F, 0.0F, 144_000L, 2, 2
    );
    public static final DeferredItem<KitchenIngredientItem> BAKING_SPICE = ingredient(
            "baking_spice", IngredientState.WHOLE_SPICE, 0.8F, 0.66F, 0.2F, 0.05F, 0.0F, 0.84F, 0.0F, Long.MAX_VALUE / 4L, 0, 1
    );
    public static final DeferredItem<KitchenIngredientItem> PIE_CRUST = ingredient(
            "pie_crust", IngredientState.CRUST_MIX, 0.72F, 0.28F, 0.34F, 0.62F, 0.24F, 0.82F, 0.04F, 36_000L, 4, 2
    );
    public static final DeferredItem<KitchenIngredientItem> PIE_FILLING = ingredient(
            "pie_filling", IngredientState.SIMMERED_FILLING, 0.78F, 0.74F, 0.5F, 0.18F, 0.68F, 0.7F, 0.0F, 36_000L, 4, 4
    );
    public static final DeferredItem<KitchenIngredientItem> APPLE_PIE = ingredient(
            "apple_pie", IngredientState.RAW_ASSEMBLED_PIE, 0.8F, 0.74F, 0.55F, 0.72F, 0.6F, 0.72F, 0.12F, 48_000L, 7, 6
    );
    public static final DeferredItem<KitchenIngredientItem> APPLE_PIE_SLICE = ingredient(
            "apple_pie_slice", IngredientState.SLICED_PIE, 0.82F, 0.78F, 0.6F, 0.68F, 0.6F, 0.74F, 0.15F, 24_000L, 7, 6
    );
    public static final DeferredItem<KitchenMealItem> PLATED_APPLE_PIE_SLICE = ITEMS.register("plated_apple_pie_slice", () ->
            new KitchenMealItem(
                    new Item.Properties().stacksTo(16),
                    IngredientState.PLATED_SLICE,
                    0.86F,
                    0.82F,
                    0.68F,
                    0.72F,
                    0.68F,
                    0.78F,
                    0.18F,
                    18_000L,
                    8,
                    8,
                    true
            ));

    public static final DeferredItem<KitchenIngredientItem> CERAMIC_PLATE = ingredient(
            "ceramic_plate", IngredientState.PANTRY_READY, 0.8F, 0.0F, 0.7F, 0.9F, 0.0F, 1.0F, 0.0F, Long.MAX_VALUE / 4L, 0, 0
    );

    public static final DeferredItem<KitchenToolItem> PARING_KNIFE = tool("paring_knife", ToolProfile.KNIFE, 0.06F, 1.1F, 128);
    public static final DeferredItem<KitchenToolItem> CHEF_KNIFE = tool("chef_knife", ToolProfile.KNIFE, 0.09F, 1.2F, 192);
    public static final DeferredItem<KitchenToolItem> WHISK = tool("whisk", ToolProfile.WHISK, 0.05F, 1.08F, 160);
    public static final DeferredItem<KitchenToolItem> ROLLING_PIN = tool("rolling_pin", ToolProfile.ROLLING_PIN, 0.07F, 1.12F, 192);
    public static final DeferredItem<KitchenToolItem> STOCK_POT = tool("stock_pot", ToolProfile.POT, 0.05F, 1.0F, 256);
    public static final DeferredItem<KitchenToolItem> PIE_TIN = tool("pie_tin", ToolProfile.PIE_TIN, 0.05F, 1.0F, 256);

    private JazzyItems() {
    }

    private static DeferredItem<KitchenIngredientItem> ingredient(
            String name,
            IngredientState defaultState,
            float baseQuality,
            float baseFlavor,
            float baseTexture,
            float baseStructure,
            float baseMoisture,
            float basePurity,
            float baseAeration,
            long decayTicks,
            int nourishment,
            int enjoyment
    ) {
        return ITEMS.register(name, () -> new KitchenIngredientItem(
                new Item.Properties(),
                defaultState,
                baseQuality,
                baseFlavor,
                baseTexture,
                baseStructure,
                baseMoisture,
                basePurity,
                baseAeration,
                decayTicks,
                nourishment,
                enjoyment
        ));
    }

    private static DeferredItem<KitchenToolItem> tool(String name, ToolProfile profile, float qualityBonus, float speedMultiplier, int durability) {
        return ITEMS.register(name, () -> new KitchenToolItem(
                new Item.Properties().stacksTo(1).durability(durability),
                profile,
                qualityBonus,
                speedMultiplier
        ));
    }
}
