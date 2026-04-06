package com.boaat.jazzy_cookin.kitchen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Mth;

public record SpoilageDisplayData(long updatedTick, float freshness) {
    public static final Codec<SpoilageDisplayData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("updated_tick").forGetter(SpoilageDisplayData::updatedTick),
            Codec.FLOAT.fieldOf("freshness").forGetter(SpoilageDisplayData::freshness)
    ).apply(instance, SpoilageDisplayData::new));

    public SpoilageDisplayData clamp() {
        return new SpoilageDisplayData(Math.max(0L, this.updatedTick), Mth.clamp(this.freshness, 0.0F, 1.0F));
    }
}
