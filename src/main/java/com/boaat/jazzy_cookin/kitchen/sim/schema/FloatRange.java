package com.boaat.jazzy_cookin.kitchen.sim.schema;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Mth;

public record FloatRange(float min, float max, float softness) {
    public static final Codec<FloatRange> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("min").forGetter(FloatRange::min),
            Codec.FLOAT.fieldOf("max").forGetter(FloatRange::max),
            Codec.FLOAT.optionalFieldOf("softness", 0.12F).forGetter(FloatRange::softness)
    ).apply(instance, FloatRange::new));

    public static FloatRange of(float min, float max, float softness) {
        return new FloatRange(min, max, softness);
    }

    public float score(float value) {
        if (value >= this.min && value <= this.max) {
            return 1.0F;
        }
        if (this.softness <= 0.0F) {
            return 0.0F;
        }
        float distance = value < this.min ? this.min - value : value - this.max;
        return Mth.clamp(1.0F - distance / this.softness, 0.0F, 1.0F);
    }
}
