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
    public static final DeferredItem<BlockItem> TOMATO_VINE_ITEM = ITEMS.registerSimpleBlockItem("tomato_vine", JazzyBlocks.TOMATO_VINE);
    public static final DeferredItem<BlockItem> HERB_BED_ITEM = ITEMS.registerSimpleBlockItem("herb_bed", JazzyBlocks.HERB_BED);
    public static final DeferredItem<BlockItem> WHEAT_PATCH_ITEM = ITEMS.registerSimpleBlockItem("wheat_patch", JazzyBlocks.WHEAT_PATCH);
    public static final DeferredItem<BlockItem> CABBAGE_PATCH_ITEM = ITEMS.registerSimpleBlockItem("cabbage_patch", JazzyBlocks.CABBAGE_PATCH);
    public static final DeferredItem<BlockItem> ONION_PATCH_ITEM = ITEMS.registerSimpleBlockItem("onion_patch", JazzyBlocks.ONION_PATCH);
    public static final DeferredItem<BlockItem> CHICKEN_COOP_ITEM = ITEMS.registerSimpleBlockItem("chicken_coop", JazzyBlocks.CHICKEN_COOP);
    public static final DeferredItem<BlockItem> DAIRY_STALL_ITEM = ITEMS.registerSimpleBlockItem("dairy_stall", JazzyBlocks.DAIRY_STALL);
    public static final DeferredItem<BlockItem> FISHING_TRAP_ITEM = ITEMS.registerSimpleBlockItem("fishing_trap", JazzyBlocks.FISHING_TRAP);
    public static final DeferredItem<BlockItem> FORAGE_SHRUB_ITEM = ITEMS.registerSimpleBlockItem("forage_shrub", JazzyBlocks.FORAGE_SHRUB);
    public static final DeferredItem<BlockItem> PANTRY_ITEM = ITEMS.registerSimpleBlockItem("pantry", JazzyBlocks.PANTRY);
    public static final DeferredItem<BlockItem> CELLAR_ITEM = ITEMS.registerSimpleBlockItem("cellar", JazzyBlocks.CELLAR);
    public static final DeferredItem<BlockItem> PREP_TABLE_ITEM = ITEMS.registerSimpleBlockItem("prep_table", JazzyBlocks.PREP_TABLE);
    public static final DeferredItem<BlockItem> SPICE_GRINDER_ITEM = ITEMS.registerSimpleBlockItem("spice_grinder", JazzyBlocks.SPICE_GRINDER);
    public static final DeferredItem<BlockItem> STRAINER_ITEM = ITEMS.registerSimpleBlockItem("strainer", JazzyBlocks.STRAINER);
    public static final DeferredItem<BlockItem> MIXING_BOWL_ITEM = ITEMS.registerSimpleBlockItem("mixing_bowl", JazzyBlocks.MIXING_BOWL);
    public static final DeferredItem<BlockItem> CANNING_STATION_ITEM = ITEMS.registerSimpleBlockItem("canning_station", JazzyBlocks.CANNING_STATION);
    public static final DeferredItem<BlockItem> DRYING_RACK_ITEM = ITEMS.registerSimpleBlockItem("drying_rack", JazzyBlocks.DRYING_RACK);
    public static final DeferredItem<BlockItem> SMOKER_ITEM = ITEMS.registerSimpleBlockItem("smoker", JazzyBlocks.SMOKER);
    public static final DeferredItem<BlockItem> FERMENTATION_CROCK_ITEM = ITEMS.registerSimpleBlockItem("fermentation_crock", JazzyBlocks.FERMENTATION_CROCK);
    public static final DeferredItem<BlockItem> STEAMER_ITEM = ITEMS.registerSimpleBlockItem("steamer", JazzyBlocks.STEAMER);
    public static final DeferredItem<BlockItem> STOVE_ITEM = ITEMS.registerSimpleBlockItem("stove", JazzyBlocks.STOVE);
    public static final DeferredItem<BlockItem> OVEN_ITEM = ITEMS.registerSimpleBlockItem("oven", JazzyBlocks.OVEN);
    public static final DeferredItem<BlockItem> COOLING_RACK_ITEM = ITEMS.registerSimpleBlockItem("cooling_rack", JazzyBlocks.COOLING_RACK);
    public static final DeferredItem<BlockItem> RESTING_BOARD_ITEM = ITEMS.registerSimpleBlockItem("resting_board", JazzyBlocks.RESTING_BOARD);
    public static final DeferredItem<BlockItem> PLATING_STATION_ITEM = ITEMS.registerSimpleBlockItem("plating_station", JazzyBlocks.PLATING_STATION);

    public static final DeferredItem<KitchenIngredientItem> ORCHARD_APPLE = ingredient(
            "orchard_apple", IngredientState.WHOLE_APPLE, 0.82F, 0.70F, 0.66F, 0.20F, 0.76F, 0.30F, 0.10F, 72_000L, 3, 2
    );
    public static final DeferredItem<KitchenIngredientItem> WILD_BERRIES_ITEM = ingredient(
            "wild_berries", IngredientState.WILD_BERRIES, 0.80F, 0.78F, 0.52F, 0.12F, 0.62F, 0.20F, 0.02F, 48_000L, 2, 3
    );
    public static final DeferredItem<KitchenIngredientItem> TOMATO = ingredient(
            "tomato", IngredientState.WHOLE_TOMATO, 0.78F, 0.72F, 0.56F, 0.12F, 0.70F, 0.28F, 0.03F, 52_000L, 3, 2
    );
    public static final DeferredItem<KitchenIngredientItem> CHOPPED_TOMATO = ingredient(
            "chopped_tomato", IngredientState.CHOPPED_TOMATO, 0.76F, 0.74F, 0.58F, 0.14F, 0.72F, 0.30F, 0.03F, 30_000L, 3, 2
    );
    public static final DeferredItem<KitchenIngredientItem> FRESH_HERB = ingredient(
            "fresh_herb", IngredientState.WHOLE_HERB, 0.80F, 0.80F, 0.40F, 0.08F, 0.54F, 0.24F, 0.05F, 28_000L, 1, 2
    );
    public static final DeferredItem<KitchenIngredientItem> GROUND_HERB = ingredient(
            "ground_herb", IngredientState.GROUND_HERB, 0.82F, 0.86F, 0.38F, 0.06F, 0.42F, 0.30F, 0.08F, 20_000L, 1, 2
    );
    public static final DeferredItem<KitchenIngredientItem> WHEAT_SHEAF = ingredient(
            "wheat_sheaf", IngredientState.WHOLE_WHEAT, 0.74F, 0.22F, 0.34F, 0.46F, 0.20F, 0.76F, 0.00F, 96_000L, 2, 1
    );
    public static final DeferredItem<KitchenIngredientItem> CABBAGE = ingredient(
            "cabbage", IngredientState.WHOLE_CABBAGE, 0.78F, 0.46F, 0.54F, 0.20F, 0.66F, 0.26F, 0.02F, 86_000L, 3, 1
    );
    public static final DeferredItem<KitchenIngredientItem> CHOPPED_CABBAGE = ingredient(
            "chopped_cabbage", IngredientState.CHOPPED_CABBAGE, 0.76F, 0.48F, 0.52F, 0.18F, 0.62F, 0.26F, 0.02F, 40_000L, 3, 1
    );
    public static final DeferredItem<KitchenIngredientItem> ONION = ingredient(
            "onion", IngredientState.WHOLE_ONION, 0.76F, 0.58F, 0.48F, 0.18F, 0.58F, 0.30F, 0.02F, 90_000L, 2, 1
    );
    public static final DeferredItem<KitchenIngredientItem> DICED_ONION = ingredient(
            "diced_onion", IngredientState.DICED_ONION, 0.78F, 0.62F, 0.54F, 0.20F, 0.58F, 0.32F, 0.02F, 42_000L, 2, 1
    );
    public static final DeferredItem<KitchenIngredientItem> FARM_EGG = ingredient(
            "farm_egg", IngredientState.WHOLE_EGG, 0.80F, 0.34F, 0.56F, 0.42F, 0.68F, 0.64F, 0.18F, 52_000L, 4, 2
    );
    public static final DeferredItem<KitchenIngredientItem> FRESH_MILK = ingredient(
            "fresh_milk", IngredientState.FRESH_MILK, 0.82F, 0.38F, 0.52F, 0.32F, 0.84F, 0.72F, 0.10F, 44_000L, 4, 2
    );
    public static final DeferredItem<KitchenIngredientItem> RAW_FISH_ITEM = ingredient(
            "raw_fish", IngredientState.RAW_FISH, 0.78F, 0.44F, 0.56F, 0.22F, 0.70F, 0.52F, 0.04F, 34_000L, 4, 2
    );
    public static final DeferredItem<KitchenIngredientItem> CLEANED_FISH = ingredient(
            "cleaned_fish", IngredientState.CLEANED_FISH, 0.80F, 0.46F, 0.58F, 0.22F, 0.68F, 0.60F, 0.04F, 28_000L, 4, 2
    );
    public static final DeferredItem<KitchenIngredientItem> PAN_FRIED_FISH = ingredient(
            "pan_fried_fish", IngredientState.PAN_FRIED, 0.84F, 0.82F, 0.74F, 0.28F, 0.58F, 0.62F, 0.04F, 18_000L, 7, 6
    );
    public static final DeferredItem<KitchenIngredientItem> RAW_PROTEIN = ingredient(
            "raw_protein", IngredientState.RAW_PROTEIN, 0.76F, 0.48F, 0.56F, 0.26F, 0.72F, 0.42F, 0.04F, 34_000L, 5, 3
    );
    public static final DeferredItem<KitchenIngredientItem> ROAST_CUT = ingredient(
            "roast_cut", IngredientState.ROAST_CUT, 0.78F, 0.52F, 0.58F, 0.30F, 0.72F, 0.46F, 0.04F, 26_000L, 5, 3
    );
    public static final DeferredItem<KitchenIngredientItem> EGG_WASH = ingredient(
            "egg_wash", IngredientState.WHISKED, 0.78F, 0.24F, 0.22F, 0.14F, 0.72F, 0.74F, 0.26F, 12_000L, 1, 1
    );
    public static final DeferredItem<KitchenIngredientItem> MARINADE = ingredient(
            "marinade", IngredientState.MARINADE, 0.80F, 0.76F, 0.18F, 0.10F, 0.80F, 0.68F, 0.08F, 18_000L, 1, 2
    );
    public static final DeferredItem<KitchenIngredientItem> BRINE = ingredient(
            "brine", IngredientState.BRINE, 0.74F, 0.22F, 0.08F, 0.00F, 0.92F, 0.96F, 0.00F, Long.MAX_VALUE / 4L, 0, 0
    );
    public static final DeferredItem<KitchenIngredientItem> CANNING_SYRUP = ingredient(
            "canning_syrup", IngredientState.CANNING_SYRUP, 0.76F, 0.60F, 0.10F, 0.00F, 0.88F, 0.94F, 0.00F, Long.MAX_VALUE / 4L, 0, 2
    );
    public static final DeferredItem<KitchenIngredientItem> BATTER = ingredient(
            "batter", IngredientState.BATTER, 0.76F, 0.30F, 0.42F, 0.52F, 0.72F, 0.74F, 0.16F, 18_000L, 3, 2
    );
    public static final DeferredItem<KitchenIngredientItem> FRYING_OIL = ingredient(
            "frying_oil", IngredientState.FRESH_OIL, 0.80F, 0.18F, 0.22F, 0.00F, 0.90F, 0.90F, 0.00F, Long.MAX_VALUE / 4L, 0, 1
    );
    public static final DeferredItem<KitchenIngredientItem> USED_OIL_ITEM = ingredient(
            "used_oil", IngredientState.USED_OIL, 0.46F, 0.10F, 0.10F, 0.00F, 0.86F, 0.30F, 0.00F, 12_000L, 0, 0
    );
    public static final DeferredItem<KitchenIngredientItem> DIRTY_OIL_ITEM = ingredient(
            "dirty_oil", IngredientState.DIRTY_OIL, 0.30F, 0.06F, 0.06F, 0.00F, 0.82F, 0.18F, 0.00F, 8_000L, 0, 0
    );
    public static final DeferredItem<KitchenIngredientItem> BURNT_OIL_ITEM = ingredient(
            "burnt_oil", IngredientState.BURNT_OIL, 0.12F, 0.00F, 0.02F, 0.00F, 0.74F, 0.08F, 0.00F, 4_000L, 0, 0
    );
    public static final DeferredItem<KitchenIngredientItem> BOILED_EGG_ITEM = ingredient(
            "boiled_egg", IngredientState.BOILED, 0.82F, 0.42F, 0.66F, 0.48F, 0.68F, 0.72F, 0.04F, 20_000L, 6, 4
    );
    public static final DeferredItem<KitchenIngredientItem> FLOUR = ingredient(
            "flour", IngredientState.PANTRY_READY, 0.76F, 0.18F, 0.32F, 0.40F, 0.10F, 0.92F, 0.00F, Long.MAX_VALUE / 4L, 1, 0
    );
    public static final DeferredItem<KitchenIngredientItem> SALT = ingredient(
            "salt", IngredientState.PANTRY_READY, 0.74F, 0.12F, 0.04F, 0.00F, 0.00F, 0.98F, 0.00F, Long.MAX_VALUE / 4L, 0, 0
    );
    public static final DeferredItem<KitchenIngredientItem> CANE_SUGAR = ingredient(
            "cane_sugar", IngredientState.PANTRY_READY, 0.78F, 0.42F, 0.15F, 0.00F, 0.00F, 0.98F, 0.00F, Long.MAX_VALUE / 4L, 0, 1
    );
    public static final DeferredItem<KitchenIngredientItem> BUTTER = ingredient(
            "butter", IngredientState.PANTRY_READY, 0.82F, 0.58F, 0.60F, 0.15F, 0.75F, 0.90F, 0.00F, 144_000L, 2, 2
    );
    public static final DeferredItem<KitchenIngredientItem> BAKING_SPICE = ingredient(
            "baking_spice", IngredientState.WHOLE_SPICE, 0.80F, 0.66F, 0.20F, 0.05F, 0.00F, 0.84F, 0.00F, Long.MAX_VALUE / 4L, 0, 1
    );
    public static final DeferredItem<KitchenIngredientItem> PIE_CRUST = ingredient(
            "pie_crust", IngredientState.CRUST_MIX, 0.72F, 0.28F, 0.34F, 0.62F, 0.24F, 0.82F, 0.04F, 36_000L, 4, 2
    );
    public static final DeferredItem<KitchenIngredientItem> PIE_FILLING = ingredient(
            "pie_filling", IngredientState.SIMMERED_FILLING, 0.78F, 0.74F, 0.50F, 0.18F, 0.68F, 0.70F, 0.00F, 36_000L, 4, 4
    );
    public static final DeferredItem<KitchenIngredientItem> APPLE_PIE = ingredient(
            "apple_pie", IngredientState.RAW_ASSEMBLED_PIE, 0.80F, 0.74F, 0.55F, 0.72F, 0.60F, 0.72F, 0.12F, 48_000L, 7, 6
    );
    public static final DeferredItem<KitchenIngredientItem> APPLE_PIE_SLICE = ingredient(
            "apple_pie_slice", IngredientState.SLICED_PIE, 0.82F, 0.78F, 0.60F, 0.68F, 0.60F, 0.74F, 0.15F, 24_000L, 7, 6
    );
    public static final DeferredItem<KitchenIngredientItem> BREAD_DOUGH = ingredient(
            "bread_dough", IngredientState.BREAD_DOUGH, 0.74F, 0.20F, 0.38F, 0.62F, 0.68F, 0.88F, 0.10F, 24_000L, 4, 2
    );
    public static final DeferredItem<KitchenIngredientItem> BREAD_LOAF = ingredient(
            "bread_loaf", IngredientState.BAKED_BREAD, 0.80F, 0.52F, 0.70F, 0.76F, 0.48F, 0.88F, 0.16F, 28_000L, 6, 5
    );
    public static final DeferredItem<KitchenIngredientItem> SLICED_BREAD = ingredient(
            "sliced_bread", IngredientState.SLICED_BREAD, 0.82F, 0.54F, 0.68F, 0.64F, 0.46F, 0.90F, 0.16F, 18_000L, 4, 4
    );
    public static final DeferredItem<KitchenIngredientItem> TOMATO_SOUP_BASE = ingredient(
            "tomato_soup_base", IngredientState.SOUP_BASE, 0.80F, 0.78F, 0.48F, 0.20F, 0.82F, 0.52F, 0.04F, 24_000L, 5, 4
    );
    public static final DeferredItem<KitchenIngredientItem> STRAINED_SOUP = ingredient(
            "strained_soup", IngredientState.STRAINED_SOUP, 0.84F, 0.80F, 0.50F, 0.18F, 0.82F, 0.86F, 0.04F, 24_000L, 5, 4
    );
    public static final DeferredItem<KitchenIngredientItem> DUMPLING_FILLING_ITEM = ingredient(
            "dumpling_filling", IngredientState.DUMPLING_FILLING, 0.78F, 0.72F, 0.50F, 0.28F, 0.66F, 0.48F, 0.06F, 24_000L, 5, 4
    );
    public static final DeferredItem<KitchenIngredientItem> DUMPLING_DOUGH_ITEM = ingredient(
            "dumpling_dough", IngredientState.DUMPLING_DOUGH, 0.76F, 0.18F, 0.42F, 0.66F, 0.64F, 0.84F, 0.10F, 24_000L, 4, 2
    );
    public static final DeferredItem<KitchenIngredientItem> RAW_DUMPLINGS_ITEM = ingredient(
            "raw_dumplings", IngredientState.RAW_DUMPLINGS, 0.78F, 0.58F, 0.46F, 0.66F, 0.70F, 0.68F, 0.10F, 22_000L, 6, 4
    );
    public static final DeferredItem<KitchenIngredientItem> STEAMED_DUMPLINGS_ITEM = ingredient(
            "steamed_dumplings", IngredientState.STEAMED_DUMPLINGS, 0.84F, 0.72F, 0.66F, 0.70F, 0.76F, 0.72F, 0.10F, 18_000L, 7, 6
    );
    public static final DeferredItem<KitchenIngredientItem> MARINATED_PROTEIN_ITEM = ingredient(
            "marinated_protein", IngredientState.MARINATED_PROTEIN, 0.80F, 0.78F, 0.56F, 0.32F, 0.76F, 0.52F, 0.06F, 18_000L, 5, 4
    );
    public static final DeferredItem<KitchenIngredientItem> BATTERED_PROTEIN_ITEM = ingredient(
            "battered_protein", IngredientState.BATTERED_PROTEIN, 0.80F, 0.74F, 0.60F, 0.36F, 0.78F, 0.60F, 0.08F, 16_000L, 6, 5
    );
    public static final DeferredItem<KitchenIngredientItem> FRIED_PROTEIN_ITEM = ingredient(
            "fried_protein", IngredientState.FRIED_PROTEIN, 0.86F, 0.86F, 0.84F, 0.48F, 0.52F, 0.64F, 0.06F, 16_000L, 8, 9
    );
    public static final DeferredItem<KitchenIngredientItem> ROASTED_PROTEIN_ITEM = ingredient(
            "roasted_protein", IngredientState.ROASTED_PROTEIN, 0.84F, 0.84F, 0.74F, 0.50F, 0.58F, 0.60F, 0.04F, 18_000L, 8, 7
    );
    public static final DeferredItem<KitchenIngredientItem> BROILED_PROTEIN_ITEM = ingredient(
            "broiled_protein", IngredientState.BROILED_PROTEIN, 0.88F, 0.90F, 0.78F, 0.52F, 0.56F, 0.62F, 0.04F, 16_000L, 8, 8
    );
    public static final DeferredItem<KitchenIngredientItem> SMOKED_PROTEIN_ITEM = ingredient(
            "smoked_protein", IngredientState.SMOKED_PROTEIN, 0.88F, 0.92F, 0.70F, 0.52F, 0.50F, 0.66F, 0.04F, 72_000L, 8, 8
    );
    public static final DeferredItem<KitchenIngredientItem> ROAST_VEGETABLES_ITEM = ingredient(
            "roast_vegetables", IngredientState.ROAST_VEGETABLES, 0.82F, 0.76F, 0.62F, 0.24F, 0.54F, 0.40F, 0.04F, 24_000L, 5, 5
    );
    public static final DeferredItem<KitchenIngredientItem> CANNED_TOMATO_ITEM = ingredient(
            "canned_tomato", IngredientState.CANNED_TOMATO, 0.80F, 0.70F, 0.50F, 0.22F, 0.80F, 0.74F, 0.02F, Long.MAX_VALUE / 4L, 4, 3
    );
    public static final DeferredItem<KitchenIngredientItem> APPLE_PRESERVE_ITEM = ingredient(
            "apple_preserve", IngredientState.APPLE_PRESERVE, 0.82F, 0.82F, 0.38F, 0.14F, 0.76F, 0.82F, 0.00F, Long.MAX_VALUE / 4L, 3, 6
    );
    public static final DeferredItem<KitchenIngredientItem> DRIED_APPLE_ITEM = ingredient(
            "dried_apple", IngredientState.DRIED_FRUIT, 0.78F, 0.84F, 0.48F, 0.16F, 0.18F, 0.74F, 0.00F, Long.MAX_VALUE / 4L, 2, 4
    );
    public static final DeferredItem<KitchenIngredientItem> FERMENTED_VEGETABLES_ITEM = ingredient(
            "fermented_vegetables", IngredientState.FERMENTED_VEGETABLE, 0.82F, 0.78F, 0.58F, 0.18F, 0.70F, 0.78F, 0.02F, Long.MAX_VALUE / 4L, 4, 4
    );
    public static final DeferredItem<KitchenIngredientItem> CULTURED_DAIRY_ITEM = ingredient(
            "cultured_dairy", IngredientState.CULTURED_DAIRY, 0.84F, 0.62F, 0.62F, 0.22F, 0.82F, 0.84F, 0.06F, Long.MAX_VALUE / 4L, 5, 4
    );
    public static final DeferredItem<KitchenIngredientItem> CERAMIC_PLATE = ingredient(
            "ceramic_plate", IngredientState.PANTRY_READY, 0.80F, 0.00F, 0.70F, 0.90F, 0.00F, 1.00F, 0.00F, Long.MAX_VALUE / 4L, 0, 0
    );

    public static final DeferredItem<KitchenMealItem> PLATED_APPLE_PIE_SLICE = meal(
            "plated_apple_pie_slice", IngredientState.PLATED_SLICE, 0.86F, 0.82F, 0.68F, 0.72F, 0.68F, 0.78F, 0.18F, 18_000L, 8, 8, true
    );
    public static final DeferredItem<KitchenMealItem> PLATED_TOMATO_SOUP_MEAL = meal(
            "plated_tomato_soup_meal", IngredientState.PLATED_SOUP_MEAL, 0.84F, 0.84F, 0.64F, 0.44F, 0.80F, 0.84F, 0.08F, 16_000L, 10, 7, true
    );
    public static final DeferredItem<KitchenMealItem> PLATED_DUMPLING_MEAL = meal(
            "plated_dumpling_meal", IngredientState.PLATED_DUMPLING_MEAL, 0.86F, 0.82F, 0.72F, 0.62F, 0.74F, 0.78F, 0.10F, 16_000L, 10, 8, true
    );
    public static final DeferredItem<KitchenMealItem> PLATED_FRIED_MEAL = meal(
            "plated_fried_meal", IngredientState.PLATED_FRIED_MEAL, 0.88F, 0.90F, 0.86F, 0.56F, 0.56F, 0.72F, 0.06F, 16_000L, 11, 10, true
    );
    public static final DeferredItem<KitchenMealItem> PLATED_ROAST_MEAL = meal(
            "plated_roast_meal", IngredientState.PLATED_ROAST_MEAL, 0.88F, 0.88F, 0.78F, 0.58F, 0.60F, 0.72F, 0.06F, 16_000L, 11, 9, true
    );

    public static final DeferredItem<KitchenToolItem> PARING_KNIFE = tool("paring_knife", ToolProfile.PARING_KNIFE, 0.06F, 1.10F, 128);
    public static final DeferredItem<KitchenToolItem> CHEF_KNIFE = tool("chef_knife", ToolProfile.CHEF_KNIFE, 0.09F, 1.20F, 192);
    public static final DeferredItem<KitchenToolItem> CLEAVER = tool("cleaver", ToolProfile.CLEAVER, 0.07F, 1.05F, 224);
    public static final DeferredItem<KitchenToolItem> WHISK = tool("whisk", ToolProfile.WHISK, 0.05F, 1.08F, 160);
    public static final DeferredItem<KitchenToolItem> ROLLING_PIN = tool("rolling_pin", ToolProfile.ROLLING_PIN, 0.07F, 1.12F, 192);
    public static final DeferredItem<KitchenToolItem> MORTAR_PESTLE = tool("mortar_pestle", ToolProfile.MORTAR_PESTLE, 0.08F, 1.06F, 192);
    public static final DeferredItem<KitchenToolItem> STOCK_POT = tool("stock_pot", ToolProfile.STOCK_POT, 0.05F, 1.00F, 256);
    public static final DeferredItem<KitchenToolItem> FRYING_SKILLET = tool("frying_skillet", ToolProfile.FRYING_SKILLET, 0.08F, 1.10F, 256);
    public static final DeferredItem<KitchenToolItem> FINE_STRAINER = tool("fine_strainer", ToolProfile.FINE_STRAINER, 0.08F, 1.06F, 192);
    public static final DeferredItem<KitchenToolItem> COARSE_STRAINER = tool("coarse_strainer", ToolProfile.COARSE_STRAINER, 0.03F, 0.98F, 176);
    public static final DeferredItem<KitchenToolItem> STEAMER_BASKET = tool("steamer_basket", ToolProfile.STEAMER_BASKET, 0.05F, 1.04F, 192);
    public static final DeferredItem<KitchenToolItem> CANNING_JAR = tool("canning_jar", ToolProfile.JAR, 0.04F, 0.98F, 96);
    public static final DeferredItem<KitchenToolItem> PIE_TIN = tool("pie_tin", ToolProfile.PIE_TIN, 0.05F, 1.00F, 256);

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

    private static DeferredItem<KitchenMealItem> meal(
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
            int enjoyment,
            boolean returnsPlate
    ) {
        return ITEMS.register(name, () -> new KitchenMealItem(
                new Item.Properties().stacksTo(16),
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
                enjoyment,
                returnsPlate
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
