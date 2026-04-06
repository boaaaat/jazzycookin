package com.boaat.jazzy_cookin.kitchen;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum StorageType implements StringRepresentable {
    PANTRY("pantry", 0.90F, 22.0F, 1.00F, 1.00F),
    FRIDGE("fridge", 0.25F, 4.0F, 0.28F, 0.55F),
    FREEZER("freezer", 0.05F, -18.0F, 0.04F, 0.20F);

    private static final Map<String, StorageType> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toMap(StorageType::getSerializedName, Function.identity()));

    public static final Codec<StorageType> CODEC = Codec.STRING.xmap(StorageType::byName, StorageType::getSerializedName);
    public static final StreamCodec<ByteBuf, StorageType> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(
            StorageType::byName,
            StorageType::getSerializedName
    );

    private final String serializedName;
    private final float decayMultiplier;
    private final float targetTempC;
    private final float microbialFactor;
    private final float oxidationFactor;

    StorageType(String serializedName, float decayMultiplier, float targetTempC, float microbialFactor, float oxidationFactor) {
        this.serializedName = serializedName;
        this.decayMultiplier = decayMultiplier;
        this.targetTempC = targetTempC;
        this.microbialFactor = microbialFactor;
        this.oxidationFactor = oxidationFactor;
    }

    public static StorageType byName(String name) {
        if ("cellar".equals(name)) {
            return FRIDGE;
        }
        return BY_NAME.getOrDefault(name, PANTRY);
    }

    public float decayMultiplier() {
        return this.decayMultiplier;
    }

    public float targetTempC() {
        return this.targetTempC;
    }

    public float microbialFactor() {
        return this.microbialFactor;
    }

    public float oxidationFactor() {
        return this.oxidationFactor;
    }

    public Component displayName() {
        return Component.translatable("storage.jazzycookin." + this.serializedName);
    }

    public String hintTranslationKey() {
        return "screen.jazzycookin." + this.serializedName + "_hint";
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }
}
