package com.boaat.jazzy_cookin.kitchen.sim.schema;

import java.util.function.Supplier;

import net.minecraft.world.item.Item;

public record DishSchemaScore(
        DishSchemaDefinition schema,
        Supplier<? extends Item> resultItem,
        float score,
        float roleScore,
        float compositionScore,
        float seasoningScore,
        float cookingScore,
        float textureScore,
        float techniqueScore,
        float presentationScore
) {
}
