package com.boaat.jazzy_cookin.kitchen.sim;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

import com.boaat.jazzy_cookin.kitchen.IngredientStateData;
import com.boaat.jazzy_cookin.registry.JazzyItems;
import com.boaat.jazzy_cookin.registry.JazzyItems.IngredientId;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class FoodMaterialProfiles {
    public static final FoodMaterialProfile EGGS = profile(0.76F, 0.22F, 0.82F, 0.02F, 0.02F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.10F,
            FoodTrait.PROTEIN, FoodTrait.ANIMAL_PROTEIN, FoodTrait.EGG);
    public static final FoodMaterialProfile BUTTER = profile(0.18F, 0.90F, 0.06F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
            FoodTrait.FAT, FoodTrait.DAIRY);
    public static final FoodMaterialProfile VEGETABLE_OIL = profile(0.02F, 0.98F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
            FoodTrait.FAT, FoodTrait.OIL);
    public static final FoodMaterialProfile CHEESE = profile(0.44F, 0.58F, 0.40F, 0.02F, 0.02F, 0.0F, 0.02F, 0.06F, 0.28F, 0.0F, 0.0F, 0.0F, 0.0F,
            FoodTrait.DAIRY, FoodTrait.PROTEIN, FoodTrait.ANIMAL_PROTEIN, FoodTrait.FAT);
    public static final FoodMaterialProfile ONIONS = profile(0.84F, 0.02F, 0.04F, 0.06F, 0.06F, 0.04F, 0.03F, 0.0F, 0.0F, 0.24F, 0.0F, 0.0F, 0.0F,
            FoodTrait.VEGETABLE, FoodTrait.ALLIUM, FoodTrait.AROMATIC);
    public static final FoodMaterialProfile PARSLEY = profile(0.64F, 0.02F, 0.04F, 0.02F, 0.02F, 0.12F, 0.03F, 0.0F, 0.0F, 0.0F, 0.20F, 0.0F, 0.0F,
            FoodTrait.VEGETABLE, FoodTrait.HERB, FoodTrait.LEAFY_GREEN);
    public static final FoodMaterialProfile SALT = profile(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.28F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
            FoodTrait.SALT, FoodTrait.CONDIMENT);
    public static final FoodMaterialProfile BLACK_PEPPER = profile(0.0F, 0.02F, 0.02F, 0.10F, 0.02F, 0.18F, 0.02F, 0.08F, 0.0F, 0.0F, 0.0F, 0.18F, 0.0F,
            FoodTrait.SPICE, FoodTrait.PEPPER, FoodTrait.CONDIMENT);

    private static final Map<Item, FoodMaterialProfile> ITEM_PROFILES = new IdentityHashMap<>();
    private static boolean initialized;

    private FoodMaterialProfiles() {
    }

    public static Optional<FoodMaterialProfile> profileFor(ItemStack stack) {
        ensureInitialized();
        return Optional.ofNullable(ITEM_PROFILES.get(stack.getItem()));
    }

    public static FoodMatterData createMatter(ItemStack stack, IngredientStateData summaryHint, boolean finalizedServing) {
        FoodMaterialProfile profile = profileFor(stack).orElse(null);
        if (profile != null) {
            return profile.create(summaryHint, finalizedServing);
        }
        return summaryHint != null ? FoodMatterData.fromLegacy(summaryHint, finalizedServing) : null;
    }

    public static long traitMaskFor(ItemStack stack) {
        return profileFor(stack).map(FoodMaterialProfile::traitMask).orElse(0L);
    }

    public static boolean hasTrait(ItemStack stack, FoodTrait trait) {
        return profileFor(stack).map(profile -> profile.hasTrait(trait)).orElse(false);
    }

    public static boolean isMixingBowlAddIn(ItemStack stack) {
        FoodMaterialProfile profile = profileFor(stack).orElse(null);
        if (profile == null) {
            return false;
        }
        return !profile.hasTrait(FoodTrait.EGG)
                && !profile.hasTrait(FoodTrait.OIL)
                && (profile.cheeseLoad() > 0.0F
                || profile.hasTrait(FoodTrait.HERB)
                || profile.hasTrait(FoodTrait.ALLIUM)
                || profile.hasTrait(FoodTrait.SALT)
                || profile.hasTrait(FoodTrait.SPICE)
                || profile.hasTrait(FoodTrait.PEPPER));
    }

    public static boolean isStoveFat(ItemStack stack) {
        FoodMaterialProfile profile = profileFor(stack).orElse(null);
        return profile != null && (profile.hasTrait(FoodTrait.FAT) || profile.hasTrait(FoodTrait.OIL));
    }

    private static synchronized void ensureInitialized() {
        if (initialized) {
            return;
        }

        register(profile(0.02F, 0.0F, 0.0F, 0.0F, 0.98F, 0.0F, 0.01F, 0.02F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.SWEETENER),
                IngredientId.WHITE_SUGAR,
                IngredientId.POWDERED_SUGAR);
        register(profile(0.05F, 0.0F, 0.0F, 0.0F, 0.90F, 0.0F, 0.03F, 0.03F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.SWEETENER),
                IngredientId.LIGHT_BROWN_SUGAR,
                IngredientId.DARK_BROWN_SUGAR);
        register(profile(0.18F, 0.0F, 0.0F, 0.0F, 0.78F, 0.0F, 0.06F, 0.04F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.SWEETENER, FoodTrait.SYRUP, FoodTrait.PRESERVE, FoodTrait.CONDIMENT),
                IngredientId.HONEY,
                IngredientId.MAPLE_SYRUP,
                IngredientId.AGAVE_NECTAR);
        register(profile(0.24F, 0.0F, 0.0F, 0.0F, 0.70F, 0.0F, 0.08F, 0.05F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.SWEETENER, FoodTrait.SYRUP, FoodTrait.PRESERVE, FoodTrait.CONDIMENT),
                IngredientId.MOLASSES,
                IngredientId.CORN_SYRUP);

        register(profile(0.10F, 0.02F, 0.10F, 0.72F, 0.02F, 0.04F, 0.01F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.FLOUR, FoodTrait.STARCH, FoodTrait.WHEAT),
                IngredientId.ALL_PURPOSE_FLOUR,
                IngredientId.BREAD_FLOUR,
                IngredientId.CAKE_FLOUR,
                IngredientId.SEMOLINA);
        register(profile(0.10F, 0.03F, 0.12F, 0.66F, 0.02F, 0.12F, 0.01F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.FLOUR, FoodTrait.STARCH, FoodTrait.WHEAT),
                IngredientId.WHOLE_WHEAT_FLOUR);
        register(profile(0.10F, 0.02F, 0.06F, 0.74F, 0.02F, 0.04F, 0.01F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.FLOUR, FoodTrait.STARCH, FoodTrait.CORN),
                IngredientId.CORN_FLOUR);
        register(profile(0.10F, 0.01F, 0.07F, 0.76F, 0.01F, 0.03F, 0.01F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.FLOUR, FoodTrait.STARCH, FoodTrait.RICE),
                IngredientId.WHITE_RICE_FLOUR,
                IngredientId.BROWN_RICE_FLOUR);
        register(profile(0.10F, 0.04F, 0.08F, 0.70F, 0.02F, 0.08F, 0.01F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.GRAIN, FoodTrait.STARCH, FoodTrait.CORN),
                IngredientId.CORNMEAL);
        register(profile(0.11F, 0.01F, 0.08F, 0.78F, 0.01F, 0.02F, 0.01F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.GRAIN, FoodTrait.STARCH, FoodTrait.RICE),
                IngredientId.RICE);
        register(profile(0.10F, 0.07F, 0.12F, 0.58F, 0.02F, 0.12F, 0.01F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.GRAIN, FoodTrait.STARCH),
                IngredientId.ROLLED_OATS,
                IngredientId.QUICK_OATS);
        register(profile(0.10F, 0.02F, 0.12F, 0.68F, 0.02F, 0.06F, 0.01F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.GRAIN, FoodTrait.STARCH, FoodTrait.PASTA, FoodTrait.WHEAT),
                IngredientId.PASTA);
        register(profile(0.10F, 0.06F, 0.14F, 0.56F, 0.02F, 0.10F, 0.01F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.GRAIN, FoodTrait.STARCH, FoodTrait.PLANT_PROTEIN),
                IngredientId.QUINOA);
        register(profile(0.08F, 0.04F, 0.10F, 0.62F, 0.03F, 0.10F, 0.02F, 0.02F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.BREAD, FoodTrait.STARCH, FoodTrait.WHEAT),
                IngredientId.BREADCRUMBS);

        register(profile(0.0F, 0.0F, 0.0F, 0.02F, 0.0F, 0.0F, 0.04F, 0.16F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.LEAVENER, FoodTrait.CONDIMENT),
                IngredientId.BAKING_SODA,
                IngredientId.BAKING_POWDER,
                IngredientId.CREAM_OF_TARTAR);
        register(profile(0.06F, 0.02F, 0.20F, 0.08F, 0.02F, 0.02F, 0.04F, 0.08F, 0.0F, 0.0F, 0.0F, 0.0F, 0.02F,
                        FoodTrait.LEAVENER, FoodTrait.FERMENTED),
                IngredientId.ACTIVE_DRY_YEAST,
                IngredientId.INSTANT_YEAST);
        register(profile(0.60F, 0.02F, 0.12F, 0.16F, 0.04F, 0.02F, 0.10F, 0.10F, 0.0F, 0.0F, 0.0F, 0.0F, 0.02F,
                        FoodTrait.LEAVENER, FoodTrait.FERMENTED),
                IngredientId.SOURDOUGH_STARTER,
                IngredientId.FERMENTATION_STARTER,
                IngredientId.VINEGAR_MOTHER);

        register(SALT,
                IngredientId.TABLE_SALT,
                IngredientId.KOSHER_SALT,
                IngredientId.SEA_SALT);
        register(BLACK_PEPPER, IngredientId.BLACK_PEPPER);
        register(profile(0.04F, 0.04F, 0.04F, 0.18F, 0.18F, 0.24F, 0.04F, 0.08F, 0.0F, 0.0F, 0.02F, 0.0F, 0.0F,
                        FoodTrait.SPICE, FoodTrait.CONDIMENT),
                IngredientId.CINNAMON,
                IngredientId.NUTMEG,
                IngredientId.CLOVES);
        register(profile(0.05F, 0.05F, 0.06F, 0.16F, 0.08F, 0.20F, 0.04F, 0.10F, 0.0F, 0.0F, 0.02F, 0.06F, 0.0F,
                        FoodTrait.SPICE, FoodTrait.CONDIMENT),
                IngredientId.PAPRIKA,
                IngredientId.CUMIN,
                IngredientId.TURMERIC,
                IngredientId.CHILI_POWDER,
                IngredientId.CURRY_POWDER);
        register(profile(0.04F, 0.02F, 0.06F, 0.20F, 0.04F, 0.22F, 0.03F, 0.10F, 0.0F, 0.14F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.SPICE, FoodTrait.ALLIUM, FoodTrait.AROMATIC, FoodTrait.CONDIMENT),
                IngredientId.GARLIC_POWDER,
                IngredientId.ONION_POWDER,
                IngredientId.DRIED_GARLIC,
                IngredientId.DRIED_ONIONS);
        register(profile(0.12F, 0.03F, 0.18F, 0.10F, 0.02F, 0.20F, 0.04F, 0.08F, 0.0F, 0.0F, 0.06F, 0.0F, 0.0F,
                        FoodTrait.HERB, FoodTrait.CAFFEINATED),
                IngredientId.MATCHA);
        register(profile(0.68F, 0.03F, 0.06F, 0.04F, 0.02F, 0.18F, 0.03F, 0.04F, 0.0F, 0.0F, 0.18F, 0.0F, 0.0F,
                        FoodTrait.HERB, FoodTrait.LEAFY_GREEN, FoodTrait.CONDIMENT),
                IngredientId.BASIL,
                IngredientId.THYME,
                IngredientId.ROSEMARY,
                IngredientId.PARSLEY,
                IngredientId.DILL,
                IngredientId.OREGANO,
                IngredientId.MINT,
                IngredientId.SAGE,
                IngredientId.ITALIAN_SEASONING);

        register(VEGETABLE_OIL,
                IngredientId.VEGETABLE_OIL,
                IngredientId.CANOLA_OIL);
        register(profile(0.01F, 0.98F, 0.0F, 0.0F, 0.0F, 0.0F, 0.02F, 0.02F, 0.0F, 0.0F, 0.04F, 0.0F, 0.0F,
                        FoodTrait.FAT, FoodTrait.OIL),
                IngredientId.OLIVE_OIL);
        register(profile(0.02F, 0.96F, 0.0F, 0.0F, 0.02F, 0.0F, 0.01F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.FAT, FoodTrait.OIL, FoodTrait.COCONUT),
                IngredientId.COCONUT_OIL);
        register(BUTTER, IngredientId.BUTTER);
        register(profile(0.02F, 0.98F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.FAT),
                IngredientId.SHORTENING,
                IngredientId.LARD);

        register(profile(0.06F, 0.26F, 0.26F, 0.26F, 0.06F, 0.0F, 0.02F, 0.06F, 0.04F, 0.0F, 0.0F, 0.0F, 0.02F,
                        FoodTrait.DAIRY, FoodTrait.PROTEIN),
                IngredientId.POWDERED_MILK);
        register(profile(0.82F, 0.14F, 0.10F, 0.02F, 0.02F, 0.0F, 0.02F, 0.04F, 0.02F, 0.0F, 0.0F, 0.0F, 0.02F,
                        FoodTrait.DAIRY),
                IngredientId.EVAPORATED_MILK,
                IngredientId.SHELF_STABLE_CREAM);
        register(profile(0.70F, 0.12F, 0.10F, 0.04F, 0.18F, 0.0F, 0.02F, 0.04F, 0.02F, 0.0F, 0.0F, 0.0F, 0.02F,
                        FoodTrait.DAIRY, FoodTrait.SWEETENER),
                IngredientId.SWEETENED_CONDENSED_MILK);
        register(profile(0.90F, 0.06F, 0.03F, 0.03F, 0.02F, 0.02F, 0.02F, 0.03F, 0.0F, 0.0F, 0.0F, 0.0F, 0.02F,
                        FoodTrait.DAIRY, FoodTrait.NUT),
                IngredientId.ALMOND_MILK);
        register(profile(0.90F, 0.04F, 0.03F, 0.06F, 0.02F, 0.03F, 0.02F, 0.03F, 0.0F, 0.0F, 0.0F, 0.0F, 0.02F,
                        FoodTrait.DAIRY, FoodTrait.GRAIN, FoodTrait.STARCH),
                IngredientId.OAT_MILK);
        register(profile(0.90F, 0.04F, 0.08F, 0.02F, 0.02F, 0.02F, 0.02F, 0.03F, 0.0F, 0.0F, 0.0F, 0.0F, 0.02F,
                        FoodTrait.DAIRY, FoodTrait.SOY, FoodTrait.PLANT_PROTEIN),
                IngredientId.SOY_MILK);
        register(CHEESE, IngredientId.CHEESE);
        register(profile(0.60F, 0.50F, 0.18F, 0.02F, 0.04F, 0.0F, 0.02F, 0.06F, 0.22F, 0.0F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.DAIRY, FoodTrait.PROTEIN, FoodTrait.ANIMAL_PROTEIN, FoodTrait.FAT),
                IngredientId.CREAM_CHEESE);
        register(profile(0.54F, 0.36F, 0.42F, 0.02F, 0.02F, 0.0F, 0.02F, 0.06F, 0.20F, 0.0F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.DAIRY, FoodTrait.PROTEIN, FoodTrait.ANIMAL_PROTEIN, FoodTrait.FAT),
                IngredientId.PANEER);

        register(profile(0.70F, 0.20F, 0.72F, 0.0F, 0.0F, 0.0F, 0.01F, 0.06F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.PROTEIN, FoodTrait.ANIMAL_PROTEIN, FoodTrait.CHICKEN),
                IngredientId.CHICKEN);
        register(profile(0.78F, 0.10F, 0.68F, 0.0F, 0.0F, 0.0F, 0.01F, 0.05F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.PROTEIN, FoodTrait.ANIMAL_PROTEIN, FoodTrait.FISH),
                IngredientId.FISH_FILLET);
        register(profile(0.64F, 0.30F, 0.68F, 0.0F, 0.0F, 0.0F, 0.01F, 0.06F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.PROTEIN, FoodTrait.ANIMAL_PROTEIN, FoodTrait.PORK),
                IngredientId.PORK);
        register(profile(0.62F, 0.24F, 0.74F, 0.0F, 0.0F, 0.0F, 0.01F, 0.06F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.PROTEIN, FoodTrait.ANIMAL_PROTEIN, FoodTrait.BEEF),
                IngredientId.BEEF);
        register(EGGS, IngredientId.EGGS);
        register(profile(0.82F, 0.08F, 0.48F, 0.04F, 0.02F, 0.04F, 0.02F, 0.04F, 0.0F, 0.0F, 0.0F, 0.0F, 0.04F,
                        FoodTrait.PROTEIN, FoodTrait.PLANT_PROTEIN, FoodTrait.SOY),
                IngredientId.TOFU);

        register(profile(0.84F, 0.02F, 0.02F, 0.08F, 0.10F, 0.06F, 0.08F, 0.02F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.FRUIT),
                IngredientId.APPLES);
        register(profile(0.94F, 0.02F, 0.04F, 0.02F, 0.04F, 0.04F, 0.14F, 0.03F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.FRUIT, FoodTrait.VEGETABLE, FoodTrait.TOMATO, FoodTrait.ACIDIC),
                IngredientId.TOMATOES);
        register(profile(0.88F, 0.02F, 0.02F, 0.10F, 0.06F, 0.08F, 0.03F, 0.02F, 0.0F, 0.0F, 0.04F, 0.0F, 0.0F,
                        FoodTrait.VEGETABLE),
                IngredientId.CARROTS);
        register(ONIONS, IngredientId.ONIONS);
        register(profile(0.78F, 0.01F, 0.04F, 0.20F, 0.02F, 0.04F, 0.02F, 0.02F, 0.0F, 0.0F, 0.02F, 0.0F, 0.0F,
                        FoodTrait.VEGETABLE, FoodTrait.STARCH),
                IngredientId.POTATOES);
        register(profile(0.88F, 0.02F, 0.02F, 0.04F, 0.04F, 0.04F, 0.28F, 0.02F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.FRUIT, FoodTrait.ACIDIC),
                IngredientId.LEMONS);
        register(profile(0.92F, 0.02F, 0.04F, 0.06F, 0.04F, 0.08F, 0.04F, 0.02F, 0.0F, 0.0F, 0.04F, 0.0F, 0.0F,
                        FoodTrait.VEGETABLE, FoodTrait.LEAFY_GREEN),
                IngredientId.CABBAGE,
                IngredientId.SPINACH);
        register(profile(0.62F, 0.02F, 0.08F, 0.20F, 0.02F, 0.08F, 0.04F, 0.06F, 0.0F, 0.18F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.VEGETABLE, FoodTrait.ALLIUM, FoodTrait.AROMATIC),
                IngredientId.GARLIC,
                IngredientId.SHALLOTS);
        register(profile(0.80F, 0.02F, 0.04F, 0.08F, 0.02F, 0.06F, 0.06F, 0.04F, 0.0F, 0.10F, 0.04F, 0.02F, 0.0F,
                        FoodTrait.VEGETABLE, FoodTrait.AROMATIC, FoodTrait.ACIDIC),
                IngredientId.GINGER);
        register(profile(0.76F, 0.02F, 0.20F, 0.10F, 0.04F, 0.10F, 0.03F, 0.03F, 0.0F, 0.0F, 0.02F, 0.0F, 0.0F,
                        FoodTrait.VEGETABLE, FoodTrait.LEGUME, FoodTrait.PLANT_PROTEIN),
                IngredientId.GREEN_PEAS);
        register(profile(0.92F, 0.02F, 0.04F, 0.02F, 0.04F, 0.06F, 0.08F, 0.04F, 0.0F, 0.0F, 0.04F, 0.10F, 0.0F,
                        FoodTrait.VEGETABLE, FoodTrait.PEPPER, FoodTrait.ACIDIC),
                IngredientId.JALAPENOS,
                IngredientId.RED_PEPPER);

        register(profile(0.90F, 0.02F, 0.04F, 0.04F, 0.06F, 0.04F, 0.14F, 0.04F, 0.0F, 0.0F, 0.02F, 0.0F, 0.0F,
                        FoodTrait.PRESERVE, FoodTrait.TOMATO, FoodTrait.SAUCE, FoodTrait.ACIDIC),
                IngredientId.CANNED_TOMATOES,
                IngredientId.TOMATO_PASTE);
        register(profile(0.72F, 0.02F, 0.26F, 0.18F, 0.02F, 0.14F, 0.04F, 0.06F, 0.0F, 0.0F, 0.02F, 0.0F, 0.0F,
                        FoodTrait.PRESERVE, FoodTrait.LEGUME, FoodTrait.PLANT_PROTEIN),
                IngredientId.BLACK_BEANS,
                IngredientId.KIDNEY_BEANS,
                IngredientId.CHICKPEAS);
        register(profile(0.94F, 0.02F, 0.08F, 0.02F, 0.0F, 0.0F, 0.04F, 0.10F, 0.0F, 0.04F, 0.02F, 0.0F, 0.0F,
                        FoodTrait.SAUCE, FoodTrait.CONDIMENT, FoodTrait.AROMATIC),
                IngredientId.BROTH,
                IngredientId.STOCK);
        register(profile(0.74F, 0.34F, 0.04F, 0.04F, 0.02F, 0.02F, 0.02F, 0.04F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.SAUCE, FoodTrait.COCONUT, FoodTrait.FAT),
                IngredientId.COCONUT_MILK);
        register(profile(0.10F, 0.52F, 0.28F, 0.12F, 0.08F, 0.10F, 0.02F, 0.08F, 0.0F, 0.0F, 0.0F, 0.0F, 0.02F,
                        FoodTrait.PRESERVE, FoodTrait.NUT, FoodTrait.PLANT_PROTEIN, FoodTrait.CONDIMENT),
                IngredientId.PEANUT_BUTTER);
        register(profile(0.36F, 0.0F, 0.0F, 0.02F, 0.54F, 0.02F, 0.12F, 0.04F, 0.0F, 0.0F, 0.04F, 0.0F, 0.0F,
                        FoodTrait.PRESERVE, FoodTrait.SWEETENER, FoodTrait.CONDIMENT),
                IngredientId.JAM,
                IngredientId.JELLY,
                IngredientId.SYRUP_PRESERVES);
        register(profile(0.78F, 0.0F, 0.02F, 0.02F, 0.04F, 0.06F, 0.24F, 0.08F, 0.0F, 0.06F, 0.02F, 0.0F, 0.0F,
                        FoodTrait.PRESERVE, FoodTrait.CONDIMENT, FoodTrait.ACIDIC, FoodTrait.FERMENTED, FoodTrait.VEGETABLE),
                IngredientId.PICKLES,
                IngredientId.BRINED_VEGETABLES);

        register(profile(0.02F, 0.28F, 0.06F, 0.12F, 0.48F, 0.04F, 0.02F, 0.02F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.CHOCOLATE, FoodTrait.SWEETENER),
                IngredientId.CHOCOLATE_CHIPS);
        register(profile(0.04F, 0.14F, 0.18F, 0.20F, 0.12F, 0.24F, 0.06F, 0.02F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.CHOCOLATE),
                IngredientId.COCOA_POWDER);
        register(profile(0.82F, 0.0F, 0.0F, 0.0F, 0.02F, 0.0F, 0.04F, 0.04F, 0.0F, 0.0F, 0.12F, 0.0F, 0.0F,
                        FoodTrait.CONDIMENT),
                IngredientId.VANILLA_EXTRACT,
                IngredientId.ROSE_WATER,
                IngredientId.FOOD_COLORING);
        register(profile(0.02F, 0.12F, 0.0F, 0.24F, 0.58F, 0.02F, 0.02F, 0.02F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.SWEETENER),
                IngredientId.SPRINKLES,
                IngredientId.CARAMEL_BITS);
        register(profile(0.06F, 0.54F, 0.20F, 0.12F, 0.04F, 0.12F, 0.02F, 0.04F, 0.0F, 0.0F, 0.02F, 0.0F, 0.0F,
                        FoodTrait.NUT, FoodTrait.PLANT_PROTEIN, FoodTrait.FAT),
                IngredientId.ALMONDS,
                IngredientId.WALNUTS,
                IngredientId.PECANS);
        register(profile(0.20F, 0.02F, 0.02F, 0.14F, 0.58F, 0.08F, 0.12F, 0.02F, 0.0F, 0.0F, 0.02F, 0.0F, 0.0F,
                        FoodTrait.FRUIT, FoodTrait.PRESERVE, FoodTrait.SWEETENER),
                IngredientId.RAISINS,
                IngredientId.DRIED_CRANBERRIES);

        register(profile(0.10F, 0.02F, 0.12F, 0.68F, 0.02F, 0.04F, 0.01F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.PASTA, FoodTrait.STARCH, FoodTrait.WHEAT),
                IngredientId.SPAGHETTI,
                IngredientId.MACARONI,
                IngredientId.RAMEN);
        register(profile(0.08F, 0.02F, 0.12F, 0.70F, 0.02F, 0.04F, 0.01F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.GRAIN, FoodTrait.STARCH),
                IngredientId.COUSCOUS);
        register(profile(0.10F, 0.02F, 0.24F, 0.48F, 0.02F, 0.12F, 0.03F, 0.02F, 0.0F, 0.0F, 0.02F, 0.0F, 0.0F,
                        FoodTrait.LEGUME, FoodTrait.PLANT_PROTEIN, FoodTrait.STARCH),
                IngredientId.LENTILS,
                IngredientId.DRY_BEANS);
        register(profile(0.34F, 0.04F, 0.10F, 0.48F, 0.04F, 0.08F, 0.03F, 0.02F, 0.0F, 0.0F, 0.02F, 0.0F, 0.0F,
                        FoodTrait.BREAD, FoodTrait.STARCH, FoodTrait.WHEAT),
                IngredientId.BREAD);

        register(profile(0.78F, 0.02F, 0.02F, 0.08F, 0.18F, 0.02F, 0.18F, 0.08F, 0.0F, 0.02F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.CONDIMENT, FoodTrait.SAUCE, FoodTrait.TOMATO, FoodTrait.ACIDIC),
                IngredientId.KETCHUP,
                IngredientId.TOMATO_SAUCE);
        register(profile(0.70F, 0.08F, 0.04F, 0.06F, 0.04F, 0.04F, 0.16F, 0.10F, 0.0F, 0.02F, 0.02F, 0.06F, 0.0F,
                        FoodTrait.CONDIMENT, FoodTrait.SAUCE, FoodTrait.ACIDIC, FoodTrait.PEPPER),
                IngredientId.MUSTARD,
                IngredientId.HOT_SAUCE,
                IngredientId.WORCESTERSHIRE_SAUCE);
        register(profile(0.58F, 0.62F, 0.08F, 0.04F, 0.02F, 0.0F, 0.10F, 0.08F, 0.04F, 0.0F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.CONDIMENT, FoodTrait.SAUCE, FoodTrait.FAT, FoodTrait.EGG),
                IngredientId.MAYONNAISE);
        register(profile(0.62F, 0.0F, 0.10F, 0.06F, 0.02F, 0.02F, 0.14F, 0.12F, 0.0F, 0.02F, 0.0F, 0.02F, 0.0F,
                        FoodTrait.CONDIMENT, FoodTrait.SAUCE, FoodTrait.SOY, FoodTrait.FERMENTED),
                IngredientId.SOY_SAUCE);
        register(profile(0.94F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.32F, 0.04F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.CONDIMENT, FoodTrait.ACIDIC, FoodTrait.FERMENTED),
                IngredientId.WHITE_VINEGAR,
                IngredientId.APPLE_CIDER_VINEGAR,
                IngredientId.BALSAMIC_VINEGAR);

        register(profile(0.06F, 0.08F, 0.10F, 0.66F, 0.06F, 0.06F, 0.02F, 0.04F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.STARCH, FoodTrait.WHEAT),
                IngredientId.CRACKERS);
        register(profile(0.08F, 0.22F, 0.08F, 0.42F, 0.28F, 0.02F, 0.02F, 0.02F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.BREAD, FoodTrait.SWEETENER, FoodTrait.WHEAT),
                IngredientId.COOKIES);
        register(profile(0.02F, 0.04F, 0.0F, 0.04F, 0.84F, 0.0F, 0.02F, 0.02F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                        FoodTrait.SWEETENER),
                IngredientId.CANDY);
        register(profile(0.04F, 0.30F, 0.08F, 0.48F, 0.02F, 0.04F, 0.02F, 0.04F, 0.0F, 0.0F, 0.02F, 0.0F, 0.0F,
                        FoodTrait.STARCH, FoodTrait.FAT),
                IngredientId.CHIPS);

        register(profile(0.08F, 0.04F, 0.12F, 0.68F, 0.08F, 0.04F, 0.02F, 0.08F, 0.0F, 0.0F, 0.02F, 0.0F, 0.0F,
                        FoodTrait.FLOUR, FoodTrait.STARCH, FoodTrait.WHEAT, FoodTrait.LEAVENER),
                IngredientId.PANCAKE_MIX,
                IngredientId.CAKE_MIX);

        verifyCoverage();
        initialized = true;
    }

    private static void register(FoodMaterialProfile profile, IngredientId... ingredientIds) {
        for (IngredientId ingredientId : ingredientIds) {
            ITEM_PROFILES.put(JazzyItems.ingredient(ingredientId).get(), profile);
        }
    }

    private static void verifyCoverage() {
        for (IngredientId ingredientId : IngredientId.values()) {
            if (!ITEM_PROFILES.containsKey(JazzyItems.ingredient(ingredientId).get())) {
                throw new IllegalStateException("Missing food material profile for ingredient " + ingredientId.id());
            }
        }
    }

    private static FoodMaterialProfile profile(
            float water,
            float fat,
            float protein,
            float starch,
            float sugar,
            float fiber,
            float acidity,
            float seasoningLoad,
            float cheeseLoad,
            float onionLoad,
            float herbLoad,
            float pepperLoad,
            float aeration,
            FoodTrait... traits
    ) {
        return new FoodMaterialProfile(
                water,
                fat,
                protein,
                starch,
                sugar,
                fiber,
                acidity,
                seasoningLoad,
                cheeseLoad,
                onionLoad,
                herbLoad,
                pepperLoad,
                aeration,
                FoodTrait.maskOf(traits)
        );
    }
}
