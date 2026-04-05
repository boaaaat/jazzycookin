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
    PANTRY("pantry", 0.90F),
    CELLAR("cellar", 0.35F);

    private static final Map<String, StorageType> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toMap(StorageType::getSerializedName, Function.identity()));

    public static final Codec<StorageType> CODEC = Codec.STRING.xmap(StorageType::byName, StorageType::getSerializedName);
    public static final StreamCodec<ByteBuf, StorageType> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(
            StorageType::byName,
            StorageType::getSerializedName
    );

    private final String serializedName;
    private final float decayMultiplier;

    StorageType(String serializedName, float decayMultiplier) {
        this.serializedName = serializedName;
        this.decayMultiplier = decayMultiplier;
    }

    public static StorageType byName(String name) {
        return BY_NAME.getOrDefault(name, PANTRY);
    }

    public float decayMultiplier() {
        return this.decayMultiplier;
    }

    public Component displayName() {
        return Component.translatable("storage.jazzycookin." + this.serializedName);
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }
}
