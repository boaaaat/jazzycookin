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

public enum ProcessMode implements StringRepresentable {
    ACTIVE("active"),
    PASSIVE("passive");

    private static final Map<String, ProcessMode> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toMap(ProcessMode::getSerializedName, Function.identity()));

    public static final Codec<ProcessMode> CODEC = Codec.STRING.xmap(ProcessMode::byName, ProcessMode::getSerializedName);
    public static final StreamCodec<ByteBuf, ProcessMode> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(
            ProcessMode::byName,
            ProcessMode::getSerializedName
    );

    private final String serializedName;

    ProcessMode(String serializedName) {
        this.serializedName = serializedName;
    }

    public static ProcessMode byName(String name) {
        return BY_NAME.getOrDefault(name, ACTIVE);
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }
}
