package com.boaat.jazzy_cookin.kitchen.sim;

import java.util.EnumSet;
import java.util.Set;

public enum FoodTrait {
    SWEETENER,
    SYRUP,
    FLOUR,
    GRAIN,
    STARCH,
    LEAVENER,
    SALT,
    SPICE,
    HERB,
    FAT,
    OIL,
    DAIRY,
    PROTEIN,
    ANIMAL_PROTEIN,
    PLANT_PROTEIN,
    CHICKEN,
    FISH,
    PORK,
    BEEF,
    EGG,
    SOY,
    FRUIT,
    VEGETABLE,
    ALLIUM,
    AROMATIC,
    LEAFY_GREEN,
    LEGUME,
    NUT,
    ACIDIC,
    PEPPER,
    TOMATO,
    BREAD,
    PASTA,
    CONDIMENT,
    SAUCE,
    PRESERVE,
    FERMENTED,
    CHOCOLATE,
    CAFFEINATED,
    COCONUT,
    RICE,
    WHEAT,
    CORN;

    public long mask() {
        return 1L << this.ordinal();
    }

    public static long maskOf(FoodTrait... traits) {
        long mask = 0L;
        for (FoodTrait trait : traits) {
            mask |= trait.mask();
        }
        return mask;
    }

    public static boolean has(long mask, FoodTrait trait) {
        return (mask & trait.mask()) != 0L;
    }

    public static Set<FoodTrait> unpack(long mask) {
        EnumSet<FoodTrait> traits = EnumSet.noneOf(FoodTrait.class);
        for (FoodTrait trait : values()) {
            if (has(mask, trait)) {
                traits.add(trait);
            }
        }
        return traits;
    }
}
