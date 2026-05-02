package com.boaat.jazzy_cookin.kitchen;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

public enum MeasureUnit implements StringRepresentable {
    COUNT("count"),
    SLICE("slice"),
    GRAM("g"),
    MILLILITER("ml");

    public static final Codec<MeasureUnit> CODEC = StringRepresentable.fromEnum(MeasureUnit::values);

    private final String serializedName;

    MeasureUnit(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }
}
