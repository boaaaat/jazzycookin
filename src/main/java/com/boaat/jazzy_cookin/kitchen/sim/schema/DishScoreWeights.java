package com.boaat.jazzy_cookin.kitchen.sim.schema;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record DishScoreWeights(
        float roles,
        float composition,
        float seasoning,
        float cooking,
        float texture,
        float technique,
        float presentation
) {
    public static final DishScoreWeights DEFAULT = new DishScoreWeights(0.24F, 0.14F, 0.16F, 0.18F, 0.12F, 0.12F, 0.04F);

    public static final Codec<DishScoreWeights> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("roles", DEFAULT.roles).forGetter(DishScoreWeights::roles),
            Codec.FLOAT.optionalFieldOf("composition", DEFAULT.composition).forGetter(DishScoreWeights::composition),
            Codec.FLOAT.optionalFieldOf("seasoning", DEFAULT.seasoning).forGetter(DishScoreWeights::seasoning),
            Codec.FLOAT.optionalFieldOf("cooking", DEFAULT.cooking).forGetter(DishScoreWeights::cooking),
            Codec.FLOAT.optionalFieldOf("texture", DEFAULT.texture).forGetter(DishScoreWeights::texture),
            Codec.FLOAT.optionalFieldOf("technique", DEFAULT.technique).forGetter(DishScoreWeights::technique),
            Codec.FLOAT.optionalFieldOf("presentation", DEFAULT.presentation).forGetter(DishScoreWeights::presentation)
    ).apply(instance, DishScoreWeights::new));
}
