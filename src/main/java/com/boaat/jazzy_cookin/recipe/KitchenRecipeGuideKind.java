package com.boaat.jazzy_cookin.recipe;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum KitchenRecipeGuideKind implements StringRepresentable {
    KNOWN_DISH("known_dish"),
    TECHNIQUE("technique"),
    TUTORIAL("tutorial"),
    GUIDE("guide");

    private static final Map<String, KitchenRecipeGuideKind> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toMap(KitchenRecipeGuideKind::getSerializedName, Function.identity()));

    public static final Codec<KitchenRecipeGuideKind> CODEC = Codec.STRING.xmap(KitchenRecipeGuideKind::byName, KitchenRecipeGuideKind::getSerializedName);
    public static final StreamCodec<ByteBuf, KitchenRecipeGuideKind> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(
            KitchenRecipeGuideKind::byName,
            KitchenRecipeGuideKind::getSerializedName
    );

    private final String serializedName;

    KitchenRecipeGuideKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public static KitchenRecipeGuideKind byName(String name) {
        return BY_NAME.getOrDefault(name, KNOWN_DISH);
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }
}
