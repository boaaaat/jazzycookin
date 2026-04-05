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

public enum FreshnessBand implements StringRepresentable {
    FRESH("fresh"),
    AGING("aging"),
    STALE("stale"),
    SPOILED("spoiled"),
    MOLDY("moldy");

    private static final Map<String, FreshnessBand> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toMap(FreshnessBand::getSerializedName, Function.identity()));

    public static final Codec<FreshnessBand> CODEC = Codec.STRING.xmap(FreshnessBand::byName, FreshnessBand::getSerializedName);
    public static final StreamCodec<ByteBuf, FreshnessBand> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(
            FreshnessBand::byName,
            FreshnessBand::getSerializedName
    );

    private final String serializedName;

    FreshnessBand(String serializedName) {
        this.serializedName = serializedName;
    }

    public static FreshnessBand byName(String name) {
        return BY_NAME.getOrDefault(name, FRESH);
    }

    public static FreshnessBand fromScore(float score) {
        if (score >= 0.75F) {
            return FRESH;
        }
        if (score >= 0.45F) {
            return AGING;
        }
        if (score >= 0.2F) {
            return STALE;
        }
        if (score > 0.0F) {
            return SPOILED;
        }
        return MOLDY;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }
}
