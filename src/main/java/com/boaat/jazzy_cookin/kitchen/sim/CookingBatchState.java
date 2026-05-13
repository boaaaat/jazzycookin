package com.boaat.jazzy_cookin.kitchen.sim;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record CookingBatchState(FoodMatterData matter, String schemaKey) {
    public CookingBatchState(FoodMatterData matter) {
        this(matter, "");
    }

    public CookingBatchState {
        schemaKey = schemaKey == null ? "" : schemaKey;
    }

    public CookingBatchState withMatter(FoodMatterData matter) {
        return new CookingBatchState(matter, this.schemaKey);
    }

    public static CookingBatchState preservingSchema(CookingBatchState previous, FoodMatterData matter) {
        return previous == null ? new CookingBatchState(matter) : previous.withMatter(matter);
    }

    public static final Codec<CookingBatchState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FoodMatterData.CODEC.fieldOf("matter").forGetter(CookingBatchState::matter),
            Codec.STRING.optionalFieldOf("schema_key", "").forGetter(CookingBatchState::schemaKey)
    ).apply(instance, CookingBatchState::new));
}
