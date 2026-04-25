package com.boaat.jazzy_cookin.kitchen.sim.schema;

import com.boaat.jazzy_cookin.kitchen.sim.FoodTrait;
import com.mojang.serialization.Codec;

final class DishCodecs {
    static final Codec<FoodTrait> FOOD_TRAIT = Codec.STRING.xmap(
            name -> FoodTrait.valueOf(name.toUpperCase(java.util.Locale.ROOT)),
            trait -> trait.name().toLowerCase(java.util.Locale.ROOT)
    );

    private DishCodecs() {
    }
}
