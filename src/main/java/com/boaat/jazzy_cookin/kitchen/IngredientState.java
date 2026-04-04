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

public enum IngredientState implements StringRepresentable {
    WHOLE_APPLE("whole_apple"),
    PEELED_APPLE("peeled_apple"),
    SLICED_APPLE("sliced_apple"),
    WHOLE_SPICE("whole_spice"),
    GROUND_SPICE("ground_spice"),
    PANTRY_READY("pantry_ready"),
    CRUST_MIX("crust_mix"),
    DOUGH("dough"),
    SIMMERED_FILLING("simmered_filling"),
    RAW_ASSEMBLED_PIE("raw_assembled_pie"),
    BAKED_PIE("baked_pie"),
    COOLED_PIE("cooled_pie"),
    RESTED_PIE("rested_pie"),
    SLICED_PIE("sliced_pie"),
    PLATED_SLICE("plated_slice");

    private static final Map<String, IngredientState> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toMap(IngredientState::getSerializedName, Function.identity()));

    public static final Codec<IngredientState> CODEC = Codec.STRING.xmap(IngredientState::byName, IngredientState::getSerializedName);
    public static final StreamCodec<ByteBuf, IngredientState> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(
            IngredientState::byName,
            IngredientState::getSerializedName
    );

    private final String serializedName;

    IngredientState(String serializedName) {
        this.serializedName = serializedName;
    }

    public static IngredientState byName(String name) {
        return BY_NAME.getOrDefault(name, PANTRY_READY);
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }
}
