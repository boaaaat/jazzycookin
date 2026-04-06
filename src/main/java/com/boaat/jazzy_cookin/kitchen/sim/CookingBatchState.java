package com.boaat.jazzy_cookin.kitchen.sim;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record CookingBatchState(FoodMatterData matter) {
    public static final Codec<CookingBatchState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FoodMatterData.CODEC.fieldOf("matter").forGetter(CookingBatchState::matter)
    ).apply(instance, CookingBatchState::new));
}
