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

public enum HeatLevel implements StringRepresentable {
    OFF("off"),
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high");

    private static final Map<String, HeatLevel> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toMap(HeatLevel::getSerializedName, Function.identity()));

    public static final Codec<HeatLevel> CODEC = Codec.STRING.xmap(HeatLevel::byName, HeatLevel::getSerializedName);
    public static final StreamCodec<ByteBuf, HeatLevel> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(
            HeatLevel::byName,
            HeatLevel::getSerializedName
    );

    private final String serializedName;

    HeatLevel(String serializedName) {
        this.serializedName = serializedName;
    }

    public static HeatLevel byName(String name) {
        return BY_NAME.getOrDefault(name, OFF);
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }
}
