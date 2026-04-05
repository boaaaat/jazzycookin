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

public enum KitchenOutcomeBand implements StringRepresentable {
    UNDER("under"),
    IDEAL("ideal"),
    OVER("over");

    private static final Map<String, KitchenOutcomeBand> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toMap(KitchenOutcomeBand::getSerializedName, Function.identity()));

    public static final Codec<KitchenOutcomeBand> CODEC = Codec.STRING.xmap(KitchenOutcomeBand::byName, KitchenOutcomeBand::getSerializedName);
    public static final StreamCodec<ByteBuf, KitchenOutcomeBand> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(
            KitchenOutcomeBand::byName,
            KitchenOutcomeBand::getSerializedName
    );

    private final String serializedName;

    KitchenOutcomeBand(String serializedName) {
        this.serializedName = serializedName;
    }

    public static KitchenOutcomeBand byName(String name) {
        return BY_NAME.getOrDefault(name, IDEAL);
    }

    public static KitchenOutcomeBand fromControlIndex(int controlIndex) {
        return switch (controlIndex) {
            case 0 -> UNDER;
            case 2 -> OVER;
            default -> IDEAL;
        };
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }
}
