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

    public static final int MIN_OVEN_TEMPERATURE = 200;
    public static final int MAX_OVEN_TEMPERATURE = 500;
    public static final int OVEN_TEMPERATURE_STEP = 25;
    public static final int DEFAULT_OVEN_TEMPERATURE = 350;

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

    public static HeatLevel fromOvenTemperature(int temperature) {
        if (temperature <= 0) {
            return OFF;
        }
        if (temperature <= 275) {
            return LOW;
        }
        if (temperature <= 400) {
            return MEDIUM;
        }
        return HIGH;
    }

    public static int normalizeOvenTemperature(int temperature) {
        int clamped = Math.max(MIN_OVEN_TEMPERATURE, Math.min(MAX_OVEN_TEMPERATURE, temperature));
        int steps = Math.round((clamped - MIN_OVEN_TEMPERATURE) / (float) OVEN_TEMPERATURE_STEP);
        return MIN_OVEN_TEMPERATURE + steps * OVEN_TEMPERATURE_STEP;
    }

    public static int legacyOvenTemperature(HeatLevel heatLevel) {
        return switch (heatLevel) {
            case LOW -> 250;
            case HIGH -> 450;
            case OFF, MEDIUM -> DEFAULT_OVEN_TEMPERATURE;
        };
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }
}
