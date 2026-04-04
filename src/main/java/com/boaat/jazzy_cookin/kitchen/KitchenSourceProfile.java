package com.boaat.jazzy_cookin.kitchen;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum KitchenSourceProfile implements StringRepresentable {
    TOMATO_VINE("tomato_vine", true, 5, 4),
    HERB_BED("herb_bed", true, 4, 3),
    WHEAT_PATCH("wheat_patch", true, 5, 4),
    CABBAGE_PATCH("cabbage_patch", true, 5, 4),
    ONION_PATCH("onion_patch", true, 5, 4),
    CHICKEN_COOP("chicken_coop", false, 5, 4),
    DAIRY_STALL("dairy_stall", false, 5, 4),
    FISHING_TRAP("fishing_trap", false, 5, 4),
    FORAGE_SHRUB("forage_shrub", true, 4, 3);

    private static final Map<String, KitchenSourceProfile> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toMap(KitchenSourceProfile::getSerializedName, Function.identity()));

    public static final Codec<KitchenSourceProfile> CODEC = Codec.STRING.xmap(KitchenSourceProfile::byName, KitchenSourceProfile::getSerializedName);
    public static final StreamCodec<ByteBuf, KitchenSourceProfile> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(
            KitchenSourceProfile::byName,
            KitchenSourceProfile::getSerializedName
    );

    private final String serializedName;
    private final boolean plantLike;
    private final int maxAge;
    private final int ripeAge;

    KitchenSourceProfile(String serializedName, boolean plantLike, int maxAge, int ripeAge) {
        this.serializedName = serializedName;
        this.plantLike = plantLike;
        this.maxAge = maxAge;
        this.ripeAge = ripeAge;
    }

    public static KitchenSourceProfile byName(String name) {
        return BY_NAME.getOrDefault(name, TOMATO_VINE);
    }

    public boolean plantLike() {
        return this.plantLike;
    }

    public int maxAge() {
        return this.maxAge;
    }

    public int ripeAge() {
        return this.ripeAge;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }
}
