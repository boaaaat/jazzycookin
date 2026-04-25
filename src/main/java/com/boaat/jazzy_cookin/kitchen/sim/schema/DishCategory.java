package com.boaat.jazzy_cookin.kitchen.sim.schema;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

public enum DishCategory implements StringRepresentable {
    EGG("egg"),
    SOUP("soup"),
    PAN_DISH("pan_dish"),
    BAKED("baked"),
    SAUCE("sauce"),
    PLATED("plated"),
    GENERIC("generic");

    public static final Codec<DishCategory> CODEC = StringRepresentable.fromEnum(DishCategory::values);

    private final String serializedName;

    DishCategory(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }
}
