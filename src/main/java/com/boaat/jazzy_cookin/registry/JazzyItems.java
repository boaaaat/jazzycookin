package com.boaat.jazzy_cookin.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.boaat.jazzy_cookin.JazzyCookin;
import com.boaat.jazzy_cookin.item.KitchenIngredientItem;
import com.boaat.jazzy_cookin.item.KitchenMealItem;
import com.boaat.jazzy_cookin.item.KitchenToolItem;
import com.boaat.jazzy_cookin.kitchen.IngredientState;
import com.boaat.jazzy_cookin.kitchen.PantrySortTab;
import com.boaat.jazzy_cookin.kitchen.ToolProfile;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class JazzyItems {
    private static final long DAY = 24_000L;

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
    public static final DeferredItem<BlockItem> FRIDGE_ITEM = ITEMS.registerSimpleBlockItem("fridge", JazzyBlocks.FRIDGE);
    public static final DeferredItem<BlockItem> FREEZER_ITEM = ITEMS.registerSimpleBlockItem("freezer", JazzyBlocks.FREEZER);
    public static final DeferredItem<BlockItem> CELLAR_ITEM = ITEMS.registerSimpleBlockItem("cellar", JazzyBlocks.CELLAR);
    public static final DeferredItem<BlockItem> PREP_TABLE_ITEM = ITEMS.registerSimpleBlockItem("prep_table", JazzyBlocks.PREP_TABLE);
    public static final DeferredItem<BlockItem> SPICE_GRINDER_ITEM = ITEMS.registerSimpleBlockItem("spice_grinder", JazzyBlocks.SPICE_GRINDER);
    public static final DeferredItem<BlockItem> STRAINER_ITEM = ITEMS.registerSimpleBlockItem("strainer", JazzyBlocks.STRAINER);
    public static final DeferredItem<BlockItem> MIXING_BOWL_ITEM = ITEMS.registerSimpleBlockItem("mixing_bowl", JazzyBlocks.MIXING_BOWL);
    public static final DeferredItem<BlockItem> MICROWAVE_ITEM = ITEMS.registerSimpleBlockItem("microwave", JazzyBlocks.MICROWAVE);
    public static final DeferredItem<BlockItem> FOOD_PROCESSOR_ITEM = ITEMS.registerSimpleBlockItem("food_processor", JazzyBlocks.FOOD_PROCESSOR);
    public static final DeferredItem<BlockItem> BLENDER_ITEM = ITEMS.registerSimpleBlockItem("blender", JazzyBlocks.BLENDER);
    public static final DeferredItem<BlockItem> JUICER_ITEM = ITEMS.registerSimpleBlockItem("juicer", JazzyBlocks.JUICER);
    public static final DeferredItem<BlockItem> FREEZE_DRYER_ITEM = ITEMS.registerSimpleBlockItem("freeze_dryer", JazzyBlocks.FREEZE_DRYER);
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

    public enum IngredientId {
        WHITE_SUGAR("white_sugar", sweetener(0, 1)),
        LIGHT_BROWN_SUGAR("light_brown_sugar", sweetener(0, 1)),
        DARK_BROWN_SUGAR("dark_brown_sugar", sweetener(0, 2)),
        POWDERED_SUGAR("powdered_sugar", sweetener(0, 1)),
        HONEY("honey", syrupSweetener(DAY * 90L, 0, 1, 2)),
        MAPLE_SYRUP("maple_syrup", syrupSweetener(DAY * 120L, 0, 0, 2)),
        MOLASSES("molasses", syrupSweetener(DAY * 150L, 0, 0, 1)),
        CORN_SYRUP("corn_syrup", syrupSweetener(DAY * 150L, 0, 0, 1)),
        AGAVE_NECTAR("agave_nectar", syrupSweetener(DAY * 120L, 0, 0, 2)),

        ALL_PURPOSE_FLOUR("all_purpose_flour", flour(DAY * 90L, 0, 1, 0)),
        BREAD_FLOUR("bread_flour", flour(DAY * 90L, 0, 1, 0)),
        CAKE_FLOUR("cake_flour", flour(DAY * 90L, 0, 1, 0)),
        WHOLE_WHEAT_FLOUR("whole_wheat_flour", flour(DAY * 60L, 0, 1, 0)),
        CORN_FLOUR("corn_flour", flour(DAY * 75L, 0, 1, 0)),
        CORNMEAL("cornmeal", grain(PantrySortTab.GRAINS_AND_FLOURS, DAY * 75L, 30, 2, 0)),
        WHITE_RICE_FLOUR("white_rice_flour", flour(DAY * 90L, 0, 1, 0)),
        BROWN_RICE_FLOUR("brown_rice_flour", flour(DAY * 60L, 0, 1, 0)),
        RICE("rice", grain(PantrySortTab.GRAINS_AND_FLOURS, DAY * 180L, 90, 3, 1)),
        ROLLED_OATS("rolled_oats", grain(PantrySortTab.GRAINS_AND_FLOURS, DAY * 90L, 40, 2, 1)),
        QUICK_OATS("quick_oats", grain(PantrySortTab.GRAINS_AND_FLOURS, DAY * 75L, 30, 2, 1)),
        PASTA("pasta", grain(PantrySortTab.GRAINS_AND_FLOURS, DAY * 120L, 100, 2, 1)),
        QUINOA("quinoa", grain(PantrySortTab.GRAINS_AND_FLOURS, DAY * 120L, 90, 3, 1)),
        SEMOLINA("semolina", grain(PantrySortTab.GRAINS_AND_FLOURS, DAY * 120L, 30, 1, 0)),
        BREADCRUMBS("breadcrumbs", flour(DAY * 60L, 0, 1, 0)),

        BAKING_SODA("baking_soda", leavener(DAY * 180L)),
        BAKING_POWDER("baking_powder", leavener(DAY * 120L)),
        ACTIVE_DRY_YEAST("active_dry_yeast", leavener(DAY * 120L)),
        INSTANT_YEAST("instant_yeast", leavener(DAY * 120L)),
        SOURDOUGH_STARTER("sourdough_starter", starter(DAY * 10L, 1, 1)),
        CREAM_OF_TARTAR("cream_of_tartar", leavener(DAY * 180L)),

        TABLE_SALT("table_salt", seasoning(DAY * 180L, 0, 0)),
        KOSHER_SALT("kosher_salt", seasoning(DAY * 180L, 0, 0)),
        SEA_SALT("sea_salt", seasoning(DAY * 180L, 0, 0)),
        BLACK_PEPPER("black_pepper", seasoning(DAY * 180L, 0, 1)),
        CINNAMON("cinnamon", seasoning(DAY * 180L, 0, 2)),
        NUTMEG("nutmeg", seasoning(DAY * 180L, 0, 2)),
        PAPRIKA("paprika", seasoning(DAY * 180L, 0, 2)),
        CUMIN("cumin", seasoning(DAY * 180L, 0, 2)),
        TURMERIC("turmeric", seasoning(DAY * 180L, 0, 2)),
        CLOVES("cloves", seasoning(DAY * 180L, 0, 2)),
        CHILI_POWDER("chili_powder", seasoning(DAY * 180L, 0, 2)),
        GARLIC_POWDER("garlic_powder", seasoning(DAY * 180L, 0, 1)),
        ONION_POWDER("onion_powder", seasoning(DAY * 180L, 0, 1)),

        BASIL("basil", herb(DAY * 45L, 18, 0, 2)),
        THYME("thyme", herb(DAY * 60L, 18, 0, 2)),
        ROSEMARY("rosemary", herb(DAY * 60L, 18, 0, 2)),
        PARSLEY("parsley", herb(DAY * 45L, 18, 0, 2)),
        DILL("dill", herb(DAY * 45L, 18, 0, 2)),
        OREGANO("oregano", herb(DAY * 60L, 18, 0, 2)),
        MINT("mint", herb(DAY * 45L, 18, 0, 2)),
        SAGE("sage", herb(DAY * 60L, 18, 0, 2)),

        VEGETABLE_OIL("vegetable_oil", oil(DAY * 120L, 0)),
        OLIVE_OIL("olive_oil", oil(DAY * 120L, 1)),
        CANOLA_OIL("canola_oil", oil(DAY * 120L, 0)),
        COCONUT_OIL("coconut_oil", oil(DAY * 90L, 0)),
        BUTTER("butter", refrigeratedFat(DAY * 20L, 12, 2, 2)),
        SHORTENING("shortening", pantryFat(DAY * 180L, 0, 0)),
        LARD("lard", chilledFat(DAY * 60L, 0, 1, 0)),

        POWDERED_MILK("powdered_milk", stableDairy(DAY * 90L, 0, 1, 1, false, false)),
        EVAPORATED_MILK("evaporated_milk", stableDairy(DAY * 60L, 24, 2, 1, true, false)),
        SWEETENED_CONDENSED_MILK("sweetened_condensed_milk", stableDairy(DAY * 90L, 24, 2, 2, true, false)),
        SHELF_STABLE_CREAM("shelf_stable_cream", stableDairy(DAY * 45L, 24, 3, 2, true, false)),
        ALMOND_MILK("almond_milk", stableDairy(DAY * 30L, 20, 2, 1, true, false)),
        OAT_MILK("oat_milk", stableDairy(DAY * 30L, 20, 2, 1, true, false)),
        SOY_MILK("soy_milk", stableDairy(DAY * 30L, 20, 2, 1, true, false)),
        CHEESE("cheese", stableDairy(DAY * 18L, 12, 3, 2, true, true)),
        CREAM_CHEESE("cream_cheese", stableDairy(DAY * 10L, 0, 2, 2, true, true)),

        CHICKEN("chicken", protein(DAY * 4L, 120, 6, 2, true)),
        FISH_FILLET("fish_fillet", protein(DAY * 3L, 70, 5, 2, true)),
        PORK("pork", protein(DAY * 4L, 110, 6, 2, true)),
        BEEF("beef", protein(DAY * 5L, 120, 6, 2, true)),
        EGGS("eggs", protein(DAY * 18L, 64, 4, 2, false)),
        TOFU("tofu", protein(DAY * 7L, 50, 4, 2, true)),

        APPLES("apples", produce(DAY * 10L, 32, 2, 2, false)),
        TOMATOES("tomatoes", produce(DAY * 6L, 40, 2, 2, false)),
        CARROTS("carrots", produce(DAY * 12L, 45, 2, 1, true)),
        ONIONS("onions", produce(DAY * 18L, 35, 1, 1, false)),
        POTATOES("potatoes", produce(DAY * 20L, 80, 3, 1, true)),
        LEMONS("lemons", produce(DAY * 12L, 20, 1, 2, false)),
        CABBAGE("cabbage", produce(DAY * 8L, 60, 2, 1, true)),
        GARLIC("garlic", produce(DAY * 20L, 18, 1, 1, false)),
        GINGER("ginger", produce(DAY * 14L, 18, 1, 1, false)),
        SHALLOTS("shallots", produce(DAY * 16L, 28, 1, 1, false)),
        JALAPENOS("jalapenos", produce(DAY * 8L, 16, 1, 2, true)),
        RED_PEPPER("red_pepper", produce(DAY * 10L, 20, 1, 2, true)),

        CANNED_TOMATOES("canned_tomatoes", canned(DAY * 120L, 30, 2, 1)),
        TOMATO_PASTE("tomato_paste", canned(DAY * 90L, 20, 1, 1)),
        BLACK_BEANS("black_beans", canned(DAY * 120L, 36, 3, 1)),
        KIDNEY_BEANS("kidney_beans", canned(DAY * 120L, 36, 3, 1)),
        CHICKPEAS("chickpeas", canned(DAY * 120L, 36, 3, 1)),
        BROTH("broth", canned(DAY * 60L, 24, 1, 1)),
        STOCK("stock", canned(DAY * 60L, 24, 1, 1)),
        COCONUT_MILK("coconut_milk", canned(DAY * 90L, 24, 2, 1)),
        PEANUT_BUTTER("peanut_butter", canned(DAY * 120L, 0, 3, 2)),
        JAM("jam", canned(DAY * 120L, 0, 1, 2)),
        JELLY("jelly", canned(DAY * 120L, 0, 1, 2)),
        PICKLES("pickles", canned(DAY * 120L, 0, 1, 1)),
        BRINED_VEGETABLES("brined_vegetables", canned(DAY * 90L, 0, 2, 1)),
        SYRUP_PRESERVES("syrup_preserves", canned(DAY * 120L, 0, 1, 2)),

        CHOCOLATE_CHIPS("chocolate_chips", addIn(DAY * 180L, 0, 1, 2)),
        COCOA_POWDER("cocoa_powder", addIn(DAY * 180L, 0, 0, 1)),
        VANILLA_EXTRACT("vanilla_extract", bakingExtract(DAY * 180L, 2)),
        SPRINKLES("sprinkles", addIn(DAY * 180L, 0, 0, 2)),
        CARAMEL_BITS("caramel_bits", addIn(DAY * 120L, 0, 1, 2)),
        ALMONDS("almonds", addIn(DAY * 120L, 0, 2, 1)),
        WALNUTS("walnuts", addIn(DAY * 90L, 0, 2, 1)),
        PECANS("pecans", addIn(DAY * 90L, 0, 2, 1)),
        RAISINS("raisins", addIn(DAY * 120L, 0, 1, 2)),
        DRIED_CRANBERRIES("dried_cranberries", addIn(DAY * 120L, 0, 1, 2)),

        SPAGHETTI("spaghetti", dryGood(DAY * 180L, 110, 3, 1)),
        MACARONI("macaroni", dryGood(DAY * 180L, 100, 3, 1)),
        RAMEN("ramen", dryGood(DAY * 120L, 70, 2, 2)),
        COUSCOUS("couscous", dryGood(DAY * 150L, 60, 2, 1)),
        LENTILS("lentils", dryGood(DAY * 180L, 90, 3, 1)),
        DRY_BEANS("dry_beans", dryGood(DAY * 210L, 140, 3, 1)),
        BREAD("bread", dryGood(DAY * 7L, 0, 2, 1)),

        KETCHUP("ketchup", condiment(DAY * 90L, 0, 0, 1, true)),
        MUSTARD("mustard", condiment(DAY * 120L, 0, 0, 1, true)),
        MAYONNAISE("mayonnaise", condiment(DAY * 45L, 0, 1, 1, true)),
        SOY_SAUCE("soy_sauce", condiment(DAY * 180L, 0, 0, 1, false)),
        WHITE_VINEGAR("white_vinegar", condiment(DAY * 180L, 0, 0, 0, false)),
        APPLE_CIDER_VINEGAR("apple_cider_vinegar", condiment(DAY * 180L, 0, 0, 0, false)),
        BALSAMIC_VINEGAR("balsamic_vinegar", condiment(DAY * 180L, 0, 0, 1, false)),
        HOT_SAUCE("hot_sauce", condiment(DAY * 180L, 0, 0, 2, false)),
        WORCESTERSHIRE_SAUCE("worcestershire_sauce", condiment(DAY * 180L, 0, 0, 1, false)),
        TOMATO_SAUCE("tomato_sauce", condiment(DAY * 30L, 12, 1, 2, true)),

        DRIED_GARLIC("dried_garlic", seasoning(DAY * 180L, 0, 1)),
        DRIED_ONIONS("dried_onions", seasoning(DAY * 180L, 0, 1)),

        CRACKERS("crackers", snack(DAY * 90L, 0, 1, 1)),
        COOKIES("cookies", snack(DAY * 45L, 0, 2, 2)),
        CANDY("candy", snack(DAY * 120L, 0, 0, 3)),
        CHIPS("chips", snack(DAY * 75L, 0, 1, 2)),

        ITALIAN_SEASONING("italian_seasoning", seasoning(DAY * 180L, 0, 2)),
        CURRY_POWDER("curry_powder", seasoning(DAY * 180L, 0, 2)),
        PANCAKE_MIX("pancake_mix", bakingMix(DAY * 120L, 40, 2, 1)),
        CAKE_MIX("cake_mix", bakingMix(DAY * 120L, 50, 2, 1)),
        FERMENTATION_STARTER("fermentation_starter", starter(DAY * 20L, 1, 1)),
        VINEGAR_MOTHER("vinegar_mother", condiment(DAY * 60L, 0, 0, 0, true)),
        MATCHA("matcha", addIn(DAY * 180L, 0, 0, 2)),
        ROSE_WATER("rose_water", bakingExtract(DAY * 180L, 2)),
        FOOD_COLORING("food_coloring", addIn(DAY * 240L, 0, 0, 1));

        private final String id;
        private final IngredientSpec spec;

        IngredientId(String id, IngredientSpec spec) {
            this.id = id;
            this.spec = spec;
        }

        public String id() {
            return this.id;
        }

        public IngredientState defaultState() {
            return IngredientState.byName(this.id);
        }
    }

    private static final Map<IngredientId, DeferredItem<KitchenIngredientItem>> INGREDIENTS = registerIngredients();
    private static final List<DeferredItem<KitchenIngredientItem>> INGREDIENT_LIST = Collections.unmodifiableList(new ArrayList<>(INGREDIENTS.values()));

    public static final DeferredItem<Item> CERAMIC_PLATE = ITEMS.register(
            "ceramic_plate",
            () -> new Item(new Item.Properties().stacksTo(16))
    );
    public static final DeferredItem<Item> TUPPERWARE = utilityItem("tupperware", 16);
    public static final DeferredItem<Item> CERAMIC_BOWL = utilityItem("ceramic_bowl", 16);
    public static final DeferredItem<Item> GLASS_CUP = utilityItem("glass_cup", 16);
    public static final DeferredItem<Item> WOODEN_BOARD = utilityItem("wooden_board", 16);
    public static final DeferredItem<Item> SERVING_TRAY = utilityItem("serving_tray", 16);
    public static final DeferredItem<Item> SERVING_SPOON = utilityItem("serving_spoon", 16);
    public static final DeferredItem<Item> BAMBOO_TRAY = utilityItem("bamboo_tray", 16);
    public static final DeferredItem<Item> CHOPSTICKS = utilityItem("chopsticks", 16);
    public static final DeferredItem<Item> STRAW = utilityItem("straw", 64);
    public static final DeferredItem<Item> BASKET = utilityItem("basket", 16);
    public static final DeferredItem<Item> PAPER_LINER = utilityItem("paper_liner", 64);
    public static final DeferredItem<Item> BUTTER_KNIFE = utilityItem("butter_knife", 16);

    public static final DeferredItem<KitchenIngredientItem> CHOPPED_PRODUCE_BLEND = prepared("chopped_produce_blend", IngredientState.ROUGH_CUT, PantrySortTab.OTHER, DAY * 2L, 0, 2, 1, true, false);
    public static final DeferredItem<KitchenIngredientItem> LEMON_JUICE = prepared("lemon_juice", IngredientState.FRESH_JUICE, PantrySortTab.SAUCES_AND_CONDIMENTS, DAY * 4L, 0, 0, 2, true, false);
    public static final DeferredItem<KitchenIngredientItem> FRUIT_PULP = prepared("fruit_pulp", IngredientState.PULP, PantrySortTab.OTHER, DAY * 2L, 0, 1, 0, true, false);
    public static final DeferredItem<KitchenIngredientItem> MIXED_JUICE = prepared("mixed_juice", IngredientState.FRESH_JUICE, PantrySortTab.OTHER, DAY * 3L, 0, 1, 2, true, false);
    public static final DeferredItem<KitchenIngredientItem> FRUIT_JUICE_BLEND = prepared("fruit_juice_blend", IngredientState.SMOOTH, PantrySortTab.OTHER, DAY * 3L, 0, 1, 2, true, false);
    public static final DeferredItem<KitchenIngredientItem> JARRED_LEMON_JUICE = prepared("jarred_lemon_juice", IngredientState.FRESH_JUICE, PantrySortTab.SAUCES_AND_CONDIMENTS, DAY * 20L, 0, 1, 2, true, false);
    public static final DeferredItem<KitchenIngredientItem> PACKED_FREEZE_DRY_APPLES = prepared("packed_freeze_dry_apples", IngredientState.FREEZE_DRIED, PantrySortTab.SNACKS, DAY * 180L, 0, 1, 2, false, false);
    public static final DeferredItem<KitchenIngredientItem> TOMATO_SOUP_BASE = prepared("tomato_soup_base", IngredientState.CHOPPED, PantrySortTab.OTHER, DAY * 3L, 20, 2, 1, true, false);
    public static final DeferredItem<KitchenIngredientItem> SEASONING_BLEND = prepared("seasoning_blend", IngredientState.FINE_POWDER, PantrySortTab.SEASONINGS, DAY * 120L, 0, 0, 2, false, false);
    public static final DeferredItem<KitchenIngredientItem> PASTA_BAKE_ASSEMBLY = prepared("pasta_bake_assembly", IngredientState.MIXED, PantrySortTab.OTHER, DAY * 3L, 40, 4, 2, true, false);
    public static final DeferredItem<KitchenIngredientItem> PIE_DOUGH = prepared("pie_dough", IngredientState.DOUGH, PantrySortTab.OTHER, DAY * 4L, 0, 2, 1, true, false);
    public static final DeferredItem<KitchenIngredientItem> ASSEMBLED_TRAY_PIE = prepared("assembled_tray_pie", IngredientState.RAW_ASSEMBLED_PIE, PantrySortTab.OTHER, DAY * 2L, 60, 3, 2, true, false);
    public static final DeferredItem<KitchenIngredientItem> PAN_SEARED_CHICKEN_PREP = prepared("pan_seared_chicken_prep", IngredientState.PAN_FRIED, PantrySortTab.OTHER, DAY * 3L, 50, 6, 2, true, true);
    public static final DeferredItem<KitchenIngredientItem> HEARTY_STEW_BASE = prepared("hearty_stew_base", IngredientState.ROUGH_CUT, PantrySortTab.OTHER, DAY * 3L, 80, 5, 2, true, false);
    public static final DeferredItem<KitchenIngredientItem> SMOKED_MEAT = prepared("smoked_meat", IngredientState.SMOKED, PantrySortTab.OTHER, DAY * 10L, 60, 5, 2, true, true);
    public static final DeferredItem<KitchenIngredientItem> DUMPLING_FILLING = prepared("dumpling_filling", IngredientState.DUMPLING_FILLING, PantrySortTab.OTHER, DAY * 2L, 0, 3, 2, true, false);
    public static final DeferredItem<KitchenIngredientItem> DUMPLING_DOUGH = prepared("dumpling_dough", IngredientState.DUMPLING_DOUGH, PantrySortTab.OTHER, DAY * 3L, 0, 2, 1, true, false);
    public static final DeferredItem<KitchenIngredientItem> RAW_DUMPLINGS = prepared("raw_dumplings", IngredientState.RAW_DUMPLINGS, PantrySortTab.OTHER, DAY * 2L, 50, 4, 2, true, true);
    public static final DeferredItem<KitchenIngredientItem> SMOOTHIE_BLEND = prepared("smoothie_blend", IngredientState.SMOOTH, PantrySortTab.OTHER, DAY * 2L, 0, 2, 2, true, false);
    public static final DeferredItem<KitchenIngredientItem> NUT_BUTTER = prepared("nut_butter", IngredientState.PASTE, PantrySortTab.SAUCES_AND_CONDIMENTS, DAY * 45L, 0, 3, 2, false, false);
    public static final DeferredItem<KitchenIngredientItem> BATTER_MIX = prepared("batter_mix", IngredientState.BATTER, PantrySortTab.OTHER, DAY * 1L, 0, 2, 1, true, false);
    public static final DeferredItem<KitchenIngredientItem> FRIED_CHICKEN_PREP = prepared("fried_chicken_prep", IngredientState.DEEP_FRIED, PantrySortTab.OTHER, DAY * 2L, 80, 6, 3, true, true);
    public static final DeferredItem<KitchenIngredientItem> JARRED_NUT_BUTTER = prepared("jarred_nut_butter", IngredientState.PASTE, PantrySortTab.SAUCES_AND_CONDIMENTS, DAY * 120L, 0, 3, 2, false, false);
    public static final DeferredItem<KitchenIngredientItem> PACKED_BREADCRUMBS = prepared("packed_breadcrumbs", IngredientState.COARSE_POWDER, PantrySortTab.DRY_GOODS, DAY * 120L, 0, 1, 0, false, false);
    public static final DeferredItem<KitchenIngredientItem> PORTIONED_MEAL = prepared("portioned_meal", IngredientState.PORTIONED_MEAL, PantrySortTab.OTHER, DAY * 2L, 0, 6, 4, true, false);
    public static final DeferredItem<KitchenIngredientItem> FREEZE_DRIED_MEAL = prepared("freeze_dried_meal", IngredientState.FREEZE_DRIED, PantrySortTab.SNACKS, DAY * 180L, 0, 6, 4, false, false);
    public static final DeferredItem<KitchenIngredientItem> JAM_TOAST_PREP = prepared("jam_toast_prep", IngredientState.MIXED, PantrySortTab.OTHER, DAY * 1L, 0, 3, 4, true, false);
    public static final DeferredItem<KitchenIngredientItem> SYRUP_MIXTURE = prepared("syrup_mixture", IngredientState.CANNING_SYRUP, PantrySortTab.SAUCES_AND_CONDIMENTS, DAY * 1L, 0, 1, 2, true, false);
    public static final DeferredItem<KitchenIngredientItem> HOT_SYRUP_PRESERVE = prepared("hot_syrup_preserve", IngredientState.HOT_PRESERVE, PantrySortTab.SAUCES_AND_CONDIMENTS, DAY * 14L, 0, 1, 2, true, false);
    public static final DeferredItem<KitchenIngredientItem> SAVORY_PIE_FILLING = prepared("savory_pie_filling", IngredientState.SIMMERED_FILLING, PantrySortTab.OTHER, DAY * 2L, 80, 5, 3, true, false);
    public static final DeferredItem<KitchenIngredientItem> ASSEMBLED_SAVORY_PIE = prepared("assembled_savory_pie", IngredientState.RAW_ASSEMBLED_PIE, PantrySortTab.OTHER, DAY * 2L, 80, 6, 4, true, false);
    public static final DeferredItem<KitchenIngredientItem> FISH_COATING = prepared("fish_coating", IngredientState.COARSE_POWDER, PantrySortTab.OTHER, DAY * 1L, 0, 1, 1, false, false);
    public static final DeferredItem<KitchenIngredientItem> FRIED_FISH_PREP = prepared("fried_fish_prep", IngredientState.DEEP_FRIED, PantrySortTab.OTHER, DAY * 2L, 70, 5, 3, true, true);
    public static final DeferredItem<KitchenIngredientItem> BRAISED_BEEF_BASE = prepared("braised_beef_base", IngredientState.ROUGH_CUT, PantrySortTab.OTHER, DAY * 2L, 90, 5, 2, true, false);
    public static final DeferredItem<KitchenIngredientItem> BRAISED_BEEF_PREP = prepared("braised_beef_prep", IngredientState.SIMMERED, PantrySortTab.OTHER, DAY * 4L, 100, 7, 4, true, true);
    public static final DeferredItem<KitchenIngredientItem> PEANUT_SAUCE = prepared("peanut_sauce", IngredientState.SMOOTH_MIXTURE, PantrySortTab.SAUCES_AND_CONDIMENTS, DAY * 20L, 0, 3, 3, true, false);
    public static final DeferredItem<KitchenIngredientItem> PEANUT_NOODLES_PREP = prepared("peanut_noodles_prep", IngredientState.MIXED, PantrySortTab.OTHER, DAY * 3L, 60, 7, 4, true, false);
    public static final DeferredItem<KitchenIngredientItem> BROWNIE_BATTER_BASE = prepared("brownie_batter_base", IngredientState.MIXED, PantrySortTab.BAKING_ADD_INS, DAY * 1L, 0, 3, 3, true, false);
    public static final DeferredItem<KitchenIngredientItem> BROWNIE_BATTER = prepared("brownie_batter", IngredientState.BATTER, PantrySortTab.OTHER, DAY * 1L, 0, 4, 4, true, false);
    public static final DeferredItem<KitchenIngredientItem> BAKED_BROWNIES = prepared("baked_brownies", IngredientState.BAKED, PantrySortTab.SNACKS, DAY * 4L, 120, 5, 5, true, false);
    public static final DeferredItem<KitchenIngredientItem> CAKE_DRY_MIX = prepared("cake_dry_mix", IngredientState.COARSE_POWDER, PantrySortTab.BAKING_ADD_INS, DAY * 30L, 0, 2, 2, false, false);
    public static final DeferredItem<KitchenIngredientItem> CAKE_BATTER = prepared("cake_batter", IngredientState.BATTER, PantrySortTab.OTHER, DAY * 1L, 0, 4, 4, true, false);
    public static final DeferredItem<KitchenIngredientItem> BAKED_CAKE = prepared("baked_cake", IngredientState.BAKED, PantrySortTab.SNACKS, DAY * 4L, 120, 5, 5, true, false);
    public static final DeferredItem<KitchenIngredientItem> SANDWICH_FILLING = prepared("sandwich_filling", IngredientState.CHOPPED, PantrySortTab.OTHER, DAY * 2L, 0, 5, 3, true, false);
    public static final DeferredItem<KitchenIngredientItem> ASSEMBLED_SANDWICH = prepared("assembled_sandwich", IngredientState.ASSEMBLED_SANDWICH, PantrySortTab.OTHER, DAY * 1L, 0, 6, 4, true, false);
    public static final DeferredItem<KitchenIngredientItem> CURRY_BASE = prepared("curry_base", IngredientState.SMOOTH_MIXTURE, PantrySortTab.SAUCES_AND_CONDIMENTS, DAY * 3L, 30, 3, 3, true, false);
    public static final DeferredItem<KitchenIngredientItem> CHICKEN_CURRY_PREP = prepared("chicken_curry_prep", IngredientState.SIMMERED, PantrySortTab.OTHER, DAY * 3L, 90, 7, 4, true, true);
    public static final DeferredItem<KitchenIngredientItem> CHEESE_SAUCE = prepared("cheese_sauce", IngredientState.CREAMY, PantrySortTab.SAUCES_AND_CONDIMENTS, DAY * 2L, 0, 4, 4, true, false);
    public static final DeferredItem<KitchenIngredientItem> MAC_AND_CHEESE_PREP = prepared("mac_and_cheese_prep", IngredientState.MIXED, PantrySortTab.OTHER, DAY * 2L, 60, 7, 5, true, false);
    public static final DeferredItem<KitchenIngredientItem> GARLIC_BUTTER = prepared("garlic_butter", IngredientState.SMOOTH_PASTE, PantrySortTab.SAUCES_AND_CONDIMENTS, DAY * 6L, 0, 3, 3, true, true);
    public static final DeferredItem<KitchenIngredientItem> GARLIC_BREAD_PREP = prepared("garlic_bread_prep", IngredientState.BAKED_BREAD, PantrySortTab.OTHER, DAY * 3L, 40, 4, 4, true, false);
    public static final DeferredItem<KitchenIngredientItem> GLAZED_CHICKEN_PREP = prepared("glazed_chicken_prep", IngredientState.GLAZED, PantrySortTab.OTHER, DAY * 3L, 60, 7, 4, true, true);
    public static final DeferredItem<KitchenIngredientItem> PANCAKE_DRY_MIX = prepared("pancake_dry_mix", IngredientState.COARSE_POWDER, PantrySortTab.BAKING_ADD_INS, DAY * 30L, 0, 2, 2, false, false);
    public static final DeferredItem<KitchenIngredientItem> PANCAKE_BATTER = prepared("pancake_batter", IngredientState.BATTER, PantrySortTab.OTHER, DAY * 1L, 0, 4, 4, true, false);
    public static final DeferredItem<KitchenIngredientItem> PANCAKES_PREP = prepared("pancakes_prep", IngredientState.PAN_FRIED, PantrySortTab.OTHER, DAY * 2L, 40, 5, 5, true, false);
    public static final DeferredItem<KitchenIngredientItem> STUFFED_JALAPENOS = prepared("stuffed_jalapenos", IngredientState.STUFFED, PantrySortTab.OTHER, DAY * 1L, 20, 3, 4, true, true);
    public static final DeferredItem<KitchenIngredientItem> FRIED_JALAPENO_BITES_PREP = prepared("fried_jalapeno_bites_prep", IngredientState.DEEP_FRIED, PantrySortTab.OTHER, DAY * 2L, 50, 5, 6, true, false);
    public static final DeferredItem<KitchenIngredientItem> GOLDEN_RICE_BASE = prepared("golden_rice_base", IngredientState.MIXED, PantrySortTab.OTHER, DAY * 2L, 0, 2, 3, true, false);
    public static final DeferredItem<KitchenIngredientItem> GOLDEN_RICE_PREP = prepared("golden_rice_prep", IngredientState.SIMMERED, PantrySortTab.OTHER, DAY * 3L, 90, 6, 4, true, false);
    public static final DeferredItem<KitchenIngredientItem> BREADED_FISH_FILLET = prepared("breaded_fish_fillet", IngredientState.BATTERED_PROTEIN, PantrySortTab.OTHER, DAY * 1L, 60, 5, 4, true, true);
    public static final DeferredItem<KitchenIngredientItem> CONFETTI_RICE_BASE = prepared("confetti_rice_base", IngredientState.DICED, PantrySortTab.OTHER, DAY * 2L, 0, 2, 3, true, false);
    public static final DeferredItem<KitchenIngredientItem> CONFETTI_RICE_PREP = prepared("confetti_rice_prep", IngredientState.SIMMERED, PantrySortTab.OTHER, DAY * 3L, 95, 6, 5, true, false);
    public static final DeferredItem<KitchenIngredientItem> FOCACCIA_DOUGH = prepared("focaccia_dough", IngredientState.BREAD_DOUGH, PantrySortTab.OTHER, DAY * 2L, 0, 3, 3, true, false);
    public static final DeferredItem<KitchenIngredientItem> SHAPED_FOCACCIA_BASE = prepared("shaped_focaccia_base", IngredientState.SHAPED_BASE, PantrySortTab.OTHER, DAY * 1L, 60, 3, 3, true, false);
    public static final DeferredItem<KitchenIngredientItem> ASSEMBLED_FOCACCIA_PIZZA = prepared("assembled_focaccia_pizza", IngredientState.RAW_ASSEMBLED_PIZZA, PantrySortTab.OTHER, DAY * 1L, 90, 5, 5, true, false);
    public static final DeferredItem<KitchenIngredientItem> STICKED_PRODUCT = prepared("sticked_product", IngredientState.STICKED_PRODUCT, PantrySortTab.OTHER, DAY * 2L, 0, 1, 0, true, false);

    private static final List<DeferredItem<KitchenIngredientItem>> PREPARED_ITEM_LIST = List.of(
            CHOPPED_PRODUCE_BLEND,
            LEMON_JUICE,
            FRUIT_PULP,
            MIXED_JUICE,
            FRUIT_JUICE_BLEND,
            JARRED_LEMON_JUICE,
            PACKED_FREEZE_DRY_APPLES,
            TOMATO_SOUP_BASE,
            SEASONING_BLEND,
            PASTA_BAKE_ASSEMBLY,
            PIE_DOUGH,
            ASSEMBLED_TRAY_PIE,
            PAN_SEARED_CHICKEN_PREP,
            HEARTY_STEW_BASE,
            SMOKED_MEAT,
            DUMPLING_FILLING,
            DUMPLING_DOUGH,
            RAW_DUMPLINGS,
            SMOOTHIE_BLEND,
            NUT_BUTTER,
            BATTER_MIX,
            FRIED_CHICKEN_PREP,
            JARRED_NUT_BUTTER,
            PACKED_BREADCRUMBS,
            PORTIONED_MEAL,
            FREEZE_DRIED_MEAL,
            JAM_TOAST_PREP,
            SYRUP_MIXTURE,
            HOT_SYRUP_PRESERVE,
            SAVORY_PIE_FILLING,
            ASSEMBLED_SAVORY_PIE,
            FISH_COATING,
            FRIED_FISH_PREP,
            BRAISED_BEEF_BASE,
            BRAISED_BEEF_PREP,
            PEANUT_SAUCE,
            PEANUT_NOODLES_PREP,
            BROWNIE_BATTER_BASE,
            BROWNIE_BATTER,
            BAKED_BROWNIES,
            CAKE_DRY_MIX,
            CAKE_BATTER,
            BAKED_CAKE,
            SANDWICH_FILLING,
            ASSEMBLED_SANDWICH,
            CURRY_BASE,
            CHICKEN_CURRY_PREP,
            CHEESE_SAUCE,
            MAC_AND_CHEESE_PREP,
            GARLIC_BUTTER,
            GARLIC_BREAD_PREP,
            GLAZED_CHICKEN_PREP,
            PANCAKE_DRY_MIX,
            PANCAKE_BATTER,
            PANCAKES_PREP,
            STUFFED_JALAPENOS,
            FRIED_JALAPENO_BITES_PREP,
            GOLDEN_RICE_BASE,
            GOLDEN_RICE_PREP,
            BREADED_FISH_FILLET,
            CONFETTI_RICE_BASE,
            CONFETTI_RICE_PREP,
            FOCACCIA_DOUGH,
            SHAPED_FOCACCIA_BASE,
            ASSEMBLED_FOCACCIA_PIZZA,
            STICKED_PRODUCT
    );

    public static final DeferredItem<KitchenToolItem> PARING_KNIFE = tool("paring_knife", ToolProfile.PARING_KNIFE, 0.06F, 1.10F, 128);
    public static final DeferredItem<KitchenToolItem> CHEF_KNIFE = tool("chef_knife", ToolProfile.CHEF_KNIFE, 0.09F, 1.20F, 192);
    public static final DeferredItem<KitchenToolItem> CLEAVER = tool("cleaver", ToolProfile.CLEAVER, 0.07F, 1.05F, 224);
    public static final DeferredItem<KitchenToolItem> WHISK = tool("whisk", ToolProfile.WHISK, 0.05F, 1.08F, 160);
    public static final DeferredItem<KitchenToolItem> ROLLING_PIN = tool("rolling_pin", ToolProfile.ROLLING_PIN, 0.07F, 1.12F, 192);
    public static final DeferredItem<KitchenToolItem> MORTAR_PESTLE = tool("mortar_pestle", ToolProfile.MORTAR_PESTLE, 0.08F, 1.06F, 192);
    public static final DeferredItem<KitchenToolItem> STOCK_POT = tool("stock_pot", ToolProfile.STOCK_POT, 0.05F, 1.00F, 256);
    public static final DeferredItem<KitchenToolItem> FRYING_SKILLET = tool("frying_skillet", ToolProfile.FRYING_SKILLET, 0.08F, 1.10F, 256);
    public static final DeferredItem<KitchenToolItem> FRYING_PAN = tool("frying_pan", ToolProfile.PAN, 0.08F, 1.08F, 256);
    public static final DeferredItem<KitchenToolItem> POT = tool("pot", ToolProfile.POT, 0.05F, 1.00F, 256);
    public static final DeferredItem<KitchenToolItem> SAUCEPAN = tool("saucepan", ToolProfile.SAUCEPAN, 0.06F, 1.04F, 224);
    public static final DeferredItem<KitchenToolItem> FINE_STRAINER = tool("fine_strainer", ToolProfile.FINE_STRAINER, 0.08F, 1.06F, 192);
    public static final DeferredItem<KitchenToolItem> COARSE_STRAINER = tool("coarse_strainer", ToolProfile.COARSE_STRAINER, 0.03F, 0.98F, 176);
    public static final DeferredItem<KitchenToolItem> STEAMER_BASKET = tool("steamer_basket", ToolProfile.STEAMER_BASKET, 0.05F, 1.04F, 192);
    public static final DeferredItem<KitchenToolItem> BAKING_TRAY = tool("baking_tray", ToolProfile.BAKING_TRAY, 0.05F, 1.00F, 256);
    public static final DeferredItem<KitchenToolItem> CANNING_JAR = tool("canning_jar", ToolProfile.JAR, 0.04F, 0.98F, 96);
    public static final DeferredItem<KitchenToolItem> GLASS_JAR = tool("glass_jar", ToolProfile.GLASS_JAR, 0.04F, 0.98F, 96);
    public static final DeferredItem<KitchenToolItem> PIE_TIN = tool("pie_tin", ToolProfile.PIE_TIN, 0.05F, 1.00F, 256);
    public static final DeferredItem<KitchenToolItem> FORK = tool("fork", ToolProfile.FORK, 0.01F, 1.00F, 96);
    public static final DeferredItem<KitchenToolItem> SPOON = tool("spoon", ToolProfile.SPOON, 0.01F, 1.00F, 96);
    public static final DeferredItem<KitchenToolItem> TABLE_KNIFE = tool("table_knife", ToolProfile.TABLE_KNIFE, 0.02F, 1.02F, 96);

    public static final DeferredItem<KitchenMealItem> FRUIT_JUICE = meal("fruit_juice", IngredientState.PLATED, DAY * 2L, 4, 4, GLASS_CUP);
    public static final DeferredItem<KitchenMealItem> CREAMY_TOMATO_SOUP = meal("creamy_tomato_soup", IngredientState.PLATED, DAY * 3L, 7, 4, CERAMIC_BOWL, SPOON);
    public static final DeferredItem<KitchenMealItem> PASTA_TRAY_BAKE = meal("pasta_tray_bake", IngredientState.PLATED, DAY * 4L, 9, 5, SERVING_TRAY, SERVING_SPOON);
    public static final DeferredItem<KitchenMealItem> PAN_SEARED_CHICKEN = meal("pan_seared_chicken", IngredientState.PLATED, DAY * 3L, 8, 5, CERAMIC_PLATE, TABLE_KNIFE, FORK);
    public static final DeferredItem<KitchenMealItem> HEARTY_STEW = meal("hearty_stew", IngredientState.PLATED, DAY * 3L, 8, 5, CERAMIC_BOWL, SPOON);
    public static final DeferredItem<KitchenMealItem> SLICEABLE_PIE = meal("sliceable_pie", IngredientState.PLATED, DAY * 6L, 7, 6, SERVING_TRAY, TABLE_KNIFE);
    public static final DeferredItem<KitchenMealItem> MEAT_PLATTER = meal("meat_platter", IngredientState.PLATED, DAY * 4L, 9, 5, WOODEN_BOARD, TABLE_KNIFE);
    public static final DeferredItem<KitchenMealItem> DUMPLING_BASKET = meal("dumpling_basket", IngredientState.PLATED, DAY * 2L, 8, 5, BAMBOO_TRAY, CHOPSTICKS);
    public static final DeferredItem<KitchenMealItem> SMOOTHIE = meal("smoothie", IngredientState.PLATED, DAY * 1L, 5, 5, GLASS_CUP, STRAW);
    public static final DeferredItem<KitchenMealItem> FRIED_CHICKEN_BASKET = meal("fried_chicken_basket", IngredientState.PLATED, DAY * 2L, 9, 6, BASKET);
    public static final DeferredItem<KitchenMealItem> FREEZE_DRIED_MEAL_PACK = meal("freeze_dried_meal_pack", IngredientState.FREEZE_DRIED, DAY * 240L, 7, 3, TUPPERWARE);
    public static final DeferredItem<KitchenMealItem> JAM_TOAST = meal("jam_toast", IngredientState.PLATED, DAY * 1L, 4, 5, CERAMIC_PLATE, BUTTER_KNIFE);
    public static final DeferredItem<KitchenMealItem> SAVORY_PIE = meal("savory_pie", IngredientState.PLATED, DAY * 4L, 8, 5, CERAMIC_PLATE);
    public static final DeferredItem<KitchenMealItem> FRIED_FISH = meal("fried_fish", IngredientState.PLATED, DAY * 2L, 7, 5, CERAMIC_PLATE);
    public static final DeferredItem<KitchenMealItem> BRAISED_BEEF = meal("braised_beef", IngredientState.PLATED, DAY * 3L, 9, 5, CERAMIC_PLATE);
    public static final DeferredItem<KitchenMealItem> PEANUT_NOODLES = meal("peanut_noodles", IngredientState.PLATED, DAY * 2L, 8, 5, CERAMIC_BOWL);
    public static final DeferredItem<KitchenMealItem> BROWNIES = meal("brownies", IngredientState.PLATED, DAY * 4L, 5, 6, CERAMIC_PLATE);
    public static final DeferredItem<KitchenMealItem> CAKE = meal("cake", IngredientState.PLATED, DAY * 4L, 5, 6, CERAMIC_PLATE);
    public static final DeferredItem<KitchenMealItem> SANDWICH_PLATE = meal("sandwich_plate", IngredientState.PLATED, DAY * 2L, 7, 5, CERAMIC_PLATE);
    public static final DeferredItem<KitchenMealItem> CHICKEN_CURRY = meal("chicken_curry", IngredientState.PLATED, DAY * 3L, 8, 5, CERAMIC_BOWL);
    public static final DeferredItem<KitchenMealItem> MAC_AND_CHEESE = meal("mac_and_cheese", IngredientState.PLATED, DAY * 2L, 8, 5, CERAMIC_PLATE);
    public static final DeferredItem<KitchenMealItem> GARLIC_BREAD = meal("garlic_bread", IngredientState.PLATED, DAY * 2L, 5, 5, CERAMIC_PLATE);
    public static final DeferredItem<KitchenMealItem> GLAZED_CHICKEN = meal("glazed_chicken", IngredientState.PLATED, DAY * 3L, 8, 5, CERAMIC_PLATE);
    public static final DeferredItem<KitchenMealItem> PANCAKES = meal("pancakes", IngredientState.PLATED, DAY * 2L, 6, 6, CERAMIC_PLATE);
    public static final DeferredItem<KitchenMealItem> FRIED_JALAPENO_BITES = meal("fried_jalapeno_bites", IngredientState.PLATED, DAY * 2L, 6, 6, CERAMIC_PLATE);
    public static final DeferredItem<KitchenMealItem> GOLDEN_RICE = meal("golden_rice", IngredientState.PLATED, DAY * 2L, 6, 4, CERAMIC_PLATE);
    public static final DeferredItem<KitchenMealItem> CONFETTI_RICE_WITH_FISH = meal("confetti_rice_with_fish", IngredientState.PLATED, DAY * 2L, 9, 6, CERAMIC_PLATE);
    public static final DeferredItem<KitchenMealItem> FOCACCIA_PIZZA = meal("focaccia_pizza", IngredientState.PLATED, DAY * 3L, 8, 6, CERAMIC_PLATE);

    private static final List<DeferredItem<KitchenMealItem>> MEAL_ITEM_LIST = List.of(
            FRUIT_JUICE,
            CREAMY_TOMATO_SOUP,
            PASTA_TRAY_BAKE,
            PAN_SEARED_CHICKEN,
            HEARTY_STEW,
            SLICEABLE_PIE,
            MEAT_PLATTER,
            DUMPLING_BASKET,
            SMOOTHIE,
            FRIED_CHICKEN_BASKET,
            FREEZE_DRIED_MEAL_PACK,
            JAM_TOAST,
            SAVORY_PIE,
            FRIED_FISH,
            BRAISED_BEEF,
            PEANUT_NOODLES,
            BROWNIES,
            CAKE,
            SANDWICH_PLATE,
            CHICKEN_CURRY,
            MAC_AND_CHEESE,
            GARLIC_BREAD,
            GLAZED_CHICKEN,
            PANCAKES,
            FRIED_JALAPENO_BITES,
            GOLDEN_RICE,
            CONFETTI_RICE_WITH_FISH,
            FOCACCIA_PIZZA
    );

    private JazzyItems() {
    }

    public static DeferredItem<KitchenIngredientItem> ingredient(IngredientId ingredientId) {
        return INGREDIENTS.get(ingredientId);
    }

    public static List<DeferredItem<KitchenIngredientItem>> ingredientItems() {
        return INGREDIENT_LIST;
    }

    public static List<DeferredItem<KitchenIngredientItem>> preparedItems() {
        return PREPARED_ITEM_LIST;
    }

    public static List<DeferredItem<KitchenMealItem>> mealItems() {
        return MEAL_ITEM_LIST;
    }

    public static ItemStack creativeIngredientStack(IngredientId ingredientId) {
        return ingredient(ingredientId).get().createCreativeStack(1);
    }

    private static Map<IngredientId, DeferredItem<KitchenIngredientItem>> registerIngredients() {
        Map<IngredientId, DeferredItem<KitchenIngredientItem>> ingredients = new EnumMap<>(IngredientId.class);
        for (IngredientId ingredientId : IngredientId.values()) {
            IngredientSpec spec = ingredientId.spec;
            DeferredItem<KitchenIngredientItem> registered = ITEMS.register(ingredientId.id(), () -> new KitchenIngredientItem(
                    new Item.Properties(),
                    ingredientId.defaultState(),
                    spec.pantryTab(),
                    spec.baseQuality(),
                    spec.baseFlavor(),
                    spec.baseTexture(),
                    spec.baseStructure(),
                    spec.baseMoisture(),
                    spec.basePurity(),
                    spec.baseAeration(),
                    spec.decayTicks(),
                    spec.cookTimeTicks(),
                    spec.nourishment(),
                    spec.enjoyment(),
                    spec.fridgeSafe(),
                    spec.freezerSafe()
            ));
            ingredients.put(ingredientId, registered);
        }
        return ingredients;
    }

    private record IngredientSpec(
            PantrySortTab pantryTab,
            float baseQuality,
            float baseFlavor,
            float baseTexture,
            float baseStructure,
            float baseMoisture,
            float basePurity,
            float baseAeration,
            long decayTicks,
            int cookTimeTicks,
            int nourishment,
            int enjoyment,
            boolean fridgeSafe,
            boolean freezerSafe
    ) {
    }

    private static IngredientSpec spec(
            PantrySortTab pantryTab,
            float baseQuality,
            float baseFlavor,
            float baseTexture,
            float baseStructure,
            float baseMoisture,
            float basePurity,
            float baseAeration,
            long decayTicks,
            int cookTimeTicks,
            int nourishment,
            int enjoyment,
            boolean fridgeSafe,
            boolean freezerSafe
    ) {
        return new IngredientSpec(
                pantryTab,
                baseQuality,
                baseFlavor,
                baseTexture,
                baseStructure,
                baseMoisture,
                basePurity,
                baseAeration,
                decayTicks,
                cookTimeTicks,
                nourishment,
                enjoyment,
                fridgeSafe,
                freezerSafe
        );
    }

    private static IngredientSpec sweetener(int nourishment, int enjoyment) {
        return spec(PantrySortTab.SWEETENERS, 0.80F, 0.56F, 0.16F, 0.00F, 0.06F, 0.98F, 0.00F, DAY * 180L, 0, nourishment, enjoyment, false, false);
    }

    private static IngredientSpec syrupSweetener(long decayTicks, int cookTimeTicks, int nourishment, int enjoyment) {
        return spec(PantrySortTab.SWEETENERS, 0.82F, 0.60F, 0.18F, 0.00F, 0.54F, 0.96F, 0.00F, decayTicks, cookTimeTicks, nourishment, enjoyment, false, false);
    }

    private static IngredientSpec flour(long decayTicks, int cookTimeTicks, int nourishment, int enjoyment) {
        return spec(PantrySortTab.GRAINS_AND_FLOURS, 0.76F, 0.28F, 0.34F, 0.42F, 0.18F, 0.90F, 0.02F, decayTicks, cookTimeTicks, nourishment, enjoyment, false, false);
    }

    private static IngredientSpec grain(PantrySortTab pantryTab, long decayTicks, int cookTimeTicks, int nourishment, int enjoyment) {
        return spec(pantryTab, 0.76F, 0.34F, 0.30F, 0.44F, 0.18F, 0.90F, 0.02F, decayTicks, cookTimeTicks, nourishment, enjoyment, false, false);
    }

    private static IngredientSpec leavener(long decayTicks) {
        return spec(PantrySortTab.LEAVENING_AGENTS, 0.74F, 0.08F, 0.12F, 0.24F, 0.06F, 0.94F, 0.30F, decayTicks, 0, 0, 0, false, false);
    }

    private static IngredientSpec starter(long decayTicks, int nourishment, int enjoyment) {
        return spec(PantrySortTab.LEAVENING_AGENTS, 0.74F, 0.12F, 0.18F, 0.22F, 0.34F, 0.88F, 0.24F, decayTicks, 0, nourishment, enjoyment, true, false);
    }

    private static IngredientSpec seasoning(long decayTicks, int cookTimeTicks, int enjoyment) {
        return spec(PantrySortTab.SEASONINGS, 0.80F, 0.72F, 0.20F, 0.04F, 0.08F, 0.88F, 0.00F, decayTicks, cookTimeTicks, 0, enjoyment, false, false);
    }

    private static IngredientSpec herb(long decayTicks, int cookTimeTicks, int nourishment, int enjoyment) {
        return spec(PantrySortTab.SEASONINGS, 0.82F, 0.74F, 0.28F, 0.08F, 0.30F, 0.84F, 0.02F, decayTicks, cookTimeTicks, nourishment, enjoyment, true, false);
    }

    private static IngredientSpec oil(long decayTicks, int enjoyment) {
        return spec(PantrySortTab.OILS_AND_FATS, 0.80F, 0.34F, 0.22F, 0.00F, 0.28F, 0.92F, 0.00F, decayTicks, 0, 0, enjoyment, false, false);
    }

    private static IngredientSpec pantryFat(long decayTicks, int nourishment, int enjoyment) {
        return spec(PantrySortTab.OILS_AND_FATS, 0.78F, 0.30F, 0.26F, 0.04F, 0.22F, 0.90F, 0.00F, decayTicks, 0, nourishment, enjoyment, false, false);
    }

    private static IngredientSpec chilledFat(long decayTicks, int cookTimeTicks, int nourishment, int enjoyment) {
        return spec(PantrySortTab.OILS_AND_FATS, 0.80F, 0.34F, 0.30F, 0.04F, 0.28F, 0.88F, 0.00F, decayTicks, cookTimeTicks, nourishment, enjoyment, true, true);
    }

    private static IngredientSpec refrigeratedFat(long decayTicks, int cookTimeTicks, int nourishment, int enjoyment) {
        return spec(PantrySortTab.OILS_AND_FATS, 0.82F, 0.42F, 0.36F, 0.08F, 0.46F, 0.88F, 0.00F, decayTicks, cookTimeTicks, nourishment, enjoyment, true, true);
    }

    private static IngredientSpec stableDairy(long decayTicks, int cookTimeTicks, int nourishment, int enjoyment, boolean fridgeSafe, boolean freezerSafe) {
        return spec(PantrySortTab.DAIRY_AND_ALTERNATIVES, 0.80F, 0.42F, 0.40F, 0.20F, 0.68F, 0.78F, 0.08F, decayTicks, cookTimeTicks, nourishment, enjoyment, fridgeSafe, freezerSafe);
    }

    private static IngredientSpec protein(long decayTicks, int cookTimeTicks, int nourishment, int enjoyment, boolean freezerSafe) {
        return spec(PantrySortTab.OTHER, 0.78F, 0.48F, 0.58F, 0.34F, 0.74F, 0.58F, 0.04F, decayTicks, cookTimeTicks, nourishment, enjoyment, true, freezerSafe);
    }

    private static IngredientSpec produce(long decayTicks, int cookTimeTicks, int nourishment, int enjoyment, boolean freezerSafe) {
        return spec(PantrySortTab.OTHER, 0.80F, 0.58F, 0.52F, 0.18F, 0.76F, 0.54F, 0.04F, decayTicks, cookTimeTicks, nourishment, enjoyment, true, freezerSafe);
    }

    private static IngredientSpec canned(long decayTicks, int cookTimeTicks, int nourishment, int enjoyment) {
        return spec(PantrySortTab.CANNED_GOODS, 0.78F, 0.50F, 0.38F, 0.18F, 0.54F, 0.88F, 0.00F, decayTicks, cookTimeTicks, nourishment, enjoyment, true, false);
    }

    private static IngredientSpec addIn(long decayTicks, int cookTimeTicks, int nourishment, int enjoyment) {
        return spec(PantrySortTab.BAKING_ADD_INS, 0.80F, 0.60F, 0.32F, 0.14F, 0.30F, 0.86F, 0.06F, decayTicks, cookTimeTicks, nourishment, enjoyment, false, false);
    }

    private static IngredientSpec bakingExtract(long decayTicks, int enjoyment) {
        return spec(PantrySortTab.BAKING_ADD_INS, 0.80F, 0.64F, 0.12F, 0.00F, 0.40F, 0.92F, 0.00F, decayTicks, 0, 0, enjoyment, false, false);
    }

    private static IngredientSpec dryGood(long decayTicks, int cookTimeTicks, int nourishment, int enjoyment) {
        return spec(PantrySortTab.DRY_GOODS, 0.76F, 0.34F, 0.30F, 0.42F, 0.12F, 0.92F, 0.02F, decayTicks, cookTimeTicks, nourishment, enjoyment, false, false);
    }

    private static IngredientSpec condiment(long decayTicks, int cookTimeTicks, int nourishment, int enjoyment, boolean fridgeSafe) {
        return spec(PantrySortTab.SAUCES_AND_CONDIMENTS, 0.78F, 0.68F, 0.12F, 0.04F, 0.52F, 0.90F, 0.00F, decayTicks, cookTimeTicks, nourishment, enjoyment, fridgeSafe, false);
    }

    private static IngredientSpec snack(long decayTicks, int cookTimeTicks, int nourishment, int enjoyment) {
        return spec(PantrySortTab.SNACKS, 0.74F, 0.62F, 0.46F, 0.36F, 0.18F, 0.78F, 0.06F, decayTicks, cookTimeTicks, nourishment, enjoyment, false, false);
    }

    private static IngredientSpec bakingMix(long decayTicks, int cookTimeTicks, int nourishment, int enjoyment) {
        return spec(PantrySortTab.BAKING_ADD_INS, 0.76F, 0.32F, 0.34F, 0.42F, 0.20F, 0.90F, 0.04F, decayTicks, cookTimeTicks, nourishment, enjoyment, false, false);
    }

    private static DeferredItem<KitchenToolItem> tool(String name, ToolProfile profile, float qualityBonus, float speedMultiplier, int durability) {
        return ITEMS.register(name, () -> new KitchenToolItem(
                new Item.Properties().stacksTo(1).durability(durability),
                profile,
                qualityBonus,
                speedMultiplier
        ));
    }

    private static DeferredItem<KitchenIngredientItem> prepared(
            String name,
            IngredientState defaultState,
            PantrySortTab pantryTab,
            long decayTicks,
            int cookTimeTicks,
            int nourishment,
            int enjoyment,
            boolean fridgeSafe,
            boolean freezerSafe
    ) {
        return ITEMS.register(name, () -> new KitchenIngredientItem(
                new Item.Properties(),
                defaultState,
                pantryTab,
                0.84F,
                0.62F,
                0.44F,
                0.24F,
                0.46F,
                0.84F,
                0.04F,
                decayTicks,
                cookTimeTicks,
                nourishment,
                enjoyment,
                fridgeSafe,
                freezerSafe
        ));
    }

    @SafeVarargs
    private static DeferredItem<KitchenMealItem> meal(
            String name,
            IngredientState defaultState,
            long decayTicks,
            int nourishment,
            int enjoyment,
            Supplier<? extends Item>... returnedItems
    ) {
        return ITEMS.register(name, () -> new KitchenMealItem(
                new Item.Properties().stacksTo(16),
                defaultState,
                0.90F,
                0.72F,
                0.64F,
                0.36F,
                0.58F,
                0.88F,
                0.06F,
                decayTicks,
                nourishment,
                enjoyment,
                List.of(returnedItems)
        ));
    }

    private static DeferredItem<Item> utilityItem(String name, int stacksTo) {
        return ITEMS.register(name, () -> new Item(new Item.Properties().stacksTo(stacksTo)));
    }
}
