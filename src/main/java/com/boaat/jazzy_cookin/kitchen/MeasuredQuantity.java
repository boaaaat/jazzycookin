package com.boaat.jazzy_cookin.kitchen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

public record MeasuredQuantity(
        float amount,
        MeasureUnit unit,
        String displayLabel,
        ResourceLocation sourceItem,
        boolean measured
) {
    public static final Codec<MeasuredQuantity> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("amount").forGetter(MeasuredQuantity::amount),
            MeasureUnit.CODEC.fieldOf("unit").forGetter(MeasuredQuantity::unit),
            Codec.STRING.optionalFieldOf("display_label", "").forGetter(MeasuredQuantity::displayLabel),
            ResourceLocation.CODEC.fieldOf("source_item").forGetter(MeasuredQuantity::sourceItem),
            Codec.BOOL.optionalFieldOf("measured", false).forGetter(MeasuredQuantity::measured)
    ).apply(instance, MeasuredQuantity::new));

    public MeasuredQuantity normalized() {
        return new MeasuredQuantity(
                Math.max(0.0F, this.amount),
                this.unit,
                this.displayLabel == null ? "" : this.displayLabel,
                this.sourceItem,
                this.measured
        );
    }
}
