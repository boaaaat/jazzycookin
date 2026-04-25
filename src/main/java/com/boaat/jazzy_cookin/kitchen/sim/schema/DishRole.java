package com.boaat.jazzy_cookin.kitchen.sim.schema;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

public enum DishRole implements StringRepresentable {
    PROTEIN("protein"),
    GRAIN("grain"),
    FAT("fat"),
    AROMATIC("aromatic"),
    ACID("acid"),
    SWEETENER("sweetener"),
    HERB("herb"),
    SALT("salt"),
    SPICE("spice"),
    BINDER("binder"),
    LIQUID("liquid"),
    VEGETABLE("vegetable"),
    FRUIT("fruit"),
    DAIRY("dairy"),
    CONTAINER("container"),
    GARNISH("garnish");

    public static final Codec<DishRole> CODEC = StringRepresentable.fromEnum(DishRole::values);

    private final String serializedName;

    DishRole(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }
}
